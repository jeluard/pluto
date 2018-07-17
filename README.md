
[![CircleCI](https://img.shields.io/circleci/project/github/status-im/pluto.svg)](https://circleci.com/gh/status-im/pluto/tree/master)

## Development

Development requires [Deps and CLI](https://clojure.org/guides/getting_started) tooling installed.

Run Clojure tests using `clojure -A:test-clj`
Run ClojureScript tests using `clojure -A:test-cljs`

Run figwheel using `clojure -A:figwheel`.

## Documentation

Documentation website is maintained using [docusaurus](https://docusaurus.io). The layout logic can be found in `website` while the content is kept in `docs`.

To hack on the documentation, run `npm run start` in `website` folder.

### IPFS

To upload a directory to IPFS:

```
ipfs daemon
```

To add a directory:

`ipfs add -r {dir-name}`

After that it will be available through localhost:8080 and the main gateway.
It will eventually disappear from the main gateway.

## Continous Deployment

To make possible deploying the new version of the page from the `master` branch a Jenkins job is configured at:

https://jenkins.status.im/job/misc/job/pluto/

This job is ran according to the [`Jenkinsfile`](Jenkinsfile) configuration.
In order for this to work there are two requirements:

* The `status-im-auto` user which API token is used needs to have at least write access.
* The repo has to have the https://jenkins.status.im/github-webhook/ webhook configured.
