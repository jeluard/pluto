Launch figwheel with ./scripts/figwheel.sh then open index.html

### IPFS

You need to start a daemon locally, as the main gateway has CORS. The mobile app
does not have this limitation and can hit directly the gateway.

Before starting the dameon setup CORS:

```
ipfs config --json API.HTTPHeaders.Access-Control-Allow-Origin '["*"]'
ipfs config --json API.HTTPHeaders.Access-Control-Allow-Methods '["GET", "POST"]'
```

and then

```
ipfs daemon
```

To add a directory:

`ipfs add -r {dir-name}`

After that it will be available through localhost:8080 and the main gateway.
It will eventually disappear from the main gateway.
