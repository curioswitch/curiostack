# cloudbuild-github

cloudbuild-github allows integrating Google Container Builder (Cloudbuild) and GitHub. It sets up a
webhook and status / comment notifications using Google Cloud Functions.

Example usage at https://github.com/curioswitch/curiostack/tree/master/tools/cloudbuild-github-configured

Eventually this package will include a CLI to make generating `config.yml` and `index.js` easier,
but for now the appropriate usage is to follow the example to make the two files.

TODO(choko): Add a configuration CLI and more documentation.
