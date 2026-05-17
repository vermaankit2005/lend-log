package com.lendlog.app.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactPickerHelper {

    data class ContactResult(
        val name: String?,
        val phone: String?,
        val contactId: String?
    )

    fun resolveContact(context: Context, contactUri: Uri): ContactResult {
        var name: String? = null
        var phone: String? = null
        var contactId: String? = null

        context.contentResolver.query(
            contactUri,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            ),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
            }
        }

        contactId?.let { id ->
            try {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        phone = cursor.getString(
                            cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )
                    }
                }
            } catch (_: SecurityException) {
                // READ_CONTACTS not granted; phone stays null, name is still usable
            }
        }

        return ContactResult(name, phone, contactId)
    }
}
