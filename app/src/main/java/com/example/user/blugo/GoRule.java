package com.example.user.blugo;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by user on 2016-06-02.
 */
public abstract class GoRule {
    public enum BoardPosState {
        EMPTY(0x00),
        BLACK(0x01),
        WHITE(0x02),
        BLACK_DEAD(0x03),
        WHITE_DEAD(0x04),
        EMPTY_NEUTRAL(0x5),
        EMPTY_BLACK(0x6),
        EMPTY_WHITE(0x7);

        private final int value;

        BoardPosState(int value) {
            this.value = value;
        }

        public static BoardPosState valueOf(int type) {
            /*
            Enumeration values must be sequential or else
            ArrayIndexoutofbound exeception may be thrown.
            */
            return (BoardPosState) BoardPosState.values()[type];
        }

        public int getValue() {
            return value;
        }
    }

    public enum RuleID {
        JAPANESE(0x00),
        CHINESE(0x01);

        private final int value;

        RuleID(int value) {
            this.value = value;
        }

        public static RuleID valueOf(int type) {
            /*
            Enumeration values must be sequential or else
            ArrayIndexoutofbound exeception may be thrown.
            */
            return (RuleID) RuleID.values()[type];
        }

        public int getValue() {
            return value;
        }

        public String get_sgf_string() {
            String sgf = "RU[";

            switch (valueOf(value)) {
                case JAPANESE:
                    sgf += "Japanese";
                    break;

                case CHINESE:
                    sgf += "Chinese";
                    break;

                default:
                    sgf += "Japanese";
                    break;
            }

            sgf += "]";

            return sgf;
        }

        @Override
        public String toString() {
            String result;

            switch (valueOf(value)) {
                case JAPANESE:
                    result = ResStrGenerator.getInstance().get_res_string(R.string.rule_japanese);
                    break;

                case CHINESE:
                    result = ResStrGenerator.getInstance().get_res_string(R.string.rule_chinese);
                    break;

                default:
                    result = ResStrGenerator.getInstance().get_res_string(R.string.rule_japanese);
                    break;
            }

            return result;
        }
    }

    public static class BoardPos {
        BoardPosState state = BoardPosState.EMPTY;
        int group_id = 0;

        @Override
        protected Object clone() throws CloneNotSupportedException {
            BoardPos pos = new BoardPos();
            pos.state = this.state;
            pos.group_id = this.group_id;

            return pos;
        }
    }


    public abstract HashSet<GoControl.GoAction> get_stones();

    public abstract ArrayList<BoardPos> get_calc_info();

    public abstract ArrayList<NewBoardState> get_time_line();

    public abstract ArrayList<GoControl.GoAction> get_action_history();

    /*public abstract ArrayList<BoardState> getTimeline();*/
    public abstract boolean putStoneAt(int x, int y, GoControl.Player stone_color, GoControl.Player next_turn, int board_size);

    public abstract void toggle_owner(int x, int y);

    public abstract void pass(GoControl.Player next_turn);

    public abstract boolean undo();

    public abstract void cancel_calc();

    public abstract void prepare_calc();

    public abstract RuleID get_rule_id();

    public abstract void get_dead(GoControl.GoInfo info);

    public abstract void get_score(GoControl.GoInfo info);
}
