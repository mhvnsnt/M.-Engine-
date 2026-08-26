import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

dep = "    implementation(\"androidx.work:work-runtime-ktx:2.9.0\")"
if dep not in content:
    content = content.replace("dependencies {", "dependencies {\n" + dep)

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
