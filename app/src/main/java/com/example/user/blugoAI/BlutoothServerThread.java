package com.example.user.blugoAI;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by user on 2016-06-08.
 */
public class BlutoothServerThread extends Thread {
    public final static UUID uuid = UUID.fromString("4a10c529-1d36-4fa2-b4b1-a95a88734c63");
    private final BluetoothServerSocket mmServerSocket;
    private GoMessageListener listener;
    private BlutoothCommThread connected_thread = null;

    private static BlutoothServerThread instance = null;

    private BlutoothServerThread() {
        mmServerSocket = null;
    }

    private BlutoothServerThread(BluetoothAdapter adapter, GoMessageListener listener) {
        BluetoothServerSocket tmp = null;
        try {
            tmp = adapter.listenUsingRfcommWithServiceRecord("BluGoServer", uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mmServerSocket = tmp;
        this.listener = listener;
    }

    public static BlutoothServerThread getInstance(BluetoothAdapter adapter, GoMessageListener listener) {
        if (instance == null) {
            instance = new BlutoothServerThread(adapter, listener);
        }
        return instance;
    }

    public static BlutoothServerThread getInstance() {
        return instance;
    }


    public BlutoothCommThread get_connected() {
        return this.connected_thread;
    }

    public void run() {
        BluetoothSocket socket = null;
        try {
            socket = mmServerSocket.accept();
        } catch (IOException e) {
            Message msg;

            Log.d("MYTAG", e.toString());

            instance = null;

            Handler h = listener.get_go_msg_handler();
            msg = Message.obtain(h, GoMessageListener.BLUTOOTH_SERVER_SOCKET_ERROR,
                    e.toString());
            h.sendMessage(msg);
            cancel();

            return;
        }

        if (socket != null) {
            connected_thread = BlutoothCommThread.getInstance(socket, this.listener);
            connected_thread.start();
            try {
                connected_thread.join();
            } catch (InterruptedException e) {
                Log.d("MYTAG", e.toString());
            }
            connected_thread = null;
        }

        instance = null;
        cancel();
    }

    /* This method calls close method of BluetoothServer,
    It must not be called simultaneously by different threads.
    So, cancel method should be synchronized
    */
    public synchronized void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.d("MYTAG", e.toString());
        }
    }
}
