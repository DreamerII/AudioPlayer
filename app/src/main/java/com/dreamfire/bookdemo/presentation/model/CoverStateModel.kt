package com.dreamfire.bookdemo.presentation.model

import android.graphics.Bitmap

data class CoverStateModel(
    val cover: Bitmap? = null,
    val isLoading: Boolean = false,
)