package com.example.ada
import android.media.session.MediaController

object IslandBus {
    var onEvent: ((String, String) -> Unit)? = null   // başlık, alt metin
    var controller: MediaController? = null
}
