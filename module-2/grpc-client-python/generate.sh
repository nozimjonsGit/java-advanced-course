#!/usr/bin/env bash
# Generates the Python stubs from the proto file the Java server and client use.
# Run it from any folder, with the virtual environment's Python on the PATH.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
proto_dir="$here/../grpc-api/src/main/protobuf"

python -m grpc_tools.protoc \
    --proto_path="$proto_dir" \
    --python_out="$here" \
    --pyi_out="$here" \
    --grpc_python_out="$here" \
    "$proto_dir/ping.proto"

echo "Generated stubs from $proto_dir/ping.proto"
