package com.miauto.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat

/** Lee la canción activa (Bluetooth u otra app) y controla anterior / play-pausa / siguiente. */
object MediaBridge {
    var controller: MediaController? = null
    val listeners = ArrayList<() -> Unit>()
    private var sessListener: MediaSessionManager.OnActiveSessionsChangedListener? = null

    private val cb = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) { fire() }
        override fun onMetadataChanged(metadata: MediaMetadata?) { fire() }
    }

    val playing: Boolean
        get() = controller?.playbackState?.state == PlaybackState.STATE_PLAYING

    private fun fire() = listeners.toList().forEach { it() }

    fun hasAccess(ctx: Context) =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    fun start(ctx: Context) {
        val app = ctx.applicationContext
        if (!hasAccess(app)) { pick(null); return }
        try {
            val m = app.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val cn = ComponentName(app, MediaListener::class.java)
            sessListener?.let { m.removeOnActiveSessionsChangedListener(it) }
            val l = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }
            m.addOnActiveSessionsChangedListener(l, cn)
            sessListener = l
            pick(m.getActiveSessions(cn))
        } catch (e: Exception) {
            pick(null)
        }
    }

    private fun pick(list: List<MediaController>?) {
        val best = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list?.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(cb)
            controller = best
            best?.registerCallback(cb)
        }
        fire()
    }

    fun title(): String {
        val md = controller?.metadata ?: return ""
        val t = md.getString(MediaMetadata.METADATA_KEY_TITLE)
        val a = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
        return listOfNotNull(t, a).filter { it.isNotBlank() }.joinToString(" — ")
    }

    fun prev(ctx: Context) {
        val c = controller
        if (c != null) c.transportControls.skipToPrevious() else key(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun next(ctx: Context) {
        val c = controller
        if (c != null) c.transportControls.skipToNext() else key(ctx, KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun playPause(ctx: Context) {
        val c = controller
        if (c != null) {
            if (playing) c.transportControls.pause() else c.transportControls.play()
        } else key(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    /** Sin sesión activa: manda la tecla multimedia (a la app Bluetooth configurada, si la hay). */
    private fun key(ctx: Context, code: Int) {
        val pkg = Prefs.btPkg(ctx)
        val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        for (action in intArrayOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            val ev = KeyEvent(action, code)
            if (pkg.isNotBlank()) {
                val i = Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(pkg)
                i.putExtra(Intent.EXTRA_KEY_EVENT, ev)
                ctx.sendBroadcast(i)
            } else audio.dispatchMediaKeyEvent(ev)
        }
    }
}
