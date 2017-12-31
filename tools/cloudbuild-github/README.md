# cloudbuild-github

cloudbuild-github allows integrating Google Container Builder (Cloudbuild) and GitHub. It sets up a
webhook and status / comment notifications using Google Cloud Functions.

## Setup

### Curiostack users

Coming soon: A gradle task that does all above the below automatically.

### Normal usage

The `gcloud` must be present on the path (see https://cloud.google.com/sdk/downloads) for how to 
install. In addition, the `GCLOUD_PROJECT` environment variable may need to be set to your GCP
project id.

Start by creating a new project with a dependency on the library.

```bash
$ yarn init # main: index.js, private: true
$ yarn add @curiostack/cloudbuild-github
$ yarn
```

This will download the library, including a CLI to help setup. To create the configured cloud function
and deploy it, run

```bash
$ yarn run cloudbuild-cli
```

This will prompt you for information about your repository, including an access token for working with
your GitHub repository (see details about tokens [here](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/)).
It will then write out an `index.js` and `config.yml` file that will be used by the webhook, deploy
the cloud functions, and set up the repository webhook. That's it!

The current version does not support an easy flow for editing configuration yet (coming soon). In the
meantime, especially for modifying the build step to match your build system, edit `config.yml` and
redeploy manually using

```bash
$ yarn run build
$ gcloud beta functions deploy cloudbuildGithubWebhook --trigger-http
$ gcloud beta functions deploy cloudbuildGithubNotifier --trigger-topic cloud-builds
```

Note, I have been unsuccessful in having new code be recognized when redeploying (bug in cloud functions?).
To work around this, you may need to delete the functions before redeploying - this will introduce
downtime between the delete and deploy.

```bash
$ gcloud beta functions delete cloudbuildGithubWebhook
$ gcloud beta functions delete cloudbuildGithubNotifier
```
