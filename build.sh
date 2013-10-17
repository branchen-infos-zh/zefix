#!/bin/sh

# The most simple build script ever!
# Requires clojure and lein to run.

echo "Building zefidx..."
(cd zefidx && lein uberjar)

echo ""
echo "Building zefxmx..."
(cd zefxmx && lein uberjar)
