with open('app/src/main/java/com/example/data/LocationRepository.kt', 'r') as f:
    content = f.read()

target = """                locationDao.insertRegionProfile(region)
                return@withContext region
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}"""
replacement = """                locationDao.insertRegionProfile(region)
                return@withContext region
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun deleteAllRegions() {
        locationDao.deleteAllRegions()
    }

    suspend fun deleteSnapshots() {
        locationDao.deleteSnapshots()
    }
}"""
content = content.replace(target, replacement)
with open('app/src/main/java/com/example/data/LocationRepository.kt', 'w') as f:
    f.write(content)
