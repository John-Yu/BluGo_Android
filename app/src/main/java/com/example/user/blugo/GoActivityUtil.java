package com.example.user.blugo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;

/**
 * Created by user on 2016-06-12.
 */
public class GoActivityUtil implements Handler.Callback, GoMessageListener {
    private static GoActivityUtil instance = null;
    private ProgressDialog load_progress = null;

    private Handler msg_handler = new Handler(this);

    private class SaveSGF_Msg {
        public String file_name;
        public Context context;
        public GoControl go_control;
    }

    private GoActivityUtil() {}

    public static GoActivityUtil getInstance()
    {
        if (instance == null) {
            instance = new GoActivityUtil();
        }
        return instance;
    }

    public Handler get_go_msg_handler()
    {
        return msg_handler;
    }

    /* pop up dialog. get input file name. save file */
    public void save_sgf(final Context context, final GoControl control)
    {
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
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Message msg;
                    SaveSGF_Msg save_sgf_msg = new SaveSGF_Msg();
                    save_sgf_msg.file_name = file_name_input.getText().toString();
                    save_sgf_msg.go_control = control;
                    save_sgf_msg.context = context;

                    msg = Message.obtain(GoActivityUtil.getInstance().get_go_msg_handler(),
                        GoMessageListener.SAVE_FILE_NAME_INPUT_FINISHED,
                        save_sgf_msg);

                    GoActivityUtil.getInstance().get_go_msg_handler().sendMessage(msg);
                }
            })
            .setNegativeButton(android.R.string.cancel, null);

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case GoMessageListener.SAVE_FILE_NAME_INPUT_FINISHED:
                save_sgf_file_as((SaveSGF_Msg)msg.obj, true);
        }
        return false;
    }

    private void save_sgf_file_as(SaveSGF_Msg save_sgf_msg, boolean add_extension)
    {
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
            os = new FileOutputStream(path + save_sgf_msg.file_name + (add_extension? ".sgf": ""));
            os.write(sgf_text.getBytes("UTF-8"));
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* SGF Loading process */
    public void load_sgf(Context ctx, final String sgf_path, final GoControl game, final Handler msg_handler)
    {
        load_progress = new ProgressDialog(ctx);
        load_progress.setCancelable(true);
        load_progress.setMessage(ctx.getString(R.string.loading_sgf));
        load_progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        load_progress.setProgress(0);
        load_progress.setMax(100);
        load_progress.show();

        new Thread(new Runnable() {
            public void run() {
                Message msg;
                FileInputStream is;
                byte [] buffer = new byte[512];
                int read;
                String tmp;

                String sgf_string = new String();

                try {
                    is = new FileInputStream(sgf_path);

                    while (true) {
                        read = is.read(buffer, 0, buffer.length);

                        if (read > 0) {
                            tmp = new String(buffer, 0, read, "UTF-8");
                            sgf_string += tmp;
                        } else
                            break;
                    }
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    msg = Message.obtain(msg_handler, MSG_LOAD_FAIL, "msg");
                    msg_handler.sendMessage(msg);
                    return;
                }

                game.load_sgf(sgf_string);
                msg = Message.obtain(msg_handler, MSG_LOAD_END, load_progress);
                msg_handler.sendMessage(msg);
            }
        }).start();
    }
}
