package com.example.user.blugoAI;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by user on 2016-06-07.
 */
public class NewBoardState implements Parcelable {
    /* May one dimensional array be better ? */
    private int[] pos;
    public int size = 19;
    public int ko_x = -1, ko_y = -1;
    public int white_dead = 0, black_dead = 0;

    private NewBoardState(boolean clone) {
        if (clone)
            return;
    }

    private NewBoardState() {
        this(19);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        int i;
        /*Prevent duplicated BoardPos object generation*/
        NewBoardState state = new NewBoardState(true);

        /* swallow copy */
        //System.arraycopy(this.pos, 0, state.pos, 0, this.pos.length);
        state.pos = new int[size * size];
        System.arraycopy(this.pos, 0, state.pos, 0, this.pos.length);

            /*
            for (i = 0 ; i  < pos.length ;i ++) {
                state.pos[i] = (BoardPos) pos[i].clone();
            }
            */

        state.size = size;

        state.ko_x = ko_x;
        state.ko_y = ko_y;

        state.white_dead = white_dead;
        state.black_dead = black_dead;

        return state;
    }

    NewBoardState(int size) {
        this(size, -1);
    }

    private NewBoardState(int size, int ko_x) {
        pos = new int[size * size];

        this.size = size;
        this.ko_x = ko_x;
        this.ko_y = -1;
    }

    private boolean isEmpty(int x, int y) {
        if (x < 0 || x >= size)
            return false;

        if (y < 0 || y >= size)
            return false;

        return (pos[x + y * size] & 0xFF) == GoRule.BoardPosState.EMPTY.getValue();
    }

    private int combine_to_int(int group_id, GoRule.BoardPosState state) {
        return group_id << 8 | state.getValue();
    }

    private int get_group_id(int value) {
        return (value >> 8) & 0xFFFF;
    }

    private GoRule.BoardPosState get_state(int value) {
        /* Enumeration's actual value cannot be zero*/
        return GoRule.BoardPosState.valueOf(value & 0xFF);
    }

    boolean put_stone(GoControl.GoAction action) {
        GoRule.BoardPos spot;
        int x = action.where.x, y = action.where.y;
        /* Because default ko_pos is -1, this value should be -2 */
        int dead_stone = 0, dead;
        Point ko_p = null;
        GoRule.BoardPosState state;
        int group_id;


        /* 1. check if there are already a stone */
        if (!isEmpty(x, y))
            return false;

        /* Put a stone. Link surrounding stones as one group */
        state = (action.player == GoControl.Player.BLACK) ? GoRule.BoardPosState.BLACK : GoRule.BoardPosState.WHITE;
        group_id = get_grpid_new_stone(x, y, state);

        pos[x + y * size] = combine_to_int(group_id, state);

        /* remove opponent dead group */

        dead_stone += dead = try_kill_position(x - 1, y, state);
        if (dead == 1)
            ko_p = new Point(x - 1, y);
        dead_stone += dead = try_kill_position(x + 1, y, state);
        if (dead == 1)
            ko_p = new Point(x + 1, y);
        dead_stone += dead = try_kill_position(x, y - 1, state);
        if (dead == 1)
            ko_p = new Point(x, y - 1);
        dead_stone += dead = try_kill_position(x, y + 1, state);
        if (dead == 1)
            ko_p = new Point(x, y + 1);

        /* Is opponent's dead stone marked as KO */
        if (dead_stone == 1 && ko_p != null && ko_p.x == ko_x && ko_p.y == ko_y) {
            return false;
        }

        /* If we killed just one stone, mark current stone position as a ko */
        if (dead_stone == 1) {
            ko_x = x;
            ko_y = y;
        } else
            /* Else we should clear ko position */
            ko_x = ko_y = -1;

        /* check suicide */
        if (dead_stone == 0 && check_dead(x, y))
            return false;

        if (action.player == GoControl.Player.BLACK)
            white_dead += dead_stone;
        else
            black_dead += dead_stone;

        return true;
    }

