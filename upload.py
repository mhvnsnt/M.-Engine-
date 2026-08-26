import http.client
import mimetypes
import os

filename = "app/build/outputs/apk/release/app-release.apk"
url = "bashupload.com"
conn = http.client.HTTPSConnection(url)
with open(filename, 'rb') as f:
    conn.request("POST", "/", f, headers={"Content-Type": "application/octet-stream", "Content-Length": str(os.path.getsize(filename))})
    res = conn.getresponse()
    print(res.read().decode())
