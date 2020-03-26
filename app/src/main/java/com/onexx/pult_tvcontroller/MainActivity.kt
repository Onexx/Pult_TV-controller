package com.onexx.pult_tvcontroller

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlinx.android.synthetic.main.activity_main.*
import android.os.Looper


class MainActivity : AppCompatActivity() {
    companion object {
        private var m_bluetoothAdapter: BluetoothAdapter? = null
        private val REQUEST_ENABLE_BLUETOOTH = 1
        var myUUID: UUID = UUID.fromString("aad0b172-c0a3-4839-9281-6b15deb5d24f")
        var serverSocket: BluetoothServerSocket? = null
        var socket: BluetoothSocket? = null
        var inStream: InputStream? = null
       // lateinit var progress: ProgressDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (m_bluetoothAdapter == null) {
            Log.i("TV_controller", "This device doesn't support bluetooth")
            return
        } else {
            Log.i("TV_controller", "This device support bluetooth")
        }
        if (!m_bluetoothAdapter!!.isEnabled) {

            //request enable bluetooth
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        } else {
            Log.i("TV_controller", "Bluetooth enabled")
        }
        //todo add button 'server start' otherwise app will crash if bluetooth is disabled


        Thread {
            while(true) {
                messages.post { messages.text = MesData }
                imageView.post{
                    if(MesData== "0") imageView.setImageResource(R.mipmap.Red_foreground)
                    if(MesData == "1") imageView.setImageResource(R.mipmap.Green_foreground)
                }
                //messages.post(MesData)
                Thread.sleep(100)
                Log.i("Tv C","I in while:" + MesData)
            }
        }.start()
    }


    override fun onResume() {
        super.onResume()
        Log.i("TV_controller", "On resume")
        messages.text = "="
        Thread{

        try {
            serverSocket =
                m_bluetoothAdapter!!.listenUsingInsecureRfcommWithServiceRecord(
                    "Pult - TV controller",
                    myUUID
                )
        } catch (e: IOException) {
            Log.i("TV_controller", "Err then creating server")
        }
        Log.i("TV_controller", "Starting search of income connections")

        SearchIncomeConnections(this).execute()

        Log.i("TV_controller", "Search finished")
        }.start()

    }
    var MesData ="NULL"
    inner class SearchIncomeConnections(c: Context) : AsyncTask<Void, Void, String>() {

        private val context: Context

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
           // progress = ProgressDialog.show(context, "Waiting for connection...", "Please wait")
            Log.i("TV_controller", "showing progress")
        }

        override fun doInBackground(vararg params: Void?): String? {
            while (socket == null || !socket!!.isConnected) {
                try {
                    Log.i("TV_controller", "waiting for connection")
                    socket = serverSocket!!.accept()
                    if (socket != null && socket!!.isConnected) {
                        Log.i("TV_controller", "connected")
                    }
                } catch (e: IOException) {
                    break
                }

                // If a connection was accepted
                if (socket != null) {
                    serverSocket!!.close()
                }
            }
            Log.i("TV_controller", "Connected")
            return null
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            //progress.dismiss()
            Thread {
                inStream = socket!!.inputStream
                while (socket!!.isConnected) {
                    var bytes: Int
                    // Keep listening to the InputStream
                    Log.i("TV_controller", "Reading input stream...")
                    while (inStream != null && socket!!.isConnected) {
                        try {
                            // Read from the InputStream
                            bytes = inStream!!.read()
                            bytes -= '0'.toInt()
                            if (bytes == -1) {
                                socket!!.close()
                                break
                            }
                            MesData = "$bytes";
                           // Toast.makeText(context, "$bytes", Toast.LENGTH_LONG).show()
                            Log.i("TV_controller", "Received message: $bytes")
                        } catch (e: IOException) {
                        }
                    }
                    Log.i("TV_controller", "finished")
                }
                Log.i("TV_controller", "Socket closed")
            }.start()
        }

    }
}