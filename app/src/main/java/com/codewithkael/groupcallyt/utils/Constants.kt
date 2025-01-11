package com.codewithkael.groupcallyt.utils

object Constants {
    const val MAIN_SCREEN = "MainScreen"
    const val ROOM_SCREEN = "RoomScreen/{roomName}/{userName}"
    fun getRoomScreen(roomName:String,userName:String) =
        "RoomScreen/$roomName/$userName"

    fun getSignallingServerUrl() = "http://95.217.13.89:3300"

    private const val BASE_URL = "95.217.13.89"
    fun getPublishStreamPath(id:String) = "ws://$BASE_URL:3333/app/$id?direction=send"
    fun getStreamPath(id:String)="ws://$BASE_URL:3333/app/$id"

}