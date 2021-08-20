package ca.ramzan.atmostate.ui.forecast

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ramzan.atmostate.R
import java.time.ZonedDateTime

val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

fun degreeToDirection(deg: Int): String {
    return directions[(deg % 360) / 45]
}

@Composable
fun TimeUpdated(time: ZonedDateTime) {
    Text(text = "Updated ${TimeFormatter.toDate(time)}", style = MaterialTheme.typography.body2)
}

@Composable
fun Rain(rain: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Image(
            painter = painterResource(R.drawable.rain_drop),
            contentDescription = "Rain",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(text = "${"%.1f".format(rain)}mm")
    }
}

@Composable
fun Snow(snow: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Image(
            painter = painterResource(R.drawable.snow_flake),
            contentDescription = "Rain",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(text = "${"%.1f".format(snow)}mm")
    }
}

@Composable
fun NoDataMessage(message: String) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = message,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun StickyListHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.secondary, RectangleShape)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        style = MaterialTheme.typography.subtitle1
    )
}