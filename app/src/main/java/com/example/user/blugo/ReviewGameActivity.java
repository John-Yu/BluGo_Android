package com.example.user.blugo;

import android.app.AlertDialog;
import android.widget.ProgressBar;
import android.content.Intent;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class ReviewGameActivity extends AppCompatActivity implements  GoBoardViewListener, SeekBar.OnSeekBarChangeListener{
    public Handler msg_handler = new Handler(new GoMsgHandler());
    public Handler view_msg_handler = new Handler(new ViewMessageHandler());

    private final static String PREFIX = "com.example.user.blugo.ReviewGameActivity";
    public final static String MSG_BOARD_STATE = PREFIX + ".MSG_BOARD_STATE";
    public final static String MSG_CURRENT_TURN = PREFIX + ".MSG_CURRENT_TURN";
    public final static String MSG_START_TURNNO = PREFIX + ".MSG_START_TURNNO";
    public final static String MSG_SETTING = PREFIX + ".MSG_SETTING";
    public final static String MSG_ENABLE_SAVE = PREFIX + ".MSG_ENABLE_SAVE";

    private GoBoardView gv;
    private SeekBar sbar;
    private TextView text_pos, text_result;
    private GoControlReview game = new GoControlReview();

    private String sgf_path;
    private boolean need_to_load = false;
    private boolean loading_finished = false;

    private Button button, btn_detail;

    private ProgressBar load_progress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_game);

	Intent intent = getIntent();
        sgf_path = intent.getStringExtra(FrontDoorActivity.EXTRA_MESSAGE);

        gv = (GoBoardView) findViewById(R.id.go_board_view);
        gv.setGo_control(game);
        gv.setView_only_mode(true);
        gv.setFocusable(true);

        sbar = (SeekBar) findViewById(R.id.seek_pos);
        sbar.setOnSeekBarChangeListener(this);

        text_pos = (TextView) findViewById(R.id.text_pos);
        text_result = (TextView) findViewById(R.id.txt_result);

        need_to_load = true;
        loading_finished = false;

        button = (Button) findViewById(R.id.btn_variation);
        btn_detail = (Button) findViewById(R.id.btn_detail);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gv.release_memory();
    }

    private void load_sgf()
    {
        GoActivityUtil.getInstance().load_sgf(this, sgf_path, game, msg_handler);
    }

    private void set_button_enables()
    {
        if (game.calc_mode()) {
            button.setEnabled(false);
        } else {
            button.setEnabled(true);
        }
    }

    private class GoMsgHandler implements Handler.Callback
    {
        @Override
        public boolean handleMessage(Message msg) {
            String tmp;


            switch (msg.what) {
                case GoMessageListener.MSG_LOAD_END:
                    load_progress = (ProgressBar) msg.obj;

                    sbar.setMax(game.get_last_pos());
                    sbar.setProgress(game.getCur_pos());
                    tmp = String.format("%d/%d", game.getCur_pos(), sbar.getMax());
                    text_pos.setText(tmp);
                    loading_finished = true;

                    set_button_enables();

                    text_result.setText(game.get_determined_result_string());

                    /* Must be after statement load_progress = ... */
                    gv.invalidate();
                    return true;
            }
            return false;
        }
    }

    private class ViewMessageHandler implements Handler.Callback
    {
        @Override
        public boolean handleMessage(Message msg) {
            String tmp;
            switch (msg.what) {
                case GoBoardViewListener.MSG_VIEW_FULLY_DRAWN:
                    if (need_to_load) {
                        Log.d("DEBUG", "TEST");
                        load_sgf();
                        need_to_load = !need_to_load;
                    } else if (load_progress != null) {
                        load_progress.setVisibility(View.GONE);     // To Hide ProgressBar
                        load_progress = null;
                    }
                    return true;
            }
            return false;
        }
    }

    @Override
    public Handler get_view_msg_handler() {
        return view_msg_handler;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        String text;

        if (!loading_finished)
            return;

        text = String.format("%d/%d", progress, this.sbar.getMax());
        text_pos.setText(text);
        game.goto_pos(progress);

        set_button_enables();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void try_varation(View view)
    {
        Intent intent = new Intent(this, GoBoardActivity.class);
        Bundle bundle = new Bundle();
        GoPlaySetting setting = game.get_game_setting();

        bundle.putParcelable(MSG_BOARD_STATE, game.get_current_board_state());
        bundle.putParcelable(MSG_SETTING, setting);
        bundle.putInt(MSG_CURRENT_TURN, game.getCurrent_turn() == GoControl.Player.BLACK ? 0 : 1);
        bundle.putInt(MSG_START_TURNNO, game.getCur_pos());
        bundle.putBoolean(MSG_ENABLE_SAVE, false);

        intent.putExtras(bundle);

        startActivity(intent);
    }

    public void goto_next_move(View view)
    {
        int progress = sbar.getProgress();
        int max = sbar.getMax();

        if (++progress > max)
            return;

        sbar.setProgress(progress);
    }

    public void goto_prev_move(View view)
    {
        int progress = sbar.getProgress();

        if (--progress < 0)
            return;

        sbar.setProgress(progress);
    }

    public void check_detail(View view)
    {
        AlertDialog.Builder builder;

        String message = "";
        GoControl.GoInfo info =  game.get_info();
        GoRule.RuleID rule = game.get_rule();

        message += getString(R.string.rule) + " : " + rule.toString() + "\n";

        if (game.calc_mode()) {
            message += String.format(getString(R.string.dead_black) + "  : %d, " +
				     getString(R.string.dead_white) + "  : %d\n",
				     info.black_dead, info.white_dead);
            message += String.format(getString(R.string.white_tr) + " : %d, " +
				     getString(R.string.black_tr) + " : %d\n",
				     info.white_score, info.black_score);
            message += String.format(getString(R.string.live_white) + "      : %d, " +
				     getString(R.string.live_black) + "      : %d\n",
				     info.white_count, info.black_count);
            message += String.format(getString(R.string.komi) + " : %.1f\n", info.komi);
            message += String.format(getString(R.string.white_total) + ": %.1f\n", info.white_final);
            message += String.format(getString(R.string.black_total) + ": %.1f\n", info.black_final);
            message += getString(R.string.result);

            if (info.score_diff == 0)
                message += getString(R.string.draw).toUpperCase();
            else if (info.score_diff > 0)
                message += String.format(getString(R.string.fmt_white_won_by), info.score_diff);
            else
                message += String.format(getString(R.string.fmt_black_won_by), Math.abs(info.score_diff));
        } else {
            message += String.format(getString(R.string.dead_white) + " : %d\n", info.white_dead);
            message += String.format(getString(R.string.dead_black) + ": %d\n", info.black_dead);
            message += String.format(getString(R.string.komi) + " : %.1f\n", info.komi);
        }

        builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
            .setTitle(getString(R.string.information))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, null);

        AlertDialog alert = builder.create();
        alert.show();
    }
}
