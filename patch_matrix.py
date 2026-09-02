import re

file_path = '/app/applet/M_ENGINE_COMPLETENESS_MATRIX.md'
with open(file_path, 'r') as f:
    content = f.read()

# Add a section for Remote Execution Fabric / Unreal Worker
# Find "Remote execution fabric"
def replace_row(content, search, new_row):
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if search in line:
            lines[i] = new_row
            break
    return '\n'.join(lines)

content = replace_row(content, "6 | Library / artifacts", "| 6 | Library / artifacts | **PARTIALLY_VERIFIED** | Physical artifact upload, hashing, and storage verified via tools/unreal-worker protocol test. Still missing canonical cross-surface UI |")
content = replace_row(content, "13 | Remote execution fabric", "| 13 | Remote execution fabric | **PARTIALLY_VERIFIED** | Control plane worker endpoints (`/enroll`, `/artifacts`) and Node.js Unreal worker implement physical transport. Unreal execution remains pending. |")

with open(file_path, 'w') as f:
    f.write(content)
