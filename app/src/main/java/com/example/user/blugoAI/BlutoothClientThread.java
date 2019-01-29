package com.example.user.blugoAI;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.os.Handler;

import java.io.IOException;

/**
 * Created by user on 2016-06-08.
 */
public class BlutoothClientThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final BluetoothAdapter mBluetoothAdapter;
    private final GoMessageListener listener;

    private static BlutoothClientThread instance;

    private BlutoothClientThread() {
        mmSocket = null;
        mmDevice = null;
        mBluetoothAdapter = null;
        listener = null;
    }

    private BlutoothClientThread(BluetoothAdapter adapter, BluetoothDevice device, GoMessageListener listener) {
        BluetoothSocket tmp = null;
        mmDevice = device;

        try {
            tmp = device.createRfcommSocketToServiceRecord(BlutoothServerThread.uuid);
        } catch (IOException e) {
        }
        mmSocket = tmp;
        this.mBluetoothAdapter = adapter;
        this.listener = listener;
    }

    public static BlutoothClientThread getInstance() {
        return instance;
    }

    public static BlutoothClientThread getInstance(BluetoothAdapter adapter, BluetoothDevice device, GoMessageListener listener) {
        if (instance == null) {
            instance = new BlutoothClientThread(adapter, device, listener);
        }

        return instance;
    }

    public void run() {
        Handler h;
        Message msg;

        mBluetoothAdapter.cancelDiscovery();
        h = listener.get_go_msg_handler();

        try {
            mmSocket.connect();
        } catch (IOException connectException) {
            instance = null;
            msg = Message.obtain(h, GoMessageListener.BLUTOOTH_CLIENT_SOCKET_ERROR,
                    connectException.toString());
            h.sendMessage(msg);
            cancel();
            return;
        }

        //manage connection
        BlutoothCommThread connected_thread = BlutoothCommThread.getInstance(mmSocket, listener);
        connected_thread.start();
        try {
            msg = Message.obtain(h, GoMessageListener.BLUTOOTH_CLIENT_CONNECT_SUCCESS,
                    "connection success");
            h.sendMessage(msg);
            connected_thread.join();
        } catch (InterruptedException e) {
            instance = null;
            msg = Message.obtain(h, GoMessageListener.BLUTOOTH_CLIENT_SOCKET_ERROR,
                    e.toString());
            h.sendMessage(msg);
        }
        connected_thread = null;
        instance = null;
        cancel();
    }

    public synchronized void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
        }
    }
}
