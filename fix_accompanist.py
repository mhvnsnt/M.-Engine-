import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

content = content.replace('// implementation(libs.accompanist.permissions)', 'implementation(libs.accompanist.permissions)')

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
