import re

with open("app/src/main/java/com/example/ai/CodingTools.kt", "r") as f:
    code = f.read()

# Add checkoutBranch
checkout_code = """
    suspend fun checkoutBranch(pat: String, owner: String, repo: String, branch: String): Boolean = withContext(Dispatchers.IO) {
        val repoDir = getRepoDir(owner, repo)
        if (!repoDir.exists()) cloneOrPull(pat, owner, repo)
        try {
            val git = Git.open(repoDir)
            val createBranch = git.branchList().call().none { it.name.endsWith(branch) }
            if (createBranch) {
                git.checkout().setCreateBranch(true).setName(branch).call()
            } else {
                git.checkout().setName(branch).call()
            }
            true
        } catch (e: Exception) {
            Log.e("CodingTools", "Checkout error", e)
            false
        }
    }

    suspend fun fetchAndPull(pat: String, owner: String, repo: String): Boolean = withContext(Dispatchers.IO) {
        val repoDir = getRepoDir(owner, repo)
        if (!repoDir.exists()) {
            cloneOrPull(pat, owner, repo)
            return@withContext true
        }
        val credentials = UsernamePasswordCredentialsProvider(pat, "")
        try {
            val git = Git.open(repoDir)
            git.fetch().setCredentialsProvider(credentials).call()
            git.pull().setCredentialsProvider(credentials).call()
            true
        } catch (e: Exception) {
            Log.e("CodingTools", "Fetch error", e)
            false
        }
    }
    
    suspend fun getCommitSha(owner: String, repo: String): String = withContext(Dispatchers.IO) {
        val repoDir = getRepoDir(owner, repo)
        try {
            val git = Git.open(repoDir)
            val head = git.repository.resolve("HEAD")
            head.name
        } catch (e: Exception) {
            "unknown"
        }
    }
"""

# Insert right before commitAndPush
code = code.replace("    suspend fun commitAndPush(", checkout_code + "\n    suspend fun commitAndPush(")

with open("app/src/main/java/com/example/ai/CodingTools.kt", "w") as f:
    f.write(code)

