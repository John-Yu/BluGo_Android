package com.example.user.blugoAI;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

/**
 * Created by user on 2016-06-02.
 */
public class GoControlSingle extends GoControl {
    private final static String alphabet = "ABCDEFGHJKLMNOPQRST";
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
    protected Player resigned = null;

    private int handicap = 0;

    private Object[] determined_result = null;
    private boolean _black_is_AI = false;
    private boolean _white_is_AI = false;
    private int current_turn_num = -1;
    private boolean isAIInited = false;
    private boolean isUndo = false;

    public Leela leela;

    GoControlSingle() {
        this(19, Player.BLACK, null, new GoRuleJapan(19), 0);
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

    GoControlSingle(int board_size, Player current_turn, float komi, int handicap, GoRule rule,
                    int start_turn) {
        this(board_size, current_turn, null, rule,
                start_turn, komi, handicap);
    }

    GoControlSingle(int board_size, Player current_turn, Callback callback_receiver, GoRule rule,
                    int start_turn) {
        this(board_size, current_turn, callback_receiver, rule,
                start_turn, 7.5f, 0);
    }

    GoControlSingle(int board_size, Player current_turn, Callback callback_receiver, GoRule rule,
                    int start_turn, float komi, int handicap) {
        this.board_size = board_size;
        this.current_turn = current_turn;
        this.callback_receiver = callback_receiver;
        this.rule = rule;
        this.start_turn = start_turn;
        this.komi = komi;
        this.handicap = handicap;
    }

    private boolean _isMyTurn() {
        if(isAI(current_turn)) return false;
        return resigned == null;
    }

    @Override
    public synchronized boolean isMyTurn() {
        return _isMyTurn();
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
    public synchronized boolean putStoneAt(String namedCoordinate, boolean pass) {
        namedCoordinate = namedCoordinate.trim();
        // coordinates take the form C16 A19 Q5 K10 etc. I is not used.
        int x = alphabet.indexOf(namedCoordinate.charAt(0));
        int y = board_size - Integer.parseInt(namedCoordinate.substring(1));
        return putStoneAt(x,y,pass);
    }

    @Override
    public synchronized boolean putStoneAt(int x, int y, boolean pass) {
        Player next_turn = (current_turn == Player.WHITE) ? Player.BLACK : Player.WHITE;

        if (this.calc_mode()) {
            rule.toggle_owner(x, y);

            if (callback_receiver != null)
                callback_receiver.callback_board_state_changed();

            return true;
        }
        /* put stone according to specified RULE */
        if (!rule.putStoneAt(x, y, current_turn, next_turn, board_size))
            return false;

        pass_count = 0;
        if(!isAI(current_turn)) {
            String color = "w";
            if(current_turn == Player.BLACK) color = "b";
            leela.playMove(color, convertCoordinatesToName(x,y));
            isUndo = false;
        }

        current_turn = next_turn;

        if (callback_receiver != null) {
            callback_receiver.put_stone_success();
            callback_receiver.callback_board_state_changed();
        }

        return true;
    }

    private String get_sgf_for_added_stones() {
        StringBuilder ab = new StringBuilder();
        StringBuilder aw = new StringBuilder();
        /* get first board state */
        ArrayList<NewBoardState> time_line = rule.get_time_line();
        NewBoardState state = time_line.get(0);
        HashSet<GoControl.GoAction> actions = state.get_stones();

        for (GoAction action : actions) {
            switch (action.player) {
                case BLACK:
                    if (ab.length() < 1)
                        ab = new StringBuilder("AB");

                    ab.append("[");
                    ab.append((char) (action.where.x + (int) ('a')));
                    ab.append((char) (action.where.y + (int) ('a')));
                    ab.append("]");
                    break;

                case WHITE:
                    if (aw.length() < 1)
                        aw = new StringBuilder("AW");

                    aw.append("[");
                    aw.append((char) (action.where.x + (int) ('a')));
                    aw.append((char) (action.where.y + (int) ('a')));
                    aw.append("]");
                    break;
            }
        }

        return ab + aw.toString();
    }

    private String get_sgf_from_calc_info() {
        int i, x, y;
        ArrayList<GoRule.BoardPos> info = rule.get_calc_info();

        StringBuilder tw = new StringBuilder();
        StringBuilder tb = new StringBuilder();

        for (i = 0; i < info.size(); i++) {
            GoRule.BoardPos cinfo = info.get(i);

            switch (cinfo.state) {
                case WHITE:
                case BLACK:
                case EMPTY:
                case EMPTY_NEUTRAL:
                    continue;

                case WHITE_DEAD:
                case EMPTY_BLACK:
                    /* black territory */
                    x = i % board_size;
                    y = i / board_size;

                    if (tb.length() < 1)
                        tb = new StringBuilder("TB");

                    tb.append("[");
                    tb.append((char) (x + (int) ('a')));
                    tb.append((char) (y + (int) ('a')));
                    tb.append("]");
                    break;

                case BLACK_DEAD:
                case EMPTY_WHITE:
                    /* white territory */
                    x = i % board_size;
                    y = i / board_size;

                    if (tw.length() < 1)
                        tw = new StringBuilder("TW");

                    tw.append("[");
                    tw.append((char) (x + (int) ('a')));
                    tw.append((char) (y + (int) ('a')));
                    tw.append("]");
                    break;
            }
        }

        return tw.toString() + tb;
    }

    @Override
    public String get_sgf() {
        ArrayList<GoAction> actions = rule.get_action_history();
        int i;

        String sgf_string = "(;GM[1]FF[4]CA[UTF-8]\n";
        sgf_string += String.format("SZ[%d]HA[%d]KM[%.1f]", board_size, handicap, komi);
        sgf_string += this.rule.get_rule_id().get_sgf_string();

        if (resigned != null) {
            sgf_string += "RE[";
            sgf_string += (resigned == Player.BLACK) ? "W+R" : "B+R";
            sgf_string += "]";
        } else if (calc_mode()) {
            GoInfo info = get_info();

            sgf_string += "RE[";

            if (info.score_diff == 0) {
                sgf_string += "Draw";
            } else {
                sgf_string += String.format("%c+%.1f",
                        (info.score_diff > 0) ? 'W' : 'B',
                        Math.abs(info.score_diff));
            }
            sgf_string += "]";
        }

        sgf_string += "\n\n";

        sgf_string += get_sgf_for_added_stones() + "\n\n";

        for (i = 0; i < actions.size(); i++) {
            sgf_string += actions.get(i).get_sgf_string() + "\n";
        }

        if (calc_mode()) {
            String calc_info = get_sgf_from_calc_info();
            sgf_string += "\n" + calc_info;
        }

        sgf_string += "\n)";

        return sgf_string;
    }

    @Override
    public synchronized boolean load_sgf(String text) {
        ArrayList<SgfParser.ParsedItem> result;
        ArrayList<Point> territory_black = new ArrayList<>();
        ArrayList<Point> territory_white = new ArrayList<>();
        ArrayList<GoAction> white_added = new ArrayList<>();
        ArrayList<GoAction> black_added = new ArrayList<>();
        SgfParser parser = new SgfParser();
        Point p;
        GoRule.RuleID rule_id = GoRule.RuleID.JAPANESE;
        NewBoardState state = null;

        pass_count = 0;

        Log.d("PARS", "SGF parsing started");
        result = parser.parse(text);
        Log.d("PARS", "SGF parsing ended");

        this.current_turn = Player.BLACK;
        this.rule = null;

        /* Two phase. we should get board size first */

        /* default board size if we cannot found board size information */
        this.board_size = 19;
        for (SgfParser.ParsedItem item : result) {
            switch (item.type) {
                case BOARD_SIZE:
                    this.board_size = (Integer) item.content;
                    break;

                case RULE:
                    rule_id = (GoRule.RuleID) item.content;
                    break;

                case ADD_WHITE:
                    white_added.add(new GoAction(Player.WHITE, (Point) item.content, Action.PUT));
                    break;

                case ADD_BLACK:
                    white_added.add(new GoAction(Player.BLACK, (Point) item.content, Action.PUT));
                    break;
            }
        }

        /* Add default stones */
        if (white_added.size() > 0) {
            state = new NewBoardState(this.board_size);

            for (GoAction each_action : white_added)
                state.put_stone(each_action);
        }

        if (black_added.size() > 0) {
            if (state == null)
                state = new NewBoardState(this.board_size);

            for (GoAction each_action : black_added)
                state.put_stone(each_action);
        }

        /* Set game rule */
        switch (rule_id) {
            case JAPANESE:
                if (state == null)
                    this.rule = new GoRuleJapan(this.board_size);
                else
                    this.rule = new GoRuleJapan(state);
                break;

            case CHINESE:
                if (state == null)
                    this.rule = new GoRuleChinese(this.board_size);
                else
                    this.rule = new GoRuleChinese(state);
                break;

            default:
                if (state == null)
                    this.rule = new GoRuleJapan(this.board_size);
                else
                    this.rule = new GoRuleJapan(state);
                break;
        }

        /* Parse remained*/
        this.determined_result = null;
        for (SgfParser.ParsedItem item : result) {
            switch (item.type) {
                case KOMI:
                    this.komi = (Float) item.content;
                    break;

                case HANDICAP:
                    this.handicap = (Integer) item.content;
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

                case TERRITORY_WHITE:
                    territory_white.add((Point) item.content);
                    break;

                case TERRITORY_BLACK:
                    territory_black.add((Point) item.content);
                    break;

                case RESULT:
                    this.determined_result = (Object[]) item.content;
                    break;
            }
        }

        /* Final territory information is available */
        if (territory_black.size() > 0 || territory_white.size() > 0) {
            ArrayList<NewBoardState> time_line = rule.get_time_line();
            NewBoardState last = null;
            try {
                last = (NewBoardState) time_line.get(time_line.size() - 1).clone();
            } catch (CloneNotSupportedException e) {
            }

            for (Point each_p : territory_black) {
                Objects.requireNonNull(last).mark_territory(each_p.x, each_p.y, 0);
            }

            for (Point each_p : territory_white) {
                Objects.requireNonNull(last).mark_territory(each_p.x, each_p.y, 1);
            }

            time_line.add(last);
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

        if (!_isMyTurn()) {
            return false;
        }

        Player next_turn = (current_turn == Player.WHITE) ? Player.BLACK : Player.WHITE;

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
    public synchronized boolean undo() {
        if (resigned != null) {
            resigned = null;
            this.callback_receiver.callback_board_state_changed();
            return true;
        }

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
        isUndo = true;
        leela.undo();

        if (last_action.action == Action.PASS && pass_count > 0) {
            pass_count--;
        }

        current_turn = last_action.player;

        this.callback_receiver.callback_board_state_changed();
        return true;
    }

    @Override
    public GoInfo get_info() {
        GoInfo info = new GoInfo();

        info.turn = this.current_turn;
        info.komi = this.komi;

        if (calc_mode()) {
            rule.get_score(info);
        } else {
            rule.get_dead(info);
        }

        info.turn_num = getTurnNum();

        info.resigned = this.resigned;

        return info;
    }

    @Override
    public GoRule.RuleID get_rule() {
        if (this.rule == null)
            return null;

        return this.rule.get_rule_id();
    }

    @Override
    public GoPlaySetting get_game_setting() {
        GoPlaySetting setting = new GoPlaySetting();

        setting.rule = this.get_rule().getValue();
        setting.komi = this.komi;
        setting.size = this.board_size;
        // setting.wb;
        // setting.handicap;

        return setting;
    }

    @Override
    public synchronized void new_game() {
        this.current_turn = Player.BLACK;
        this.rule = null;
        this.rule = new GoRuleJapan(board_size);
        this.isAIInited = false;
        this.callback_receiver.callback_board_state_changed();
    }

    public synchronized void load_game(String sgf_string) {
        this.callback_receiver.callback_board_state_changed();
    }

    public boolean calc_mode() {
        return pass_count >= MAX_PASS_COUNT;
    }

    @Override
    public Point get_cur_coord() {
        ArrayList<GoAction> history = rule.get_action_history();
        if (history.size() < 1 )
            return null;

        GoAction last_action;
        last_action = history.get(history.size() - 1);

        return last_action.where;

    }

    @Override
    public ArrayList<GoRule.BoardPos> get_calc_info() {
        return rule.get_calc_info();
    }

    @Override
    public synchronized void resign() {
        this.resigned = current_turn;
    }

    @Override
    public Player is_resigned() {
        return resigned;
    }

    @Override
    public synchronized void callAI() {
        if(!isAI(current_turn))
        {
            if (callback_receiver != null)
                callback_receiver.callback_send_message(GoBoardViewListener.MSG_VIEW_ENABLE_BUTTON);
            return;
        }
        if(isUndo) return;
        if (this.calc_mode()) return;
        int turnNumber = getTurnNum();
        if (turnNumber == current_turn_num ) return;
        current_turn_num = turnNumber;
        if (callback_receiver != null)
            callback_receiver.callback_send_message(GoBoardViewListener.MSG_VIEW_DISABLE_BUTTON);
        if(!isAIInited){
            InitAI();
            leela.startMonitor();
            isAIInited = true;
        }
        String color = "w";
        if(current_turn == Player.BLACK) color = "b";
        leela.genMove(color);
     }
     private void InitAI(){
        leela.clearBoard();
         HashSet<GoControl.GoAction> stone_pos = getStone_pos();
         for (GoControl.GoAction p : stone_pos) {
             if (p.action != GoControl.Action.PUT || p.where == null)
                 continue;

             String color = "w";
             if(p.player == Player.BLACK) color = "b";
             leela.playMove(color, convertCoordinatesToName( p.where.x, p.where.y));
         }
     }
    /**
     * Converts a x and y coordinate to a named coordinate eg C16, T5, K10, etc
     *
     * @param x x coordinate -- must be valid
     * @param y y coordinate -- must be valid
     * @return a string representing the coordinate
     */
    private  String convertCoordinatesToName(int x, int y) {
        // coordinates take the form C16 A19 Q5 K10 etc. I is not used.
        return alphabet.charAt(x) + "" + (this.board_size - y);
    }
     private int getTurnNum() {
         ArrayList<GoAction> history = rule.get_action_history();
         return  history.size() + 1 + start_turn;
     }
    public boolean isAI(Player player)
    {
        if(player == Player.BLACK) return _black_is_AI;
        else return _white_is_AI;
    }
    public void setAI(Player player)
    {
        if(player == Player.BLACK) _black_is_AI = true;
        else  _white_is_AI = true;
    }

    public int getHandicap() {
        return handicap;
    }

    public void setHandicap(int handicap) {
        this.handicap = handicap;
    }

    public String get_determined_result_string() {
        String result = null;
        String result_convert;

        ResStrGenerator generator = ResStrGenerator.getInstance();

        if (this.determined_result == null)
            return "";

        if (this.determined_result.length < 2)
            return "";

        if (determined_result[0] == null) {
            return generator.get_res_string(R.string.draw);
        }

        switch ((Player) determined_result[0]) {
            case BLACK:
                result = generator.get_res_string(R.string.fmt_black_won_by_2);
                break;

            case WHITE:
                result = generator.get_res_string(R.string.fmt_white_won_by_2);
                break;
        }

        result_convert = (String) determined_result[1];

        if (result_convert.compareToIgnoreCase("r") == 0) {
            result_convert = generator.get_res_string(R.string.resign);
        }

        result = String.format(result, result_convert);

        return result;
    }
}
