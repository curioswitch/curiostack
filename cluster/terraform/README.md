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

#### Note on terraform-provider-k8s

`terraform-provider-k8s` is deprecated. It was a very useful provider for allowing managing any
Kubernetes resource with Terraform, but has some tricky design issues due to spawning out to 
`kubectl`. There is a new provider that similarly supports any Kubernetes resource but without using
the CLI [here](https://github.com/mingfang/terraform-provider-k8s). It is very promising, but
unfortunately currently requires using a forked version of Terraform 0.12, which we won't be doing
here. Until then, it's recommended to use the normal `kubernetes` provider for as many resources as
you can as we wait for this new provider to work with vanilla Terraform.

### Upgrading to Terraform 0.12

Using Terraform 0.12 requires CurioStack 185 and the latest versions of providers as documented
above.

Terraform 0.12 is a major release with significant changes to the Terraform syntax language. It is
very unlikely that a 0.11 configuration will work with 0.12. As such, it will be several versions
before CurioStack defaults to using Terraform 0.12 to allow time for migration. Please go through
the below steps to migrate your configuration to 0.12.

First, BACKUP your Terraform state, most simply by downloading the state bucket(s). Once you apply
with 0.12, you will not be able to go back without restoring a backup.

Add `org.curioswitch.curiostack.tools.terraform = 0.12.2` to the top level `gradle.properties` file.
Then run 

```bash
./gradlew :toolsSetupTerraform
``` 

This will install Terraform 0.12 and set it up on your `PATH`.

For each terraform project, run `terraformPlan` and correct errors until you get output you expect.
Except for your Kubernetes deployments, you should have no changes to apply.

Take a look at https://github.com/curioswitch/curiostack/pull/376 to see the migration for CurioStack.
You'll probably just need to do the same things.

Some common migrations

- Referencing a field as `x.0.y.0` is now `x[0].y[0]`
- It was possible to assign maps without a `=`. Now, you will need to add `=`
- `depends_on` is reserved, so cannot be used for the module dependencies [workaround](https://medium.com/@bonya/terraform-adding-depends-on-to-your-custom-modules-453754a8043e).
Rename it to anything else.
- `google_container_cluster.ip_allocation_policy` needs to be fully specified with `null` for some reason

For projects that use the `kubernetes` provider, set the version to `1.7.1-choko`. The current `1.7.0`
has a longstanding critical bug https://github.com/terraform-providers/terraform-provider-kubernetes/issues/201.
`1.7.1-choko` is the same as `1.7.0` with this PR merged in. If you do not make this change, you will
have downtime as deployments are recreated instead of updated.

If you are using HCL for a `curio-server` deployment like this repository, you will find many whitespace
and similar no-op changes applied to the deployment causing it to be updated. It should be safe to
update resources for these changes. Do make sure the plan output shows the resource is _changed_, not
_add_ and _destroy_ which would incur downtime.

#### Note on YAML vs HCL

CurioStack has up to now encouraged writing Terraform configuration in YAML instead of the official
HCL. This is because it prevents having to learn yet-another-configuration-language and there was
little advantage to using HCL. With Terraform 0.12, that has changed as first-class expression
support make configurations in HCL far less noisy (e.g., you can use `field = var.foo` instead of
having to interpolate with `field = '${var.foo}'`).

On the flip side, many whitespace and default value changes when parsing the latest HCL cause differences
in the state and no-op updates to resources. It would always be better to not have such no-op updates
just when updating Terraform - it seems that JSON, which is the backend for our YAML support, has
a more stable syntax and therefore less chance of no-op updates.

Given these, CurioStack is switching to giving a slight recommendation towards using HCL, for its
cleaner syntax, but YAML is also a great choice for stability and the ability to automatically
generate them trivially. YAML support won't be dropped.

It is also unclear whether the new dynamic resource definitions in 0.12 provide enough power to model
all use cases. CurioStack will continue to use a conversion step to convert YAML to JSON and render
Jinja templates, which are known to provide lots of expressiveness. We feel this provides both the
power of templates, while also still the ability to issue raw commands without trouble since it is
always possible to run `:terraformInit` and `cd build/terraform` to run terraform commands in the
converted directory.
