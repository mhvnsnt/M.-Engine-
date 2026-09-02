import json
import os

uproject_path = "/workspace/Bannon/unreal/Bannon.uproject"
if not os.path.exists(uproject_path):
    print("uproject not found!")
    exit(1)

with open(uproject_path, "r") as f:
    data = json.load(f)

modules = data.get("Modules", [])
has_engine = any(m.get("Name") == "BannonEngine" for m in modules)

if not has_engine:
    modules.append({
        "Name": "BannonEngine",
        "Type": "Runtime",
        "LoadingPhase": "Default",
        "AdditionalDependencies": [
            "Engine",
            "BannonCore"
        ]
    })
    data["Modules"] = modules
    with open(uproject_path, "w") as f:
        json.dump(data, f, indent=4)
    print("Added BannonEngine to uproject.")
else:
    print("BannonEngine already in uproject.")
