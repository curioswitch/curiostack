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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Sleeper;
import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.CreateClusterRequest;
import com.google.api.services.container.model.IPAllocationPolicy;
import com.google.api.services.container.model.NodeConfig;
import com.google.api.services.container.model.NodeManagement;
import com.google.api.services.container.model.NodePool;
import com.google.api.services.container.model.NodePoolAutoscaling;
import com.google.api.services.container.model.Operation;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.curioswitch.gradle.plugins.gcloud.GcloudExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class CreateClusterTask extends DefaultTask {

  public static final String NAME = "gcloudCreateCluster";

  private static final Duration MAX_WAIT_TIME = Duration.ofMinutes(10);

  private final Clock clock;
  private final Sleeper sleeper;

  @Inject
  public CreateClusterTask(Clock clock, Sleeper sleeper) {
    this.clock = clock;
    this.sleeper = sleeper;
  }

  @TaskAction
  void exec() throws Exception {
    var config = getProject().getExtensions().getByType(GcloudExtension.class);

    var request =
        new CreateClusterRequest()
            .setParent("projects/" + config.clusterProject() + "/locations/" + config.cloudRegion())
            .setCluster(
                new Cluster()
                    .setName(config.clusterName())
                    .setInitialClusterVersion(config.clusterKubernetesVersion())
                    .setLocations(config.clusterZones())
                    .setNodePools(
                        ImmutableList.of(
                            new NodePool()
                                .setName("default-pool")
                                .setConfig(
                                    new NodeConfig().setMachineType(config.clusterMachineType()))
                                .setInitialNodeCount(1)
                                .setAutoscaling(
                                    new NodePoolAutoscaling()
                                        .setEnabled(true)
                                        .setMaxNodeCount(3)
                                        .setMinNodeCount(1))
                                .setManagement(
                                    new NodeManagement().setAutoRepair(true).setAutoUpgrade(true))))
                    .setIpAllocationPolicy(
                        new IPAllocationPolicy().setUseIpAliases(true).setCreateSubnetwork(true)));

    var containerClient =
        new Container.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                GoogleCredential.getApplicationDefault())
            .setApplicationName("curiostack-plugin/0.0.144")
            .build();

    getLogger().lifecycle("Creating Kubernetes cluster.");
    Operation operation =
        containerClient
            .projects()
            .locations()
            .clusters()
            .create(request.getParent(), request)
            .execute();

    var now = clock.instant();
    var end = now.plus(MAX_WAIT_TIME);
    for (; now.isBefore(end); now = clock.instant()) {
      if (operation.getStatus().equals("DONE") || operation.getStatus().equals("ABORTING")) {
        break;
      }
      getLogger()
          .lifecycle(
              "Waiting for cluster creation to complete, current status: {}",
              operation.getStatus());
      sleeper.sleep(TimeUnit.SECONDS.toMillis(5));
      operation =
          containerClient
              .projects()
              .locations()
              .operations()
              .get(
                  "projects/"
                      + config.clusterProject()
                      + "/locations/"
                      + config.cloudRegion()
                      + "/operations/"
                      + operation.getName())
              .execute();
    }

    if (operation.getStatus().equals("DONE")) {
      getLogger().lifecycle("Cluster creation complete.");
    } else {
      throw new IllegalStateException("Could not create cluster: " + operation.getStatusMessage());
    }
  }
}
