/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.curioswitch.gradle.plugins.gcloud.ClusterExtension;
import org.curioswitch.gradle.plugins.gcloud.GcloudExtension;
import org.curioswitch.gradle.plugins.gcloud.ImmutableClusterExtension;
import org.curioswitch.gradle.plugins.gcloud.ImmutableGcloudExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class CreateGcpNamespaceServiceAccountTask extends DefaultTask {

  private final Path keyFile;

  public CreateGcpNamespaceServiceAccountTask() {
    ImmutableGcloudExtension gcloud =
        getProject().getRootProject().getExtensions().getByType(GcloudExtension.class);
    ImmutableClusterExtension config =
        getProject().getExtensions().getByType(ClusterExtension.class);

    String serviceAccountName = config.serviceAccountName();

    GcloudTask createAccountTask =
        getProject()
            .getTasks()
            .create(
                "createServiceAccount",
                GcloudTask.class,
                t ->
                    t.setArgs(
                        ImmutableList.of(
                            "iam",
                            "service-accounts",
                            "create",
                            serviceAccountName,
                            "--display-name",
                            serviceAccountName)));

    try {
      keyFile = Files.createTempFile("gcp", ".json");
    } catch (IOException e) {
      throw new UncheckedIOException("Could not create temp file.", e);
    }
    GcloudTask createKeyTask =
        getProject()
            .getTasks()
            .create(
                "createServiceAccountKey",
                GcloudTask.class,
                t ->
                    t.setArgs(
                        ImmutableList.of(
                            "iam",
                            "service-accounts",
                            "keys",
                            "create",
                            keyFile.toString(),
                            "--iam-account",
                            serviceAccountName
                                + "@"
                                + gcloud.clusterProject()
                                + ".iam.gserviceaccount.com")));

    GcloudTask addIamPolicyBinding =
        getProject()
            .getTasks()
            .create(
                "addIamPolicyBinding",
                GcloudTask.class,
                t ->
                    t.setArgs(
                        ImmutableList.of(
                            "projects",
                            "add-iam-policy-binding",
                            gcloud.clusterProject(),
                            "--member",
                            "serviceAccount:"
                                + serviceAccountName
                                + "@"
                                + gcloud.clusterProject()
                                + ".iam.gserviceaccount.com",
                            "--role",
                            "projects/" + gcloud.clusterProject() + "/roles/ClusterNamespace")));

    createKeyTask.dependsOn(createAccountTask);
    addIamPolicyBinding.dependsOn(createAccountTask);
    dependsOn(createKeyTask, addIamPolicyBinding);
  }

  @TaskAction
  public void exec() {
    ImmutableClusterExtension config =
        getProject().getExtensions().getByType(ClusterExtension.class);
    KubernetesClient client = new DefaultKubernetesClient();
    final Secret tokenSecret;
    try {
      tokenSecret =
          new SecretBuilder()
              .withMetadata(
                  new ObjectMetaBuilder()
                      .withName("gcloud")
                      .withNamespace(config.namespace())
                      .build())
              .withType("Opaque")
              .withData(
                  ImmutableMap.of(
                      "service-account.json",
                      Base64.getEncoder().encodeToString(Files.readAllBytes(keyFile))))
              .build();
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read key json.", e);
    }
    client.resource(tokenSecret).createOrReplace();
  }
}
