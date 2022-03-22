package com.example.sendbirddemo.utils

import android.content.Context
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource

class ExoPlayerUtils {

    private var mSimpleExoPlayer: SimpleExoPlayer? = null

    fun initPlayer(context: Context, playerView: PlayerView, url: String) {
        if (mSimpleExoPlayer != null){
            mSimpleExoPlayer!!.release()
            mSimpleExoPlayer = null
        }
        mSimpleExoPlayer = SimpleExoPlayer.Builder(context).build()
        playerView.player = mSimpleExoPlayer
        mSimpleExoPlayer!!.playWhenReady = true
        mSimpleExoPlayer!!.setMediaSource(buildMediaSource(url))
        mSimpleExoPlayer!!.prepare()
    }

    fun releasePlayer() {
        if (mSimpleExoPlayer == null) {
            return
        } else {
            mSimpleExoPlayer!!.release()
            mSimpleExoPlayer = null
        }
    }

    private fun buildMediaSource(url: String): MediaSource {
        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))
    }
}