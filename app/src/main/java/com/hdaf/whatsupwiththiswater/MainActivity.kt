package com.hdaf.whatsupwiththiswater
import com.bumptech.glide.Glide
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private lateinit var apiService: WaterQualityService

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d("Arshia", "Location changed: ${location.latitude}, ${location.longitude}")
            val locString = "${location.latitude},${location.longitude}"
            Log.d("Arshia", "${location.latitude},${location.longitude}")
            fetchWaterQualityData(locString)
            getCityNameFromLocation(locString) { cityName ->
                if (cityName != null) {

                    fetchCurrentWeatherTemperatureForCity(cityName)
                } else {
                    Log.e("Arshia", "City name is null after geocoding.")
                }
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d("Arshia", "Location provider status changed.")
        }

        override fun onProviderEnabled(provider: String) {
            Log.d("Arshia", "Location provider enabled: $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Log.e("Arshia", "Location provider disabled: $provider")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("Arshia", "onCreate() called")

        apiService = ApiServiceBuilder.waterQualityService

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("Arshia", "Location permission not granted. Requesting...")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            Log.d("Arshia", "Location permission already granted. Requesting location updates.")
            requestLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(
                        "Arshia",
                        "Location permission granted by user. Requesting location updates."
                    )
                    requestLocationUpdates()
                } else {
                    Log.e("Arshia", "Location permission denied by user.")
                }
            }

            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }
    private fun fetchCityImage(cityName: String) {
        val apiKey = "" // replace your own api
        ApiServiceBuilder.placesService.getCityImage(cityName, apiKey)
            .enqueue(object : Callback<PlacesResponse> {
                override fun onResponse(call: Call<PlacesResponse>, response: Response<PlacesResponse>) {
                    if (response.isSuccessful) {
                        val imageUrl = response.body()?.results?.firstOrNull()?.photos?.firstOrNull()?.photo_reference
                        if (imageUrl != null) {
                            val fullImageUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=$imageUrl&key=$apiKey"
                            val cityImageView: ImageView = findViewById(R.id.cityImage)
                            Glide.with(this@MainActivity).load(fullImageUrl).into(cityImageView)
                        }
                    } else {
                        Log.e("Arshia", "Failed to fetch city image: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<PlacesResponse>, t: Throwable) {
                    Log.e("Arshia", "Error fetching city image: ${t.message}")
                }
            })
    }

    private fun requestLocationUpdates() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        Log.d("Arshia", "Requesting location updates.")

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            Log.d("Arshia", "Requested location permissions.")
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            2 * 60 * 1000L,
            500f,
            locationListener
        )
    }

    private fun getCityNameFromLocation(location: String, callback: (String?) -> Unit) {
        val apiKey = "AIzaSyBSp2BJpe0LQgY7LPtSRCzhX-AJK5rNwgk"
        ApiServiceBuilder.geocodingService.getCityFromLocation(location, apiKey)
            .enqueue(object : Callback<GeocodingResponse> {
                override fun onResponse(
                    call: Call<GeocodingResponse>,
                    response: Response<GeocodingResponse>
                ) {
                    if (response.isSuccessful) {
                        val city =
                            response.body()?.results?.firstOrNull()?.address_components?.find {
                                it.types.contains("locality")
                            }?.long_name
                        callback(city)

                        val cityNameTextView: TextView = findViewById(R.id.cityName)
                        cityNameTextView.text = city
                        Log.d("Arshia", "City retrieved and set: $city")
                        fetchCityImage(city ?: "")
                    } else {
                        Log.e(
                            "Arshia",
                            "Geocoding response failure: ${response.errorBody()?.string()}"
                        )
                    }
                }

                override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                    Log.e("Arshia", "Failed to retrieve city: ${t.message}")
                }
            })
    }

    override fun onPause() {
        super.onPause()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)
        Log.d("Arshia", "Location updates removed on pause.")
    }


    fun fetchWaterQualityData(location: String) {
        val parts = location.split(",")
        if (parts.size >= 2) {
            val latRange = parts[0]
            val lonRange = parts[1]
            Log.d("Arshia","$lonRange")
            Log.d("Arshia","$latRange")
            val token = "Bearer eyJ0eXAiOiJKV1QiLCJvcmlnaW4iOiJFYXJ0aGRhdGEgTG9naW4iLCJzaWciOiJlZGxqd3RwdWJrZXlfb3BzIiwiYWxnIjoiUlMyNTYifQ.eyJ0eXBlIjoiVXNlciIsInVpZCI6ImFyc2hpYV9wYWsiLCJleHAiOjE3MDE5MjI4ODksImlhdCI6MTY5NjczODg4OSwiaXNzIjoiRWFydGhkYXRhIExvZ2luIn0.3nw1s3N9yEOiQVg7VgPzvK5jDY81wkV-0PD9X2sxo2RBf7IucGyzuEunQw7WJqag66tW0iXs0ltwNDwHAKHk1jkKeYmn1FVEnLhfzIOETbb8qdLbcEdo7YlglguypwV7Bu3jf57W1DWAKvgBskscrqvHYGnz7xICeAHhgp2Lbdh6cE16AEPe6r_HpBZlo4qbvFcJwONdY2bGYYSMss7eNDNCKotB9IHIZclNkJzmBN_WHeBm0taUpn270zKsKzQbU42j8KRUN5SxQHfO-KUCzt8HU0ZKvlan87Acrbxih-alhkorBLotM4f5GYZMUDhA_3_m5rmSgiPUPIeTU4Q1mg"
            val coverageId = "MYD28M"
            val timeRange = "\"2021-01-01T00:00:00Z\",\"2021-01-31T23:59:59Z\""

            val call = apiService.getWaterQuality(token, coverageId, timeRange, latRange, lonRange)
            Log.d("Arshia", "$call")
            call.enqueue(object : Callback<WaterQualityResponse> {
                override fun onResponse(call: Call<WaterQualityResponse>, response: Response<WaterQualityResponse>) {
                    if (response.isSuccessful) {
                        val data = response.body()
                        Log.d("Arshia", data.toString())
                        // Process and display the data in the UI
                    } else {
                        Log.e("Arshia", "Failed to fetch data: ${response.message()}")
                        Log.e("Arshia", "Response body: ${response.errorBody()?.string()}")
                        Log.e("Arshia", "Failed with HTTP code: ${response.code()}")

                    }
                }

                override fun onFailure(call: Call<WaterQualityResponse>, t: Throwable) {
                    Log.e("Arshia", "Failed to fetch data: ${t.message}")
                }
            })
        } else {
            Log.e("Arshia", "Location string not in expected format: $location")
        }
    }


    private fun fetchCurrentWeatherTemperatureForCity(cityName: String) {
        val apiKey = "YOUR_API_KEY_HERE"
        val call = ApiServiceBuilder.weatherService.getCurrentWeatherByCityName(cityName, apiKey)
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful) {
                    val temperature = response.body()?.main?.temp

                } else {

                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {

            }
        })
    }


}


