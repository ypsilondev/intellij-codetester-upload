package com.github.yniklas.intellijcodetesterupload.data

import com.google.gson.JsonObject

data class TestCase(val id: Int, val name: String, val creator: String, val checkType: String,
val approved: Boolean, val creationTime: Long, val catId: Int, val catName: String)
