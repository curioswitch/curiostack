# Terraform

## Common and uncommon operations

### Splitting up a configuration

It is possible to split up a large terraform configuration into multiple ones backed by 
individual states. This is useful to speed up terraform operations for teams that only
work with a subset of resources.

Splitting up state without preparation can completely destroy your infrastructure.
BE CAREFUL. That being said, as long as you backup your state file while doing the
migration, you should always be able to restore the file even if you make a mistake so
don't be scared of doing this - your team will thank you with faster terraform applies.

When beginning this operation, announce to all your teams to prevent terraform operations
throughout its duration.

To split up state, first create a new terraform project to host the split-out config.
Configure it first to have a local state file using this `backend.tf.yaml`

```yaml
terraform:
  backend:
    local:
      path: ../../default.tfstate
```

This will reference `default.tfstate` in the project folder. Copy in all the resource configurations
you want to split out.

Then, download the state file for your current terraform configuration from cloud storage.
It will be at the bucket and prefix listed in `backend.tf.yaml` of the project. While we
can split the configuration out of the remote state directly, if someone happens to work
on terraform during that time things will go very wrong and this minimizes that risk
(of course, you already should have announced no one should do this but it may still
happen accidentally). For this document, we assume you downloaded the state file for
the project `sysadmin` into the folder `cluster/sysadmin`.

Then, move the resources you want to split from the old state file to the new state file,
for example.

```bash
$ terraform state mv -state=cluster/terraform/sysadmin/main/default.tfstate -state-out=cluster/terraform/sysadmin/tls/default.tfstate tls_private_key.cluster-ca-key tls_private_key.cluster-ca-key
```

This moves the resource named `tls_private_key.cluster-ca-key` to the state for the `tls` project,
keeping the same name.

Repeat for all resources you want to move out. This is tedious but should be done with care.
To prevent mistakes, it is strongly recommended to copy-paste the resource name in your
terminal for the second reference. It is also useful to run `terraformPlan` in the new project
after `mv` to see what resources are left to move.

After moving all the desired resources, run `terraformPlan` on the new project. If there are
no changes, you moved the resources correctly. Change the backend in `backend.tf.yaml` from the
local path to the remote `gcs` backend, similar to the old project but with `prefix` set to a unique
value for this project. The easiest way to migrate the state to this remote backend is to manually
upload it - go to the GCP console and open the storage viewer for your terraform bucket. Create a
folder with the same value as the value of `prefix` and upload `default.tfstate` to this folder.
Run `terraformPlan` again - it should indicate it is initializing the `gcs` backend and it should
again have no changes. If there are changes, you did something wrong and may need to start over from
the beginning. 

Now, go through configs in the split-out configuration and add outputs for any variables you
want to reference from the old configuration. 

Use the [`remote_state`](https://www.terraform.io/docs/providers/terraform/d/remote_state.html)
data type to reference the new state in the old configuration. It's generally fine to have
a `remote_states.tf.yaml` file at the top level to contain all these values. After that, remove the
configurations for the moved resources from the old project and change references to use this remote
state as documented there.

After migrating your references, run `terraformPlan` in the old project and make sure the only
resources scheduled for destruction are the ones that were split out. Now, remove these from the old
project's state. If it is safe to destroy the resources (e.g., they do not correspond to GCP
infrastructure), the easiest way is to just run `terraformApply`, but otherwise, please remove the
resources manually.

```bash
$ ./gradlew :cluster:terraform:sysadmin:main:terraformConvertConfigs
$ cd cluster/terraform/sysadmin/main/build/terraform
$ terraform rm resource.name
```

After removing all the migrated resources, run `terraformPlan` again - there should be no changes to
made. You're done! Clean out all the `default.tfstate` files left on your computer and merge and
others can finally get back to using terraform.

### Upgrade providers

#### Upgrading to google 2.x

`terraform-provider-google` 2.x splits out a separate provider named `google-beta` with access to
GCP beta APIs. If you happen to be using beta APIs, see if it's safe to remove them, or otherwise
just add `provider: google-beta` to the resource.

#### Upgrading to kubernetes 1.7+

`terraform-provider-kubernetes` 1.7+ have some backwards incompatible changes for deployments and
ingresses.

Deployments must specify `spec.selector.match_labels.*` instead of `spec.selector.*`.
Deployments have a bad default for `spec.template.metadata.namespace` which will force all your
deployments to be recreating, causing downtime. Stick to the correct behavior by setting it to `""`
explicitly.

Ingresses use `path` instead of `path_regex`.

### Upgrading to Terraform 0.12

Terraform 0.12 is a major release with significant changes to the Terraform syntax language. It is
very unlikely that a 0.11 configuration will work with 0.12. As such, it will be several versions
before CurioStack defaults to using Terraform 0.12 to allow time for migration. Please go through
the below steps to migrate your configuration to 0.12.

Add `org.curioswitch.curiostack.tools.terraform = 0.12.2` to the top level `gradle.properties` file.
Then run 

```bash
./gradlew :toolsSetupTerraform
``` 

This will install Terraform 0.12 and set it up on your `PATH`.

For each terraform project, do the following:

Convert the configs from YAML to JSON with e.g., 

```bash
./gradlew :cluster:terraform:sysadmin:main:terraformConvertConfigs
```
