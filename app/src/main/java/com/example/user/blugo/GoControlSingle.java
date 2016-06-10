package com.example.user.blugo;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by user on 2016-06-02.
 */
public class GoControlSingle extends GoControl {
    private int board_size = 19;
    protected Player current_turn = Player.BLACK;
    protected GoRule rule;
    protected float komi = 6.5f;
    private int pass_count = 0;
    private static final int MAX_PASS_COUNT = 2;
    private int start_turn = 0;
    /*
    null : Result not determined.
    Positive : W won by this value
    Negative : B won by this value
    0 : draw
     */
    protected Float final_score_diff = null;
    /*
    -1 : No one resigned
    0 : white resigned
    1 : black resigned
     */
    protected int resigned = -1;

    GoControlSingle() {
        this(19, Player.BLACK, null, new GoRuleJapan(19),0);
    }

    GoControlSingle(Callback callback_receiver) {
        this(19, Player.BLACK, callback_receiver, new GoRuleJapan(19), 0);
    }

    GoControlSingle(int board_size, Player current_turn, GoRule rule) {
        this(board_size, current_turn, null, rule, 0);
    }

    GoControlSingle(int board_size, Player current_turn, GoRule rule, int start_turn) {
        this(board_size, current_turn, null, rule, start_turn);
    }

    GoControlSingle(int board_size, Player current_turn, Callback callback_receiver, GoRule rule, int start_turn) {
        this.board_size = board_size;
        this.current_turn = current_turn;
        this.callback_receiver = callback_receiver;
        this.rule = rule;
        this.start_turn = start_turn;
    }

    @Override
    public synchronized boolean isMyTurn() {
        return true;
    }

    @Override
    public synchronized HashSet<GoAction> getStone_pos() {
        return rule.get_stones();
    }

    @Override
    public synchronized int getBoardSize() {
        return board_size;
    }

    @Override
    public synchronized Player getCurrent_turn() {
        return current_turn;
    }

    @Override
    public synchronized boolean putStoneAt(int x, int y, boolean pass) {
        Player next_turn = (current_turn == Player.WHITE)? Player.BLACK : Player.WHITE;

        if (this.calc_mode()) {
            rule.toggle_owner(x, y);

            if (callback_receiver != null)
                callback_receiver.callback_board_state_changed();

            return true;
        }
        /* put stone according to specified RULE */
        if (rule.putStoneAt(x, y, current_turn, next_turn, board_size) == false)
            return false;

        pass_count = 0;

	current_turn = next_turn;

        if (callback_receiver != null)
            callback_receiver.callback_board_state_changed();

        return true;
    }

    @Override
    public String get_sgf() {
        ArrayList<GoAction> actions = rule.get_action_history();
        int i;

        String sgf_string = "(;GM[1]FF[4]CA[UTF-8]\n";
        sgf_string += String.format("SZ[%d]HA[0]KM[%.1f]\n\n", board_size, komi);

        for (i = 0 ; i < actions.size() ; i++) {
            sgf_string += actions.get(i).get_sgf_string() + "\n";
        }
        sgf_string += "\n)";

        return sgf_string;
    }

    @Override
    public synchronized boolean load_sgf(String text) {
        ArrayList<SgfParser.ParsedItem> result;
        SgfParser parser = new SgfParser();
        Point p;
        boolean tmp = false;

        pass_count = 0;

        Log.d("PARS", "SGF parsing started");
        result = parser.parse(text);
        Log.d("PARS", "SGF parsing ended");

        this.current_turn = Player.BLACK;
        this.rule = null;

        /* Two phase first we should get board size first */
        tmp = false;
        /* default board size if we cannot found board size information */
        this.board_size = 19;
        for (SgfParser.ParsedItem item : result) {
            switch (item.type) {
                case BOARD_SIZE:
                    Integer size = (Integer) item.content;
                    this.board_size = size;
                    /* Leave out of loop as soon as possible */
                    tmp = true;
                    break;
            }

            if (tmp == true)
                break;
        }

        this.rule = new GoRuleJapan(this.board_size);

        for (SgfParser.ParsedItem item : result) {
            switch (item.type) {
                /*
                //We already processed board size.
                case BOARD_SIZE:
                    Integer size = (Integer) item.content;
                    this.board_size = size;
                    break;
                    */

                case KOMI:
                    this.komi = (Float) item.content;
                    break;

                case WHITE_PUT:
                    p = (Point) item.content;
                    rule.putStoneAt(p.x, p.y, Player.WHITE, Player.BLACK, board_size);

                    current_turn = Player.BLACK;
                    break;

                case WHITE_PASS:
                    current_turn = Player.BLACK;
                    rule.pass(Player.BLACK);
                    break;

                case BLACK_PUT:
                    p = (Point) item.content;
                    rule.putStoneAt(p.x, p.y, Player.BLACK, Player.WHITE, board_size);

                    current_turn = Player.WHITE;
                    break;

                case BLACK_PASS:
                    current_turn = Player.WHITE;
                    rule.pass(Player.WHITE);
                    break;
            }
        }

        Log.d("PARS", "Game data generation completed");
        //this.callback_receiver.callback_board_state_changed();
        Log.d("PARS", "Draw done");

        return true;
    }

