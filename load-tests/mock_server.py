#!/usr/bin/env python3
"""Local-only HTTP stub used to validate the k6 script in CI."""
from http.server import BaseHTTPRequestHandler, HTTPServer


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"ok")
            return

        if not self.path.startswith("/api/v1/"):
            self.send_response(404)
            self.end_headers()
            return

        center_id = self.headers.get("X-Center-Id")
        authorization = self.headers.get("Authorization")
        if authorization != "Bearer ci-load-test-token":
            self.send_response(401)
        elif center_id == "202" and self.path == "/api/v1/students":
            self.send_response(401)
        elif center_id == "101":
            self.send_response(200)
        else:
            self.send_response(403)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(b"[]")

    def log_message(self, _format, *_args):
        return


if __name__ == "__main__":
    HTTPServer(("0.0.0.0", 8088), Handler).serve_forever()
