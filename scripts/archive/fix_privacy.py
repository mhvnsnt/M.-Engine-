with open("app/src/main/java/com/example/ui/PrivacyScreen.kt", "r") as f:
    content = f.read()

content = content.replace("Icons.AutoMirrored.Filled.ArrowBack", "Icons.Default.ArrowBack")

with open("app/src/main/java/com/example/ui/PrivacyScreen.kt", "w") as f:
    f.write(content)
