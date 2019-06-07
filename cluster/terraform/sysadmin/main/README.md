# sysadmin terraform (infrastructure & secrets)

## Setting up a cluster for the first time

Tiller must be set up before any other resources. To do this, run `terraformApply` twice, like this

```bash
$ ./gradlew :cluster:terraform:sysadmin:terraformApply --target kubernetes_service.tiller-service --target tls_locally_signed_cert.tiller-client-cert
$ ./gradlew :cluster:terraform:sysadmin:terraformApply
```
