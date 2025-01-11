package com.codewithkael.groupcallyt.utils

data class SignalServerMessageModel(
    var command: String? = null,
    var data:Any?=null,
    var sender:String?=null
)