package com.example.user.blugo;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
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
        TERRITORY_WHITE,
        TERRITORY_BLACK,
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

    public ArrayList<ParsedItem> parse(String sgf_string) {
        ArrayList<ParsedItem> parsed_item = new ArrayList<>();
        ArrayList<ParsedItem> result;

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

    private ParsedItem parse_opt_board_size(String opt)
    {
        ParsedItem parsed = null;
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

    private ParsedItem parse_opt_komi(String opt)
    {
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

    private ParsedItem parse_white_put(String opt)
    {
        ParsedItem parsed = null;
        char c;
        Point p;
        int x, y;

        if (opt == null || opt.length() < 1) {
            parsed = new ParsedItem();
            parsed.type = ItemType.WHITE_PASS;
            parsed.content = null;
            return  parsed;
        }

        Matcher m = position.matcher(opt);

        if (!m.find())
            return null;

        c = opt.charAt(0);
        if (c >= 'a' && c <= 'z') {
            x = (int) c -  (int) 'a';
        } else {
            x = (int) c -  (int) 'A' + 25;
        }

        c = opt.charAt(1);
        if (c >= 'a' && c <= 'z') {
            y = (int) c -  (int) 'a';
        } else {
            y = (int) c - (int) 'A' + 25;
        }

        p = new Point(x, y);

        parsed = new ParsedItem();
        parsed.type = ItemType.WHITE_PUT;
        parsed.content = p;

        return parsed;
    }

    private ParsedItem parse_black_put(String opt)
    {
        ParsedItem parsed = null;
        char c;
        Point p;
        int x, y;

        if (opt == null || opt.length() < 1) {
            parsed = new ParsedItem();
            parsed.type = ItemType.BLACK_PASS;
            parsed.content = null;
            return  parsed;
        }

        Matcher m = position.matcher(opt);

        if (!m.find())
            return null;

        c = opt.charAt(0);
        if (c >= 'a' && c <= 'z') {
            x = (int) c -  (int) 'a';
        } else {
            x = (int) c -  (int) 'A' + 25;
        }

        c = opt.charAt(1);
        if (c >= 'a' && c <= 'z') {
            y = (int) c -  (int) 'a';
        } else {
            y = (int) c - (int) 'A' + 25;
        }

        p = new Point(x, y);

        parsed = new ParsedItem();
        parsed.type = ItemType.BLACK_PUT;
        parsed.content = p;

        return parsed;
    }

    private ParsedItem parse_territory_black(String opt)
    {
        ParsedItem parsed = null;
        char c;
        Point p;
        int x, y;

        Matcher m = position.matcher(opt);

        if (!m.find())
            return null;

        c = opt.charAt(0);
        if (c >= 'a' && c <= 'z') {
            x = (int) c -  (int) 'a';
        } else {
            x = (int) c -  (int) 'A' + 25;
        }

        c = opt.charAt(1);
        if (c >= 'a' && c <= 'z') {
            y = (int) c -  (int) 'a';
        } else {
            y = (int) c - (int) 'A' + 25;
        }

        p = new Point(x, y);

        parsed = new ParsedItem();
        parsed.type = ItemType.TERRITORY_BLACK;
        parsed.content = p;

        return parsed;
    }

    private ParsedItem parse_territory_white(String opt)
    {
        ParsedItem parsed = null;
        char c;
        Point p;
        int x, y;

        Matcher m = position.matcher(opt);

        if (!m.find())
            return null;

        c = opt.charAt(0);
        if (c >= 'a' && c <= 'z') {
            x = (int) c -  (int) 'a';
        } else {
            x = (int) c -  (int) 'A' + 25;
        }

        c = opt.charAt(1);
        if (c >= 'a' && c <= 'z') {
            y = (int) c -  (int) 'a';
        } else {
            y = (int) c - (int) 'A' + 25;
        }

        p = new Point(x, y);

        parsed = new ParsedItem();
        parsed.type = ItemType.TERRITORY_WHITE;
        parsed.content = p;

        return parsed;
    }

    private ArrayList<String> split_options(String text)
    {
	ArrayList<String> result = new ArrayList<>();
	Matcher m = options.matcher(text);

	while (m.find()) {
	    result.add(m.group(1));
	}

        return result;
    }

    private ArrayList<ParsedItem> find_commands(String text)
    {
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

            for (i = 0 ; i < ItemType_ID.length ; i++) {
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
            }

        }

        return parsed_items;
    }
}
