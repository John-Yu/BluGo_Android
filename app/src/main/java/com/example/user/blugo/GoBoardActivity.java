package com.example.user.blugo;

import android.widget.ProgressBar;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.util.Objects;

public class GoBoardActivity extends AppCompatActivity implements FileChooser.FileSelectedListener, GoBoardViewListener {
    private GoBoardView gv;
    private TextView txt_info;
    private GoControl single_game;
    private ProgressBar progressBar;
    private String sgf_string = null;
    private File file;

    public Handler msg_handler = new Handler(new GoMsgHandler());
    public Handler view_msg_handler = new Handler(new ViewMessageHandler());

    private String get_info_text() {
        String str, result;
        GoControl.GoInfo info = single_game.get_info();

        GoControl.Player resigned;

        resigned = single_game.is_resigned();

        if (resigned != null) {
            if (resigned == GoControl.Player.BLACK) {
                return getString(R.string.white_won_by_resign_short);
            } else {
                return getString(R.string.black_won_by_resign_short);
            }
        }

        if (single_game.calc_mode()) {
            if (info.score_diff == 0) {
                result = getString(R.string.draw);
            } else if (info.score_diff > 0) {
                result = String.format(getString(R.string.white_short) + "+%.1f",
                        info.score_diff);
            } else {
                result = String.format(getString(R.string.black_short) + "+%.1f",
                        Math.abs(info.score_diff));
            }

            str = String.format(getString(R.string.white_tr_short) +
                            ": %.1f, " +
                            getString(R.string.black_tr_short) +
                            ": %.1f, %s",
                    info.white_final, info.black_final, result);
        } else {
            str = String.format("%s(%d), %s: %d, %s: %d",
                    info.turn == GoControl.Player.WHITE ?
                            getString(R.string.white_short) : getString(R.string.black_short),
                    info.turn_num,
                    getString(R.string.dead_white_short),
                    info.white_dead,
                    getString(R.string.dead_black_short),
                    info.black_dead);
        }

        return str;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GoRule rule;
        NewBoardState state;
        GoPlaySetting setting;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_go_board);

        Intent intent = getIntent();
        Bundle bundle;

        bundle = intent.getExtras();

        if (bundle == null) {
            single_game = new GoControlSingle();
        } else {
            state = bundle.getParcelable(ReviewGameActivity.MSG_BOARD_STATE);
            setting = bundle.getParcelable(ReviewGameActivity.MSG_SETTING);
            int bw = bundle.getInt(ReviewGameActivity.MSG_CURRENT_TURN);
            int start_turn = bundle.getInt(ReviewGameActivity.MSG_START_TURNNO);

            switch (GoRule.RuleID.valueOf(Objects.requireNonNull(setting).rule)) {
                case JAPANESE:
                    rule = new GoRuleJapan(state);
                    break;

                case CHINESE:
                    rule = new GoRuleChinese(state);
                    break;

                default:
                    rule = new GoRuleJapan(state);
                    break;
            }

            /*
            single_game = new GoControlSingle(state.size,
                bw == 0? GoControl.Player.BLACK : GoControl.Player.WHITE,
                rule, start_turn);
                */

            single_game = new GoControlSingle(Objects.requireNonNull(state).size,
                    bw == 0 ? GoControl.Player.BLACK : GoControl.Player.WHITE,
                    setting.komi, setting.handicap, rule, start_turn);

            /*
                Because SGF format cannot save initial dead stone information.
                We lose dead stone information after saving.
                To avoid this problem, there are no choice but only copy entire board state list.
                But you don't want to do that. Because we want to try out variation only.
                */
            boolean enable_save = bundle.getBoolean(ReviewGameActivity.MSG_ENABLE_SAVE);
            Button btn_save = (Button) findViewById(R.id.btn_save);
            btn_save.setEnabled(enable_save);
        }

        gv = (GoBoardView) findViewById(R.id.go_board_view);
        gv.setGo_control(single_game);
        gv.setFocusable(true);

        txt_info = (TextView) findViewById(R.id.text_info);
        txt_info.setText(get_info_text());

        /* Set volume control to music */
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gv.release_memory();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void undo(View view) {
        /* board clear */
        // single_game.new_game();

        /* undo last move */
        single_game.undo();
    }

    public void pass(View view) {
        single_game.pass();
    }

    public void load_SGF(View view) {
        FileChooser f = new FileChooser(this);

        f.setExtension("sgf");
        f.setFileListener(this);
        f.showDialog();
    }

    public void save_SGF(View view) {
        GoActivityUtil.getInstance().save_sgf(this, single_game);
    }

    @Override
    public void fileSelected(File file) {
        progressBar = new ProgressBar(this);
        progressBar.setProgress(0);
        progressBar.setMax(100);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);  //To show ProgressBar

        this.file = file;

        Log.d("TEST", "selected file : " + file.toString());

        FileInputStream is;
        byte[] buffer = new byte[512];
        int read;
        String tmp;

        sgf_string = "";

        try {
            is = new FileInputStream(file.getPath());

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
        }

        new Thread(() -> {
            single_game.load_sgf(sgf_string);
            Message msg;
            msg = Message.obtain(GoBoardActivity.this.msg_handler, GoMessageListener.MSG_LOAD_END, "msg");
            GoBoardActivity.this.msg_handler.sendMessage(msg);
        }).start();


    }

    private class GoMsgHandler implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            String tmp;
            switch (msg.what) {
                case GoMessageListener.MSG_LOAD_END:
                    gv.invalidate();
                    txt_info.setText(get_info_text());
                    progressBar.setVisibility(View.GONE);
                    return true;
            }
            return false;
        }
    }

    private class ViewMessageHandler implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            String tmp;
            switch (msg.what) {
                case GoBoardViewListener.MSG_VIEW_FULLY_DRAWN:
                    txt_info.setText(get_info_text());
                    return true;
            }
            return false;
        }
    }

    @Override
    public Handler get_view_msg_handler() {
        return this.view_msg_handler;
    }

    public void resign(View view) {
        single_game.resign();
        txt_info.setText(get_info_text());
    }
}
