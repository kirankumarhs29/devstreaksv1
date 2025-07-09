package com.dailydevchallenge.devstreaks.notification

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.widget.TimePicker
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.*

@SuppressLint("DefaultLocale")
@Composable
fun TimePickerSetting(
    initialHour: Int = 9,
    initialMinute: Int = 0,
    onTimeSelected: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var timeText by remember { mutableStateOf(String.format("%02d:%02d", initialHour, initialMinute)) }

    Button(onClick = {
        showTimePicker(context, initialHour, initialMinute) { hour, minute ->
            timeText = String.format("%02d:%02d", hour, minute)
            onTimeSelected(hour, minute)
        }
    }) {
        Text("⏰ Reminder Time: $timeText")
    }
}

fun showTimePicker(
    context: Context,
    initialHour: Int,
    initialMinute: Int,
    onTimePicked: (Int, Int) -> Unit
) {
    TimePickerDialog(
        context,
        { _: TimePicker, hour: Int, minute: Int ->
            onTimePicked(hour, minute)
        },
        initialHour,
        initialMinute,
        true
    ).show()
}
