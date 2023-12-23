package com.example.wtt01

import WeatherData
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wtt01.data.HourlyWeather
import com.example.wtt01.data.RegeoResponse
import com.example.wtt01.data.WeatherData24h
import com.example.wtt01.data.WeatherData7d
import com.example.wtt01.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity(),LocationListener {

    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2
    private val apiKey = " "// 和风天气api

    private var formattedLocation: String = ""


//    private var currentLocation: Location? = null
//    private val initialLocation = "101090213" // 初始位置

    private var hourlyWeatherData: List<HourlyWeather> = listOf()
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // 检查GPS是否开启
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // GPS未开启，提示用户
            Toast.makeText(this, "GPS未开启，请开启位置服务!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "GPS已开启", Toast.LENGTH_SHORT).show()
//            val handler = Handler()
//            handler.postDelayed(
//                {
//                    requestLocationUpdates() // 获取位置
//                }, 500
//            ) //0.5秒后执行
            requestLocationUpdates()

        }

        // 在适当的时机调用此函数以发起网络请求
//        mBinding.btn01.setOnClickListener {
//            fetchWeatherData()
//            fetchWeatherData24h()
//        }

//        fetchWeatherData()
//        fetchWeatherData24h()
        mBinding.tvCityName.setOnClickListener {
            showMultiChoiceDialog()
        }

        //下拉刷新
        mBinding.swipeRefresh.setOnRefreshListener {
            // 在这里执行刷新逻辑，从服务器获取数据
            fetchWeatherData(formattedLocation)

            // 数据加载完毕后，停止刷新动画
            mBinding.swipeRefresh.isRefreshing = false
        }

        mBinding.view1.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = HourlyWeatherAdapter(hourlyWeatherData) // hourlyWeatherData存储每小时data
        }

    }


    interface WeatherApiService {
        @GET("weather/now")
        fun getCurrentWeather(@Query("location") location: String,
                              @Query("key") apiKey: String,
                              @Query("lang") lang: String = "zh",
                              @Query("unit") unit: String = "m"): Call<WeatherData>
    }
    interface WeatherApiService24h {
        @GET("weather/24h")
        fun getHourlyWeather(@Query("location") location: String,
                             @Query("key") apiKey: String,
                             @Query("lang") lang: String = "zh",
                             @Query("unit") unit: String = "m"): Call<WeatherData24h>
    }
    // 逆向经纬度转地区名
    interface AmapRegeoApiService {
        @GET("v3/geocode/regeo")
        fun getRegeo(
            @Query("output") output: String = "json",
            @Query("location") location: String,
            @Query("key") apiKey: String,
            @Query("radius") radius: Int = 1000,
            @Query("extensions") extensions: String = "all"
        ): Call<RegeoResponse>
    }


    interface WeatherApiService7d {
        @GET("weather/7d")
        fun getWeeklyWeather(@Query("location") location: String,
                             @Query("key") apiKey: String,
                             @Query("lang") lang: String = "zh",
                             @Query("unit") unit: String = "m"): Call<WeatherData7d>
    }

    // ATODO: 添加一周天气Api(api已经创建)


    // Api地址: https://api.qweather.com/v7/
    // weather/7d
    // 位置: 101090213
    // KEY :  
    // TODO: 添加调用一周天气Api

    // BTODO: GPS定位更换城市代码,101090213(河北省保定市莲池区)(废弃方案)
    // ATODO: GPS定位获取经纬度,格式化经纬度最大两位小数,将城市代码替换为格式化好的经纬度.(已创建)
    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)// 最小更改时间minTimeMs 最小更改距离(单位米)minDistanceM
        } else {
            // 请求位置权限
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        }
    }

    override fun onLocationChanged(location: Location) {
        formattedLocation = String.format("%.2f,%.2f", location.longitude, location.latitude)
        val toast = Toast.makeText(this, formattedLocation, Toast.LENGTH_SHORT)
        toast.show()
        fetchWeatherData(formattedLocation)
        fetchWeatherData24h(formattedLocation)
        fetchLocationAndWeatherData(formattedLocation)
        stopLocationUpdates()
    }
    private fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
    }


    private fun fetchLocationAndWeatherData(location: String) {
        // 创建 Retrofit 实例
        val retrofit = Retrofit.Builder()
            .baseUrl("https://restapi.amap.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val regeoService = retrofit.create(AmapRegeoApiService::class.java)
        val regeoCall = regeoService.getRegeo(location = location, apiKey = " ")

        regeoCall.enqueue(object : Callback<RegeoResponse> {
            override fun onResponse(call: Call<RegeoResponse>, response: Response<RegeoResponse>) {
                if (response.isSuccessful) {
                    // 解析城市名称
                    val cityName = response.body()?.regeocode?.addressComponent?.district
                    if (cityName !== null && cityName.isNotBlank()) {
                        mBinding.tvCityName.text = cityName
                    } else {
                        // 城市名称为空时的处理
                        mBinding.tvCityName.text = "城市名称未找到"
                    }
                } else {
                    // 请求不成功时的处理
                    mBinding.tvCityName.text = "${response.errorBody()?.string()}"
                }
            }

            override fun onFailure(call: Call<RegeoResponse>, t: Throwable) {
                // 请求失败时的处理
                mBinding.tvCityName.text = "${t.message}"
            }
        })
    }




    private fun fetchWeatherData(location: String) {

        // 创建 Retrofit 实例
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.qweather.com/v7/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherApiService::class.java)
        // 使用合适的位置ID和API密钥
        val call = service.getCurrentWeather(location, apiKey)

        // 发起异步网络请求
        call.enqueue(object : Callback<WeatherData> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<WeatherData>, response: Response<WeatherData>) {
                if (response.isSuccessful) {
                    // 处理返回的天气数据
                    val weatherData = response.body()
                    // 使用绑定更新UI
                    mBinding.tvWeatherStatus.text = weatherData?.now?.text
                    mBinding.tvTemperature.text = "${weatherData?.now?.temp}°C"
                    mBinding.tvUpdateTime.text = "${weatherData?.updateTime?.let { formatFxTime(it) }}"
                    mBinding.tvHumidity.text = "${weatherData?.now?.humidity}%"
                    mBinding.tvWindSpeed.text = "${weatherData?.now?.windSpeed} Km/h"
                    mBinding.tvPrecip.text = "${weatherData?.now?.precip}mm"
                    mBinding.tvWindDir.text = weatherData?.now?.windDir
                    mBinding.tvPressure.text = "${weatherData?.now?.pressure}hPa"
                    mBinding.tvVis.text = "${weatherData?.now?.vis}Km"
                    mBinding.tvCloud.text = "${weatherData?.now?.cloud}%"
                    mBinding.tvDew.text = "${weatherData?.now?.dew}°C"
                    mBinding.tvObsTime.text = "${weatherData?.now?.obsTime?.let { formatFxTime(it) }}"

                    val imageResId = getDrawableResourceId("icon_${weatherData?.now?.icon}_fill", this@MainActivity)
                    if (imageResId != 0) { // 确保资源存在
                        mBinding.imageView.setImageResource(imageResId)
                    } else {
                        // 设置默认图片或隐藏ImageView
                        mBinding.imageView.setImageResource(R.drawable.icon_100)
                    }
                }
            }

            override fun onFailure(call: Call<WeatherData>, t: Throwable) {
                // 处理失败情况
                mBinding.tvCityName.text = "获取失败"
                mBinding.tvWeatherStatus.text = "获取失败"
                mBinding.tvTemperature.text = "获取失败"
//                mBinding.tvUpdateTime.text = "获取失败"
//                mBinding.tvHumidity.text = "获取失败"
//                mBinding.tvWindSpeed.text = "获取失败"
//                mBinding.tvPrecip.text = "获取失败"
//                mBinding.tvWindDir.text = "获取失败"
//                mBinding.tvPressure.text = "获取失败"
//                mBinding.tvVis.text = "获取失败"
//                mBinding.tvCloud.text = "获取失败"
//                mBinding.tvDew.text = "获取失败"
//                mBinding.tvObsTime.text = "获取失败"
            }
        })


    }

    private fun fetchWeatherData24h(location: String) {
        // 创建 Retrofit 实例
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.qweather.com/v7/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherApiService24h::class.java)
        // 获取 24 小时天气数据
        val call = service.getHourlyWeather(location, apiKey)

        call.enqueue(object : Callback<WeatherData24h> {
            override fun onResponse(call: Call<WeatherData24h>, response: Response<WeatherData24h>) {
                if (response.isSuccessful) {
                    // 提取小时天气数据并更新 RecyclerView
                    hourlyWeatherData = response.body()?.hourly ?: listOf()
                    mBinding.view1.adapter = HourlyWeatherAdapter(hourlyWeatherData)
                }
            }

            override fun onFailure(call: Call<WeatherData24h>, t: Throwable) {
                // 处理失败
            }
        })
    }


