import os
import re
import json

capabilities = []

def analyze_file(filepath):
    with open(filepath, 'r') as f:
        try:
            content = f.read()
        except:
            return
            
    filename = os.path.basename(filepath)
    if "AppDatabase.kt" in filename:
        capabilities.append("Local Database (Room)")
    if "RetrofitClient" in filename:
        capabilities.append("Networking (Retrofit)")
    if "UniversalRealityLoop.kt" in content:
        capabilities.append("Universal Reality Loop")
    if "interface MissionEngine" in content:
        capabilities.append("Mission Engine")
    if "EvidenceAssuranceEngine" in content:
        capabilities.append("Evidence Engine")
    if "PersonalContextEngine" in content:
        capabilities.append("Personal Context Engine")
    if "RemoteSandbox" in content:
        capabilities.append("Sandbox Manager")
    if "GitHubApiService" in content:
        capabilities.append("GitHub API")
    if "Ollama" in content:
        capabilities.append("Local LLM (Ollama)")
    if "OpenRouter" in content:
        capabilities.append("Cloud LLM (OpenRouter)")
    if "FirebaseManager" in content:
        capabilities.append("Firebase Integration")
    if "PhysicalActuators" in content:
        capabilities.append("Physical Actuators")
    if "AcquisitionEngine" in content:
        capabilities.append("Acquisition Engine")

for root, _, files in os.walk('app/src/main/java'):
    for file in files:
        if file.endswith('.kt'):
            analyze_file(os.path.join(root, file))

capabilities = list(set(capabilities))
print("Found Capabilities:")
for c in capabilities:
    print(f"- {c}")

