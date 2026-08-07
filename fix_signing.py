with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

old_signing = """    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }"""
new_signing = """    create("release") {
      val keystorePath = "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = "android"
      keyAlias = "upload"
      keyPassword = "android"
    }"""

content = content.replace(old_signing, new_signing)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
