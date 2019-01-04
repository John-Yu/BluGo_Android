package com.example.user.blugo;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by user on 2016-06-04.
 */
public class SgfParser {
    public enum ItemType {
        BOARD_SIZE, /* Type Integer */
        KOMI, /* Type Float */
        WHITE_PUT, /* Type Point */
        BLACK_PUT,
        WHITE_PASS,
        BLACK_PASS,
        TERRITORY_WHITE, /*Specifies the white territory or area*/
        TERRITORY_BLACK, /*Specifies the black territory or area*/
        RULE,
        RESULT,
        ADD_BLACK,
        ADD_WHITE,
        HANDICAP, /*Defines the number of handicap stones (>=2).*/
        COMMENT,
        UNKNOWN,
    }

    private static String[] ItemType_ID = {
            "SZ",
            "KM",
            "W",
            "B",
            "????",
            "????",
            "TW",
            "TB",
            "RU",
            "RE",
            "AB",
            "AW",
            "HA",
            "C",
            "????",
    };

    public static class ParsedItem {
        public ItemType type = ItemType.UNKNOWN;
        public Object content = null;
    }

    //private static final Pattern semicolon = Pattern.compile("(?m);([^;]*)");
    private static final Pattern semicolon = Pattern.compile(";([^;]*)");
    //private static final Pattern command = Pattern.compile("(?m)([A-Z]+)\\[(.*?)\\]");
    //private static final Pattern command = Pattern.compile("([A-Z]+)\\[(.*?)\\]");
    private static final Pattern command = Pattern.compile("([A-Z]+)((?:\\[.*?\\])+)");
    private static final Pattern options = Pattern.compile("\\[(.*?)\\]");
    private static final Pattern position = Pattern.compile("^[a-zA-Z][a-zA-Z]$");
    private static final Pattern win_by = Pattern.compile("([BW])\\+(.*)");

    public ArrayList<ParsedItem> parse(String sgf_string) {
        ArrayList<ParsedItem> parsed_item = new ArrayList<>();
        ArrayList<ParsedItem> result;

        sgf_string = get_main_branch(sgf_string);
        /* Find pattern ';.*?' */
        Matcher m = semicolon.matcher(sgf_string);

        while (m.find()) {
            if (m.groupCount() < 1)
                continue;
            /* Find commands */
            result = find_commands(m.group(1));

            if (result != null)
                parsed_item.addAll(result);
        }

        return parsed_item;
    }

    private String get_main_branch(String sgf_string) {
        // Drop anything outside "(;...)"
        final Pattern SGF_PATTERN = Pattern.compile("(?s).*?(\\(\\s*;.*\\)).*?");
        Matcher sgfMatcher = SGF_PATTERN.matcher(sgf_string);
        if (sgfMatcher.matches()) {
            sgf_string = sgfMatcher.group(1);
        } else {
            return "";
        }
        boolean isMultiGo = false;
        // MultiGo 's branch: (Main Branch (Main Branch) (Branch) )
        // Other 's branch: (Main Branch (Branch) Main Branch)
        if (sgf_string.matches("(?s).*\\)\\s*\\)")) {
            isMultiGo = true;
        }
        int start = 0, end;
        int subTreeDepth = 0;
        StringBuilder res = new StringBuilder();
        // Support unicode characters (UTF-8)
        for (int i = 0; i < sgf_string.length(); i++) {
            char c = sgf_string.charAt(i);
            switch (c) {
                case '(':
                    end = i;
                    if (start < end) {
                        if (subTreeDepth <= 1 || isMultiGo) {
                            res.append(sgf_string.substring(start, end));
                        }
                        start = end + 1;
                    }
                    subTreeDepth += 1;
                    break;
                case ')':
                    end = i;
                    if (start < end) {
                        if (subTreeDepth <= 1 || isMultiGo) {
                            res.append(sgf_string.substring(start, end));
                        }
                        start = end + 1;
                    }
                    subTreeDepth -= 1;
                    if (isMultiGo) {
                        i = sgf_string.length();  // force stop for loop
                    }
                    break;
            }
        }
        res.append(")");
        return res.toString();
    }

    private ParsedItem parse_opt_board_size(String opt) {
        ParsedItem parsed;
        Integer size;

        if (opt == null || opt.length() < 1)
            return null;

        try {
            size = Integer.parseInt(opt);
        } catch (NumberFormatException e) {
            return null;
        }

        parsed = new ParsedItem();
        parsed.type = ItemType.BOARD_SIZE;
        parsed.content = size;

        return parsed;
    }

