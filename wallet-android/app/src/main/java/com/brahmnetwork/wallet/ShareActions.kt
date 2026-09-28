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
        val base = Constants.shareableNodeBase(nodeBase)
        if (!base.startsWith("http://") && !base.startsWith("https://")) return null
        return Constants.shareClaimUrl(address, base)
    }

    fun shareInvite(
        context: Context,
        address: String,
        nodeBase: String,
        title: String = "Share Brahma Coin invite",
        signedFields: Map<String, String>? = null
    ) {
        val link = inviteMessage(address, nodeBase)
        if (link == null || !CryptoManager.isSafeClaimUrl(link)) {
            Toast.makeText(
                context,
                "Need a checksummed Brahma Coin address and a valid node URL before sharing",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val mesh = if (signedFields != null) SentinelRuntime.gossipInvite(signedFields) else false
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Brahma Coin invite", link))
        shareText(context, link, title)
        Toast.makeText(
            context,
            if (mesh) "Link copied. Sentinel invite is on the LAN mesh."
            else "Link copied. Open the share sheet or scan the QR.",
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
