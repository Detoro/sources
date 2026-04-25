package toro.sources

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory


const val url: String = "http://192.168.1.141:8000/"

object RetrofitClient {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(ScalarsConverterFactory.create()).build()
    }

    val apiInterface: ApiInterface by lazy {
        retrofit.create(ApiInterface::class.java)
    }
}