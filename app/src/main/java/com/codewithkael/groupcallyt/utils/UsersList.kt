package com.codewithkael.groupcallyt.utils

data class UsersList(
    val users:List<UserModel>
)

data class UserModel(
    val userName:String,
    val userId:String
)