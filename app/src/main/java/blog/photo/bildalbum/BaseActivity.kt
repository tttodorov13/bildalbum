package blog.photo.bildalbum

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import blog.photo.bildalbum.receiver.ConnectivityReceiver

// THIS IS THE BASE ACTIVITY OF ALL ACTIVITIES OF THE APPLICATION
@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), ConnectivityReceiver.ConnectivityReceiverListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(ConnectivityReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onResume() {
        super.onResume()
        ConnectivityReceiver.connectivityReceiverListener = this
    }

    /**
     * Callback will be called when there is network change
     */
    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        if (isConnected) {
            showMessage(getString(R.string.internet_connection))
        } else {
            showMessage(getString(R.string.no_internet_connection))
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}