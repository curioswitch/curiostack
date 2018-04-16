/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.curioswitch.gradle.plugins.gcloud.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemGenerationException;
import org.bouncycastle.util.io.pem.PemObject;
import org.curioswitch.gradle.plugins.gcloud.ClusterExtension;
import org.curioswitch.gradle.plugins.gcloud.GcloudExtension;
import org.curioswitch.gradle.plugins.gcloud.ImmutableClusterExtension;
import org.curioswitch.gradle.plugins.gcloud.ImmutableGcloudExtension;
import org.curioswitch.gradle.plugins.shared.CommandUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class CreateClientCertTask extends DefaultTask {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  @TaskAction
  public void exec() {
    ImmutableClusterExtension cluster =
        getProject().getExtensions().getByType(ClusterExtension.class);

    String commonName = (String) getProject().getRootProject().findProperty("commonName");
    checkNotNull(commonName, "-PcommonName must be set");

    final KeyPairGenerator keygen;
    try {
      keygen = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
      throw new IllegalStateException("Could not find RSA, can't happen.", e);
    }

    keygen.initialize(256, new SecureRandom());
    KeyPair keyPair = keygen.generateKeyPair();

    PKCS10CertificationRequestBuilder p10Builder =
        new JcaPKCS10CertificationRequestBuilder(
            new X500Principal("CN=" + commonName), keyPair.getPublic());

    final ContentSigner signer;
    try {
      signer = new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate());
    } catch (OperatorCreationException e) {
      throw new IllegalStateException("Could not find signer, can't happen.", e);
    }
    PKCS10CertificationRequest csr = p10Builder.build(signer);
    StringWriter csrWriter = new StringWriter();
    try (JcaPEMWriter pemWriter = new JcaPEMWriter(csrWriter)) {
      pemWriter.writeObject(csr);
    } catch (IOException e) {
      throw new IllegalStateException("Could not encode csr, can't happen.", e);
    }
    String encodedCsr =
        Base64.getEncoder().encodeToString(csrWriter.toString().getBytes(StandardCharsets.UTF_8));

    String csrName = cluster.namespace() + "." + commonName + ".client.crt";

    Map<Object, Object> csrApiRequest =
        ImmutableMap.of(
            "apiVersion",
            "certificates.k8s.io/v1beta1",
            "kind",
            "CertificateSigningRequest",
            "metadata",
            ImmutableMap.of("name", csrName),
            "spec",
            ImmutableMap.of(
                "request",
                encodedCsr,
                "usages",
                ImmutableList.of(
                    "digital signature", "key encipherment", "server auth", "client auth")));

    final byte[] encodedApiRequest;
    try {
      encodedApiRequest = OBJECT_MAPPER.writeValueAsBytes(csrApiRequest);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not encode yaml", e);
    }

    ImmutableGcloudExtension config =
        getProject().getRootProject().getExtensions().getByType(GcloudExtension.class);

    String command =
        config.download()
            ? CommandUtil.getGcloudSdkBinDir(getProject())
                .resolve("kubectl")
                .toAbsolutePath()
                .toString()
            : "kubectl";
    getProject()
        .exec(
            exec -> {
              exec.executable(command);
              exec.args("create", "-f", "-");
              exec.setStandardInput(new ByteArrayInputStream(encodedApiRequest));
            });
    getProject()
        .exec(
            exec -> {
              exec.executable(command);
              exec.args("certificate", "approve", csrName);
            });

    // Need to wait a bit for certificate to propagate before fetching.
    try {
      TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    ByteArrayOutputStream certStream = new ByteArrayOutputStream();
    getProject()
        .exec(
            exec -> {
              exec.executable(command);
              exec.args("get", "csr", csrName, "-o", "jsonpath={.status.certificate}");
              exec.setStandardOutput(certStream);
            });
    String certificate =
        new String(Base64.getDecoder().decode(certStream.toByteArray()), StandardCharsets.UTF_8);

    final JcaPKCS8Generator keyGenerator;
    final PemObject keyObject;
    try {
      keyGenerator = new JcaPKCS8Generator(keyPair.getPrivate(), null);
      keyObject = keyGenerator.generate();
    } catch (PemGenerationException e) {
      throw new IllegalStateException("Could not encode to pkcs8.", e);
    }

    StringWriter keyWriter = new StringWriter();
    try (JcaPEMWriter pemWriter = new JcaPEMWriter(keyWriter)) {
      pemWriter.writeObject(keyObject);
    } catch (IOException e) {
      throw new IllegalStateException("Could not encode csr, can't happen.", e);
    }
    String key = keyWriter.toString();

    KubernetesClient client = new DefaultKubernetesClient();
    Secret certificateSecret =
        new SecretBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(commonName + "-client-tls")
                    .withNamespace(cluster.namespace())
                    .build())
            .withType("Opaque")
            .withData(
                ImmutableMap.of(
                    "client.crt",
                        Base64.getEncoder()
                            .encodeToString(certificate.getBytes(StandardCharsets.UTF_8)),
                    "client-key.pem",
                        Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8))))
            .build();
    client.resource(certificateSecret).createOrReplace();
  }
}
