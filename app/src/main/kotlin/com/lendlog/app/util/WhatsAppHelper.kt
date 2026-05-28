package com.lendlog.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsAppHelper {

    fun sendNudge(context: Context, phone: String, itemName: String) {
        val cleanPhone = phone.replace(Regex("[^\\d+]"), "")
        val message = "Hey! Just a reminder — you still have my $itemName. Would love to get it back soon 😊"
        val url = "https://wa.me/$cleanPhone?text=${Uri.encode(message)}"

        // Try WhatsApp then WhatsApp Business; fall back to SMS (not browser).
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { setPackage(pkg) })
                return
            } catch (_: ActivityNotFoundException) { }
        }
        SmsHelper.openSmsApp(context, phone, itemName)
    }
}
