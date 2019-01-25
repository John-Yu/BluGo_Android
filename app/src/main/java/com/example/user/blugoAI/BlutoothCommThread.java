package com.example.user.blugoAI;

import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.util.Log;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Semaphore;

/**
 * Created by user on 2016-06-08.
 */
public class BlutoothCommThread extends Thread {
    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private GoMessageListener listener;
    private final Semaphore mutex = new Semaphore(1);

    private static BlutoothCommThread instance = null;

    private BlutoothCommThread() {

    }

    private BlutoothCommThread(BluetoothSocket socket, GoMessageListener listener) {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        mmSocket = socket;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        this.listener = listener;
    }

    public static BlutoothCommThread getInstance(BluetoothSocket socket, GoMessageListener listener) {
        if (instance == null) {
            instance = new BlutoothCommThread(socket, listener);
        }

        return instance;
    }

    public static BlutoothCommThread getInstance() {
        return instance;
    }

    public void changeListener(GoMessageListener listener) {
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
        }
        this.listener = listener;
        mutex.release();
    }

    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        String message = null;
        Message msg;
        Handler h;
        BlutoothMsgParser parser = new BlutoothMsgParser();

        while (true) {
            try {
                bytes = mmInStream.read(buffer);
                try {
                    message = new String(buffer, 0, bytes, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                }

                try {
                    mutex.acquire();
                } catch (InterruptedException e) {
                }
                if (listener != null) {
                    h = listener.get_go_msg_handler();
                    msg = Message.obtain();
                    msg.what = GoMessageListener.BLUTOOTH_COMM_MSG;
                    msg.obj = parser.parse(message);
                    h.sendMessage(msg);
                }
                mutex.release();

            } catch (IOException e) {
                Log.d("MYTAG", e.toString());

                instance = null;

                try {
                    mutex.acquire();
                } catch (InterruptedException ie) {
                }
                if (listener != null) {
                    h = listener.get_go_msg_handler();
                    msg = Message.obtain(h, GoMessageListener.BLUTOOTH_COMM_ERROR,
                            e.toString());
                    h.sendMessage(msg);
                    cancel();
                    break;
                }
                mutex.release();
            }
        }

        instance = null;
    }

    public void write(String message) {
        if (message == null)
            return;

        try {
            this.write(message.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }
    }

    private void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
        }
    }

    public synchronized void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
        }
    }
}