    private int get_grpid_new_stone(int x, int y, GoRule.BoardPosState state) {
        int group_id;
        int gid_l, gid_r, gid_u, gid_d;
        boolean lc, rc, uc, dc;

        /*  Get minimum grp_id from surrounding stones */
        group_id = gid_l = left_grp_id(x, y, state);
        lc = gid_l >= 1;

        gid_r = right_grp_id(x, y, state);
        if ((group_id < 1) || (gid_r >= 1 && gid_r < group_id))
            group_id = gid_r;
        rc = gid_r >= 1;

        gid_u = up_grp_id(x, y, state);
        if ((group_id < 1) || (gid_u >= 1 && gid_u < group_id))
            group_id = gid_u;
        uc = gid_u >= 1;

        gid_d = down_grp_id(x, y, state);
        if ((group_id < 1) || (gid_d >= 1 && gid_d < group_id))
            group_id = gid_d;
        dc = gid_d >= 1;

        if (group_id < 1) {
            /* New unlinked single stone */
            group_id = get_next_grpid();
        } else {
                /*
                    minimum grp_id surrounding given stone has been found
                    Adjust surrounding stone's grp_id to minimum because it's now in same group
                */
            if (lc)
                change_grp_id(gid_l, group_id);

            if (rc)
                change_grp_id(gid_r, group_id);

            if (uc)
                change_grp_id(gid_u, group_id);

            if (dc)
                change_grp_id(gid_d, group_id);
        }

        return group_id;
    }

    private int get_next_grpid() {
        int grp_id = 0;
        int i, tmp;

        for (i = 0; i < pos.length; i++) {
            tmp = get_group_id(pos[i]);
            if (tmp > grp_id)
                grp_id = tmp;
        }

        /* now grp_id is max */

        return grp_id + 1;
    }

    private int change_grp_id(int from, int to) {
        int i, j;
        /* No target to change */
        if (from < 0)
            return to;

        /* Cannot change target to */
        if (to < 0)
            return from;

        /* No need to change */
        if (from == to)
            return to;

        /* need optimization */
        for (i = 0; i < pos.length; i++) {
            if (get_group_id(pos[i]) == from)

                pos[i] = combine_to_int(to, get_state(pos[i]));
        }

        return to;
    }

    private boolean check_dead(int x, int y) {
        int i;
        int group_id;
        group_id = get_group_id(pos[x + y * size]);

        /* need optimization */
        for (i = 0; i < pos.length; i++) {
            if (get_group_id(pos[i]) == group_id && calc_life_count(i % size, i / size) > 0) {
                return false;
            }
        }

        return true;
    }

    private int try_kill_position(int x, int y, GoRule.BoardPosState color) {
        int group_id;
        int i, j;
        int dead_count = 0;

        if (x < 0 || x >= size)
            return 0;

        if (y < 0 || y >= size)
            return 0;

        if (get_state(pos[x + y * size]) == GoRule.BoardPosState.EMPTY)
            return 0;

        if (get_state(pos[x + y * size]) == color)
            return 0;

        group_id = get_group_id(pos[x + y * size]);

        /* need optimization */
        for (i = 0; i < pos.length; i++) {
            if (get_group_id(pos[i]) != group_id)
                continue;

            if (calc_life_count(i % size, i / size) > 0)
                return 0;

            dead_count++;
        }

        if (dead_count < 1)
            return dead_count;

        for (i = 0; i < pos.length; i++) {
            if (get_group_id(pos[i]) != group_id)
                continue;
            pos[i] = combine_to_int(0, GoRule.BoardPosState.EMPTY);
        }

        return dead_count;
    }

    private int calc_life_count(int x, int y) {
        int count = 0;

        count += (isEmpty(x - 1, y)) ? 1 : 0;
        count += (isEmpty(x + 1, y)) ? 1 : 0;
        count += (isEmpty(x, y - 1)) ? 1 : 0;
        count += (isEmpty(x, y + 1)) ? 1 : 0;

        return count;
    }

    private int left_grp_id(int x, int y, GoRule.BoardPosState color) {
        x--;
        if (x < 0)
            return 0;

        if (get_state(pos[x + y * size]) == color)
            return get_group_id(pos[x + y * size]);

        return 0;
    }

