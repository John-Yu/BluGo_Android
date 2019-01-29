package com.example.user.blugoAI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

/**
 * Created by user on 2016-06-02.
 */
public class GoRuleJapan extends GoRule {
    final ArrayList<NewBoardState> new_timeline = new ArrayList<>();
    private final ArrayList<GoControl.GoAction> action_history = new ArrayList<>();
    private int seq_no = 0;

    private GoRuleJapan() {
    }

    GoRuleJapan(int board_size) {
        /* Always add single empty board */
        new_timeline.add(new NewBoardState(board_size));
    }

    GoRuleJapan(NewBoardState initial_time_line) {
        new_timeline.add(initial_time_line);
    }

    @Override
    public HashSet<GoControl.GoAction> get_stones() {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);
        return state.get_stones();
    }

    @Override
    public ArrayList<BoardPos> get_calc_info() {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);
        return state.get_calc_info();
    }

    @Override
    public ArrayList<NewBoardState> get_time_line() {
        return new_timeline;
    }

    @Override
    public ArrayList<GoControl.GoAction> get_action_history() {
        return action_history;
    }

    public void pass(GoControl.Player next_turn) {
        NewBoardState state = null;

        /* copy time line */
        try {
            NewBoardState tmp = new_timeline.get(new_timeline.size() - 1);
            state = (NewBoardState) (tmp.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        action_history.add(new GoControl.GoAction(
                (next_turn == GoControl.Player.BLACK) ? GoControl.Player.WHITE : GoControl.Player.BLACK,
                null, GoControl.Action.PASS));

        Objects.requireNonNull(state).ko_x = state.ko_y = -1;
        seq_no++;
        new_timeline.add(state);
    }

    @Override
    public boolean putStoneAt(int x, int y, GoControl.Player stone_color, GoControl.Player next_turn, int board_size) {
        NewBoardState state = null;
        GoControl.GoAction pos;

        try {
            NewBoardState tmp = new_timeline.get(new_timeline.size() - 1);
            state = (NewBoardState) (tmp.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        pos = new GoControl.GoAction(stone_color, x, y);

        if (!Objects.requireNonNull(state).put_stone(pos)) {
            state = null;
            return false;
        }

        action_history.add(pos);
        new_timeline.add(state);
        return true;
    }

    @Override
    public void toggle_owner(int x, int y) {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);
        state.toggle_owner(x, y);
    }

    @Override
    public boolean undo() {
        if (new_timeline.size() <= 1)
            return false;

        action_history.remove(action_history.size() - 1);

        new_timeline.remove(new_timeline.size() - 1);
        return true;
    }

    @Override
    public void get_dead(GoControl.GoInfo info) {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);

        info.white_dead = state.white_dead;
        info.black_dead = state.black_dead;
    }

    @Override
    public void get_score(GoControl.GoInfo info) {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);
        state.get_score(info);

        /* japanese counting */
        info.white_final = info.white_score + info.black_dead + info.komi;
        info.black_final = info.black_score + info.white_dead;
        info.score_diff = info.white_final - info.black_final;
    }

    @Override
    public void cancel_calc() {
        new_timeline.remove(new_timeline.size() - 1);
    }

    @Override
    public void prepare_calc() {
        NewBoardState state = null;

        /* copy time line */
        try {
            NewBoardState tmp = new_timeline.get(new_timeline.size() - 1);
            state = (NewBoardState) (tmp.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        Objects.requireNonNull(state).prepare_calc();

        new_timeline.add(state);
    }

    @Override
    public RuleID get_rule_id() {
        return RuleID.JAPANESE;
    }
}
