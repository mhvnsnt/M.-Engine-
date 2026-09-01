import os

with open('app/src/main/python/native_worker.py', 'r') as f:
    worker_script = f.read()

# Let's save it to a location that's easy to access for the user
with open('federated_worker.py', 'w') as f:
    f.write(worker_script)
