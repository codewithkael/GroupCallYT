package com.codewithkael.groupcallyt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codewithkael.groupcallyt.ui.screens.MainScreen
import com.codewithkael.groupcallyt.ui.screens.RoomScreen
import com.codewithkael.groupcallyt.utils.Constants.MAIN_SCREEN
import com.codewithkael.groupcallyt.utils.Constants.ROOM_SCREEN

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = MAIN_SCREEN){
        composable(MAIN_SCREEN){
            MainScreen(navController)
        }
        composable(ROOM_SCREEN,
            arguments = listOf(
                navArgument("roomName") { type = NavType.StringType},
                navArgument("userName") { type = NavType.StringType}
            )
        ){
            val roomName = it.arguments?.getString("roomName")
            val userName = it.arguments?.getString("userName")
            RoomScreen(roomName,userName)
        }
    }
}