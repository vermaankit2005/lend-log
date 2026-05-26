package com.lendlog.app.util

// AUTO_SMS_DISABLED: SmsManager import removed — requires SEND_SMS permission; re-enable when Play Store approved
// import android.telephony.SmsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
// import android.os.Build  // AUTO_SMS_DISABLED: only needed by sendAutoSms
import android.widget.Toast

object SmsHelper {

    private fun buildMessage(itemName: String) =
        "Hey! Just a reminder — you still have my $itemName. Would love to get it back soon 😊\n\nSent via LendLog"

    fun openSmsApp(context: Context, phone: String, itemName: String) {
        val cleanPhone = phone.replace(Regex("[^\\d+]"), "")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$cleanPhone")
            putExtra("sms_body", buildMessage(itemName))
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No SMS app found", Toast.LENGTH_SHORT).show()
        }
    }

    // AUTO_SMS_DISABLED: sends SMS programmatically via SmsManager — needs SEND_SMS permission
    // Re-enable after Play Store approves the SEND_SMS declaration.
    /*
    fun sendAutoSms(context: Context, phone: String, itemName: String) {
        val cleanPhone = phone.replace(Regex("[^\\d+]"), "")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
                    ?.sendTextMessage(cleanPhone, null, buildMessage(itemName), null, null)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
                    .sendTextMessage(cleanPhone, null, buildMessage(itemName), null, null)
            }
        } catch (_: Exception) { }
    }
    */
}
