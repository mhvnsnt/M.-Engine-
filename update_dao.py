import re

with open("app/src/main/java/com/example/data/EndpointDao.kt", "r") as f:
    content = f.read()

content = content.replace("fun getAllEndpoints(): Flow<List<EndpointEntity>>", "fun getAllEndpoints(): Flow<List<EndpointEntity>>\n    @Query(\"SELECT * FROM endpoints\")\n    suspend fun getAllEndpointsSync(): List<EndpointEntity>")

with open("app/src/main/java/com/example/data/EndpointDao.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/data/ChatRepository.kt", "r") as f:
    content = f.read()

content = content.replace("suspend fun getActiveEndpoints() = endpointDao.getActiveEndpoints()", "suspend fun getActiveEndpoints() = endpointDao.getActiveEndpoints()\n    suspend fun getAllEndpointsSync() = endpointDao.getAllEndpointsSync()")

with open("app/src/main/java/com/example/data/ChatRepository.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("val allEndpoints = repository.getAllEndpoints()", "val allEndpoints = repository.getAllEndpointsSync()")

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
