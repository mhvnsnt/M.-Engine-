import re

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'r') as f:
    content = f.read()

content = content.replace("SessionEntity::class]", "SessionEntity::class, UserConstraints::class, LocationSnapshot::class, RegionProfile::class, AstroProfile::class]")

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'w') as f:
    f.write(content)
