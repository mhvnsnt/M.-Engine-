import re

with open("app/src/main/java/com/example/network/RetrofitClient.kt", "r") as f:
    content = f.read()

target1 = "    private const val OPENROUTER_BASE_URL = \"https://openrouter.ai/api/v1/\""
new1 = """    private const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1/"
    private const val TELEGRAM_BASE_URL = "https://api.telegram.org/"
"""
content = content.replace(target1, new1)

target2 = """    val githubService: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApiService::class.java)
    }"""
new2 = """    val githubService: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApiService::class.java)
    }
    
    val telegramService: TelegramApiService by lazy {
        Retrofit.Builder()
            .baseUrl(TELEGRAM_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TelegramApiService::class.java)
    }"""
content = content.replace(target2, new2)

with open("app/src/main/java/com/example/network/RetrofitClient.kt", "w") as f:
    f.write(content)
