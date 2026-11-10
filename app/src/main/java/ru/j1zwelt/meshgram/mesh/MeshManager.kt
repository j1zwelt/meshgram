package ru.j1zwelt.meshgram.mesh

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import com.google.gson.Gson
import ru.j1zwelt.meshgram.model.Chat
import ru.j1zwelt.meshgram.model.Message
import ru.j1zwelt.meshgram.model.Message.Companion.toMessage
import ru.j1zwelt.meshgram.model.User.Companion.toUser
import java.io.IOException
import java.util.UUID
import kotlin.concurrent.thread

class MeshManager(val context: Context) {
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val discoveredDevices = HashSet<BluetoothDevice>()
    private var serverSocket: BluetoothServerSocket? = null
    private var isServerRunning = false

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    @Suppress("DEPRECATION") val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (ActivityCompat.checkSelfPermission(
                                context, Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) return

                        discoveredDevices.add(it)
                        Log.i("MeshManager", "onReceive: [${it.address}]")
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i("MeshManager", "onReceive: ACTION_DISCOVERY_FINISHED")
                }
            }
        }
    }

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
        Log.i("MeshManager", "registerReceiver()")
    }

    fun findDevices(onTurnBluetooth: () -> Unit, onTurnLocation: () -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("MeshManager", "findDevices: BLUETOOTH_SCAN != PERMISSION_GRANTED")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.w("MeshManager", "findDevices: Bluetooth выключен")
            onTurnBluetooth()
        }

        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            Log.w("MeshManager", "findDevices: GPS выключен")
            onTurnLocation()
        }

        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        registerReceiver()
        discoveredDevices.clear()

        val started = bluetoothAdapter.startDiscovery()
        Log.i("MeshManager", "findDevices: $started")
    }

    fun startServer(
        chats: SnapshotStateList<Chat>, sharedPref: SharedPreferences, onTurnBluetooth: () -> Unit
    ) {
        if (isServerRunning) return
        isServerRunning = true

        if (!bluetoothAdapter.isEnabled) {
            Log.w("MeshManager", "startServer: Bluetooth выключен")
            onTurnBluetooth()
        }

        thread(true) {
            try {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) return@thread

                serverSocket =
                    bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("MeshChat", uuid)
                Log.i("MeshManager", "startServer: Сервер запущен и слушает эфир...")

                while (isServerRunning) {
                    var socket: BluetoothSocket?
                    try {
                        socket = serverSocket?.accept()
                    } catch (e: IOException) {
                        Log.e("MeshManager", "startServer: ${e.message}")
                        break
                    }

                    socket?.let { currentSocket ->
                        thread {
                            currentSocket.use { s ->
                                try {
                                    val buffer = ByteArray(1024)
                                    val bytes = s.inputStream.read(buffer)

                                    if (bytes > 0) {
                                        val message =
                                            String(buffer, 0, bytes, Charsets.UTF_8).toMessage()
                                        val profile =
                                            sharedPref.getString("my_profile", "").toUser()

                                        if (message.toUser == profile.userName) {
                                            val copyMessage = message.copy(isFromMe = false)
                                            val existingChat =
                                                chats.find { it.user.userName == copyMessage.fromUser!!.userName }

                                            if (existingChat != null) {
                                                existingChat.messages.add(copyMessage)
                                            } else {
                                                val newMessagesList =
                                                    mutableStateListOf<Message>().apply {
                                                        add(copyMessage)
                                                    }
                                                val newChat = Chat(
                                                    copyMessage.fromUser!!, newMessagesList
                                                )
                                                chats.add(newChat)
                                            }

                                            Log.i(
                                                "MeshManager",
                                                "startServer: ПРИНЯТО СООБЩЕНИЕ ${copyMessage.text}"
                                            )
                                        } else {
//                                            sendMessage(message)

                                            Log.w(
                                                "MeshManager",
                                                "startServer: Принято чужое сообщение (${message.toUser}/${profile.userName})"
                                            )
                                        }
                                    } else if (bytes == -1) {
                                        Log.e(
                                            "MeshManager",
                                            "startServer: Соединение закрыто удаленным устройством"
                                        )
                                    }
                                } catch (e: IOException) {
                                    Log.e("MeshManager", "startServer: ${e.message} (socket)")
                                }
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("MeshManager", "startServer: ${e.message} (thread)")
            }
        }
    }

    fun sendMessage(message: Message) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("MeshManager", "BLUETOOTH_CONNECT != PERMISSION_GRANTED")
            return
        }

        discoveredDevices.forEach { device ->
            thread(true) {
                var socket: BluetoothSocket? = null
                try {
                    Log.i("MeshManager", "sendMessage: Попытка подключения к [${device.address}]")

                    socket = device.createInsecureRfcommSocketToServiceRecord(uuid)

                    if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()

                    socket.connect()
                    Log.i("MeshManager", "sendMessage: Успешно подключились к [${device.address}]")

                    val outputStream = socket.outputStream

                    outputStream.write(Gson().toJson(message).toByteArray(Charsets.UTF_8))
                    outputStream.flush()

                    Log.i(
                        "MeshManager",
                        "sendMessage: Сообщение успешно отправлено на [${device.address}]"
                    )
                } catch (e: IOException) {
                    Log.e("MeshManager", "sendMessage: ${e.message}")
                }
            }
        }
    }

    fun stopServer() {
        isServerRunning = false

        try {
            serverSocket?.close()
            Log.i("MeshManager", "stopServer()")
        } catch (e: IOException) {
            Log.e("MeshManager", "stopServer: ${e.message}")
        }
    }

    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
            Log.i("MeshManager", "unregisterReceiver()")
        } catch (e: IllegalArgumentException) {
            Log.e("MeshManager", "unregisterReceiver: ${e.message}")
        }
    }
}