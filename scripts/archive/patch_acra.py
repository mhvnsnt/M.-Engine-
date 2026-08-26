with open("gradle/libs.versions.toml", "r") as f:
    content = f.read()

content = content.replace(
    '[versions]\n',
    '[versions]\nacra = "5.11.3"\n'
)

content = content.replace(
    '[libraries]\n',
    '[libraries]\nacra-mail = { group = "ch.acra", name = "acra-mail", version.ref = "acra" }\nacra-toast = { group = "ch.acra", name = "acra-toast", version.ref = "acra" }\n'
)

with open("gradle/libs.versions.toml", "w") as f:
    f.write(content)

with open("app/build.gradle.kts", "r") as f:
    app_content = f.read()

app_content = app_content.replace(
    'implementation(libs.androidx.core.ktx)',
    'implementation(libs.androidx.core.ktx)\n  implementation(libs.acra.mail)\n  implementation(libs.acra.toast)'
)

with open("app/build.gradle.kts", "w") as f:
    f.write(app_content)
