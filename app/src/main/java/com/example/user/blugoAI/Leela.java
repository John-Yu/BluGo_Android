package com.example.user.blugoAI;


import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Leela {
    public boolean isLoad = false;
    private stdoutListen sListener = null;
    private boolean isThinking = false;
    private Handler hMsg;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public Leela(final String weightFile) {
        new Thread(() -> {
            // TODO Auto-generated method stub
            StartEngine(weightFile);
            isLoad = true;
        }).start();
    }

    public void startMonitor() {
        if (sListener != null) {
            sListener.interrupt();
        }
        sListener = new stdoutListen();
        sListener.start();
    }

    private void sendCmd(String cmd) {
        if (!isLoad) return;
        SendGTP(cmd);
    }

    public void clearBoard() {
        sendCmd("clear_board");
    }
    public void timeSetting(String settings) {
        sendCmd("time_settings " + settings);
    }

    public void genMove(final String color) {
        synchronized (this) {
            isThinking = true;
            sendCmd("genmove " + color);
            //isThinking = false;
        }
    }

    /**
     * @param colorString color of stone to play
     * @param move        coordinate of the coordinate
     */
    public void playMove(String colorString, String move) {
        synchronized (this) {
            sendCmd("play " + colorString + " " + move);
        }
    }

    public void undo() {
        synchronized (this) {
            sendCmd("undo");
        }
    }

    public void setHandler(Handler h) {
        hMsg = h;
    }

    /**
     * Parse a line of Leelaz output
     *
     * @param line output line
     */
    private void parseLine(String line) {
        if (line.startsWith("=") || line.startsWith("?")) {
            String[] params = line.trim().split(" ");
            if (line.startsWith("?") || params.length == 1) return;
            if (isThinking) {
                Message msg = Message.obtain(hMsg, GoBoardViewListener.MSG_VIEW_PUT_STONE, params[1]);
                hMsg.sendMessage(msg);
                isThinking = false;
            }
        }
    }

    private class stdoutListen extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                String TAG = "example.user.blugoAI";
                try {
                    String sOut = getStdoutFromJNI();
                    if (sOut.isEmpty()) {
                        Thread.sleep(10);
                        continue;
                    }
                    parseLine(sOut);
                    Log.i(TAG, sOut);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.i(TAG, Thread.currentThread().getName() + "InterruptedException");
                    break;
                }
            }
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    private native String getStdoutFromJNI();

    private native int StartEngine(String weight);

    private native int SendGTP(String cmd);
}
