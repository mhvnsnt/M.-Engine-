import retrofit2.Retrofit
import retrofit2.http.GET
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody

interface TestApi {
    @GET("status/500")
    suspend fun get500(): ResponseBody
}

fun main() = runBlocking {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://httpbin.org/")
        .build()
    val api = retrofit.create(TestApi::class.java)
    try {
        api.get500()
        println("Success")
    } catch (e: Exception) {
        println("Caught: ${e::class.java.name}")
    }
}
