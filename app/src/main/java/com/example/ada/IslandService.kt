package com.example.ada
import android.animation.ValueAnimator
import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraManager
import android.os.*
import android.view.*
import android.widget.*

class IslandService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var lp: WindowManager.LayoutParams
    private lateinit var root: LinearLayout
    private lateinit var title: TextView
    private lateinit var sub: TextView
    private lateinit var controls: LinearLayout
    private val h = Handler(Looper.getMainLooper())
    private var expanded = false
    private var torch = false
    private var timer: CountDownTimer? = null
    private val collapse = Runnable { setExpanded(false) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        val ch = "ada"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(ch, "Ada Island", NotificationManager.IMPORTANCE_MIN))
        startForeground(1, Notification.Builder(this, ch).setContentTitle("Ada Island çalışıyor")
            .setSmallIcon(android.R.drawable.ic_menu_info_details).build())

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        lp = WindowManager.LayoutParams(dp(120), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT)
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        val sb = resources.getIdentifier("status_bar_height", "dimen", "android")
        lp.y = if (sb > 0) resources.getDimensionPixelSize(sb) else dp(28)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(6), dp(14), dp(6))
            background = GradientDrawable().apply { setColor(Color.BLACK); cornerRadius = dp(22).toFloat() }
            setOnClickListener { setExpanded(!expanded) }
        }
        title = TextView(this).apply { setTextColor(Color.WHITE); textSize = 13f; maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END; gravity = Gravity.CENTER }
        sub = TextView(this).apply { setTextColor(0xFFAAAAAA.toInt()); textSize = 11f; maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END; gravity = Gravity.CENTER; visibility = View.GONE }
        controls = LinearLayout(this).apply { gravity = Gravity.CENTER; visibility = View.GONE }
        fun btn(t: String, a: () -> Unit) = TextView(this).apply {
            text = t; textSize = 20f; setPadding(dp(14), dp(6), dp(14), dp(6)); setOnClickListener { a() } }
        controls.addView(btn("⏮") { IslandBus.controller?.transportControls?.skipToPrevious() })
        controls.addView(btn("⏯") {
            val c = IslandBus.controller
            if (c?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING)
                c.transportControls.pause() else c?.transportControls?.play() })
        controls.addView(btn("⏭") { IslandBus.controller?.transportControls?.skipToNext() })
        controls.addView(btn("🔦") { toggleTorch() })
        controls.addView(btn("⏱") { startTimer(5) })
        root.addView(title); root.addView(sub); root.addView(controls)
        title.text = ""
        wm.addView(root, lp)

        IslandBus.onEvent = { t, s -> h.post { flash(t, s) } }
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val b = getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                if (i.action == Intent.ACTION_POWER_CONNECTED) flash("⚡ Şarj oluyor", "%$b")
            }
        }, IntentFilter(Intent.ACTION_POWER_CONNECTED))
    }

    private fun flash(t: String, s: String) {
        title.text = t; sub.text = s
        sub.visibility = if (s.isEmpty()) View.GONE else View.VISIBLE
        setExpanded(true, false)
        h.removeCallbacks(collapse); h.postDelayed(collapse, 4000)
    }

    private fun setExpanded(e: Boolean, showControls: Boolean = e) {
        expanded = e
        controls.visibility = if (e && showControls) View.VISIBLE else View.GONE
        if (!e) { sub.visibility = View.GONE; if (timer == null) title.text = "" }
        val to = if (e) dp(320) else dp(120)
        ValueAnimator.ofInt(lp.width, to).apply {
            duration = 250; interpolator = android.view.animation.OvershootInterpolator(0.8f)
            addUpdateListener { lp.width = it.animatedValue as Int; if (root.isAttachedToWindow) wm.updateViewLayout(root, lp) }
        }.start()
        if (root.isAttachedToWindow) wm.updateViewLayout(root, lp)
    }

    private fun toggleTorch() {
        val cm = getSystemService(CameraManager::class.java)
        torch = !torch
        try { cm.setTorchMode(cm.cameraIdList[0], torch) } catch (_: Exception) {}
    }

    private fun startTimer(min: Int) {
        timer?.cancel()
        timer = object : CountDownTimer(min * 60_000L, 1000) {
            override fun onTick(ms: Long) { title.text = "⏱ %d:%02d".format(ms / 60000, ms / 1000 % 60) }
            override fun onFinish() { timer = null; flash("⏱ Süre doldu", "") }
        }.start()
        setExpanded(false)
    }

    override fun onDestroy() {
        IslandBus.onEvent = null; timer?.cancel(); h.removeCallbacksAndMessages(null)
        if (root.isAttachedToWindow) wm.removeView(root)
    }
}
