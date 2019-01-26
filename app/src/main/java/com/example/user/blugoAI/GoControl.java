package com.example.user.blugoAI;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by user on 2016-06-02.
 */
public abstract class GoControl {
    protected Callback callback_receiver = null;

    public enum Action {
        PUT, PASS,
    }

    public enum Player {
        WHITE, BLACK,
    }

    public static class GoAction {
        /* who */
        public Player player;
        /* what */
        public Action action = Action.PUT;
        /* where */
        public Point where = null;
        public int group_id = 0;

        public GoAction(int x, int y) {
            this(Player.BLACK, new Point(x, y), Action.PUT);
        }

        public GoAction(Player player, int x, int y) {
            this(player, new Point(x, y), Action.PUT);
        }

        public GoAction(Player player, int x, int y, Action action) {
            this(player, new Point(x, y), action);
        }

        public GoAction(Player player, Point where, Action action) {
            this(player, where, action, 0);
        }

        public GoAction(Player player, Point where, Action action, int group_id) {
            this.player = player;
            this.where = where;
            this.action = action;
            this.group_id = group_id;
        }

        public String get_sgf_string() {
            String string = ";";
            switch (player) {
                case WHITE:
                    string += "W";
                    break;
                case BLACK:
                    string += "B";
                    break;
            }

            string += "[";

            if (action == Action.PUT && where != null) {
                /* a - z : 0 ~ 24 */
                /* A - Z : 25 ~ 49 */
                string += (char) (where.x + (int) ('a'));
                string += (char) (where.y + (int) ('a'));
            }

            string += "]";

            return string;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            Point p;
            if (where == null)
                p = null;
            else
                p = new Point(where.x, where.y);

            return new GoAction(player, p, action, group_id);
        }

        @Override
        public boolean equals(Object o) {
            GoAction go_action;
            Point p;

            go_action = (GoAction) o;

            if (go_action.where == null)
                return false;

            if (this.where == null)
                return false;

            return this.where.equals(go_action.where);
        }

        @Override
        public int hashCode() {
            if (where != null)
                return where.hashCode();

            return super.hashCode();
        }

        @Override
        public String toString() {
            return get_sgf_string();
        }
    }

    public interface Callback {
        void callback_board_state_changed();
        void callback_send_message(int msgID);
        void put_stone_success();
    }

    public static class GoInfo {
        public int white_dead = 0;
        public int black_dead = 0;

        public int white_score = 0;
        public int black_score = 0;

        public int white_count = 0;
        public int black_count = 0;

        public float white_final = 0.0f;
        public float black_final = 0.0f;
        /*white final score - black final score */
        public float score_diff = 0.0f;

        public Player resigned = null;

        public float komi = 7.5f;
        public int turn_num = 0;
        public Player turn = Player.BLACK;
    }

    public abstract boolean isMyTurn();

    public abstract HashSet<GoAction> getStone_pos();

    public abstract int getBoardSize();

    public abstract Player getCurrent_turn();

    public abstract boolean putStoneAt(int x, int y, boolean pass);

    public abstract String get_sgf();

    public abstract boolean load_sgf(String text);

    public abstract boolean calc_mode();

    public abstract ArrayList<GoRule.BoardPos> get_calc_info();

    public abstract Point get_cur_coord();

    public abstract void callAI();

    public abstract boolean pass();

    public abstract boolean undo();

    public abstract void resign();

    public abstract Player is_resigned();

    public abstract GoInfo get_info();

    public abstract GoRule.RuleID get_rule();

    public abstract GoPlaySetting get_game_setting();

    public void new_game() {

    }

    public Callback getCallback_receiver() {
        return callback_receiver;
    }

    public void setCallback_receiver(Callback callback_receiver) {
        this.callback_receiver = callback_receiver;

        callback_receiver.callback_board_state_changed();
    }
}