    private ParsedItem parse_opt_handicap(String opt) {
        ParsedItem parsed;
        Integer value;

        if (opt == null || opt.length() < 1)
            return null;

        try {
            value = Integer.parseInt(opt);
        } catch (NumberFormatException e) {
            return null;
        }

        parsed = new ParsedItem();
        parsed.type = ItemType.HANDICAP;
        parsed.content = value;

        return parsed;
    }

    private ParsedItem parse_opt_komi(String opt) {
        ParsedItem parsed = null;
        Float value;

        if (opt == null || opt.length() < 1)
            return null;

        try {
            value = Float.parseFloat(opt);
        } catch (NumberFormatException e) {
            return null;
        }

        parsed = new ParsedItem();
        parsed.type = ItemType.KOMI;
        parsed.content = value;

        return parsed;
    }

    private ParsedItem parse_rule(String opt) {
        ParsedItem parsed = null;
        GoRule.RuleID rule = GoRule.RuleID.JAPANESE;

        if (opt == null || opt.length() < 1)
            return null;

        if (opt.compareToIgnoreCase("japanese") == 0) {
            rule = GoRule.RuleID.JAPANESE;
        } else if (opt.compareToIgnoreCase("chinese") == 0) {
            rule = GoRule.RuleID.CHINESE;
        }

        parsed = new ParsedItem();
        parsed.type = ItemType.RULE;
        parsed.content = rule;

        return parsed;
    }

    private ParsedItem parse_result(String opt) {
        ParsedItem parsed = null;
        Object[] result = new Object[2];

        if (opt == null || opt.length() < 1)
            return null;

        if (opt.compareToIgnoreCase("draw") == 0) {
            result[0] = null;
            result[1] = opt;

            parsed.content = result;
            parsed.type = ItemType.RESULT;
            return parsed;
        }

        Matcher m = win_by.matcher(opt);

        if (!m.find())
            return null;

        String who_win = m.group(1);
        String win_by_what = m.group(2);

        if (who_win.equals("B")) {
            result[0] = GoControl.Player.BLACK;
        } else
            result[0] = GoControl.Player.WHITE;

        result[1] = win_by_what;

        parsed = new ParsedItem();
        parsed.content = result;
        parsed.type = ItemType.RESULT;

        return parsed;
    }

    private ParsedItem parse_add_white(String opt) {
        ParsedItem parsed = null;
        char c;
        Point p;
        int x, y;

        if (opt == null || opt.length() < 1) {
            return parsed;
        }

        Matcher m = position.matcher(opt);

        if (!m.find())
            return null;

        c = opt.charAt(0);
        x= get_coor(c);

        c = opt.charAt(1);
        y= get_coor(c);

        p = new Point(x, y);

        parsed = new ParsedItem();
        parsed.type = ItemType.ADD_WHITE;
        parsed.content = p;

        return parsed;
    }

    private ParsedItem parse_add_black(String opt) {
        ParsedItem parsed = null;
        char c;
        Point p;
        int x, y;

        if (opt == null || opt.length() < 1) {
            return null;
        }

        Matcher m = position.matcher(opt);

        if (!m.find())
            return null;

        c = opt.charAt(0);
        x= get_coor(c);

        c = opt.charAt(1);
        y= get_coor(c);

        p = new Point(x, y);

        parsed = new ParsedItem();
        parsed.type = ItemType.ADD_BLACK;
        parsed.content = p;

        return parsed;
    }

    private ParsedItem parse_white_put(String opt) {
        ParsedItem parsed = null;
        char c;
        Point p;
        int x, y;

        if (opt == null || opt.length() < 1) {
            parsed = new ParsedItem();
            parsed.type = ItemType.WHITE_PASS;
            parsed.content = null;
            return parsed;
        }

        Matcher m = position.matcher(opt);

        if (!m.find())
            return null;

        c = opt.charAt(0);
        x= get_coor(c);

        c = opt.charAt(1);
        y= get_coor(c);

        p = new Point(x, y);

        parsed = new ParsedItem();
        parsed.type = ItemType.WHITE_PUT;
        parsed.content = p;

        return parsed;
    }

    private ParsedItem parse_black_put(String opt) {
        ParsedItem parsed = null;
        char c;
        Point p;
        int x, y;

        if (opt == null || opt.length() < 1) {
            parsed = new ParsedItem();
            parsed.type = ItemType.BLACK_PASS;
            parsed.content = null;
            return parsed;
        }

        Matcher m = position.matcher(opt);

        if (!m.find())
            return null;

        c = opt.charAt(0);
        x= get_coor(c);

        c = opt.charAt(1);
        y= get_coor(c);

        p = new Point(x, y);

        parsed = new ParsedItem();
        parsed.type = ItemType.BLACK_PUT;
        parsed.content = p;

        return parsed;
    }

