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

    operator fun component1(result: ContactResult) = result.name
    operator fun component2(result: ContactResult) = result.phone
    operator fun component3(result: ContactResult) = result.contactId

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
        }

        return ContactResult(name, phone, contactId)
    }
}
