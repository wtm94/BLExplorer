package org.ligi.blexplorer.util

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.utils.StandardUUIDsParser
import org.ligi.blexplorer.R

object DevicePropertiesDescriber {
    private val property2stringMap = mapOf(
            BluetoothGattCharacteristic.PROPERTY_BROADCAST to "boadcast",
            BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS to "extended",
            BluetoothGattCharacteristic.PROPERTY_INDICATE to "indicate",
            BluetoothGattCharacteristic.PROPERTY_NOTIFY to "notify",
            BluetoothGattCharacteristic.PROPERTY_READ to "read",
            BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE to "signed write",
            BluetoothGattCharacteristic.PROPERTY_WRITE to "write",
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE to "write no response"
    )

    fun getProperty(from: BluetoothGattCharacteristic): String {
        return property2stringMap.keys
            .filter { from.properties and it > 0 }
            .map { property2stringMap[it] }
            .joinToString(",")
            .ifEmpty { "no property" }
    }
}

internal fun RxBleConnection.RxBleConnectionState.stringDesc(context: Context) = when (this) {
    RxBleConnection.RxBleConnectionState.DISCONNECTED -> context.getString(R.string.disconnected)
    RxBleConnection.RxBleConnectionState.CONNECTING -> context.getString(R.string.connecting)
    RxBleConnection.RxBleConnectionState.CONNECTED -> context.getString(R.string.connected)
    RxBleConnection.RxBleConnectionState.DISCONNECTING -> context.getString(R.string.disconnecting)
}

internal fun BluetoothGattCharacteristic.name(defaultString: String): String = StandardUUIDsParser.getCharacteristicName(uuid) ?: defaultString

internal fun BluetoothGattService.serviceTypeDesc() = when (type) {
    BluetoothGattService.SERVICE_TYPE_PRIMARY -> R.string.primary
    BluetoothGattService.SERVICE_TYPE_SECONDARY -> R.string.secondary
    else -> R.string.unknown_service_type
}

internal fun BluetoothGattService.name(defaultString: String): String {
    return StandardUUIDsParser.getServiceName(uuid) ?: defaultString
}

internal fun BluetoothDevice.nameOrAddressAsFallback() = if(name.isNullOrEmpty()) address else name

internal fun BluetoothDevice.describeBondState() = when (bondState) {
    BluetoothDevice.BOND_NONE -> "not bonded"
    BluetoothDevice.BOND_BONDING -> "bonding"
    BluetoothDevice.BOND_BONDED -> "bonded"
    else -> "unknown bondstate"
}

internal fun BluetoothDevice.describeType() = when(type) {
    BluetoothDevice.DEVICE_TYPE_CLASSIC -> "classic"
    BluetoothDevice.DEVICE_TYPE_DUAL -> "dual"
    BluetoothDevice.DEVICE_TYPE_LE -> "LE"
    BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "unknown device type"
    else -> "unknown device type"
}

internal fun BluetoothGattCharacteristic.getPermission() = when (permissions) {
    BluetoothGattCharacteristic.PERMISSION_READ -> "read"
    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED -> "read encrypted"
    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM -> "read encrypted mitm"
    BluetoothGattCharacteristic.PERMISSION_WRITE -> "write"
    BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED -> "write encrypted"
    BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM -> "write encrypted mitm"
    BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED -> "write signed"
    BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM -> "write signed mitm"
    else -> "unknown permission $permissions"
}