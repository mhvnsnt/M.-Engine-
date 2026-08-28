with open("app/src/main/java/com/example/network/GitHubApiService.kt", "r") as f:
    code = f.read()

import re
code = re.sub(r'    @GET\("repos/\{owner\}/\{repo\}/issues/\{issue_number\}"\)', r'''    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadme(@Header("Authorization") auth: String?, @Path("owner") owner: String, @Path("repo") repo: String): GitHubReadmeDto

    @GET("repos/{owner}/{repo}/issues/{issue_number}")''', code)

code += "\n\ndata class GitHubReadmeDto(val name: String, val path: String, val content: String, val encoding: String)\n"

with open("app/src/main/java/com/example/network/GitHubApiService.kt", "w") as f:
    f.write(code)
