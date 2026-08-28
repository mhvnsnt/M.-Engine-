with open("app/src/main/java/com/example/ai/capabilities/ResearchEngineImpl.kt", "r") as f:
    code = f.read()

import re
code = code.replace("integrationComplexity = 5,", """recencyScore = 80,
            adoptionScore = 75,
            maintenanceScore = 85,
            dependencyHealth = "GOOD",
            integrationComplexity = 5,""")

with open("app/src/main/java/com/example/ai/capabilities/ResearchEngineImpl.kt", "w") as f:
    f.write(code)
