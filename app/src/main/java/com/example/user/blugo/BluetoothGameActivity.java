package com.example.user.blugo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

public class BluetoothGameActivity extends AppCompatActivity implements
        GoBoardViewListener, GoMessageListener {
    private Handler msg_handler = new Handler(new GoMsgHandler());
    private Handler view_msg_handler = new Handler(new ViewMessageHandler());

    private GoBoardView gv;
    private TextView txt_info;
    private GoControlBluetooth game;
    private Button btn_confirm, btn_undo, btn_pass, btn_resign;

    private boolean refuse_undo_permanently = false;

    /*
    0x01 : you confirm,
    0x02: opponent confirm
    0x04: you accepted,
    0x08: opponent accepted.
    */
    private final static int YOU_CONFIRMED = 0x01;
    private final static int OPPONENT_CONFIRMED = 0x02;
    private final static int BOTH_CONFIRMED = YOU_CONFIRMED | OPPONENT_CONFIRMED;
    private final static int YOU_ACCEPTED = 0x04;
    private final static int OPPONENT_ACCEPTED = 0x08;
    private final static int BOTH_ACCEPTED = YOU_ACCEPTED | OPPONENT_ACCEPTED;

    private int calc_result_confirm = 0x00;


    private String get_info_text() {
        String str, result;
        GoControl.GoInfo info = game.get_info();

        if (game.calc_mode()) {
            if (info.resigned == GoControl.Player.WHITE) {
                /* white resigned */
                result = getString(R.string.black_won_by_resign_short);
            } else if (info.resigned == GoControl.Player.BLACK) {
                /* black resigned */
                result = getString(R.string.white_won_by_resign_short);
            } else if (info.score_diff == 0) {
                result = getString(R.string.draw).toUpperCase();
            } else if (info.score_diff > 0) {
                result = String.format(getString(R.string.white_short) + "+%.1f", info.score_diff);
            } else {
                result = String.format(getString(R.string.black_short) + "+%.1f", Math.abs(info.score_diff));
            }

            str = String.format(getString(R.string.white_tr_short) + ": %.1f, " +
                            getString(R.string.black_tr_short) + ": %.1f, %s",
                    info.white_final,
                    info.black_final,
                    result);
        } else {
            str = String.format("%s(%d), %s: %d, %s: %d",
                    info.turn == GoControl.Player.WHITE ?
                            getString(R.string.white_short) : getString(R.string.black_short),
                    info.turn_num,
                    getString(R.string.dead_white_short),
                    info.white_dead,
                    getString(R.string.dead_black_short),
                    info.black_dead);

            if (info.resigned == GoControl.Player.WHITE) {
                str += ", " + getString(R.string.black_won_by_resign_short);
            } else if (info.resigned == GoControl.Player.BLACK) {
                str += ", " + getString(R.string.white_won_by_resign_short);
            }
        }

        return str;
    }

    private BlutoothCommThread comm_thread;

    {
        comm_thread = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_game);

        Bundle bundle;
        Intent intent = getIntent();
        bundle = intent.getExtras();

        GoPlaySetting setting = Objects.requireNonNull(bundle).getParcelable(GoMessageListener.GAME_SETTING_MESSAGE);

        int bw = Objects.requireNonNull(setting).wb;
        Log.d("TEST", "bw: " + bw);

        //game = new GoControlBluetooth(bw == 0? GoControl.Player.BLACK : GoControl.Player.WHITE);
        /* Giving handicap = 0 returns null */
        NewBoardState handicapped = NewBoardState.build_handicapped_game(setting.handicap);
        GoRule rule;

        switch (GoRule.RuleID.valueOf(setting.rule)) {
            case JAPANESE:
                if (handicapped != null)
                    rule = new GoRuleJapan(handicapped);
                else
                    rule = new GoRuleJapan(setting.size);
                break;

            case CHINESE:
                if (handicapped != null)
                    rule = new GoRuleChinese(handicapped);
                else
                    rule = new GoRuleChinese(setting.size);
                break;

            default:
                if (handicapped != null)
                    rule = new GoRuleJapan(handicapped);
                else
                    rule = new GoRuleJapan(setting.size);
                break;
        }

        game = new GoControlBluetooth(setting.size, setting.komi,
                setting.handicap, rule,
                bw == 0 ? GoControl.Player.BLACK : GoControl.Player.WHITE,
                setting.handicap > 0 ? GoControl.Player.WHITE : GoControl.Player.BLACK
        );

        gv = (GoBoardView) findViewById(R.id.go_board_view);
        gv.setGo_control(game);
        gv.setFocusable(true);

        txt_info = (TextView) findViewById(R.id.text_info);
        txt_info.setText(get_info_text());

        comm_thread = BlutoothCommThread.getInstance();
        if (comm_thread != null) {
            comm_thread.changeListener(this);
        }

        btn_confirm = (Button) findViewById(R.id.btn_confirm);
        btn_undo = (Button) findViewById(R.id.btn_undo);
        btn_pass = (Button) findViewById(R.id.btn_pass);
        btn_resign = (Button) findViewById(R.id.btn_resign);

        /* Set volume control to music */
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        String start_message = null;

        if (game.isMyTurn()) {
            if (game.get_my_color() == GoControl.Player.BLACK) {
                start_message = getString(R.string.your_turn_black);
            } else {
                start_message = getString(R.string.your_turn_white);
            }
        }

        if (start_message != null)
            Toast.makeText(this, start_message, Toast.LENGTH_SHORT).show();
    }

    private void stop_server_client() {
        BlutoothServerThread server;
        BlutoothClientThread client;

        /* stop communicator */
        BlutoothCommThread comm;
        comm = BlutoothCommThread.getInstance();
        if (comm != null) {
            comm.cancel();
            try {
                comm.join();
            } catch (InterruptedException e) {
            }
        }

        /* stop server */
        server = BlutoothServerThread.getInstance();
        if (server != null) {
            server.cancel();
            try {
                server.join();
            } catch (InterruptedException e) {
            }
        }

        /* stop client */
        client = BlutoothClientThread.getInstance();
        if (client != null) {
            client.cancel();
            try {
                client.join();
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop_server_client();
        gv.release_memory();
    }

    private class GoMsgHandler implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            String tmp;
            switch (msg.what) {
                case GoMessageListener.MSG_LOAD_END:
                    return true;

                case GoMessageListener.BLUTOOTH_COMM_ERROR:
                    stop_server_client();
                    Toast.makeText(BluetoothGameActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    return true;

                case GoMessageListener.BLUTOOTH_COMM_MSG:
                    BlutoothMsgParser.MsgParsed parsed = (BlutoothMsgParser.MsgParsed) msg.obj;
                    handle_comm_message(parsed);
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
                    if (game.calc_mode()) {
                        btn_confirm.setEnabled(true);
                        btn_pass.setEnabled(false);
                    } else {
                        btn_confirm.setEnabled(false);
                        btn_pass.setEnabled(true);
                    }
                    txt_info.setText(get_info_text());

                    return true;
            }
            return false;
        }
    }

    public void pass(View view) {
        game.pass();
    }

    private void handle_comm_message(BlutoothMsgParser.MsgParsed msg) {
        String m;
        Point p;
        AlertDialog.Builder builder;
        AlertDialog alert;

        switch (msg.type) {
            case PUTSTONE:
                p = (Point) msg.content;
                game.op_putStoneAt(p.x, p.y);
                break;

            case PASS:
                game.op_pass();
                Toast.makeText(this, (String) getString(R.string.opponent_passed),
                        Toast.LENGTH_SHORT).show();
                break;

            case RESIGN:
                //game.finish();
                builder = new AlertDialog.Builder(this);
                builder.setMessage(String.format(getString(R.string.fmt_you_won_by),
                        game.get_my_color() == GoControl.Player.BLACK ?
                                getString(R.string.black) : getString(R.string.white)))
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, null);
                alert = builder.create();
                alert.show();
                game.opponent_resigned();
                finish_game();
                break;

            case RESULT_CONFIRM:
                calc_result_confirm |= OPPONENT_CONFIRMED;
                Toast.makeText(this, (String) getString(R.string.opponent_confirmed_result),
                        Toast.LENGTH_SHORT).show();
                check_result();
                break;

            case DECLINE_RESULT:
                calc_result_confirm = 0x00; /* From the beginning */
                Toast.makeText(this, (String) getString(R.string.opponent_declined_result),
                        Toast.LENGTH_SHORT).show();

                game.setConfirm_check(false);
                btn_confirm.setEnabled(true);
                break;

            case ACCEPT_RESULT:
                calc_result_confirm |= OPPONENT_ACCEPTED;
                Toast.makeText(this, (String) getString(R.string.opponent_accepted_result),
                        Toast.LENGTH_SHORT).show();

                /*try finish game*/
                if ((calc_result_confirm & BOTH_ACCEPTED) == BOTH_ACCEPTED) {
                    Toast.makeText(this, (String) getString(R.string.game_finished),
                            Toast.LENGTH_SHORT).show();
                    finish_game();
                }
                break;

            case REQUEST_UNDO:
                if (refuse_undo_permanently) {
                    BlutoothCommThread comm;
                    comm = BlutoothCommThread.getInstance();

                    if (comm == null)
                        break;

                    comm.write(BlutoothMsgParser.make_message(
                            BlutoothMsgParser.MsgType.DECLINE_UNDO, null));

                    break;
                }
                builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.question_undo_request_accept_it))
                        /*.setMessage("Opponent requested undo. Would you accept request?")*/
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            BlutoothCommThread comm;
                            comm = BlutoothCommThread.getInstance();

                            if (comm == null)
                                return;

                            game.undo_apply(true, false);

                            comm.write(BlutoothMsgParser.make_message(
                                    BlutoothMsgParser.MsgType.ACCEPT_UNDO, null));
                        })
                        .setNegativeButton(android.R.string.no, (dialog, which) -> {
                            BlutoothCommThread comm;
                            comm = BlutoothCommThread.getInstance();

                            if (comm == null)
                                return;

                            comm.write(BlutoothMsgParser.make_message(
                                    BlutoothMsgParser.MsgType.DECLINE_UNDO, null));
                        })
                        .setMultiChoiceItems(new String[]{getString(R.string.refuse_undo_request_permanently)},
                                null, (dialog, which, isChecked) -> refuse_undo_permanently = isChecked)
                ;
                alert = builder.create();
                alert.show();
                break;

            case DECLINE_UNDO:
                game.undo_apply(false, true);
                Toast.makeText(this, (String) getString(R.string.your_undo_request_was_rejected),
                        Toast.LENGTH_SHORT).show();
                break;

            case ACCEPT_UNDO:
                game.undo_apply(true, true);
                break;
        }
    }

    @Override
    public Handler get_view_msg_handler() {
        return this.view_msg_handler;
    }

    @Override
    public Handler get_go_msg_handler() {
        return this.msg_handler;
    }

    public void resign(View view) {
        game.resign();

        finish_game();
    }

    public void confirm_result(View view) {
        if (!game.calc_mode())
            return;

        BlutoothCommThread comm;
        comm = BlutoothCommThread.getInstance();

        if (comm == null)
            return;

        calc_result_confirm |= YOU_CONFIRMED;

        comm.write(BlutoothMsgParser.make_message(BlutoothMsgParser.MsgType.RESULT_CONFIRM,
                null
        ));
        check_result();
    }

    private void finish_game() {
        game.finish_game();
        btn_confirm.setEnabled(false);
        btn_undo.setEnabled(false);
        btn_pass.setEnabled(false);
        btn_resign.setEnabled(false);

        txt_info.setText(get_info_text());
    }

    private void check_result() {
        AlertDialog.Builder builder;

        if ((calc_result_confirm & BOTH_CONFIRMED) != BOTH_CONFIRMED)
            return;

        calc_result_confirm = calc_result_confirm & ~BOTH_ACCEPTED;

        game.setConfirm_check(true);
        btn_confirm.setEnabled(false);

        String message = "";
        GoControl.GoInfo info = game.get_info();
        GoRule.RuleID rule = game.get_rule();

        message += getString(R.string.rule) + " : " + rule.toString() + "\n";

        message += String.format(getString(R.string.dead_white) + " : %d, " +
                        getString(R.string.dead_black) + " : %d\n",
                info.white_dead, info.black_dead);
        message += String.format(getString(R.string.white_tr) + " : %d, " +
                        getString(R.string.black_tr) + " : %d\n",
                info.white_score, info.black_score);
        message += String.format(getString(R.string.live_white) + " : %d\n",
                info.white_count);
        message += String.format(getString(R.string.live_black) + " : %d\n",
                info.black_count);
        message += String.format(getString(R.string.komi) + " : %.1f\n", info.komi);
        message += String.format(getString(R.string.white_total) + " : %.1f\n", info.white_final);
        message += String.format(getString(R.string.black_total) + " : %.1f\n", info.black_final);
        message += getString(R.string.result) + " : ";

        if (info.score_diff == 0)
            message += getString(R.string.draw);
        else if (info.score_diff > 0)
            message += String.format(getString(R.string.fmt_white_won_by), info.score_diff);
        else
            message += String.format(getString(R.string.fmt_black_won_by), Math.abs(info.score_diff));

        builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(getString(R.string.question_accept_result))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    calc_result_confirm |= YOU_ACCEPTED;
                    BlutoothCommThread comm;
                    comm = BlutoothCommThread.getInstance();

                    if (comm == null)
                        return;
                    comm.write(BlutoothMsgParser.make_message(
                            BlutoothMsgParser.MsgType.ACCEPT_RESULT, null));

                    /*try finish game*/
                    if ((calc_result_confirm & BOTH_ACCEPTED) == BOTH_ACCEPTED) {
                        Toast.makeText(BluetoothGameActivity.this, (String) getString(R.string.game_finished),
                                Toast.LENGTH_SHORT).show();
                        finish_game();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        calc_result_confirm = 0x00;
                        BlutoothCommThread comm;
                        comm = BlutoothCommThread.getInstance();

                        if (comm == null)
                            return;
                        comm.write(BlutoothMsgParser.make_message(
                                BlutoothMsgParser.MsgType.DECLINE_RESULT, null));

                        game.setConfirm_check(false);
                        btn_confirm.setEnabled(true);
                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public void undo(View view) {
        game.undo();
    }

    public void save_SGF(View view) {
        GoActivityUtil.getInstance().save_sgf(this, game);
    }
}
