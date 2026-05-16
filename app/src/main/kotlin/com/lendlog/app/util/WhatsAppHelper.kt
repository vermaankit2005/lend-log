package com.lendlog.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object WhatsAppHelper {

    fun sendNudge(context: Context, phone: String, itemName: String) {
        val cleanPhone = phone.replace(Regex("[^\\d+]"), "")
        val message = "Hey! Just a reminder — you still have my $itemName. Would love to get it back soon 😊"
        val encoded = Uri.encode(message)

        val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/$cleanPhone?text=$encoded")
            setPackage("com.whatsapp")
        }

        if (whatsappIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(whatsappIntent)
        } else {
            // Fallback: open WhatsApp web in browser
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$cleanPhone?text=$encoded")
            }
            if (fallback.resolveActivity(context.packageManager) != null) {
                context.startActivity(fallback)
            } else {
                Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
