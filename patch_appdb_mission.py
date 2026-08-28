import re

with open("app/src/main/java/com/example/data/AppDatabase.kt", "r") as f:
    code = f.read()

# Add MissionEntity to the array of entities
if "MissionEntity::class" not in code:
    code = code.replace("JobEntity::class]", "JobEntity::class, MissionEntity::class]")
    # Increment version
    code = re.sub(r'version = \d+', 'version = 9', code)
    
    # Add DAO
    code = code.replace("abstract fun jobDao(): JobDao", "abstract fun jobDao(): JobDao\n    abstract fun missionDao(): MissionDao")

with open("app/src/main/java/com/example/data/AppDatabase.kt", "w") as f:
    f.write(code)
