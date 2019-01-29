package com.example.user.blugoAI;

import android.graphics.Point;

/**
 * Created by user on 2016-06-12.
 */
public class GoHandicap {
    private static GoHandicap instance = null;
    private final Point[][] position = new Point[26][];

    private GoHandicap() {
        position[2] = new Point[]{new Point(15, 3), new Point(3, 15)};
        position[3] = new Point[]{new Point(3, 3), new Point(15, 3), new Point(3, 15)};
        position[4] = new Point[]{
                new Point(3, 3), new Point(15, 3),
                new Point(3, 15), new Point(15, 15)};
        position[5] = new Point[]{
                new Point(3, 3), new Point(15, 3),
                new Point(9, 9),
                new Point(3, 15), new Point(15, 15)};
        position[6] = new Point[]{
                new Point(3, 3), new Point(15, 3),
                new Point(3, 9), new Point(15, 9),
                new Point(3, 15), new Point(15, 15)};
        position[7] = new Point[]{
                new Point(3, 3), new Point(15, 3),
                new Point(3, 9), new Point(9, 9), new Point(15, 9),
                new Point(3, 15), new Point(15, 15)};
        position[8] = new Point[]{
                new Point(3, 3), new Point(9, 3), new Point(15, 3),
                new Point(3, 9), new Point(15, 9),
                new Point(3, 15), new Point(9, 15), new Point(15, 15)};
        position[9] = new Point[]{
                new Point(3, 3), new Point(9, 3), new Point(15, 3),
                new Point(3, 9), new Point(9, 9), new Point(15, 9),
                new Point(3, 15), new Point(9, 15), new Point(15, 15)};
        position[13] = new Point[]{
                new Point(3, 3), new Point(9, 3), new Point(15, 3),
                new Point(6, 6), new Point(12, 6),
                new Point(3, 9), new Point(9, 9), new Point(15, 9),
                new Point(6, 12), new Point(12, 12),
                new Point(3, 15), new Point(9, 15), new Point(15, 15)};
        position[16] = new Point[]{
                new Point(3, 3), new Point(7, 3), new Point(11, 3), new Point(15, 3),
                new Point(3, 7), new Point(7, 7), new Point(11, 7), new Point(15, 7),
                new Point(3, 11), new Point(7, 11), new Point(11, 11), new Point(15, 11),
                new Point(3, 15), new Point(7, 15), new Point(11, 15), new Point(15, 15)};
        position[25] = new Point[]{
                new Point(3, 3), new Point(6, 3), new Point(9, 3), new Point(12, 3), new Point(15, 3),
                new Point(3, 6), new Point(6, 6), new Point(9, 6), new Point(12, 6), new Point(15, 6),
                new Point(3, 9), new Point(6, 9), new Point(9, 9), new Point(12, 9), new Point(15, 9),
                new Point(3, 12), new Point(6, 12), new Point(9, 12), new Point(12, 12), new Point(15, 12),
                new Point(3, 15), new Point(6, 15), new Point(9, 15), new Point(12, 15), new Point(15, 15),
        };
    }

    public static GoHandicap getInstance() {
        if (instance == null) {
            instance = new GoHandicap();
        }

        return instance;
    }

    public Point[] get_handicap(int handicap) {
        if (handicap < 0 || handicap >= position.length)
            return null;
        return position[handicap];
    }
}
