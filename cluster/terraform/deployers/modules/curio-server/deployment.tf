# Due to https://github.com/hashicorp/terraform/issues/18330 it is not possible to generate
# a JSON for a deployment that will parse, so we go ahead and use HCL...

resource kubernetes_deployment deployment {
  depends_on = ["kubernetes_config_map.rpcacls"]
  lifecycle {
    ignore_changes = ["metadata[0].labels.revision", "spec[0].template[0].metadata[0].labels.revision"]
  }
  metadata {
    labels = {
      name = "${var.name}"
    }
    name = "${var.name}"
    namespace = "${var.namespace}"
  }
  spec {
    replicas = "${var.replicas}"
    selector {
      match_labels = {
        name = "${var.name}"
      }
    }
    strategy {
      rolling_update {
        max_unavailable = 0
      }
      type = "RollingUpdate"
    }
    template {
      metadata {
        annotations = {
          "prometheus.io/path" = "/internal/metrics"
          "prometheus.io/port" = "8080"
          "prometheus.io/scheme" = "https"
          "prometheus.io/scrape" = "true"
        }
        labels = {
          name = "${var.name}"
        }
        namespace = ""
      }
      spec {
        container {
          image = "${var.gcr_url}/${var.project_id}/${var.image_name}:${var.image_tag}"
          image_pull_policy = "Always"
          name = "${var.name}"
          env {
            name = "GOOGLE_APPLICATION_CREDENTIALS"
            value = "/etc/gcloud/service-account.json"
          }
          env {
            name = "JAVA_TOOL_OPTIONS"
            value = <<ARGS
              -Dserver.additionalCaCertificatePath=/etc/internal-tls/ca.crt
              --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
              --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED
              -Xms${ceil(var.request_memory_mb * 0.5)}m
              -Xmx${ceil(var.request_memory_mb * 0.5)}m
              -Dconfig.resource=application-${var.type}.conf
              -Dmonitoring.stackdriverProjectId=${var.project_id}
              -Dmonitoring.serverName=${var.name}
              -Dlog4j2.ContextDataInjector=org.curioswitch.common.server.framework.logging.RequestLoggingContextInjector
              -Dlog4j.configurationFile=log4j2-json.yml
              -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
              -Dserver.caCertificatePath=/etc/internal-tls/ca.crt
              -Dserver.tlsCertificatePath=/etc/internal-tls/server.crt
              -Dserver.tlsPrivateKeyPath=/etc/internal-tls/server-key.pem
              ${var.type != "prod" ? "-Dcom.linecorp.armeria.verboseExceptions=true" : ""}
              ${var.extra_jvm_args}
            ARGS
          }

          dynamic "env_from" {
            for_each = var.environment_secrets
            content {
              secret_ref {
                name = env_from.value
              }
            }
          }

          port {
            container_port = 8080
            name = "http"
          }
          liveness_probe {
            failure_threshold = 3
            http_get {
              path = "/internal/health"
              port = 8080
              scheme = "HTTPS"
            }
            initial_delay_seconds = 30
            period_seconds = 15
            timeout_seconds = 5
          }
          readiness_probe {
            failure_threshold = 3
            http_get {
              path = "/internal/health"
              port = 8080
              scheme = "HTTPS"
            }
            initial_delay_seconds = 30
            period_seconds = 5
            timeout_seconds = 5
          }
          resources {
            requests {
              cpu = "${var.request_cpu}"
              memory = "${var.request_memory_mb}Mi"
            }
          }
          volume_mount {
            mount_path = "/etc/internal-tls"
            name = "internal-tls"
            read_only = true
          }
          volume_mount {
            mount_path = "/etc/gcloud"
            name = "gcloud"
            read_only = true
          }
          volume_mount {
            mount_path = "/etc/rpcacls"
            name = "rpcacls"
            read_only = true
          }
        }
        volume {
          name = "internal-tls"
          secret {
            secret_name = "internal-tls"
          }
        }
        volume {
          name = "gcloud"
          secret {
            secret_name = "gcloud"
          }
        }
        volume {
          name = "rpcacls"
          config_map {
            name = "rpcacls-${var.name}"
          }
        }

        affinity {
          pod_anti_affinity {
            preferred_during_scheduling_ignored_during_execution {
              pod_affinity_term {
                label_selector {
                  match_expressions {
                    key = "name"
                    operator = "In"
                    values = [ "${var.name}" ]
                  }
                }
                topology_key = "failure-domain.beta.kubernetes.io/zone"
              }
              weight = 99
            }
            preferred_during_scheduling_ignored_during_execution {
              pod_affinity_term {
                label_selector {
                  match_expressions {
                    key = "name"
                    operator = "In"
                    values = [ "${var.name}" ]
                  }
                }
                topology_key = "kubernetes.io/hostname"
              }
              weight = 1
            }
          }
        }
      }
    }
  }
}
