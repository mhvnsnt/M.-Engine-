import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

content = content.replace('// implementation(libs.coil.compose)', 'implementation(libs.coil.compose)')

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
