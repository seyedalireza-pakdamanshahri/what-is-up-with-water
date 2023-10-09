package com.hdaf.whatsupwiththiswater


import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Header

// Data classes
data class WaterQualityResponse(val status: String, val data: Any)
data class GeocodingResponse(
    val results: List<Result>
) {
    data class Result(
        val address_components: List<AddressComponent>
    ) {
        data class AddressComponent(
            val long_name: String,
            val short_name: String,
            val types: List<String>
        )
    }
}
data class WeatherResponse(
    val main: Main
) {
    data class Main(
        val temp: Double
    )
}
data class PlacesResponse(
    val results: List<PlaceResult>
) {
    data class PlaceResult(
        val photos: List<Photo>
    ) {
        data class Photo(
            val photo_reference: String
        )
    }
}


interface PlacesService {
    @GET("api/place/textsearch/json")
    fun getCityImage(
        @Query("query") query: String,
        @Query("key") apiKey: String
    ): Call<PlacesResponse>
}

interface GeocodingService {
    @GET("maps/api/geocode/json")
    fun getCityFromLocation(
        @Query("latlng") latlng: String,
        @Query("key") apiKey: String
    ): Call<GeocodingResponse>
}
interface WaterQualityService {
    @GET("ncWCS/ncWCS?service=WCS&version=2.0.1&request=GetCoverage&format=NetCDF4")
    fun getWaterQuality(
        @Header("Authorization") token: String,
        @Query("coverageid") coverageId: String,
        @Query("subset") timeRange: String,
        @Query("subset") latRange: String,
        @Query("subset") lonRange: String
    ): Call<WaterQualityResponse>
}


interface WeatherService {
    @GET("weather")
    fun getCurrentWeatherByCityName(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Call<WeatherResponse>
}

// Retrofit singleton instances
object ApiServiceBuilder {
    private const val WATER_QUALITY_BASE_URL = "https://oceandata.sci.gsfc.nasa.gov/"
    private const val WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private const val GEOCODING_BASE_URL = "https://maps.googleapis.com/"
    private const val PLACES_BASE_URL = "https://maps.googleapis.com/"

    private val placesRetrofit = Retrofit.Builder()
        .baseUrl(PLACES_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val placesService: PlacesService = placesRetrofit.create(PlacesService::class.java)

    private val geocodingRetrofit = Retrofit.Builder()
        .baseUrl(GEOCODING_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val geocodingService: GeocodingService = geocodingRetrofit.create(GeocodingService::class.java)
    private val waterQualityRetrofit = Retrofit.Builder()
        .baseUrl(WATER_QUALITY_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val weatherRetrofit = Retrofit.Builder()
        .baseUrl(WEATHER_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val waterQualityService: WaterQualityService = waterQualityRetrofit.create(WaterQualityService::class.java)
    val weatherService: WeatherService = weatherRetrofit.create(WeatherService::class.java)
}





