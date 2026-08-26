import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

target = """secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}"""
replacement = """secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("Mengine_Github_PAT")
}"""

content = content.replace(target, replacement)

# Add buildConfigField
target2 = """    buildConfigField("String", "GEMINI_API_KEY", "\\\"\\\"")"""
replacement2 = """    buildConfigField("String", "GEMINI_API_KEY", "\\\"\\\"")
    buildConfigField("String", "Mengine_Github_PAT", "\\\"${System.getenv(\\\"Mengine_Github_PAT\\\") ?: \\\"\\\"}\\\"")"""

if "buildConfigField" not in content:
    target2 = """  defaultConfig {"""
    replacement2 = """  defaultConfig {
    buildConfigField("String", "Mengine_Github_PAT", "\\\"${System.getenv(\\\"Mengine_Github_PAT\\\") ?: \\\"\\\"}\\\"")"""
    content = content.replace(target2, replacement2)
else:
    content = content.replace(target2, replacement2)

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
