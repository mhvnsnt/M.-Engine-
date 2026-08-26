import re

with open("app/src/main/java/com/example/network/RetrofitClient.kt", "r") as f:
    content = f.read()

target2 = """    val githubService: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApiService::class.java)
    }"""
new2 = """    val githubService: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApiService::class.java)
    }
    
    val telegramService: TelegramApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.telegram.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TelegramApiService::class.java)
    }"""
content = content.replace(target2, new2)

with open("app/src/main/java/com/example/network/RetrofitClient.kt", "w") as f:
    f.write(content)