    private int right_grp_id(int x, int y, GoRule.BoardPosState color) {
        x++;
        if (x >= size)
            return 0;

        if (get_state(pos[x + y * size]) == color)
            return get_group_id(pos[x + y * size]);

        return 0;
    }

    private int up_grp_id(int x, int y, GoRule.BoardPosState color) {
        y--;
        if (y < 0)
            return 0;

        if (get_state(pos[x + y * size]) == color)
            return get_group_id(pos[x + y * size]);

        return 0;
    }

    private int down_grp_id(int x, int y, GoRule.BoardPosState color) {
        y++;
        if (y >= size)
            return 0;

        if (get_state(pos[x + y * size]) == color)
            return get_group_id(pos[x + y * size]);

        return 0;
    }

    public HashSet<GoControl.GoAction> get_stones() {
        int i, j;
        GoRule.BoardPosState cur_state;

        HashSet<GoControl.GoAction> stones = new HashSet<>();
        GoControl.GoAction action;

        /* need optimization */
        for (i = 0; i < pos.length; i++) {
            cur_state = get_state(pos[i]);

            if (cur_state == GoRule.BoardPosState.EMPTY) {
                continue;
            }

            action = new GoControl.GoAction(
                    cur_state == GoRule.BoardPosState.BLACK ?
                            GoControl.Player.BLACK : GoControl.Player.WHITE,
                    i % size, i / size);
            stones.add(action);
        }

        return stones;
    }

    public ArrayList<GoRule.BoardPos> get_calc_info() {
        ArrayList<GoRule.BoardPos> result = new ArrayList<>();
        GoRule.BoardPos bpos;
        int i;

        for (i = 0; i < pos.length; i++) {
            bpos = new GoRule.BoardPos();
            bpos.state = get_state(pos[i]);
            bpos.group_id = get_group_id(pos[i]);
            result.add(bpos);
        }

        return result;
    }

    public void mark_territory(int x, int y, int bw) {
        /*
        bw = 0 : black;
        bw = 1 : white;
         */

        GoRule.BoardPosState state_from = get_state(pos[x + y * size]);
        GoRule.BoardPosState state_to = GoRule.BoardPosState.EMPTY;

        switch (state_from) {
            case EMPTY:
            case EMPTY_NEUTRAL:
            case EMPTY_WHITE:
            case EMPTY_BLACK:
                state_to = (bw == 0) ?
                        GoRule.BoardPosState.EMPTY_BLACK :
                        GoRule.BoardPosState.EMPTY_WHITE;
                break;

            case BLACK:
            case BLACK_DEAD:
                state_to = (bw == 0) ?
                        GoRule.BoardPosState.BLACK :
                        GoRule.BoardPosState.BLACK_DEAD;
                break;

            case WHITE:
            case WHITE_DEAD:
                state_to = (bw == 0) ?
                        GoRule.BoardPosState.WHITE_DEAD :
                        GoRule.BoardPosState.WHITE;
                break;
        }

        if (state_from != state_to)
            pos[x + y * size] = combine_to_int(get_group_id(pos[x + y * size]), state_to);
    }

    public void cancel_calc() {
        int i, j;

        for (i = 0; i < pos.length; i++) {
            switch (get_state(pos[i])) {
                case EMPTY_NEUTRAL:
                case EMPTY_BLACK:
                case EMPTY_WHITE:
                        /*
                        Set group ID to 0. We used group ID in calc mode temporarily
                        to call find_boder function.
                         */
                    pos[i] = combine_to_int(0, GoRule.BoardPosState.EMPTY);
                    break;

                case BLACK_DEAD:
                    pos[i] = combine_to_int(get_group_id(pos[i]), GoRule.BoardPosState.BLACK);
                    break;

                case WHITE_DEAD:
                    pos[i] = combine_to_int(get_group_id(pos[i]), GoRule.BoardPosState.WHITE);
                    break;
            }
        }
    }

