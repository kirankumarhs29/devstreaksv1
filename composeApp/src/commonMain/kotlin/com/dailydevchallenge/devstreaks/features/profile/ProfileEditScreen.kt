package com.dailydevchallenge.devstreaks.features.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.ProfileViewModel
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.mohamedrejeb.calf.core.LocalPlatformContext
import com.mohamedrejeb.calf.io.readByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import com.mohamedrejeb.calf.picker.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ProfileEditScreen(
    viewModel: ProfileEditViewModel = koinInject(),
    onSaveSuccess: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    var username by remember { mutableStateOf(user?.username ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val context = LocalPlatformContext.current
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(user) {
        user?.let {
            username = it.username
            email = it.email
        }
    }

    val filePickerLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.Image,
        selectionMode = FilePickerSelectionMode.Single,
        onResult = { files ->
            val file = files.firstOrNull() ?: return@rememberFilePickerLauncher
            coroutineScope.launch {
                val bytes = withContext(Dispatchers.Default) {
                    file.readByteArray(context)
                }
                selectedImageBytes = bytes
                viewModel.onAvatarSelected(bytes)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .clickable { filePickerLauncher.launch() }
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("Tap to change", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Username
        OutlinedTextField(
            value = username,
            onValueChange = { viewModel.onUsernameChange(it) },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.onEmailChange(it) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.saveProfile {
                    // Handle success — show snackbar, navigate back, etc.
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
        if (viewModel.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = viewModel.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
