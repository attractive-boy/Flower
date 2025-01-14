import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

interface BarcodeListener {
    fun onBarcodeScanned(barcode: String)
}

class BarcodeReceiver(private val listener: BarcodeListener) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val barcode = intent?.getStringExtra("barcode")
        barcode?.let {
            listener.onBarcodeScanned(it) // 调用回调方法
        }
    }
}