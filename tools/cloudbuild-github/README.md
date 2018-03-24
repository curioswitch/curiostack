# cloudbuild-github

cloudbuild-github allows integrating Google Container Builder (Cloudbuild) and GitHub. It sets up a
webhook and status / comment notifications using Google Cloud Functions.

## Setup

### Curiostack users

Create a new project with the `org.curioswitch.gradle-curio-cloudbuild-github-plugin` applied and
run the `:deploy` task, e.g.,

```bash
$ ./gradlew :tools:cloudbuild-github-functions:deploy
```

### Normal usage

The `gcloud` must be present on the path (see https://cloud.google.com/sdk/downloads) for how to 
install. In addition, the `GCLOUD_PROJECT` environment variable may need to be set to your GCP
project ID.

Start by creating a new project with a dependency on the library.

```bash
$ yarn init # main: index.tsx, private: true
$ yarn add @curiostack/cloudbuild-github
$ yarn
```

This will download the library, including a CLI to help setup. To create the configured cloud function
and deploy it, run

```bash
$ yarn run cloudbuild-cli setup
$ # Customize config.yml (e.g., replacing build step)
$ yarn run cloudbuild-cli deploy
```

This will prompt you for information about your repository, including an access token for working with
your GitHub repository (see details about tokens [here](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/)).
It will then write out an `index.tsx` and `config.yml` file that will be used by the webhook, deploy
the cloud functions, and set up the repository webhook. That's it.

If you update the configuration, just run deploy again.

```bash
$ yarn run cloudbuild-cli deploy
```

If you have trouble getting redeployed functions to update, add the `--delete` option when deploying.
This will delete the function before deploying, so there will be some downtime. Hopefully as cloud
functions matures, redeploys will work reliably and this option will go away.

```bash
$ yarn run cloudbuild-cli deploy --delete
```

## Details

This package includes two cloud functions, a GitHub webhook and a pubsub subscriber to cloudbuild events.
In response to an incoming webhook request, the first function will use the cloudbuild REST API to 
start a new build, which is configured to fetch the source from GitHub and then run a build command.
The subscriber function is notified of all build events, and appropriately sets the GitHub status and
comments on success or failure for change.

The webhook only supports pull requests, it does not implement support for repository push events.
It is trivial to set up a triggered cloudbuild within GCP itself and is recommended to do so. The
subscriber will properly run on events for triggered builds too.
