package com.example.user.blugoAI;

import android.graphics.Point;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by user on 2016-06-08.
 */
public class BlutoothMsgParser {
    public enum MsgType {
        CHAT(0), /* chatting message */
        REQUEST_PLAY(1), /* PLAY REQUEST */
        REQUEST_PLAY_ACK(2), /* PLAY REQUEST ACK */
        PUTSTONE(3), /* PLAY REQUEST ACK */
        PASS(4),
        RESIGN(5),
        RESULT_CONFIRM(6),
        DECLINE_RESULT(7),
        ACCEPT_RESULT(8),
        REQUEST_UNDO(9),
        ACCEPT_UNDO(10),
        DECLINE_UNDO(11),
        UNKNOWN(12);

        private final int value;

        MsgType(int value) {
            this.value = value;
        }

        public static MsgType valueOf(int type) {
            /*
            Enumeration values must be sequential or else
            ArrayIndexoutofbound exeception may be thrown.
            */
            return (MsgType) MsgType.values()[type];
        }

        public int getValue() {
            return value;
        }
    }

    private static String[] msg_type_string = {
            "CHAT",
            "RQ_PLAY",
            "ACK_PLAY",
            "PUTSTONE",
            "PASS",
            "RESIGN",
            "RESULT_CONFIRM",
            "DECLINE_RESULT",
            "ACCEPT_RESULT",
            "REQUEST_UNDO",
            "ACCEPT_UNDO",
            "DECLINE_UNDO",
            "?????"
    };

    public class MsgParsed {
        MsgType type = MsgType.UNKNOWN;
        Object content = null;
    }

    private final Pattern command = Pattern.compile("(?m)\\[(.*?)\\](.*)");
    private final Pattern coord = Pattern.compile("(?m)([a-z])([a-z])");
    /*[RQ_PLAY]r=j,komi=6.5,size=19,wb=0,handicap=0*/
    private final Pattern gsetting =
            Pattern.compile("(?m)r=([jc]),komi=([0-9.]+),size=(\\d+),wb=(\\d+),handicap=(\\d+)");

    private static String make_request_play_message(Object opt) {
        /*
        Example message.
        [RQ_PLAY]r=j,komi=6.5,size=19,wb=0,handicap=0
        */
        String message = "";

        GoPlaySetting setting = (GoPlaySetting) opt;

        message += String.format("r=%c,", setting.rule == 0 ? 'j' : 'c');
        message += String.format("komi=%.1f,size=%d,wb=%d,handicap=%d",
                setting.komi, setting.size, setting.wb, setting.handicap);

        return message;
    }

    public static String make_message(MsgType type, Object opt) {
        String message;
        message = String.format("[%s]", msg_type_string[type.getValue()]);

        if (opt == null)
            return message;

        switch (type) {
            case PUTSTONE:
                Point p = (Point) opt;
                message += String.format("%c%c", (char) (p.x + 'a'), (char) (p.y + 'a'));
                break;

            case REQUEST_PLAY:
                message += make_request_play_message(opt);
                break;

            case REQUEST_PLAY_ACK:
                message += ((Integer) opt).toString();
                break;
        }

        return message;
    }

    private Object parse_putstone(String opt) {
        Matcher m = coord.matcher(opt);
        String x, y;
        Point p;

        if (!m.find())
            return null;

        if (m.groupCount() < 2)
            return null;

        x = m.group(1);
        y = m.group(2);

        p = new Point();

        p.x = (int) (x.charAt(0) - 'a');
        p.y = (int) (y.charAt(0) - 'a');

        return p;
    }

    private Object parse_request_play(String opt) {
        Matcher m = gsetting.matcher(opt);
        GoPlaySetting setting = new GoPlaySetting();
        String tmp;

        if (!m.find())
            return null;

        tmp = m.group(1);
        setting.rule = tmp.charAt(0) == 'c' ? 1 : 0;
        setting.komi = Float.parseFloat(m.group(2));
        setting.size = Integer.parseInt(m.group(3));
        setting.wb = Integer.parseInt(m.group(4));
        setting.handicap = Integer.parseInt(m.group(5));

        return setting;
    }

    private Object parse_request_play_ack(String opt) {
        return Integer.parseInt(opt);
    }

    public MsgParsed parse(String message) {
        String cmd, opt;
        MsgType type;
        int i;

        /* Find pattern ';.*?' */
        Matcher m = command.matcher(message);
        MsgParsed parsed = new MsgParsed();

        if (!m.find())
            return parsed;

        if (m.groupCount() < 1)
            return parsed;

        cmd = m.group(1);
        opt = m.group(2);

        type = MsgType.UNKNOWN;

        for (i = 0; i < msg_type_string.length; i++) {
            if (cmd.equals(msg_type_string[i])) {
                type = MsgType.values()[i];
                break;
            }
        }

        parsed.type = type;

        switch (type) {
            case REQUEST_PLAY_ACK:
                parsed.content = parse_request_play_ack(opt);
                break;

            case REQUEST_PLAY:
                parsed.content = parse_request_play(opt);
                break;

            case PUTSTONE:
                parsed.content = parse_putstone(opt);
                break;
        }

        return parsed;
    }
}
