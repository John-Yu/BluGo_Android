package com.example.user.blugo;

import android.graphics.Point;

import java.util.ArrayList;

/**
 * Created by user on 2016-06-08.
 */
public class GoControlBluetooth extends GoControlSingle {
    private Player my_turn;
    private boolean game_finished = false;
    private boolean confirm_check = false;
    private boolean undo_waiting = false;

    GoControlBluetooth(Player my_turn) {
        this(19, 6.5f, 0, new GoRuleJapan(19), my_turn, Player.BLACK);
    }

    GoControlBluetooth(int board_size, float komi, int handicap, GoRule rule, Player my_turn,
                       Player start_turn) {
        super(board_size, start_turn, rule);

        this.komi = komi;
        this.my_turn = my_turn;
    }

    private boolean _isMyTurn() {
        if (game_finished)
            return false;

        if (confirm_check)
            return false;

        if (undo_waiting)
            return false;

        BlutoothCommThread comm = BlutoothCommThread.getInstance();

        if (comm == null)
            return false;

        if (calc_mode())
            return true;

        return this.current_turn == my_turn;
    }

    public Player get_my_color() {
        return my_turn;
    }

    @Override
    public synchronized boolean isMyTurn() {
        return _isMyTurn();
    }

    @Override
    public synchronized boolean putStoneAt(int x, int y, boolean pass) {
        boolean success;
        BlutoothCommThread comm = BlutoothCommThread.getInstance();

        success = super.putStoneAt(x, y, pass);

        if (success) {
            comm.write(BlutoothMsgParser.make_message(BlutoothMsgParser.MsgType.PUTSTONE,
                    new Point(x, y)
            ));
        }

        return success;
    }

    public synchronized boolean op_putStoneAt(int x, int y) {

        return super.putStoneAt(x, y, false);
    }

    @Override
    public synchronized boolean pass() {
        BlutoothCommThread comm;

        if (!_isMyTurn())
            return false;

        if (!super.pass())
            return false;

        comm = BlutoothCommThread.getInstance();
        comm.write(BlutoothMsgParser.make_message(BlutoothMsgParser.MsgType.PASS,
                null
        ));

        return true;
    }

    public synchronized void op_pass() {
        super.pass();
    }

    @Override
    public synchronized boolean undo() {
        BlutoothCommThread comm;
        comm = BlutoothCommThread.getInstance();


        ArrayList<GoAction> history = rule.get_action_history();
        if (history.size() < 1)
            return false;

        if (comm == null)
            return false;

        /* super.undo(); */
        if (undo_waiting)
            return false;

        undo_waiting = true;

        comm.write(BlutoothMsgParser.make_message(BlutoothMsgParser.MsgType.REQUEST_UNDO,
                null
        ));

        return true;
    }

    public synchronized void undo_apply(boolean accepted, boolean i_requested) {
        if (accepted) {
            super.undo();

            if (i_requested) {
                while (current_turn != my_turn) {
                    if (!super.undo())
                        break;
                }
                /* now again current_turn == my_turn */
            } else {
                while (current_turn == my_turn) {
                    if (!super.undo())
                        break;
                }

                /* now again current_turn != my_turn */
            }
        }
        undo_waiting = false;
    }

    @Override
    public synchronized void resign() {
        BlutoothCommThread comm;
        comm = BlutoothCommThread.getInstance();

        if (comm == null)
            return;

        this.resigned = my_turn;

        comm.write(BlutoothMsgParser.make_message(BlutoothMsgParser.MsgType.RESIGN,
                null
        ));
    }

    public synchronized void opponent_resigned() {
        if (this.my_turn == Player.BLACK) {
            this.resigned = Player.WHITE;
        } else {
            this.resigned = Player.BLACK;
        }
    }

    public void finish_game() {
        game_finished = true;
    }

    public void setConfirm_check(boolean confirm_check) {
        this.confirm_check = confirm_check;
    }
}
