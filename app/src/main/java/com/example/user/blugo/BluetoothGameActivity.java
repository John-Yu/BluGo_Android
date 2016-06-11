package com.example.user.blugo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothGameActivity extends AppCompatActivity implements Handler.Callback,
    GoBoardViewListener, GoMessageListener {
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
        GoControl.GoInfo info =  game.get_info();

        if (game.calc_mode()) {
            if (info.resigned == GoControl.Player.WHITE) {
                /* white resigned */
                result = "B+R";
            } else if (info.resigned == GoControl.Player.BLACK) {
                /* black resigned */
                result = "W+R";
            } else if (info.score_diff == 0) {
                result = "DRAW";
            } else if (info.score_diff > 0) {
                result = String.format("W+%.1f", info.score_diff);
            } else {
                result = String.format("B+%.1f", Math.abs(info.score_diff));
            }

            str = String.format("ws: %.1f, bs: %d, %s", info.white_final,
                (int) info.black_final, result);
        } else {
            str = String.format("%s(%d), wd: %d, bd: %d",
                info.turn == GoControl.Player.WHITE? "W" : "B",
                info.turn_num,
                info.white_dead, info.black_dead);

            if (info.resigned == GoControl.Player.WHITE) {
                str += ", B+R";
            } else if (info.resigned == GoControl.Player.BLACK) {
                str += ", W+R";
            }
        }

        return str;
    }

    private Handler msg_handler = new Handler(this);

    private BlutoothCommThread comm_thread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_game);

        Bundle bundle;
        Intent intent = getIntent();
        bundle = intent.getExtras();

        GoPlaySetting setting = bundle.getParcelable(GoMessageListener.GAME_SETTING_MESSAGE);

        int bw = setting.wb;
        Log.d("TEST", "bw: " + bw);

        //game = new GoControlBluetooth(bw == 0? GoControl.Player.BLACK : GoControl.Player.WHITE);
        game = new GoControlBluetooth(setting.size, setting.komi,
            setting.handicap, new GoRuleJapan(setting.size),
            bw == 0? GoControl.Player.BLACK : GoControl.Player.WHITE);

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
    }

    private void stop_server_client()
    {
        BlutoothServerThread server;
        BlutoothClientThread client;

        /* stop communicator */
        BlutoothCommThread comm;
        comm = BlutoothCommThread.getInstance();
        if (comm != null) {
            comm.cancel();
            try {
                comm.join();
            } catch (InterruptedException e) {}
        }

        /* stop server */
        server = BlutoothServerThread.getInstance();
        if (server != null) {
            server.cancel();
            try {
                server.join();
            } catch (InterruptedException e) {}
        }

        /* stop client */
        client = BlutoothClientThread.getInstance();
        if (client != null) {
            client.cancel();
            try {
                client.join();
            } catch (InterruptedException e) {}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop_server_client();
    }

    @Override
    public boolean handleMessage(Message msg) {
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

            case GoMessageListener.BLUTOOTH_COMM_ERROR:
                stop_server_client();
                Toast.makeText(this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                return true;

            case GoMessageListener.BLUTOOTH_COMM_MSG:
                BlutoothMsgParser.MsgParsed parsed = (BlutoothMsgParser.MsgParsed) msg.obj;
                handle_comm_message(parsed);
                return true;
        }

        return false;
    }

    public void pass(View view)
    {
        game.pass();
    }

    private void handle_comm_message(BlutoothMsgParser.MsgParsed msg)
    {
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
                Toast.makeText(this, (String) "Opponent passed",
                    Toast.LENGTH_SHORT).show();
                break;

            case RESIGN:
                //game.finish();
                builder = new AlertDialog.Builder(this);
                builder.setMessage(String.format("You(%s) won by resign",
                    game.get_my_color() == GoControl.Player.BLACK? "black" : "white"))
                    .setCancelable(false)
                    .setPositiveButton("OK", null);
                alert = builder.create();
                alert.show();
                game.opponent_resigned();
                finish_game();
                break;

            case RESULT_CONFIRM:
                calc_result_confirm |= OPPONENT_CONFIRMED;
                Toast.makeText(this, (String) "Opponent confirmed result",
                    Toast.LENGTH_SHORT).show();
                check_result();
                break;

            case DECLINE_RESULT:
                calc_result_confirm = 0x00; /* From the beginning */
                Toast.makeText(this, (String) "Opponent declined result",
                    Toast.LENGTH_SHORT).show();

                game.setConfirm_check(false);
                btn_confirm.setEnabled(true);
                break;

            case ACCEPT_RESULT:
                calc_result_confirm |= OPPONENT_ACCEPTED;
                Toast.makeText(this, (String) "Opponent accepted result",
                    Toast.LENGTH_SHORT).show();

                /*try finish game*/
                if ((calc_result_confirm & BOTH_ACCEPTED) == BOTH_ACCEPTED) {
                    Toast.makeText(this, (String) "Game finished",
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
                builder.setTitle("Undo request, accept it?")
                    /*.setMessage("Opponent requested undo. Would you accept request?")*/
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            BlutoothCommThread comm;
                            comm = BlutoothCommThread.getInstance();

                            if (comm == null)
                                return;

                            game.undo_apply(true, false);

                            comm.write(BlutoothMsgParser.make_message(
                                BlutoothMsgParser.MsgType.ACCEPT_UNDO, null));
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            BlutoothCommThread comm;
                            comm = BlutoothCommThread.getInstance();

                            if (comm == null)
                                return;

                            comm.write(BlutoothMsgParser.make_message(
                                BlutoothMsgParser.MsgType.DECLINE_UNDO, null));
                        }
                    })
                    .setMultiChoiceItems(new String[]{"Refuse undo request permanently"},
                        null, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked)
                                    refuse_undo_permanently = true;
                                else
                                    refuse_undo_permanently = false;
                            }
                        })
                ;
                alert = builder.create();
                alert.show();
                break;

            case DECLINE_UNDO:
                game.undo_apply(false, true);
                Toast.makeText(this, (String) "Your undo request was rejected",
                    Toast.LENGTH_SHORT).show();
                break;

            case ACCEPT_UNDO:
                game.undo_apply(true, true);
                break;
        }
    }

    @Override
    public Handler get_msg_handler() {
        return msg_handler;
    }

    public void resign(View view)
    {
        game.resign();

        finish_game();
    }

    public void confirm_result(View view)
    {
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

    private void finish_game()
    {
        game.finish_game();
        btn_confirm.setEnabled(false);
        btn_undo.setEnabled(false);
        btn_pass.setEnabled(false);
        btn_resign.setEnabled(false);

        txt_info.setText(get_info_text());
    }

    private void check_result()
    {
        AlertDialog.Builder builder;

        if ((calc_result_confirm & BOTH_CONFIRMED) != BOTH_CONFIRMED)
            return;

        calc_result_confirm = calc_result_confirm & ~BOTH_ACCEPTED;

        game.setConfirm_check(true);
        btn_confirm.setEnabled(false);

        String message = "";
        GoControl.GoInfo info =  game.get_info();

        message += String.format("white dead : %d, black dead : %d\n",
            info.white_dead, info.black_dead);
        message += String.format("white house : %d, black house : %d\n",
            info.white_score, info.black_score);
        message += String.format("Live W on board : %d\n",
            info.white_count);
        message += String.format("Live B on board : %d\n",
            info.black_count);
        message += String.format("komi : %.1f\n", info.komi);
        message += String.format("white total : %.1f\n", info.white_final);
        message += String.format("black total : %.1f\n", info.black_final);
        message += "Result : ";

        if (info.score_diff == 0)
            message += "DRAW";
        else if (info.score_diff > 0)
            message += String.format("White won by %.1f", info.score_diff);
        else
            message += String.format("Black won by %.1f", Math.abs(info.score_diff));

        builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
            .setTitle("Accept result?")
            .setCancelable(false)
            .setPositiveButton("ACCEPT", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    calc_result_confirm |= YOU_ACCEPTED;
                    BlutoothCommThread comm;
                    comm = BlutoothCommThread.getInstance();

                    if (comm == null)
                        return;
                    comm.write(BlutoothMsgParser.make_message(
                        BlutoothMsgParser.MsgType.ACCEPT_RESULT, null));

                    /*try finish game*/
                    if ((calc_result_confirm & BOTH_ACCEPTED) == BOTH_ACCEPTED) {
                        Toast.makeText(BluetoothGameActivity.this, (String) "Game finished",
                            Toast.LENGTH_SHORT).show();
                        finish_game();
                    }
                }
            })
            .setNegativeButton("DECLINE", new DialogInterface.OnClickListener() {
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

    public void undo(View view)
    {
        game.undo();
    }
}
