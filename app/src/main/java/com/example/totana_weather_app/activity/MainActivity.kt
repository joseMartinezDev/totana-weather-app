package com.example.totana_weather_app.activity

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.totana_weather_app.R
import com.example.totana_weather_app.api.OkHttpRequest
import com.example.totana_weather_app.utils.FetchCompleteListener
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import okhttp3.internal.toLongOrDefault
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(), FetchCompleteListener {
    private val client = OkHttpClient()
    private val request = OkHttpRequest(client)
    private val url = "https://totanaapi.nw.r.appspot.com/"
    private lateinit var currentObject: JSONObject
    private lateinit var dailyForecast: JSONArray
    private lateinit var currentWeather: JSONObject
    private lateinit var a: Animation


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)


        a = AnimationUtils.loadAnimation(this, R.anim.blink)


        val btn_refresh = findViewById<Button>(R.id.buttonRefresh)
        btn_refresh.setOnClickListener { makeRequest() }

        val acc = supportActionBar
        acc?.setTitle("")
        val d = ResourcesCompat.getDrawable(getResources(), R.drawable.header, null);
        acc?.setBackgroundDrawable(d)

        pullToRefresh.setOnRefreshListener {
            makeRequest()
            pullToRefresh.isRefreshing = false

        }
    }

    override fun onResume() {
        super.onResume()
        makeRequest()
    }

    fun String.capitalizeWords(): String = split(" ").map { it.capitalize() }.joinToString(" ")

    private fun RunAnimation(startAnimation: Boolean) {
        a.reset()
        lastLabel.clearAnimation()
        if (startAnimation) {
            lastLabel.setText("Generando pronóstico")
            lastLabel.startAnimation(a)
        }
    }

    private fun RunAnimationLive() {

            a.reset()
            textViewTodayIcon.visibility = View.VISIBLE
            textViewTodayIcon.clearAnimation()
            textViewTodayIcon.startAnimation(a)

            Timer().schedule(120000) {
                runOnUiThread {
                textViewTodayIcon.clearAnimation()
                }
            }

    }

    private fun makeRequest() {

        RunAnimation(true)

        request.GET(url, object : Callback {
            var headerMessage = ""
            override fun onResponse(call: Call, response: Response) {

                val responseData = response.body?.string()
                runOnUiThread {
                    try {
                        var json = JSONObject(responseData)

                        currentObject = json.getJSONObject("current")
                        dailyForecast = json.getJSONArray("daily")
                        currentWeather = currentObject.getJSONArray("weather").get(0) as JSONObject
                        val current = java.util.Calendar.getInstance()
                        headerMessage =
                            "Actualizado ${padDigits(current.get(Calendar.HOUR_OF_DAY))}:${
                                padDigits(
                                    current.get(
                                        Calendar.MINUTE
                                    )
                                )
                            }:${
                                padDigits(
                                    current.get(
                                        Calendar.SECOND
                                    )
                                )
                            }"
                        RunAnimation(false)
                        RunAnimationLive()
                        lastLabel.setText(headerMessage)
                        this@MainActivity.fetchComplete()

                    } catch (e: JSONException) {
                        e.printStackTrace()
                        headerMessage =
                            "Se produjo un fallo. Por favor, consulta con el administrador"
                        lastLabel.setText(headerMessage)

                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    e.printStackTrace()
                    headerMessage = "Se produjo un fallo. Por favor, revisa tu conexión."
                    lastLabel.setText(headerMessage)
                }
            }
        })

    }

    private fun padDigits(number: Int): String {
        val dec = DecimalFormat("00")
        return dec.format(number)
    }

    private fun setForecastIcon(forecastIcon: ImageView, iconCode: String?) {

        if (iconCode != null) {
            val iconName = "forecast_$iconCode"
            val iconResourceId = resources.getIdentifier(iconName, "drawable", packageName)

            if (iconResourceId != 0) {
                val forecastIconDrawable = ContextCompat.getDrawable(this, iconResourceId)
                forecastIcon.setImageDrawable(forecastIconDrawable)
            }
        }
    }

    private fun getDayOfWeek(day: Int): String {

        when (day) {
            1 -> return "Domingo"
            2 -> return "Lunes"
            3 -> return "Martes"
            4 -> return "Miércoles"
            5 -> return "Jueves"
            6 -> return "Viernes"
            else -> return "Sábado"
        }
    }

    private fun getMonthName(month: Int): String {

        when (month) {
            0 -> return "Enero"
            1 -> return "Febrero"
            2 -> return "Marzo"
            3 -> return "Abril"
            4 -> return "Mayo"
            5 -> return "Junio"
            6 -> return "Julio"
            7 -> return "Agosto"
            8 -> return "Septiembre"
            9 -> return "Octubre"
            10 -> return "Noviembre"
            else -> return "Diciembre"
        }
    }

    private fun getDate(s: String): Date {
        val l = s.toLongOrDefault(0)
        val forecastDate = Date(l * 1000)

        return forecastDate

    }

    private fun setCurrentWeather() {
        textViewToday.visibility = View.VISIBLE
        labelDescCur.setText(currentWeather.getString("description").capitalizeWords())
        labelCurTemp.setText(getTempFormatted(currentObject.getDouble("temp")) + " °C")
        labelCurFeel.setText(getTempFormatted(currentObject.getDouble("feels_like")) + " °C")
        setForecastIcon(forecastIconCur, currentWeather.getString("icon"))

    }

    private fun getTempFormatted(tempt: Double): String {
        val dec = DecimalFormat("00")
        return dec.format(tempt.roundToInt())
    }

    private fun setForecastWeather(position: Int, forecast: JSONObject, c: Calendar) {
        var JSONObjectAux: JSONObject
        val forecastDate = getDate(forecast.get("dt").toString())
        c.setTime(forecastDate);
        JSONObjectAux = forecast.get("temp") as JSONObject

        val labelTempMinID =
            this.resources.getIdentifier("labelTempMin" + position.toString(), "id", packageName)
        val textTempMin = findViewById<TextView>(labelTempMinID)
        textTempMin.setText(getTempFormatted(JSONObjectAux.getDouble("min")) + " °C")

        val labelTempMaxID =
            this.resources.getIdentifier("labelTempMax" + position.toString(), "id", packageName)
        val textTempMax = findViewById<TextView>(labelTempMaxID)
        textTempMax.setText(getTempFormatted(JSONObjectAux.getDouble("max")) + " °C")

        val labelDescID =
            this.resources.getIdentifier("labelDesc" + position.toString(), "id", packageName)
        val labelDesc = findViewById<TextView>(labelDescID)

        JSONObjectAux = (forecast.get("weather") as JSONArray).get(0) as JSONObject
        labelDesc.setText(JSONObjectAux.getString("description").capitalizeWords())

        val forecastIconID =
            this.resources.getIdentifier("forecastIcon" + position.toString(), "id", packageName)
        val forecastIcon = findViewById<ImageView>(forecastIconID)
        setForecastIcon(forecastIcon, JSONObjectAux.getString("icon"))

        val textDateID = this.resources.getIdentifier(
            "textViewForecast" + position.toString(),
            "id",
            packageName
        )
        val textDate = findViewById<TextView>(textDateID)
        textDate.visibility = View.VISIBLE
        textDate.setText(
            "${getDayOfWeek(c.get(Calendar.DAY_OF_WEEK))} ${padDigits(c.get(Calendar.DAY_OF_MONTH))} de ${
                getMonthName(
                    c.get(Calendar.MONTH)
                )
            }"
        )

    }

    override fun fetchComplete() {
        val c = Calendar.getInstance()

        setCurrentWeather()
        var forecast = dailyForecast.get(1) as JSONObject
        setForecastWeather(1, forecast, c)
        forecast = dailyForecast.get(2) as JSONObject
        setForecastWeather(2, forecast, c)

        forecast = dailyForecast.get(3) as JSONObject
        setForecastWeather(3, forecast, c)

        forecast = dailyForecast.get(4) as JSONObject
        setForecastWeather(4, forecast, c)

        forecast = dailyForecast.get(5) as JSONObject
        setForecastWeather(5, forecast, c)

        forecast = dailyForecast.get(6) as JSONObject
        setForecastWeather(6, forecast, c)

        forecast = dailyForecast.get(7) as JSONObject
        setForecastWeather(7, forecast, c)

    }

}
