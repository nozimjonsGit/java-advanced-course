"""Tests the client against a fake server on a free port."""

import contextlib
import io
import unittest
from concurrent import futures

import grpc

import ping_client
import ping_pb2
import ping_pb2_grpc


class FakePingService(ping_pb2_grpc.PingServiceServicer):
    """Answers every request with Pong, or rejects it when reject is set."""

    def __init__(self, reject: bool = False):
        self.requests = []
        self.reject = reject

    def Ping(self, request, context):
        self.requests.append(request)
        if self.reject:
            context.abort(grpc.StatusCode.INVALID_ARGUMENT, "bad message")
        response = ping_pb2.PingResponse(message="Pong")
        response.server_time.FromSeconds(1_700_000_000)
        return response


class PingClientTest(unittest.TestCase):

    def start_server(self, service: FakePingService) -> str:
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
        ping_pb2_grpc.add_PingServiceServicer_to_server(service, server)
        port = server.add_insecure_port("localhost:0")
        server.start()
        self.addCleanup(server.stop, None)
        return f"localhost:{port}"

    def test_sends_message_and_client_name(self):
        service = FakePingService()
        target = self.start_server(service)

        with grpc.insecure_channel(target) as channel:
            response = ping_client.ping(channel, "Ping", "test-client", timeout=5)

        self.assertEqual("Pong", response.message)
        self.assertEqual([ping_pb2.PingRequest(message="Ping", client="test-client")], service.requests)

    def test_main_prints_response(self):
        target = self.start_server(FakePingService())
        output = io.StringIO()

        with contextlib.redirect_stdout(output):
            exit_code = ping_client.main(["--target", target])

        self.assertEqual(0, exit_code)
        self.assertEqual("Received 'Pong' (server time 2023-11-14T22:13:20+00:00)\n", output.getvalue())

    def test_main_reports_error_from_server(self):
        target = self.start_server(FakePingService(reject=True))
        errors = io.StringIO()

        with contextlib.redirect_stderr(errors):
            exit_code = ping_client.main(["--target", target, "--message", "Hello"])

        self.assertEqual(1, exit_code)
        self.assertIn("INVALID_ARGUMENT: bad message", errors.getvalue())


if __name__ == "__main__":
    unittest.main()
