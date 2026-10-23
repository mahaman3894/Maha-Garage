package maha.garage.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import maha.garage.data.remote.api.VehicleApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val BASE_URL = "https://apis.data.go.kr/"

    /**
     * 공공데이터포털(data.go.kr)에서 발급받은 Decoding 서비스 인증키
     * (참고: OkHttp addQueryParameter 사용 시 자동 인코딩되므로 'Decoding 인증키'를 권장합니다.)
     */
    var publicDataServiceKey: String = "YOUR_DECODED_SERVICE_KEY_HERE"

    val isApiKeyConfigured: Boolean
        get() = publicDataServiceKey.isNotBlank() && publicDataServiceKey != "YOUR_DECODED_SERVICE_KEY_HERE"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val apiKeyInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            val urlWithKey = originalUrl.newBuilder()
                .addQueryParameter("serviceKey", publicDataServiceKey)
                .build()

            val newRequest = originalRequest.newBuilder()
                .url(urlWithKey)
                .build()

            chain.proceed(newRequest)
        }

        OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    val vehicleApiService: VehicleApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(VehicleApiService::class.java)
    }
}
