with open('app/src/main/java/com/example/data/LocationRepository.kt', 'r') as f:
    content = f.read()

content = content.replace("}", """
    suspend fun deleteAllRegions() {
        locationDao.deleteAllRegions()
    }

    suspend fun deleteSnapshots() {
        locationDao.deleteSnapshots()
    }
}""")
with open('app/src/main/java/com/example/data/LocationRepository.kt', 'w') as f:
    f.write(content)
