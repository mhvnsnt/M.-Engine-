import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = re.sub(r'\s*implementation\("ch.acra:acra-mail:5.11.3"\)', '', content)
content = re.sub(r'\s*implementation\("ch.acra:acra-toast:5.11.3"\)', '', content)
content = re.sub(r'\s*implementation\("ch.acra:acra-core:5.11.3"\)', '', content)

with open("app/build.gradle.kts", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/MengineApplication.kt", "w") as f:
    f.write("""package com.example

import android.app.Application
import android.content.Context

class MengineApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }
}
""")
