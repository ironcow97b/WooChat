package com.kcw.woochat.dataclass

data class ChatData(
    val sender: String,
    val receiver: String,
    val message: String,
    val time: String,
    val isChecked: Boolean
)