    private ParsedItem parse_territory_black(String opt) {
        ParsedItem parsed = null;
        char c;
        Point p;
        int x, y;

        Matcher m = position.matcher(opt);

        if (!m.find())
            return null;

        c = opt.charAt(0);
        x= get_coor(c);

        c = opt.charAt(1);
        y= get_coor(c);

        p = new Point(x, y);

        parsed = new ParsedItem();
        parsed.type = ItemType.TERRITORY_BLACK;
        parsed.content = p;

        return parsed;
    }

    private int get_coor(char c) {
        int x = 0;
        if (c >= 'a' && c <= 'z') {
            x = (int) c - (int) 'a';
        } else {
            x = (int) c - (int) 'A' + 25;
        }
        return x;
    }

    private ParsedItem parse_territory_white(String opt) {
        ParsedItem parsed = null;
        char c;
        Point p;
        int x, y;

        Matcher m = position.matcher(opt);

        if (!m.find())
            return null;

        c = opt.charAt(0);
        x = get_coor(c);

        c = opt.charAt(1);
        y= get_coor(c);

        p = new Point(x, y);

        parsed = new ParsedItem();
        parsed.type = ItemType.TERRITORY_WHITE;
        parsed.content = p;

        return parsed;
    }

    private ArrayList<String> split_options(String text) {
        ArrayList<String> result = new ArrayList<>();
        Matcher m = options.matcher(text);

        while (m.find()) {
            result.add(m.group(1));
        }

        return result;
    }

    private ArrayList<ParsedItem> find_commands(String text) {
        String cmd, opt_string, opt;
        Matcher m = command.matcher(text);
        ItemType type = ItemType.UNKNOWN;
        ArrayList<ParsedItem> parsed_items = new ArrayList<>();
        ParsedItem parsed = null;
        int i;
        ArrayList<String> opts;

        while (m.find()) {
            cmd = m.group(1);
            opt_string = m.group(2);

            opts = split_options(opt_string);
            opt = opts.get(0);

            type = ItemType.UNKNOWN;

            for (i = 0; i < ItemType_ID.length; i++) {
                if (cmd.equals(ItemType_ID[i])) {
                    type = ItemType.values()[i];
                    break;
                }
            }

            switch (type) {
                case BOARD_SIZE:
                    parsed = parse_opt_board_size(opt);
                    if (parsed != null)
                        parsed_items.add(parsed);
                    break;

                case KOMI:
                    parsed = parse_opt_komi(opt);
                    if (parsed != null)
                        parsed_items.add(parsed);
                    break;
                case HANDICAP:
                    parsed = parse_opt_handicap(opt);
                    if (parsed != null)
                        parsed_items.add(parsed);
                    break;
                case WHITE_PUT:
                    parsed = parse_white_put(opt);
                    if (parsed != null)
                        parsed_items.add(parsed);
                    break;

                case BLACK_PUT:
                    parsed = parse_black_put(opt);
                    if (parsed != null)
                        parsed_items.add(parsed);
                    break;

                case ADD_WHITE:
                    for (String each_opt : opts) {
                        parsed = parse_add_white(each_opt);
                        if (parsed != null)
                            parsed_items.add(parsed);
                    }
                    break;

                case ADD_BLACK:
                    for (String each_opt : opts) {
                        parsed = parse_add_black(each_opt);
                        if (parsed != null)
                            parsed_items.add(parsed);
                    }
                    break;

                case RULE:
                    parsed = parse_rule(opt);
                    if (parsed != null)
                        parsed_items.add(parsed);
                    break;

                case TERRITORY_BLACK:
                    for (String each_opt : opts) {
                        parsed = parse_territory_black(each_opt);
                        if (parsed != null)
                            parsed_items.add(parsed);
                    }
                    break;

                case TERRITORY_WHITE:
                    for (String each_opt : opts) {
                        parsed = parse_territory_white(each_opt);
                        if (parsed != null)
                            parsed_items.add(parsed);
                    }
                    break;

                case RESULT:
                    parsed = parse_result(opt);
                    if (parsed != null)
                        parsed_items.add(parsed);
                    break;
            }
        }
        return parsed_items;
    }
}
