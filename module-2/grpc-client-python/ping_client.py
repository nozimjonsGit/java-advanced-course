"""Sends a ping to the gRPC server and prints the response."""

import argparse
import sys
from datetime import timezone

import grpc

import ping_pb2
import ping_pb2_grpc


def ping(channel: grpc.Channel, message: str, client: str, timeout: float) -> ping_pb2.PingResponse:
    """Sends one ping. Raises grpc.RpcError if the call fails or takes longer than timeout seconds."""
    stub = ping_pb2_grpc.PingServiceStub(channel)
    request = ping_pb2.PingRequest(message=message, client=client)
    return stub.Ping(request, timeout=timeout)


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", default="localhost:8080", help="server address (default: %(default)s)")
    parser.add_argument("--message", default="Ping", help="message to send (default: %(default)s)")
    parser.add_argument("--client", default="python-client", help="name the server logs (default: %(default)s)")
    parser.add_argument("--timeout", type=float, default=5.0, help="seconds to wait (default: %(default)s)")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    with grpc.insecure_channel(args.target) as channel:
        try:
            response = ping(channel, args.message, args.client, args.timeout)
        except grpc.RpcError as error:
            print(f"Ping to {args.target} failed: {error.code().name}: {error.details()}", file=sys.stderr)
            return 1

    server_time = response.server_time.ToDatetime(tzinfo=timezone.utc).isoformat()
    print(f"Received '{response.message}' (server time {server_time})")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
