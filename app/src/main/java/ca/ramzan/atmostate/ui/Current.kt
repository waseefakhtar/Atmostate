package ca.ramzan.atmostate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.ramzan.atmostate.network.Current
import ca.ramzan.atmostate.network.Weather
import coil.compose.rememberImagePainter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt
import com.ramzan.atmostate.R as AtmostateR

private val gridPadding = 24.dp

@Composable
fun CurrentForecast(listState: LazyListState, current: Current) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        current.run {
            item { TimeUpdated(dt) }
            item {
                Weather(
                    weather.first(),
                    temp.roundToInt(),
                    feelsLike.roundToInt(),
                    uvi.roundToInt()
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    GridColumn {
                        Wind(
                            (windSpeed * 3.6).roundToInt(),
                            ((windGust ?: 0.0) * 3.6).roundToInt(),
                            degreeToDirection(windDeg)
                        )
                        Cloudiness(clouds.toInt())
                    }
                    GridColumn {
                        Humidity("${humidity.roundToInt()}%")
                        Sunrise(sunrise)

                    }
                    GridColumn {
                        Pressure("${"%.1f".format(pressure / 10)}kPa")
                        Sunset(sunset)

                    }
                    GridColumn {
                        Visibility((visibility / 1000).toInt())
                        DewPoint("${dewPoint.roundToInt()}°C")
                    }

                }
            }
        }
    }
}

@Composable
fun GridColumn(content: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxHeight()
    ) {
        content()
    }
}

@Composable
fun Wind(speed: Int, gust: Int, direction: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(AtmostateR.drawable.wind),
            contentDescription = "Wind"
        )
        Text(text = "Wind", style = TextStyle(fontWeight = FontWeight.Light))
        Text(text = "${speed}km/h $direction")
        Text(text = "${gust}km/h")
    }
}

@Composable
fun Cloudiness(clouds: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(AtmostateR.drawable.cloud),
            contentDescription = "Cloud"
        )
        Text(text = "Cloudiness", style = TextStyle(fontWeight = FontWeight.Light))
        Text(text = "$clouds%")
    }
}

@Composable
fun Visibility(visibility: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(bottom = gridPadding)
    ) {
        Image(
            painter = painterResource(AtmostateR.drawable.telescope),
            contentDescription = "Telescope"
        )
        Text(text = "Visibility", style = TextStyle(fontWeight = FontWeight.Light))
        Text(text = "${visibility}km")
    }
}

@Composable
fun Pressure(pressure: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(bottom = gridPadding)
    ) {
        Image(
            painter = painterResource(AtmostateR.drawable.barometer),
            contentDescription = "Barometer"
        )
        Text(text = "Pressure", style = TextStyle(fontWeight = FontWeight.Light))
        Text(pressure)
    }
}

@Composable
fun Humidity(humidity: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(bottom = gridPadding)
    ) {
        Image(
            painter = painterResource(AtmostateR.drawable.humidity),
            contentDescription = "Humidity"
        )
        Text(text = "Humidity", style = TextStyle(fontWeight = FontWeight.Light))
        Text(humidity)
    }
}

@Composable
fun DewPoint(dewPoint: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(AtmostateR.drawable.dewpoint),
            contentDescription = "Dew point"
        )
        Text(text = "Dew point", style = TextStyle(fontWeight = FontWeight.Light))
        Text(dewPoint)
    }
}

@Composable
fun Weather(weather: Weather, temp: Int, feelsLike: Int, uvi: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberImagePainter("https://openweathermap.org/img/wn/${weather.icon}@4x.png"),
            contentDescription = "Forecast image",
            modifier = Modifier.size(128.dp)
        )
        Column {
            Text(text = "$temp°C", style = MaterialTheme.typography.h3)
            Text(text = "Feels like $feelsLike")
            Text(weather.description.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
            Text("UV Index: $uvi")
        }
    }
}

@Composable
fun Sunrise(sunrise: Long) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(AtmostateR.drawable.sunrise),
            contentDescription = "Sunrise"
        )
        Text(text = "Sunrise", style = TextStyle(fontWeight = FontWeight.Light))
        Text(text = TimeFormatter.toDayHour(sunrise))
    }
}

@Composable
fun Sunset(sunset: Long) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(AtmostateR.drawable.sunset),
            contentDescription = "Sunset"
        )
        Text(text = "Sunset", style = TextStyle(fontWeight = FontWeight.Light))
        Text(text = TimeFormatter.toDayHour(sunset))
    }
}

object TimeFormatter {
    private val dayHourFormatter =
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    private val hourOfDayFormatter =
        DateTimeFormatter.ofPattern("ha").withZone(ZoneId.systemDefault())

    fun toDayHour(time: Long): String {
        return Instant.ofEpochSecond(time).run {
            dayHourFormatter.format(this)
        }
    }

    fun toDate(time: Long): String {
        return Instant.ofEpochSecond(time).run {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()).format(this)
        }
    }

    fun toHourOfDay(time: Long): String {
        return Instant.ofEpochSecond(time).run {
            hourOfDayFormatter.format(this)
        }
    }
}