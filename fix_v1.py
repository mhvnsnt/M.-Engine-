with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

old_signing = """    create("release") {
      val keystorePath = "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = "android"
      keyAlias = "upload"
      keyPassword = "android"
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
      enableV4Signing = true
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
