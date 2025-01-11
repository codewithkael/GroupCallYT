package com.codewithkael.groupcallyt.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.codewithkael.groupcallyt.ui.components.SurfaceViewRendererComposable
import com.codewithkael.groupcallyt.ui.viewmodels.MainViewModel

@Composable
fun RoomScreen(roomName: String?, userName: String?) {
    val viewModel: MainViewModel = hiltViewModel()
    val mediaStreamsMap = viewModel.mediaStreamsMap.value

    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        //local surface or holder of the local stream ( camera )
        SurfaceViewRendererComposable(
            username = userName?:"",
            Modifier.fillMaxWidth()
                .height(250.dp)
        ) {  localSurface ->
            viewModel.joinRoom(localSurface,roomName!!,userName!!)
        }

        Spacer(Modifier.height(20.dp))
        mediaStreamsMap.forEach { (name, mediaStream) ->
            SurfaceViewRendererComposable(
                username = name,
                modifier = Modifier.fillMaxWidth()
                    .height(250.dp)
            ) { remoteSurface->
                viewModel.prepareRemoteSurface(remoteSurface)
                mediaStream.videoTracks[0]?.addSink(remoteSurface)
            }
        }
    }

}