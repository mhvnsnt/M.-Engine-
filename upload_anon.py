import subprocess
import json

def upload():
    result = subprocess.run(
        ["curl", "-F", "file=@app/build/outputs/apk/release/app-release.apk", "https://api.anonfiles.com/upload"],
        capture_output=True, text=True
    )
    try:
        data = json.loads(result.stdout)
        if data.get("status"):
            print(data["data"]["file"]["url"]["full"])
        else:
            print("Failed:", result.stdout)
    except:
        print("Error parsing output:", result.stdout)

upload()