    public void toggle_owner(int x, int y) {
        GoRule.BoardPosState from, to;

        try {
            to = from = get_state(pos[x + y * size]);
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        }

        switch (from) {
            case BLACK:
                to = GoRule.BoardPosState.BLACK_DEAD;
                break;

            case WHITE:
                to = GoRule.BoardPosState.WHITE_DEAD;
                break;

            case BLACK_DEAD:
                to = GoRule.BoardPosState.BLACK;
                break;

            case WHITE_DEAD:
                to = GoRule.BoardPosState.WHITE;
                break;

            default:
                return;
        }

        HashSet<Point> empty_pos = new HashSet<>();
        flood_fill(x, y, from, to, empty_pos);

        AtomicInteger boarder_color = new AtomicInteger(0x00);
        HashSet<Point> vhistory = new HashSet<>();
        HashSet<Point> vhistory_all = new HashSet<>();
        GoRule.BoardPosState empty_from, empty_to;

	/*
	  It's slightly faster using vhistory_all then not using it.
	  ms times when using vhistory_all : 203, 203, 204, 211
	  else                             : 320, 266, 305, 289
	 */

        // Log.d("DEBUG", "TOGGLE START");
        for (Point p : empty_pos) {
            if (vhistory_all.contains(p))
                continue;

            empty_from = get_state(pos[p.x + p.y * size]);
            boarder_color.set(0x00);
            /* check border again */
	    /*
	      Because we don't have additional information to stop recursive function call,
	      we provide information for already visited point as a function parameter.
	    */
            vhistory.clear();
            find_border(p.x, p.y, boarder_color, vhistory);

            vhistory_all.addAll(vhistory);

            if (boarder_color.get() == 0x01) {
                flood_fill(p.x, p.y, empty_from, GoRule.BoardPosState.EMPTY_WHITE);
            } else if (boarder_color.get() == 0x02) {
                flood_fill(p.x, p.y, empty_from, GoRule.BoardPosState.EMPTY_BLACK);
            } else
                flood_fill(p.x, p.y, empty_from, GoRule.BoardPosState.EMPTY_NEUTRAL);
        }

        /* Release memory */
        vhistory = null;
        vhistory_all = null;
        // Log.d("DEBUG", "TOGGLE END");
    }

    /* Do flood_fill and find surrounding empty points */
    private void flood_fill(int x, int y, GoRule.BoardPosState from, GoRule.BoardPosState to, HashSet<Point> empty_pos) {
        GoRule.BoardPosState cur_state;
        ArrayList<Point> queue = new ArrayList<>();
        Point p;

        /*We need to change it to non-recursive version*/
        if (x < 0 || x >= size || y < 0 || y >= size)
            return;

        cur_state = get_state(pos[x + y * size]);
        p = new Point(x, y);

        if (empty_pos != null) {
            switch (cur_state) {
                case EMPTY:
                case EMPTY_NEUTRAL:
                case EMPTY_WHITE:
                case EMPTY_BLACK:
                    empty_pos.add(p);
                    break;
            }
        }

        if (cur_state == to || cur_state != from)
            return;

        queue.add(p);

        while (!queue.isEmpty()) {
            p = queue.get(0);
            queue.remove(0);

            /* Boundary over check */
            if (p.x < 0 || p.x >= size || p.y < 0 || p.y >= size)
                continue;

            cur_state = get_state(pos[p.x + p.y * size]);
            if (empty_pos != null) {
                switch (cur_state) {
                    case EMPTY:
                    case EMPTY_NEUTRAL:
                    case EMPTY_WHITE:
                    case EMPTY_BLACK:
                        empty_pos.add(new Point(p.x, p.y));
                        break;
                }
            }

            /* check if we visited it already. */
            if (cur_state == to || cur_state != from)
                continue;

            /* Mark visited */
            pos[p.x + p.y * size] = combine_to_int(get_group_id(pos[p.x + p.y * size]), to);

            /* Apply recursively for each direction of up, down, left, right */
            queue.add(new Point(p.x - 1, p.y));
            queue.add(new Point(p.x + 1, p.y));
            queue.add(new Point(p.x, p.y - 1));
            queue.add(new Point(p.x, p.y + 1));
        }
    }

