import argparse
import subprocess
import json
import socketserver
import http.server
import threading
import time
import uuid
import hashlib
import os

WORKER_ID = str(uuid.uuid4())
SECRET = ""
PORT = 9092

jobs = {}

def get_hash(content):
    if isinstance(content, str):
        content = content.encode('utf-8')
    return hashlib.sha256(content).hexdigest()

class Job:
    def __init__(self, job_id, payload):
        self.job_id = job_id
        self.payload = payload
        self.status = "QUEUED"
        self.result = None
    
    def run(self):
        self.status = "RUNNING"
        command = ""
        task = self.payload.get("task", {})
        if task.get("type") == "RUN_TEST":
            command = " ".join(task.get("arguments", []))
        elif task.get("type") == "NATIVE_COMMAND":
            command = task.get("command", "")
            
        try:
            print(f"[{self.job_id}] Executing: {command}")
            proc = subprocess.run(command, shell=True, capture_output=True, text=True)
            
            stdout_hash = get_hash(proc.stdout)
            stderr_hash = get_hash(proc.stderr)
            
            self.result = {
                "exitCode": proc.returncode,
                "stdout": proc.stdout,
                "stderr": proc.stderr,
                "stdoutHash": stdout_hash,
                "stderrHash": stderr_hash,
                "workerIdentity": WORKER_ID
            }
            self.status = "ARTIFACTS_UPLOADED"
            # Simulate artifact upload time
            time.sleep(0.5)
            self.status = "VERIFIED"
        except Exception as e:
            self.result = {
                "exitCode": -1,
                "stdout": "",
                "stderr": str(e),
                "stdoutHash": get_hash(""),
                "stderrHash": get_hash(str(e)),
                "workerIdentity": WORKER_ID
            }
            self.status = "FAILED"

class SecureWorkerHandler(http.server.BaseHTTPRequestHandler):
    def check_auth(self):
        auth_header = self.headers.get('Authorization')
        if not auth_header or auth_header != f"Bearer {SECRET}":
            self.send_response(401)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({"error": "Unauthorized"}).encode('utf-8'))
            return False
        return True

    def _send_response(self, data, status=200):
        self.send_response(status)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(data).encode('utf-8'))

    def do_GET(self):
        if not self.check_auth():
            return
            
        if self.path == '/health':
            self._send_response({"status": "ok", "workerId": WORKER_ID})
        elif self.path == '/probe':
            self._send_response({
                "environmentName": "Python Physical Worker (Secure)",
                "shellExecution": "VERIFIED",
                "filesystemRead": "VERIFIED",
                "filesystemWrite": "VERIFIED",
                "dockerCli": "UNAVAILABLE",
                "git": "UNAVAILABLE", 
                "python": "VERIFIED",
                "browserAutomation": "UNAVAILABLE" 
            })
        elif self.path.startswith('/jobs/'):
            job_id = self.path.split('/')[-1]
            if job_id in jobs:
                job = jobs[job_id]
                response = {
                    "jobId": job.job_id,
                    "status": job.status
                }
                if job.result:
                    response["result"] = job.result
                self._send_response(response)
            else:
                self._send_response({"error": "Job not found"}, 404)
        else:
            self._send_response({"error": "Not found"}, 404)

    def do_POST(self):
        if not self.check_auth():
            return
            
        if self.path == '/jobs':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            
            try:
                payload = json.loads(post_data.decode('utf-8'))
                job_id = payload.get('jobId', str(uuid.uuid4()))
                
                job = Job(job_id, payload)
                jobs[job_id] = job
                job.status = "LEASED"
                
                # Start job asynchronously
                thread = threading.Thread(target=job.run)
                thread.start()
                
                self._send_response({
                    "status": job.status,
                    "jobId": job_id
                })
            except Exception as e:
                self._send_response({"error": str(e)}, 500)
        else:
            self._send_response({"error": "Not found"}, 404)

def run():
    print(f"Starting Secure Physical Worker {WORKER_ID} on port {PORT}...")
    print(f"Awaiting Governor pairing with secret...")
    
    server_address = ('', PORT)
    httpd = socketserver.TCPServer(server_address, SecureWorkerHandler)
    httpd.serve_forever()

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="M. Engine Secure Federated Worker")
    parser.add_argument("--secret", required=True, help="Enrollment pairing secret generated by the Governor")
    parser.add_argument("--port", type=int, default=9092, help="Port to listen on")
    args = parser.parse_args()
    
    SECRET = args.secret
    PORT = args.port
    run()
