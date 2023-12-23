package com.example.wtt01.data

data class WeatherData24h(
    val code: String,
    val updateTime: String,
    val fxLink: String,
    val hourly: List<HourlyWeather>,
    val refer: Refer1
)

data class HourlyWeather(
    val fxTime: String,
    val temp: String,
    val icon: String,
    val text: String,
    val wind360: String,
    val windDir: String,
    val windScale: String,
    val windSpeed: String,
    val humidity: String,
    val pop: String,
    val precip: String,
    val pressure: String,
    val cloud: String,
    val dew: String
)
data class Refer1(
    val sources: List<String>,
    val license: List<String>
)
