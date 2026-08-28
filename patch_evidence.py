with open("app/src/main/java/com/example/ai/capabilities/EvidenceEngine.kt", "r") as f:
    lines = f.readlines()

lines = [line for line in lines if not ("interface EvidenceAssuranceEngine {}" in line or "class EvidenceAssuranceEngineImpl : EvidenceAssuranceEngine {}" in line)]

with open("app/src/main/java/com/example/ai/capabilities/EvidenceEngine.kt", "w") as f:
    f.writelines(lines)
