import re

file_path = '/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/PostgresLedgerRepository.kt'
with open(file_path, 'r') as f:
    content = f.read()

content = content.replace("        return null\n    }\n\n    override fun createJob", "    override fun createJob")

with open(file_path, 'w') as f:
    f.write(content)
