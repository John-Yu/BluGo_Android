package com.example.user.blugo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by user on 2016-06-12.
 */
public class GoActivityUtil implements Handler.Callback, GoMessageListener {
    private static GoActivityUtil instance = null;

    private Handler m_msg_handler = new Handler(this);

    private class SaveSGF_Msg {
        public String file_name;
        public Context context;
        public GoControl go_control;
    }

    private GoActivityUtil() {
    }

    public static String detectEncoding(FileInputStream fis) {
        String encoding = "UTF-8"; //default encoding
        try {
            byte[] buf = new byte[4096];

            UniversalDetector detector = new UniversalDetector(null);

            int nread;
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
            detector.dataEnd();

            String detect = detector.getDetectedCharset();
            if (detect != null) {
                encoding = detect;
            }

            detector.reset();

        } catch (IOException e) {
        }

        return encoding;
    }

    public static GoActivityUtil getInstance() {
        if (instance == null) {
            instance = new GoActivityUtil();
        }
        return instance;
    }

    public Handler get_go_msg_handler() {
        return m_msg_handler;
    }

    /* pop up dialog. get input file name. save file */
    public void save_sgf(final Context context, final GoControl control) {
        String file_name;
        Calendar cal = Calendar.getInstance();

        AlertDialog.Builder builder;
        final EditText file_name_input = new EditText(context);

        file_name = String.format(
                "%04d%02d%02d_%02d%02d%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DATE),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND));

        file_name_input.setText(file_name);


        builder = new AlertDialog.Builder(context);
        builder.setView(file_name_input)
                .setTitle(ResStrGenerator.getInstance().get_res_string(R.string.input_save_file_name))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Message msg;
                    SaveSGF_Msg save_sgf_msg = new SaveSGF_Msg();
                    save_sgf_msg.file_name = file_name_input.getText().toString();
                    save_sgf_msg.go_control = control;
                    save_sgf_msg.context = context;

                    msg = Message.obtain(GoActivityUtil.getInstance().get_go_msg_handler(),
                            GoMessageListener.SAVE_FILE_NAME_INPUT_FINISHED,
                            save_sgf_msg);

                    GoActivityUtil.getInstance().get_go_msg_handler().sendMessage(msg);
                })
                .setNegativeButton(android.R.string.cancel, null);

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case GoMessageListener.SAVE_FILE_NAME_INPUT_FINISHED:
                save_sgf_file_as((SaveSGF_Msg) msg.obj);
                break;
        }
        return false;
    }

    private void save_sgf_file_as(SaveSGF_Msg save_sgf_msg) {
        String app_name;
        String sgf_text;

        app_name = save_sgf_msg.context.getString(save_sgf_msg.context.getApplicationInfo().labelRes);

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.d("TEST", "External storage not mounted");
            return;
        }

        String path = Environment.getExternalStorageDirectory() + File.separator + app_name;
        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs())
                Log.d("TEST", "Directory creation failed");
        }

        path += File.separator;

        sgf_text = save_sgf_msg.go_control.get_sgf();

        FileOutputStream os;
        try {
            os = new FileOutputStream(path + save_sgf_msg.file_name + (true ? ".sgf" : ""));
            os.write(sgf_text.getBytes("UTF-8"));
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* SGF Loading process */
    public void load_sgf(final String sgf_path, final GoControl game, final Handler msg_handler) {

        new Thread(() -> {
            Message msg;
            FileInputStream fis;
            String sgf_string = null;
            boolean isReadOK = false;
            // Send BEGIN message
            msg = Message.obtain(msg_handler, MSG_LOAD_BEGIN, "msg");
            msg_handler.sendMessage(msg);

            try {
                File file = new File(sgf_path);
                if (file.exists() && file.canRead()) {
                    fis = new FileInputStream(file);
                    String encoding = detectEncoding(new FileInputStream(sgf_path)); //detectEncoding will read file
                    int fileSize = fis.available();
                    if (fileSize > 0) {
                        byte[] bytes = new byte[fileSize];
                        fis.read(bytes);
                        sgf_string = new String(bytes,encoding);
                        if (!sgf_string.isEmpty()) {
                            isReadOK = true;
                        }
                    }
                    /*
                    // It works but very slow when read big file ( >20MB )
                    InputStreamReader reader = new InputStreamReader(is, encoding);
                    StringBuilder builder = new StringBuilder();
                    while (reader.ready()) {
                        builder.append((char) reader.read());
                    }
                    sgf_string = builder.toString();
                    reader.close();
                    */
                    fis.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                msg = Message.obtain(msg_handler, MSG_LOAD_FAIL, "msg");
                msg_handler.sendMessage(msg);
                return;
            }
            if (isReadOK) {
                game.load_sgf(sgf_string);
                msg = Message.obtain(msg_handler, MSG_LOAD_END, "msg");
                msg_handler.sendMessage(msg);
            } else {
                msg = Message.obtain(msg_handler, MSG_LOAD_FAIL, "msg");
                msg_handler.sendMessage(msg);
            }
        }).start();
    }
}
