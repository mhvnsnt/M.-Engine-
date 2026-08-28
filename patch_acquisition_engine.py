with open("app/src/main/java/com/example/ai/capabilities/AcquisitionEngine.kt", "r") as f:
    code = f.read()

import re
code = code.replace("BenchmarkComparisonResult", "BenchmarkComparison")
code = code.replace("val buildResult = sandboxManager.buildCapability(candidate)", """val sandboxId = sandboxManager.provisionSandbox("build-${candidate.id}", SandboxConfig(SandboxLimits(1024, 1.0f, 10), NetworkPolicy.ISOLATED, "ubuntu"))
            val buildResult = verificationEngine.build(targetRepo, sandboxId)""")

with open("app/src/main/java/com/example/ai/capabilities/AcquisitionEngine.kt", "w") as f:
    f.write(code)
