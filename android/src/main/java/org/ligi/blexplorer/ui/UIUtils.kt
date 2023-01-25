package org.ligi.blexplorer.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.recyclerview.widget.RecyclerView
import com.polidea.rxandroidble2.RxBleClient
import com.uber.autodispose.AutoDispose.autoDisposable
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import org.ligi.blexplorer.bluetoothController

internal const val BUG_REPORT_EMAIL = "sandy.8925@gmail.com"

class ExitActivityOnBluetoothDisable : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothController.bluetoothStateEvents
                .toFlowable(BackpressureStrategy.LATEST)
                .filter { it == RxBleClient.State.BLUETOOTH_NOT_ENABLED }
                .take(1)
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(autoDisposable(from(this, ON_DESTROY)))
                .subscribe({ activity?.finish() } , {})
    }
}

internal val RecyclerView.ViewHolder.context: Context
    get() = itemView.context

internal fun Context.shortToast(toastString: String) = Toast.makeText(this, toastString, Toast.LENGTH_SHORT).show()