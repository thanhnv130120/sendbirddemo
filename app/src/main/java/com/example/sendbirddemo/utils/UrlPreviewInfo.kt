package com.example.sendbirddemo.utils

import org.json.JSONObject

data class UrlPreviewInfo(
    val mUrl: String? = null,
    val mSiteName: String? = null,
    val mTitle: String? = null,
    val mDescription: String? = null,
    val mImageUrl: String? = null
) {
    fun toJsonString(): String {
        val jsonObject = JSONObject()
        jsonObject.put("url", mUrl)
        jsonObject.put("site_name", mSiteName)
        jsonObject.put("title", mTitle)
        jsonObject.put("description", mDescription)
        jsonObject.put("image", mImageUrl)
        return jsonObject.toString()
    }
}


