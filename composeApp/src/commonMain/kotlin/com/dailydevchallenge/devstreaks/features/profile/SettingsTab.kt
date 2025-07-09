package com.dailydevchallenge.devstreaks.features.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.auth.AuthService
import com.dailydevchallenge.devstreaks.settings.DarkModeSettings
import org.koin.compose.koinInject
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.notification.getNotificationScheduler
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch

@Composable
fun SettingsTab(onLogout: () -> Unit) {
    val notificationsEnabledState = remember { mutableStateOf(UserPreferences.isNotificationsEnabled()) }
    val darkModeEnabled by DarkModeSettings.darkModeFlow.collectAsState()
    val authService: AuthService = koinInject()
    val currentTime = remember { mutableStateOf(UserPreferences.getReminderTime()) }
    val (hour, minute) = currentTime.value
    val hourText = remember { mutableStateOf(hour.toString()) }
    val minuteText = remember { mutableStateOf(minute.toString()) }
    val coroutineScope = rememberCoroutineScope()
    val hourBringIntoView = remember { BringIntoViewRequester() }
    val minuteBringIntoView = remember { BringIntoViewRequester() }

    val hourFocusRequester = remember { FocusRequester() }
    val minuteFocusRequester = remember { FocusRequester() }

    val focusManager = LocalFocusManager.current


    Scaffold { innerPadding ->
        val combinedPadding = PaddingValues(
            start = 16.dp + innerPadding.calculateStartPadding(LayoutDirection.Ltr),
            top = 16.dp + innerPadding.calculateTopPadding(),
            end = 16.dp + innerPadding.calculateEndPadding(LayoutDirection.Ltr),
            bottom = 16.dp + innerPadding.calculateBottomPadding()
        )
        LazyColumn(
            contentPadding =  combinedPadding,
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .consumeWindowInsets(innerPadding)
        ) {
            item {
                SettingsSection(title = "Preferences") {
                    SettingItem(
                        label = "Notifications",
                        isToggle = true,
                        toggled = notificationsEnabledState.value,
                        onToggleChange = { enabled ->
                            notificationsEnabledState.value = enabled
                            UserPreferences.setNotificationsEnabled(enabled)
                            if (enabled) {
                                getNotificationScheduler().scheduleDailyReminderNotification(hour, minute)
                            } else {
                                getNotificationScheduler().cancelDailyReminderNotification()
                            }
                        }
                    )

                    SettingItem(
                        label = "Dark Mode",
                        isToggle = true,
                        toggled = darkModeEnabled,
                        onToggleChange = { DarkModeSettings.toggleDarkMode(it) }
                    )
                }
            }

            item {
                SettingsSection(title = "Daily Reminder Time") {
                    OutlinedTextField(
                        value = hourText.value,
                        onValueChange = { newValue ->
                            if (newValue.toIntOrNull() in 0..23 || newValue.isEmpty()) {
                                hourText.value = newValue
                            }
                        },
                        label = { Text("Hour (0–23)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { minuteFocusRequester.requestFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(hourFocusRequester)
                            .bringIntoViewRequester(hourBringIntoView)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    coroutineScope.launch {
                                        hourBringIntoView.bringIntoView()
                                    }
                                }
                            }
                    )

                    OutlinedTextField(
                        value = minuteText.value,
                        onValueChange = { newValue ->
                            if (newValue.toIntOrNull() in 0..59 || newValue.isEmpty()) {
                                minuteText.value = newValue
                            }
                        },
                        label = { Text("Minute (0–59)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(minuteFocusRequester)
                            .bringIntoViewRequester(minuteBringIntoView)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    coroutineScope.launch {
                                        minuteBringIntoView.bringIntoView()
                                    }
                                }
                            }
                    )

                    Button(
                        onClick = {
                            val h = hourText.value.toIntOrNull() ?: 9
                            val m = minuteText.value.toIntOrNull() ?: 0
                            UserPreferences.setReminderTime(h, m)
                            getNotificationScheduler().scheduleDailyReminderNotification(h, m)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Reminder Time")
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            item {
                SettingsSection(title = "Account") {
                    SettingItem(
                        label = "Account Settings",
                        onClick = { /* future logic */ }
                    )
                    SettingItem(
                        label = "Log Out",
                        onClick = {
                            authService.logout()
                            UserPreferences.logout()
                            onLogout()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingItem(
    label: String,
    isToggle: Boolean = false,
    toggled: Boolean = false,
    onToggleChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isToggle && onToggleChange != null) {
                    onToggleChange(!toggled) // toggle the value
                } else {
                    onClick?.invoke()
                }
            }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (isToggle && onToggleChange != null) {
            Switch(
                checked = toggled,
                onCheckedChange = onToggleChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                )
            )
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "$label setting",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content,

            )
        }
    }
}

