package org.ligi.blexplorer.ui

import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.polidea.rxandroidble2.RxBleDevice
import com.uber.autodispose.AutoDispose.autoDisposable
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import net.steamcrafted.loadtoast.LoadToast
import org.ligi.blexplorer.R
import org.ligi.blexplorer.bluetoothController
import org.ligi.blexplorer.databinding.ActivityWithRecyclerBinding
import org.ligi.blexplorer.databinding.ItemServiceBinding
import org.ligi.blexplorer.util.*
import org.ligi.blexplorer.util.KEY_BLUETOOTH_DEVICE
import timber.log.Timber


class DeviceServiceExploreActivity : AppCompatActivity() {

    private val  binding by lazy { ActivityWithRecyclerBinding.inflate(layoutInflater) }
    private lateinit var device: BluetoothDevice
    private var gattServicesListDisposable : Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!intent.hasExtra(KEY_BLUETOOTH_DEVICE)) {
            finish()
            return
        }
        device = intent.obtainParcelableExtra<BluetoothDevice>(KEY_BLUETOOTH_DEVICE) as BluetoothDevice

        setContentView(binding.root)
        supportActionBar?.run {
            subtitle = device.nameOrAddressAsFallback()
            setDisplayHomeAsUpEnabled(true)
        }
        val adapter = ServiceRecycler(device)
        binding.contentList.adapter = adapter

        val rxbleDevice = bluetoothController.getDeviceInfo(device)?.scanResult?.bleDevice
        rxbleDevice ?: run {
            finish()
            return
        }

        val loadToast = LoadToast(this)

        //keeping the BLE device connection open so that actions in CharacteristicActivity are faster
        bluetoothController.getConnection(rxbleDevice)
                .`as`(autoDisposable(from(this, ON_DESTROY)))
                .subscribe({}, {})

        gattServicesListDisposable = Completable.fromAction { loadToast.setText(getString(R.string.connecting)).show() }
                .subscribeOn(AndroidSchedulers.mainThread())
                .andThen(bluetoothController.getConnection(rxbleDevice))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    Completable.fromAction { loadToast.setText(getString(R.string.discovering)) }
                            .subscribeOn(AndroidSchedulers.mainThread()).subscribe()
                }.flatMapSingle { it.discoverServices() }
                .take(1)
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(autoDisposable(from(this, ON_DESTROY)))
                .subscribe(
                        { services ->
                            adapter.submitList(services.bluetoothGattServices)
                            loadToast.success()
                        },
                        { throwable ->
                            Timber.e(throwable, "Failed to discover services for device ${rxbleDevice.bluetoothDevice.name}")
                            supportFragmentManager.beginTransaction().add(BLEDeviceConnectionFailedDialog(rxbleDevice), BLEDeviceConnectionFailedDialog.TAG).commit()
                        }
                )

        supportFragmentManager.beginTransaction()
                .add(ExitActivityOnBluetoothDisable(), null)
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        fun createIntent(context : Context, device : BluetoothDevice) : Intent = Intent(context, DeviceServiceExploreActivity::class.java)
                .putExtra(KEY_BLUETOOTH_DEVICE, device)
    }
}

private class ServiceRecycler(private val device: BluetoothDevice) : ListAdapter<BluetoothGattService, ServiceViewHolder>(BluetoothGattServiceDiffCallback()) {
    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): ServiceViewHolder {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        val binding = ItemServiceBinding.inflate(layoutInflater, viewGroup, false)
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(deviceViewHolder: ServiceViewHolder, position: Int) {
        val service = getItem(position)
        deviceViewHolder.applyService(device, service)
    }
}

private class BluetoothGattServiceDiffCallback : DiffUtil.ItemCallback<BluetoothGattService>() {
    override fun areItemsTheSame(oldItem: BluetoothGattService, newItem: BluetoothGattService) = oldItem.uuid == newItem.uuid

    override fun areContentsTheSame(oldItem: BluetoothGattService, newItem: BluetoothGattService) = true
}

private class ServiceViewHolder(private val binding: ItemServiceBinding) : RecyclerView.ViewHolder(binding.root) {
    fun applyService(device: BluetoothDevice, service: BluetoothGattService) {
        itemView.setOnClickListener { v ->
            val intent = CharacteristicActivity.createIntent(v.context, device, service)
            v.context.startActivity(intent)
        }
        binding.uuid.text = service.uuid.toString()
        binding.type.setText(service.serviceTypeDesc())
        binding.name.text = service.name(context.getString(R.string.unknown))
    }
}

class BLEDeviceConnectionFailedDialog(private val rxbleDevice: RxBleDevice) : AppCompatDialogFragment() {
    companion object {
        const val TAG = "exit_on_dismiss_alert_dialog"
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        requireActivity().finish()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().finish()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
                .setTitle(R.string.error)
                .setMessage(getString(R.string.device_connect_failed_dialog_text, rxbleDevice.bluetoothDevice.name))
                .setPositiveButton(android.R.string.ok) { _,_  -> requireActivity().finish() }
                .create()
    }
}