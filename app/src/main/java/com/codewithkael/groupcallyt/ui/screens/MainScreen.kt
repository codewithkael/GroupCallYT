package com.codewithkael.groupcallyt.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.codewithkael.groupcallyt.utils.Constants

@Composable
fun MainScreen(navController: NavController) {

    val roomName = remember { mutableStateOf("") }
    val userName = remember { mutableStateOf("") }

    val context = LocalContext.current

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.all { it.value }) {
            Toast.makeText(
                context,
                "Camera And Microphone permissions are required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(key1 = Unit) {
        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CAMERA
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("Welcome To Group Call App", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))
        BasicTextField(
            value = roomName.value,
            onValueChange = { roomName.value = it },
            decorationBox = { innerTextField ->
                Row(
                    Modifier
                        .border(1.dp, Color.Gray, shape = RectangleShape)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    if (roomName.value.isEmpty()) {
                        Text("Room Name", color = Color.Gray)
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        BasicTextField(
            value = userName.value,
            onValueChange = { userName.value = it },
            decorationBox = { innerTextField ->
                Row(
                    Modifier
                        .border(1.dp, Color.Gray, shape = RectangleShape)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    if (userName.value.isEmpty()) {
                        Text("User Name", color = Color.Gray)
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Button(onClick = {
            //navigate to video conference screen later
            navController.navigate(
                Constants.getRoomScreen(roomName.value,userName.value)
            )
        }) {
            Text("Join Room")
        }
    }
}