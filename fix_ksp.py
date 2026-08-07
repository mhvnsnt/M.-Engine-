with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

source_sets = """
  sourceSets {
    getByName("debug") {
      java.srcDirs("build/generated/ksp/debug/kotlin")
    }
    getByName("release") {
      java.srcDirs("build/generated/ksp/release/kotlin")
    }
  }
"""

if "sourceSets {" not in content:
    content = content.replace('compileOptions {', source_sets + '\n  compileOptions {')
    with open('app/build.gradle.kts', 'w') as f:
        f.write(content)
