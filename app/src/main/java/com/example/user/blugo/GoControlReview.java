package com.example.user.blugo;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by user on 2016-06-07.
 */
public class GoControlReview extends GoControlSingle {
    private int cur_pos = 0;

    GoControlReview() {
        super();
    }

    @Override
    public synchronized HashSet<GoAction> getStone_pos() {
        ArrayList<NewBoardState> time_line;

        time_line = rule.get_time_line();

        return time_line.get(cur_pos).get_stones();
    }

    @Override
    public synchronized Player getCurrent_turn() {
        ArrayList<GoAction> history = rule.get_action_history();
        if (history.size() < 1 || cur_pos < 1)
            return Player.BLACK;

        GoAction last_action;
        last_action = history.get(cur_pos - 1);

        return (last_action.player == Player.WHITE) ? Player.BLACK : Player.WHITE;
    }

    @Override
    public synchronized boolean load_sgf(String text) {
        boolean result = super.load_sgf(text);

        cur_pos = get_last_pos();

        return result;
    }

    @Override
    public boolean calc_mode() {
        int diff = rule.get_time_line().size() - rule.get_action_history().size();
        int last_pos = rule.get_time_line().size() - 1;

        if (diff >= 2 && cur_pos >= last_pos) {
            return true;
        }

        return false;
    }

    public int get_last_pos() {
        ArrayList<NewBoardState> time_line;
        time_line = rule.get_time_line();
        return time_line.size() - 1;
    }

    public NewBoardState get_current_board_state() {
        ArrayList<NewBoardState> time_line;

        time_line = rule.get_time_line();

        return time_line.get(cur_pos);
    }

    public synchronized boolean goto_pos(int pos) {
        ArrayList<NewBoardState> time_line;

        if (pos < 0)
            return false;

        time_line = rule.get_time_line();

        if (pos >= time_line.size())
            return false;

        cur_pos = pos;

        this.callback_receiver.callback_board_state_changed();

        return true;
    }

    public int getCur_pos() {
        return cur_pos;
    }

    public Point get_cur_coord() {
        ArrayList<GoAction> history = rule.get_action_history();
        if (history.size() < 1 || cur_pos < 1 || cur_pos > history.size())
            return null;

        GoAction last_action;
        last_action = history.get(cur_pos - 1);

        return last_action.where;
    }
}
