import re

with open('app/src/main/java/com/example/data/MemoryFragmentDao.kt', 'r') as f:
    content = f.read()

content = content.replace("}", """
    @Query("DELETE FROM memory_fragments WHERE type = :type")
    suspend fun deleteFragmentsByType(type: String)

    @Query("DELETE FROM memory_fragments")
    suspend fun deleteAllFragments()
}""")

with open('app/src/main/java/com/example/data/MemoryFragmentDao.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/data/LocationDao.kt', 'r') as f:
    content = f.read()

content = content.replace("}", """
    @Query("DELETE FROM region_profiles")
    suspend fun deleteAllRegions()

    @Query("DELETE FROM region_profiles WHERE regionId = :regionId")
    suspend fun deleteRegion(regionId: String)

    @Query("DELETE FROM location_snapshots")
    suspend fun deleteSnapshots()
}""")

with open('app/src/main/java/com/example/data/LocationDao.kt', 'w') as f:
    f.write(content)