//    private fun fetchWeatherData7d() {
//        val retrofit = Retrofit.Builder()
//            .baseUrl("https://api.qweather.com/v7/")
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        val service = retrofit.create(WeatherApiService7d::class.java)
//        val call = service.getWeeklyWeather("101090213", " ")
//
//        call.enqueue(object : Callback<WeatherData7d> {
//            override fun onResponse(call: Call<WeatherData7d>, response: Response<WeatherData7d>) {
//                if (response.isSuccessful) {
//                    // 处理一周天气数据
//                }
//            }
//
//            override fun onFailure(call: Call<WeatherData7d>, t: Throwable) {
//                // 处理失败情况
//            }
//        })
//    }





    fun formatFxTime(fxTime: String): String {
        val parser = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val formatter = DateTimeFormatter.ofPattern("MM月dd日 HH:mm")
        val dateTime = LocalDateTime.parse(fxTime, parser)
        return dateTime.format(formatter)
    }

    @SuppressLint("DiscouragedApi")
    fun getDrawableResourceId(resourceName: String, context: Context): Int {
        return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }

    // TODO: 添加一周api的组件

//    abstract val location: Location
//    private val latitude = location.latitude
//    private val longitude = location.longitude

//    // 格式化字符串，使经纬度最多只有两位小数
//    val lastLocation = String.format("%.2f, %.2f", latitude, longitude)


