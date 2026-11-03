package com.example.ada
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class IslandListener : NotificationListenerService() {
    private val cb = object : MediaController.Callback() {
        override fun onMetadataChanged(m: MediaMetadata?) {
            m ?: return
            IslandBus.onEvent?.invoke("♪ " + (m.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Çalıyor"),
                m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "")
        }
    }
    override fun onListenerConnected() {
        val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val me = ComponentName(this, IslandListener::class.java)
        fun pick(l: List<MediaController>?) {
            IslandBus.controller?.unregisterCallback(cb)
            IslandBus.controller = l?.firstOrNull()?.also { it.registerCallback(cb) }
        }
        pick(msm.getActiveSessions(me))
        msm.addOnActiveSessionsChangedListener({ pick(it) }, me)
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.isOngoing || sbn.packageName == packageName) return
        val e = sbn.notification.extras
        val t = e.getCharSequence("android.title")?.toString() ?: return
        IslandBus.onEvent?.invoke(t, e.getCharSequence("android.text")?.toString() ?: "")
    }
}