    private void flood_fill(int x, int y, GoRule.BoardPosState from, GoRule.BoardPosState to) {
        GoRule.BoardPosState cur_state;
        ArrayList<Point> queue = new ArrayList<>();
        Point p;

        if (x < 0 || x >= size || y < 0 || y >= size)
            return;

        cur_state = get_state(pos[x + y * size]);

        if (cur_state == to || cur_state != from)
            return;

        p = new Point(x, y);
        queue.add(p);

        while (!queue.isEmpty()) {
            p = queue.get(0);
            queue.remove(0);

            /* Boundary over check */
            if (p.x < 0 || p.x >= size || p.y < 0 || p.y >= size)
                continue;

            cur_state = get_state(pos[p.x + p.y * size]);
            /* check if we visited it already. */
            if (cur_state == to || cur_state != from)
                continue;

            /* Mark visited */
            pos[p.x + p.y * size] = combine_to_int(get_group_id(pos[p.x + p.y * size]), to);

            /* Apply recursively for each direction of up, down, left, right */
            queue.add(new Point(p.x - 1, p.y));
            queue.add(new Point(p.x + 1, p.y));
            queue.add(new Point(p.x, p.y - 1));
            queue.add(new Point(p.x, p.y + 1));
        }
    }

    private void find_border(int x, int y, AtomicInteger color, HashSet<Point> vhistory) {
        GoRule.BoardPosState state;
        ArrayList<Point> queue = new ArrayList<>();

	/* It's aready visited point.
	   Don't do anything to prevent infinite recursion
	*/
        if (vhistory.contains(new Point(x, y)))
            return;

        if (x < 0 || x >= size || y < 0 || y >= size)
            return;

        queue.add(new Point(x, y));

        while (!queue.isEmpty()) {
            Point p = queue.get(0);
            queue.remove(0);

            /* Boundary over check */
            if (p.x < 0 || p.x >= size || p.y < 0 || p.y >= size)
                continue;

            /* check if we visited it already. */
            if (vhistory.contains(p))
                continue;

            state = get_state(pos[p.x + p.y * size]);

            switch (state) {
                case WHITE:
                case BLACK_DEAD:
                    color.set(color.get() | 0x01);
                    break;
                case BLACK:
                case WHITE_DEAD:
                    color.set(color.get() | 0x02);
                    break;
            }

            switch (state) {
                case EMPTY:
                case EMPTY_NEUTRAL:
                case EMPTY_WHITE:
                case EMPTY_BLACK:
                    break;

                default:
                    /* If it's not empty */
                    continue;
            }

            /* Mark visited */
            vhistory.add(p);

            /* Apply recursively for each direction of up, down, left, right */
            queue.add(new Point(p.x - 1, p.y));
            queue.add(new Point(p.x + 1, p.y));
            queue.add(new Point(p.x, p.y - 1));
            queue.add(new Point(p.x, p.y + 1));
        }
    }

    private void find_border(int x, int y, int group_id, AtomicInteger color) {
        int cur_group_id;
        GoRule.BoardPosState state;
        ArrayList<Point> queue = new ArrayList<>();

        if (x < 0 || x >= size || y < 0 || y >= size)
            return;

        cur_group_id = get_group_id(pos[x + y * size]);

        if (cur_group_id == group_id)
            return;

        queue.add(new Point(x, y));

        while (!queue.isEmpty()) {
            Point p = queue.get(0);
            queue.remove(0);

            /* Boundary over check */
            if (p.x < 0 || p.x >= size || p.y < 0 || p.y >= size)
                continue;

            /* check if we visited it already. */
            cur_group_id = get_group_id(pos[p.x + p.y * size]);
            if (cur_group_id == group_id)
                continue;

            state = get_state(pos[p.x + p.y * size]);

            switch (state) {
                case WHITE:
                case BLACK_DEAD:
                    color.set(color.get() | 0x01);
                    break;
                case BLACK:
                case WHITE_DEAD:
                    color.set(color.get() | 0x02);
                    break;
            }

            switch (state) {
                case EMPTY:
                case EMPTY_NEUTRAL:
                case EMPTY_WHITE:
                case EMPTY_BLACK:
                    break;

                default:
                    /* If it's not empty */
                    continue;
            }

            /* Mark visited */
            pos[p.x + p.y * size] = combine_to_int(group_id, get_state(pos[p.x + p.y * size]));

            /* Apply recursively for each direction of up, down, left, right */
            queue.add(new Point(p.x - 1, p.y));
            queue.add(new Point(p.x + 1, p.y));
            queue.add(new Point(p.x, p.y - 1));
            queue.add(new Point(p.x, p.y + 1));
        }
    }