// ATODO:点击地区弹出多选(以写)
private fun showMultiChoiceDialog() {
    val items = arrayOf("北京市昌平区", "沧州市孟村县", "涿州市","GPS定位") // 选项

    AlertDialog.Builder(this)
        .setTitle("选择城市")
        .setItems(items) { dialog, which ->
            // 处理选项的选择
            // which 是被选中的选项的索引
            handleSelection(items[which])
            dialog.dismiss()
        }
        .show()
}
    // 选择
    private fun handleSelection(choice: String) {
        val loc1 = "116.23,40.22" //when 1
        val loc2 = "117.10,38.05" //when 2
        val loc3 = "115.97,39.49" //when 3
        // 这里处理选中的选项
        when (choice) {
            "北京市昌平区" -> {
                Toast.makeText(this, "北京市昌平区($loc1)", Toast.LENGTH_SHORT).show()
                fetchWeatherData(loc1)
                fetchWeatherData24h(loc1)
                fetchLocationAndWeatherData(loc1)
                formattedLocation = loc1
            }
            "沧州市孟村县" -> {
                Toast.makeText(this, "沧州市孟村县($loc2)", Toast.LENGTH_SHORT).show()
                fetchWeatherData(loc2)
                fetchWeatherData24h(loc2)
                fetchLocationAndWeatherData(loc2)
                formattedLocation = loc2

            }
            "涿州市" -> {
                Toast.makeText(this, "涿州市($loc3)", Toast.LENGTH_SHORT).show()
                fetchWeatherData(loc3)
                fetchWeatherData24h(loc3)
                fetchLocationAndWeatherData(loc3)
                formattedLocation = loc3
            }
            "GPS定位"->{
                requestLocationUpdates()
            }
        }

    }


}
