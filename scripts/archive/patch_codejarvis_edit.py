import re

with open("app/src/main/java/com/example/ai/CodeJarvis.kt", "r") as f:
    content = f.read()

# Add edit tool logic
edit_logic = """
            } else if (responseText.contains("TOOL: edit_file")) {
                val path = responseText.substringAfter("PATH:").substringBefore("SEARCH:").trim()
                val searchStr = responseText.substringAfter("SEARCH:").substringBefore("REPLACE:").trim()
                val replaceStr = responseText.substringAfter("REPLACE:").substringBefore("TOOL:").trim()
                
                val currentContent = codingTools.readFile(githubPat, owner, repo, branch, path)
                if (currentContent != null) {
                    if (currentContent.contains(searchStr)) {
                        val newContent = currentContent.replace(searchStr, replaceStr)
                        val success = codingTools.commitAndPush(
                            pat = githubPat,
                            owner = owner,
                            repo = repo,
                            branch = branch,
                            message = "Auto-commit: CodeJarvis edited $path",
                            files = mapOf(path to newContent)
                        )
                        return@withContext if (success) {
                            "Successfully edited and pushed $path"
                        } else {
                            "Edited file but failed to push to GitHub."
                        }
                    } else {
                        return@withContext "Failed to edit $path: Search string not found in the file."
                    }
                } else {
                    return@withContext "Failed to read $path for editing."
                }
            } else if (responseText.contains("TOOL: commit")) {"""

content = content.replace("            } else if (responseText.contains(\"TOOL: commit\")) {", edit_logic)

with open("app/src/main/java/com/example/ai/CodeJarvis.kt", "w") as f:
    f.write(content)
