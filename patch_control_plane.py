import re

file_path = '/app/applet/cloud_control_plane/src/main/kotlin/com/example/ai/cloud/ControlPlaneServer.kt'
with open(file_path, 'r') as f:
    content = f.read()

endpoints = """
            // Real Worker Protocol Endpoints
            post("/api/v1/worker/enroll") {
                val body = try { call.receive<Map<String, Any>>() } catch (e: Exception) { emptyMap<String, Any>() }
                val workerId = body["workerId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing workerId"))
                val caps = body["capabilities"] as? Map<String, Any> ?: emptyMap()
                val os = caps["os"] as? String ?: "unknown"
                val unrealVersion = caps["unrealVersion"] as? String ?: "unknown"
                val repository = caps["repository"] as? String ?: "unknown"
                val currentBranch = caps["currentBranch"] as? String ?: "unknown"
                val currentCommit = caps["currentCommit"] as? String ?: "unknown"
                
                val result = ledger.enrollWorker(workerId, os, unrealVersion, repository, currentBranch, currentCommit)
                call.respond(result)
            }

            post("/api/v1/worker/heartbeat") {
                val body = try { call.receive<Map<String, Any>>() } catch (e: Exception) { emptyMap<String, Any>() }
                val workerId = body["workerId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing workerId"))
                val state = body["state"] as? String ?: "UNKNOWN"
                
                val result = ledger.heartbeatWorker(workerId, state)
                call.respond(result)
            }

            post("/api/v1/worker/jobs/lease") {
                val body = try { call.receive<Map<String, Any>>() } catch (e: Exception) { emptyMap<String, Any>() }
                val workerId = body["workerId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing workerId"))
                
                val job = ledger.leaseJob(workerId)
                if (job != null) {
                    call.respond(job)
                } else {
                    call.respond(HttpStatusCode.NoContent, mapOf("message" to "No jobs available"))
                }
            }

            post("/api/v1/worker/jobs/{jobId}/complete") {
                val jobId = call.parameters["jobId"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing jobId"))
                val body = try { call.receive<Map<String, Any>>() } catch (e: Exception) { emptyMap<String, Any>() }
                
                val exitStatus = (body["exitStatus"] as? Number)?.toInt() ?: 1
                val evidenceLevel = body["evidenceLevel"] as? String ?: "NONE"
                val stdout = body["stdout"] as? String ?: ""
                val stderr = body["stderr"] as? String ?: ""
                
                val success = ledger.completeJob(jobId, exitStatus, evidenceLevel, stdout, stderr)
                call.respond(mapOf("success" to success))
            }

            post("/api/v1/worker/artifacts") {
                // In a real implementation this would parse multipart/form-data for the file stream.
                // For architecture/protocol completeness, we assume a JSON payload describing the artifact and a mock upload, or base64.
                // To support a real test, let's accept a JSON payload with file content as base64, save it, and register it.
                val body = try { call.receive<Map<String, Any>>() } catch (e: Exception) { emptyMap<String, Any>() }
                
                val jobId = body["jobId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing jobId"))
                val workerId = body["workerId"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing workerId"))
                val sha256 = body["sha256"] as? String ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing sha256"))
                val size = (body["size"] as? Number)?.toLong() ?: 0L
                val path = body["path"] as? String ?: "unknown"
                val contentBase64 = body["contentBase64"] as? String
                
                // Store file physically in the library
                val artifactDir = java.io.File("/app/applet/library/artifacts")
                artifactDir.mkdirs()
                val artifactFile = java.io.File(artifactDir, sha256)
                
                if (contentBase64 != null) {
                    val decoded = java.util.Base64.getDecoder().decode(contentBase64)
                    artifactFile.writeBytes(decoded)
                } else {
                    artifactFile.writeText("empty artifact or multipart used in real environment")
                }
                
                val uri = "file://${artifactFile.absolutePath}"
                
                val result = ledger.registerArtifact(jobId, workerId, sha256, size, path, uri)
                call.respond(result)
            }
"""

content = content.replace('// Governance Controls', endpoints + '\n            // Governance Controls')

with open(file_path, 'w') as f:
    f.write(content)
