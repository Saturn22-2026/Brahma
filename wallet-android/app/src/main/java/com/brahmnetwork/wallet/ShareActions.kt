package com.brahmnetwork.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.Toast
import com.brahmnetwork.wallet.sentinel.SentinelRuntime
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object ShareActions {
    fun inviteMessage(address: String, nodeBase: String = Constants.RPC_URL): String? {
        if (!CryptoManager.isChecksummedAddress(address)) return null
        val link = Constants.shareClaimUrl(address, nodeBase)
        if (!link.startsWith("https://") || Constants.isPrivateLanRpc(link)) return null
        if (!CryptoManager.isSafeClaimUrl(link)) return null
        return link
    }

    fun shareInvite(
        context: Context,
        address: String,
        nodeBase: String,
        title: String = "Share Brahma Coin invite",
        signedFields: Map<String, String>? = null
    ) {
        val link = inviteMessage(address, nodeBase)
        if (link == null) {
            Toast.makeText(
                context,
                "Need a checksummed Brahma Coin address before sharing",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val mesh = if (signedFields != null) SentinelRuntime.gossipInvite(signedFields) else false
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Brahma Coin invite", link))
        } catch (_: Exception) {
        }
        val opened = try {
            shareText(context, link, title)
            true
        } catch (_: Exception) {
            false
        }
        Toast.makeText(
            context,
            when {
                !opened -> "Invite copied. The share sheet did not open."
                mesh -> "Invite link copied. Sentinel invite is on the LAN mesh."
                else -> "Invite link copied."
            },
            Toast.LENGTH_LONG
        ).show()
    }

    fun shareText(context: Context, text: String, title: String = "Share Brahma Coin invite") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TITLE, title)
        }
        val chooser = Intent.createChooser(intent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun qrBitmap(text: String, size: Int = 512): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
