package com.example.user.blugoAI;

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
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

public class GoBoardActivity extends AppCompatActivity implements FileChooser.FileSelectedListener, GoBoardViewListener {
    private GoBoardView gv;
    private TextView txt_info;
    private GoControl single_game;
    private ProgressBar progressBar;
    private ProgressBar pbBlack;
    private ProgressBar pbWhite;
    private String sgf_string = null;
    private Button btn_save ;
    private Button btn_undo ;
    private Button btn_pass ;
    private Button btn_resign ;

    private final Handler msg_handler = new Handler(new GoMsgHandler());
    private final Handler view_msg_handler = new Handler(new ViewMessageHandler());

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
            str = String.format(Locale.ENGLISH,"%s(%d), %s: %d, %s: %d",
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
        btn_save = (Button) findViewById(R.id.btn_save);
        btn_undo = (Button) findViewById(R.id.button3);
        btn_pass = (Button) findViewById(R.id.button6);
        btn_resign = (Button) findViewById(R.id.btn_resign);
        pbBlack = (ProgressBar)findViewById(R.id.progressBarBlack);
        pbWhite = (ProgressBar)findViewById(R.id.progressBarWhite);
        TextView tvBlack = (TextView) findViewById(R.id.textViewBlack);
        TextView tvWhite = (TextView) findViewById(R.id.textViewWhite);
        gv = (GoBoardView) findViewById(R.id.go_board_view);

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

            GoControlSingle gcs = new GoControlSingle(Objects.requireNonNull(state).size,
                    bw == 0 ? GoControl.Player.BLACK : GoControl.Player.WHITE,
                    setting.komi, setting.handicap, rule, start_turn);

            if(Objects.requireNonNull(setting).black ==1) {
                gcs.setAI(GoControl.Player.BLACK);
                tvBlack.setText("B: AI");
            }
            if(Objects.requireNonNull(setting).white ==1) {
                gcs.setAI(GoControl.Player.WHITE);
                tvWhite.setText("W: AI");
            }
            App mApp = (App)getApplication();
            gcs.leela = mApp.leela;
            gcs.leela.setHandler(view_msg_handler);
            single_game = gcs;
            /*
                Because SGF format cannot save initial dead stone information.
                We lose dead stone information after saving.
                To avoid this problem, there are no choice but only copy entire board state list.
                But you don't want to do that. Because we want to try out variation only.
                */
            boolean enable_save = bundle.getBoolean(ReviewGameActivity.MSG_ENABLE_SAVE);
            btn_save.setEnabled(enable_save);
        }

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

        File file1 = file;

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
                    tmp = new String(buffer, 0, read, StandardCharsets.UTF_8);
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
                    //TODO: here is a good point for check current player is a AI or human
                    single_game.callAI();
                    return true;
                case GoBoardViewListener.MSG_VIEW_ENABLE_BUTTON:
                    enableButton();
                    pbBlack.setVisibility(View.INVISIBLE);
                    pbWhite.setVisibility(View.INVISIBLE);
                    return true;
                case GoBoardViewListener.MSG_VIEW_DISABLE_BUTTON:
                    disableButton();
                    if(single_game.getCurrent_turn() == GoControl.Player.BLACK) {
                        pbBlack.setVisibility(View.VISIBLE);
                    }
                    else pbWhite.setVisibility(View.VISIBLE);
                    return true;
                case GoBoardViewListener.MSG_VIEW_PUT_STONE:
                    String namedCoordinate = (String)msg.obj;
                    single_game.putStoneAt(namedCoordinate, false);
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
    private void enableButton()
    {
        btn_save.setEnabled(true);
        btn_undo.setEnabled(true);
        btn_pass.setEnabled(true);
        btn_resign.setEnabled(true);
    }
    private void disableButton()
    {
        btn_save.setEnabled(false);
        btn_undo.setEnabled(false);
        btn_pass.setEnabled(false);
        btn_resign.setEnabled(false);
    }
}
