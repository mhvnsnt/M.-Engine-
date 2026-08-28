with open("app/src/main/java/com/example/ai/capabilities/connections/FirebaseConnectionProvider.kt", "r") as f:
    code = f.read()

code = code.replace("FirebaseApp.getInstance() != null", "try { FirebaseApp.getInstance(); true } catch (e: Exception) { false }")

with open("app/src/main/java/com/example/ai/capabilities/connections/FirebaseConnectionProvider.kt", "w") as f:
    f.write(code)