    @Override
    public synchronized boolean pass() {
        int pass;

        if (calc_mode()) {
            return false;
        }

        Player next_turn = (current_turn == Player.WHITE)? Player.BLACK : Player.WHITE;

        current_turn = next_turn;
        rule.pass(next_turn);

        pass_count++;

        /* prepare calc territory */
        if (calc_mode()) {
            rule.prepare_calc();
        }

        if (callback_receiver != null)
            callback_receiver.callback_board_state_changed();

        return true;
    }

    @Override
    public synchronized boolean undo()
    {
        if (calc_mode()) {
            rule.cancel_calc();
        }

        ArrayList<GoAction> history = rule.get_action_history();
        if (history.size() < 1)
            return false;

        GoAction last_action;
        last_action = history.get(history.size() - 1);

	if (!this.rule.undo()) {
	    return false;
	}

        if (last_action.action == Action.PASS && pass_count > 0) {
            pass_count--;
        }

        current_turn = last_action.player;

        this.callback_receiver.callback_board_state_changed();
        return  true;
    }

    @Override
    public GoInfo get_info() {
        GoInfo info = new GoInfo();
        AtomicInteger value1 = new AtomicInteger(0), value2  = new AtomicInteger(0);
        AtomicInteger value3 = new AtomicInteger(0), value4  = new AtomicInteger(0);
        AtomicInteger value5 = new AtomicInteger(0), value6  = new AtomicInteger(0);
        info.turn = this.current_turn;
        info.komi = this.komi;

        rule.get_dead(value1, value2);
        info.white_dead = value1.get();
        info.black_dead = value2.get();

        if (calc_mode()) {
            rule.get_score(value1, value2, value3, value4, value5, value6);
            info.white_score = value1.get();
            info.black_score = value2.get();
            info.white_dead += value3.get();
            info.black_dead += value4.get();
            info.white_count += value5.get();
            info.black_count += value6.get();
        }

        ArrayList<GoAction> history = rule.get_action_history();
        info.turn_num = history.size() + 1 + start_turn;

        /* japanese counting */
        info.white_final = info.white_score + info.black_dead + info.komi;
        info.black_final = info.black_score + info.white_dead;
        info.score_diff = info.white_final - info.black_final;

        info.resigned = this.resigned;

        return info;
    }

    @Override
    public synchronized void new_game() {
        this.current_turn = Player.BLACK;
        this.rule = null;
        this.rule = new GoRuleJapan(board_size);
        this.callback_receiver.callback_board_state_changed();
    }

    public synchronized void load_game(String sgf_string) {
        this.callback_receiver.callback_board_state_changed();
    }

    public boolean calc_mode()
    {
        if (pass_count >= MAX_PASS_COUNT)
            return true;

        return false;
    }

    @Override
    public Point get_cur_coord()
    {
        return null;
    }

    @Override
    public ArrayList<GoRule.BoardPos> get_calc_info() {
        return rule.get_calc_info();
    }

    @Override
    public synchronized void resign() {
        if (current_turn == Player.BLACK) {
            this.resigned = 1;
        } else {
            this.resigned = 0;
        }
    }
}
