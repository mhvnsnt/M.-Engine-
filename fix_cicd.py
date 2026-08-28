import re

with open("app/src/main/java/com/example/ai/capabilities/CiCdPipeline.kt", "r") as f:
    lines = f.readlines()

with open("app/src/main/java/com/example/ai/capabilities/CiCdPipeline.kt", "w") as f:
    for line in lines:
        if "val apkFile = generateApk(repoDir) ?: return CiCdResult(" in line:
            f.write('        val apkFile = generateApk(repoDir) ?: return CiCdResult(CiCdState.FAILED, "APK generation failed", null)\n')
        else:
            f.write(line)
