package com.example.sendbirddemo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.example.sendbirddemo.utils.SyncManagerUtils.getMyUserId
import com.sendbird.android.GroupChannel
import java.text.SimpleDateFormat
import java.util.*

object Utils {

    fun getGroupChannelTitle(context: Context, channel: GroupChannel): String? {
        val members = channel.members
        val myUserId: String? = getMyUserId(context)
        return if (members.size < 2 || myUserId == null) {
            "No Members"
        } else if (members.size == 2) {
            val names = StringBuffer()
            for (member in members) {
                if (member.userId == myUserId) {
                    continue
                }
                names.append(", " + member.nickname)
            }
            names.delete(0, 2).toString()
        } else {
            var count = 0
            val names = StringBuffer()
            for (member in members) {
                if (member.userId == myUserId) {
                    continue
                }
                count++
                names.append(", " + member.nickname)
                if (count >= 10) {
                    break
                }
            }
            names.delete(0, 2).toString()
        }
    }

    /**
     * Gets timestamp in millis and converts it to HH:mm (e.g. 16:44).
     */
    fun formatTime(timeInMillis: Long): String? {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(timeInMillis)
    }

    fun formatTimeWithMarker(timeInMillis: Long): String? {
        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        return dateFormat.format(timeInMillis)
    }

    fun getHourOfDay(timeInMillis: Long): Int {
        val dateFormat = SimpleDateFormat("H", Locale.getDefault())
        return Integer.valueOf(dateFormat.format(timeInMillis))
    }

    fun getMinute(timeInMillis: Long): Int {
        val dateFormat = SimpleDateFormat("m", Locale.getDefault())
        return Integer.valueOf(dateFormat.format(timeInMillis))
    }

    /**
     * If the given time is of a different date, display the date.
     * If it is of the same date, display the time.
     * @param timeInMillis  The time to convert, in milliseconds.
     * @return  The time or date.
     */
    fun formatDateTime(timeInMillis: Long): String? {
        return if (isToday(timeInMillis)) {
            formatTime(timeInMillis)
        } else {
            formatDate(timeInMillis)
        }
    }

    /**
     * Formats timestamp to 'date month' format (e.g. 'February 3').
     */
    fun formatDate(timeInMillis: Long): String? {
        val dateFormat = SimpleDateFormat("MMMM dd", Locale.getDefault())
        return dateFormat.format(timeInMillis)
    }

    /**
     * Returns whether the given date is today, based on the user's current locale.
     */
    fun isToday(timeInMillis: Long): Boolean {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val date = dateFormat.format(timeInMillis)
        return date == dateFormat.format(System.currentTimeMillis())
    }

    /**
     * Checks if two dates are of the same day.
     * @param millisFirst   The time in milliseconds of the first date.
     * @param millisSecond  The time in milliseconds of the second date.
     * @return  Whether {@param millisFirst} and {@param millisSecond} are off the same day.
     */
    fun hasSameDate(millisFirst: Long, millisSecond: Long): Boolean {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return dateFormat.format(millisFirst) == dateFormat.format(millisSecond)
    }

    /**
     * Displays an image from a URL in an ImageView.
     */
    fun displayImageFromUrl(
        context: Context?, url: String?,
        imageView: ImageView?, placeholderDrawable: Drawable?, listener: RequestListener<Drawable>?
    ) {
        val myOptions = RequestOptions()
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .placeholder(placeholderDrawable)
        if (listener != null) {
            Glide.with(context!!)
                .load(url)
                .apply(myOptions)
                .listener(listener)
                .into(imageView!!)
        } else {
            Glide.with(context!!)
                .load(url)
                .apply(myOptions)
                .listener(listener)
                .into(imageView!!)
        }
    }

    /**
     * Crops image into a circle that fits within the ImageView.
     */
    fun displayRoundImageFromUrl(context: Context, url: String?, imageView: ImageView) {
        val myOptions = RequestOptions()
            .centerCrop()
            .dontAnimate()
        Glide.with(context)
            .asBitmap()
            .apply(myOptions)
            .load(url)
            .into(object : BitmapImageViewTarget(imageView) {
                override fun setResource(resource: Bitmap?) {
                    val circularBitmapDrawable =
                        RoundedBitmapDrawableFactory.create(context.resources, resource)
                    circularBitmapDrawable.isCircular = true
                    imageView.setImageDrawable(circularBitmapDrawable)
                }
            })
    }

    fun displayImageFromUrl(
        context: Context?, url: String?,
        imageView: ImageView?
    ) {
        displayImageFromUrl(
            context,
            url,
            imageView,
            null,
            null
        )
    }

    /**
     * Displays an image from a URL in an ImageView.
     */
    fun displayGifImageFromUrl(
        context: Context?,
        url: String?,
        imageView: ImageView?,
        placeholderDrawable: Drawable?,
        listener: RequestListener<GifDrawable>?
    ) {
        val myOptions = RequestOptions()
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .placeholder(placeholderDrawable)
        if (listener != null) {
            Glide.with(context!!)
                .asGif()
                .load(url)
                .apply(myOptions)
                .listener(listener)
                .into(imageView!!)
        } else {
            Glide.with(context!!)
                .asGif()
                .load(url)
                .apply(myOptions)
                .into(imageView!!)
        }
    }

    fun displayGifImageFromUrl(
        context: Context?,
        url: String?,
        imageView: ImageView?,
        thumbnailUrl: String?
    ) {
        displayGifImageFromUrl(context, url, imageView, thumbnailUrl, null)
    }

    /**
     * Displays an GIF image from a URL in an ImageView.
     */
    fun displayGifImageFromUrl(
        context: Context?,
        url: String?,
        imageView: ImageView?,
        thumbnailUrl: String?,
        placeholderDrawable: Drawable?
    ) {
        val myOptions = RequestOptions()
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .placeholder(placeholderDrawable)
        if (thumbnailUrl != null) {
            Glide.with(context!!)
                .asGif()
                .load(url)
                .apply(myOptions)
                .thumbnail(Glide.with(context).asGif().load(thumbnailUrl))
                .into(imageView!!)
        } else {
            Glide.with(context!!)
                .asGif()
                .load(url)
                .apply(myOptions)
                .into(imageView!!)
        }
    }
}