package com.example.sendbirddemo.utils

import org.json.JSONObject

data class UrlPreviewInfo(
    private val mUrl: String? = null,
    private val mSiteName: String? = null,
    private val mTitle: String? = null,
    private val mDescription: String? = null,
    private val mImageUrl: String? = null
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