    /* Roughly determine owner of empty space */
    public void prepare_calc() {
        /* find empty space who's group is 0 */
        int i;
            /*
                0x00 : MET NOTHING -> UNDETERMINED.
                0x01 : MET ONLY WHITE -> Belongs to white territory
                0x02 : MET BLACK -> Belongs to black territory
                0x03 : MET BOTH -> UNDETERMINED.
                */
        AtomicInteger boarder_color = new AtomicInteger(0x00);

        for (i = 0; i < pos.length; i++) {
            if (get_group_id(pos[i]) != 0)
                continue;

            if (get_state(pos[i]) != GoRule.BoardPosState.EMPTY)
                continue;

            boarder_color.set(0x00);

            /* empty space will have respective group id */
            find_border(i % size, i / size, get_next_grpid(), boarder_color);

            if (boarder_color.get() == 0x01) {
                flood_fill(i % size, i / size, get_state(pos[i]), GoRule.BoardPosState.EMPTY_WHITE);
            } else if (boarder_color.get() == 0x02) {
                flood_fill(i % size, i / size, get_state(pos[i]), GoRule.BoardPosState.EMPTY_BLACK);
            }
        }
    }

    public void get_score(GoControl.GoInfo info) {
        int i, j;
        int white_score = 0, black_score = 0;
        int add_wd_count = 0, add_bd_count = 0;
        int wcount_v = 0, bcount_v = 0;

        for (i = 0; i < pos.length; i++) {
            switch (get_state(pos[i])) {
                case BLACK:
                    bcount_v++;
                    break;

                case WHITE:
                    wcount_v++;
                    break;

                case EMPTY:
                case EMPTY_NEUTRAL:
                    break;

                case EMPTY_BLACK:
                    black_score++;
                    break;

                case EMPTY_WHITE:
                    white_score++;
                    break;

                case BLACK_DEAD:
                    white_score++;
                    add_bd_count++;
                    break;

                case WHITE_DEAD:
                    black_score++;
                    add_wd_count++;
                    break;
            }
        }

        info.white_score = white_score;
        info.black_score = black_score;

        info.white_dead = add_wd_count + this.white_dead;
        info.black_dead = add_bd_count + this.black_dead;

        info.white_count = wcount_v;
        info.black_count = bcount_v;
    }


    /* Implementations for Parcelable interface */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(size);
        dest.writeInt(ko_x);
        dest.writeInt(ko_y);
        dest.writeInt(white_dead);
        dest.writeInt(black_dead);

        dest.writeIntArray(pos);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public final static Parcelable.Creator<NewBoardState> CREATOR = new Parcelable.Creator<NewBoardState>() {
        public NewBoardState createFromParcel(Parcel in) {
            return new NewBoardState(in);
        }

        public NewBoardState[] newArray(int size) {
            return new NewBoardState[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private NewBoardState(Parcel in) {
        size = in.readInt();
        ko_x = in.readInt();
        ko_y = in.readInt();
        white_dead = in.readInt();
        black_dead = in.readInt();

        pos = new int[size * size];
        in.readIntArray(pos);
    }

    /* handicap game builder */
    public static NewBoardState build_handicapped_game(int handicap) {
        NewBoardState handicapped;
        int i;

        Point[] handicap_position = GoHandicap.getInstance().get_handicap(handicap);
        if (handicap_position == null)
            return null;

        handicapped = new NewBoardState();

        for (i = 0; i < handicap_position.length; i++) {
            handicapped.put_stone(
                    new GoControl.GoAction(GoControl.Player.BLACK,
                            handicap_position[i],
                            GoControl.Action.PUT));
        }

        return handicapped;
    }
}
