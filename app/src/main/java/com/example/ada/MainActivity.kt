package com.example.ada
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(48, 48, 48, 48) }
        fun add(t: String, a: () -> Unit) = l.addView(Button(this).apply { text = t; setOnClickListener { a() } })
        l.addView(TextView(this).apply { text = "Ada Island\n1) İzinleri ver  2) Başlat"; textSize = 18f; gravity = Gravity.CENTER })
        add("1. Ekran üstünde göster izni") { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
        add("2. Bildirim erişimi izni") { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
        add("3. Başlat") {
            if (Settings.canDrawOverlays(this)) startForegroundService(Intent(this, IslandService::class.java))
            else Toast.makeText(this, "Önce 1. izni ver", Toast.LENGTH_SHORT).show() }
        add("Durdur") { stopService(Intent(this, IslandService::class.java)) }
        setContentView(l)
    }
}
