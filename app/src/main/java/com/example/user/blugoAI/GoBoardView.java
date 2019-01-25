package com.example.user.blugoAI;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.PathShape;
import android.graphics.drawable.shapes.RectShape;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import static java.lang.Math.round;

/**
 * TODO: document your custom view class.
 */
public class GoBoardView extends View implements GoControl.Callback {
    private Drawable mExampleDrawable;
    private Paint paint = null;

    private final static int OPAQUE_ALPHA = 255;
    private final static int GHOST_ALPHA = 150;
    private final static String alphabet = "ABCDEFGHJKLMNOPQRST";
    private MediaPlayer mplayer;

    private Point ghost_pos = new Point(-1, -1);

    private int board_canvas_x = -1, board_canvas_y = -1;
    private int board_canvas_w = -1, board_canvas_h = -1;
    private int board_square_size = -1;
    private int board_size = 0;

    private GoControl go_control = null;

    private int ssize = 1;
    private int start_p;
    private int end_p;

    public boolean isView_only_mode() {
        return view_only_mode;
    }

    public void setView_only_mode(boolean view_only_mode) {
        this.view_only_mode = view_only_mode;
    }

    private boolean view_only_mode = false;

    public GoControl getGo_control() {
        return go_control;
    }

    public void setGo_control(GoControl go_control) {
        this.go_control = go_control;
        go_control.setCallback_receiver(this);
    }

    public GoBoardView(Context context) {
        super(context);
        init(null, 0);
        if (!isInEditMode())
            mplayer = MediaPlayer.create(this.getContext(), R.raw.tick);
    }

    public GoBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
        if (!isInEditMode())
            mplayer = MediaPlayer.create(this.getContext(), R.raw.tick);
    }

    public GoBoardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
        if (!isInEditMode())
            mplayer = MediaPlayer.create(this.getContext(), R.raw.tick);
    }

    private Point getGhost_pos(float x, float y) {
        Point p = new Point(-1, -1);
        int i, j;
        int found_x = -1, found_y = -1;
        float pos_x, pos_y;
        float tol = board_square_size;
        int board_size;

        if (go_control == null)
            return p;

        if (x < board_canvas_x || x > (board_canvas_x + board_canvas_w))
            return p;

        if (y < board_canvas_y || y > (board_canvas_y + board_canvas_h))
            return p;

        board_size = go_control.getBoardSize();

        for (i = 0; i < board_size; i++) {
            pos_x = board_canvas_x + start_p + board_square_size * i;

            if (x > pos_x - tol && x <= pos_x + tol) {
                found_x = i;
                break;
            }
        }

        if (found_x == -1)
            return p;

        for (j = 0; j < board_size; j++) {
            pos_y = board_canvas_y + start_p + board_square_size * j;

            if (y > pos_y - tol && y <= pos_y + tol) {
                found_y = j;
                break;
            }
        }

        if (found_y == -1)
            return p;

        p.x = found_x;
        p.y = found_y;

        return p;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Point p;

        if (view_only_mode)
            return super.onTouchEvent(event);

        if (go_control == null)
            return true;

        if (!go_control.isMyTurn()) {
            ghost_pos.x = ghost_pos.y = -1;
            return true;
        }

        //Log.d("TOUCHEVT", event.toString());
        p = getGhost_pos(event.getX(), event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!ghost_pos.equals(p)) {
                    ghost_pos.x = p.x;
                    ghost_pos.y = p.y;
                    this.invalidate();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (!ghost_pos.equals(p)) {
                    ghost_pos.x = p.x;
                    ghost_pos.y = p.y;
                    this.invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!p.equals(-1, -1)) {
                    ghost_pos.x = -1;
                    ghost_pos.y = -1;
                    /* If putStoneAt is successful then this view is updated automatically */
                    go_control.putStoneAt(p.x, p.y, false);
                }
                break;
        }
        return true;
        //return super.onTouchEvent(event);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.GoBoardView, defStyle, 0);

        if (a.hasValue(R.styleable.GoBoardView_exampleDrawable)) {
            mExampleDrawable = a.getDrawable(
                    R.styleable.GoBoardView_exampleDrawable);
            Objects.requireNonNull(mExampleDrawable).setCallback(this);
        }

        a.recycle();

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        ShapeDrawable boardline, flower_point, rect;
        Path path;
        super.onDraw(canvas);
        //canvas.drawText();

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        int i;
        int tmp;

        /*Log.d("DIM", getWidth() + "x" + getHeight());*/

        if (contentWidth > contentHeight) {
            board_canvas_h = board_canvas_w = contentHeight;
            board_canvas_x = paddingLeft + (contentWidth - contentHeight) / 2;
            board_canvas_y = paddingTop;
        } else {
            board_canvas_h = board_canvas_w = contentWidth;
            board_canvas_x = paddingLeft;
            board_canvas_y = paddingTop + (contentHeight - contentWidth) / 2;
        }

        // Draw the example drawable on top of the text.
        if (mExampleDrawable != null) {
            mExampleDrawable.setBounds(board_canvas_x, board_canvas_y, board_canvas_x + board_canvas_w, board_canvas_y + board_canvas_h);
            mExampleDrawable.draw(canvas);
        }

        if (go_control == null)
            return;

        board_size = go_control.getBoardSize();

        board_square_size = board_canvas_w / (board_size + 1);
        drawCoordinates(canvas);
        board_canvas_h -= board_square_size;
        board_canvas_w -= board_square_size;
        board_canvas_x += board_square_size;
        board_canvas_y += board_square_size;

        /*
        START_POS_X = (int)(CAN_W - BL_STROKE_SIZE * 19 +  (int) (CAN_W / 19) * 18)/2
         */

        board_square_size = board_canvas_w / board_size;
        path = new Path();

        start_p = (board_canvas_w - board_square_size * (board_size - 1)) / 2;
        end_p = start_p + board_square_size * (board_size - 1);

        for (i = 0; i < board_size; i++) {
            tmp = start_p + board_square_size * i;
            path.moveTo(tmp, start_p);
            path.lineTo(tmp, end_p);

            path.moveTo(start_p, tmp);
            path.lineTo(end_p, tmp);
        }

        path.addRect(start_p, start_p, end_p, end_p, Path.Direction.CW);

        /* draw GoBoard lines */
        //TODO: shoud avoid objects allocations during drawing/layout opeations
        boardline = new ShapeDrawable(new PathShape(path, board_canvas_w, board_canvas_h));
        boardline.getPaint().setColor(0xff000000);
        boardline.getPaint().setStrokeWidth(3);
        boardline.getPaint().setStyle(Paint.Style.STROKE);
        boardline.setBounds(board_canvas_x, board_canvas_y, board_canvas_x + board_canvas_w, board_canvas_y + board_canvas_h);
        boardline.draw(canvas);

        /*
        // Debugging code to show every stone position using circles.
        path = new Path();
        for (i = 0 ; i < board_size ; i++) {
            for (int j = 0 ; j < board_size ; j++) {
                path.addCircle(start_p + board_square_size * i,
                    start_p + board_square_size * j, (board_square_size / 2), Path.Direction.CW);
            }
        }
        boardline = new ShapeDrawable(new PathShape(path, board_canvas_w, board_canvas_h));
        boardline.getPaint().setColor(0xff000000);
        boardline.getPaint().setStrokeWidth(3);
        boardline.getPaint().setStyle(Paint.Style.STROKE);
        boardline.setBounds(board_canvas_x, board_canvas_y, board_canvas_x + board_canvas_w, board_canvas_y + board_canvas_h);
        boardline.draw(canvas);
        */

        int[] flower_pos;

        /* draw star point */
        if (board_size >= 19) {

            flower_pos = new int[]{
                    3, 3, 9, 3, 15, 3,
                    3, 9, 9, 9, 15, 9,
                    3, 15, 9, 15, 15, 15
            };
        } else {
            flower_pos = new int[]{board_size / 2, board_size / 2};
        }

        path = new Path();
        for (i = 0; i < flower_pos.length - 1; ) {
            path.addCircle(start_p + board_square_size * flower_pos[i],
                    start_p + board_square_size * flower_pos[i + 1],
                    6, Path.Direction.CW);

            i += 2;
        }

        flower_point = new ShapeDrawable(new PathShape(path, board_canvas_w, board_canvas_h));
        flower_point.getPaint().setColor(0xff000000);
        flower_point.getPaint().setStrokeWidth(0);
        flower_point.getPaint().setStyle(Paint.Style.FILL);
        flower_point.setBounds(board_canvas_x, board_canvas_y, board_canvas_x + board_canvas_w, board_canvas_y + board_canvas_h);
        flower_point.draw(canvas);

        boolean draw_ghost = false;

        if (go_control.calc_mode()) {
            ArrayList<GoRule.BoardPos> info = go_control.get_calc_info();

            rect = new ShapeDrawable(new RectShape());

            for (i = 0; i < info.size(); i++) {
                GoRule.BoardPos cinfo = info.get(i);

                switch (cinfo.state) {
                    case BLACK:
                        draw_stone(canvas, GoControl.Player.BLACK,
                                i % board_size, i / board_size,
                                OPAQUE_ALPHA);
                        continue;
                    case WHITE:
                        draw_stone(canvas, GoControl.Player.WHITE,
                                i % board_size, i / board_size,
                                OPAQUE_ALPHA);
                        continue;
                }

                switch (cinfo.state) {
                    case EMPTY:
                    case EMPTY_NEUTRAL:
                        rect.getPaint().setColor(0xffff0000);
                        break;

                    case WHITE_DEAD:
                        draw_stone(canvas, GoControl.Player.WHITE,
                                i % board_size, i / board_size,
                                OPAQUE_ALPHA);
                    case EMPTY_BLACK:
                        rect.getPaint().setColor(0xff000000);
                        break;

                    case BLACK_DEAD:
                        draw_stone(canvas, GoControl.Player.BLACK,
                                i % board_size, i / board_size,
                                OPAQUE_ALPHA);
                    case EMPTY_WHITE:
                        rect.getPaint().setColor(0xffffffff);
                        break;
                }

                draw_rect(canvas, rect,
                        i % board_size, i / board_size);
            }
        } else {
            HashSet<GoControl.GoAction> stone_pos = go_control.getStone_pos();

            for (GoControl.GoAction p : stone_pos) {
                if (p.action != GoControl.Action.PUT || p.where == null)
                    continue;

                draw_stone(canvas, p.player, p.where.x, p.where.y, OPAQUE_ALPHA);
            }

            if (view_only_mode) {
                draw_ghost = false;
            } else {
                draw_ghost = go_control.getCurrent_turn() == GoControl.Player.BLACK || go_control.getCurrent_turn() == GoControl.Player.WHITE;
                draw_ghost = draw_ghost && (ghost_pos.x >= 0 && ghost_pos.y < board_size);
                draw_ghost = draw_ghost && (ghost_pos.y >= 0 && ghost_pos.y < board_size);
                draw_ghost = draw_ghost && !stone_pos.contains(new GoControl.GoAction(ghost_pos.x, ghost_pos.y));
            }
        }

        /* draw ghost */
        if (draw_ghost) {
            draw_stone(canvas, go_control.getCurrent_turn(), ghost_pos.x, ghost_pos.y, GHOST_ALPHA);
            draw_guide(canvas, ghost_pos.x, ghost_pos.y);
        } else if (!view_only_mode && go_control.calc_mode()) {
            draw_ghost = (ghost_pos.x >= 0 && ghost_pos.y < board_size);
            draw_ghost = draw_ghost && (ghost_pos.y >= 0 && ghost_pos.y < board_size);

            if (draw_ghost)
                draw_guide(canvas, ghost_pos.x, ghost_pos.y);
        }

        Point cur_coord = go_control.get_cur_coord();

        if (/*view_only_mode &&*/ cur_coord != null) {
            ShapeDrawable red_dot = new ShapeDrawable(new OvalShape());
            red_dot.getPaint().setColor(0xffff0000);

            draw_reddot(canvas, red_dot, cur_coord.x, cur_coord.y);
        }
        /*
        draw_stone(canvas, 0, x, y, 2, 2, width, height, (int) square);
        draw_stone(canvas, 1, x, y, 3, 2, width, height, (int) square);
        draw_stone(canvas, 0, x, y, 4, 2, width, height, (int) square);
        draw_stone(canvas, 0, x, y, 3, 1, width, height, (int) square);
        draw_stone(canvas, 0, x, y, 3, 3, width, height, (int) square);
        */

        Message msg;
        GoBoardViewListener parent = (GoBoardViewListener) this.getContext();
        Handler h;

        h = parent.get_view_msg_handler();
        msg = Message.obtain(h, GoBoardViewListener.MSG_VIEW_FULLY_DRAWN, "msg");
        h.sendMessage(msg);
    }

    private void draw_stone(Canvas canvas, GoControl.Player stone_color, int i, int j, int alpha) {
        int tmpx, tmpy, tmpw, tmph;
        Drawable image;

        if (go_control == null)
            return;

        Context ctx = getContext();

        if (stone_color == GoControl.Player.BLACK)
            image = ctx.getDrawable(R.drawable.go_b_no_bg);
        else
            image = ctx.getDrawable(R.drawable.go_w_no_bg);

        tmph = tmpw = (int) (board_square_size / 2);

        tmpx = board_canvas_x + start_p + board_square_size * i;
        tmpy = board_canvas_y + start_p + board_square_size * j;

        image.setBounds(tmpx - tmpw, tmpy - tmph, tmpx + tmpw, tmpy + tmph);
        image.setAlpha(alpha);
        image.draw(canvas);
    }

    private void draw_guide(Canvas canvas, int i, int j) {
        Path path;
        int tmp;
        ShapeDrawable guid;

        if (go_control == null)
            return;

        path = new Path();

        tmp = start_p + board_square_size * i;
        path.moveTo(tmp, 0);
        path.lineTo(tmp, board_canvas_h);

        tmp = start_p + board_square_size * j;
        path.moveTo(0, tmp);
        path.lineTo(board_canvas_w, tmp);

        /* draw GoBoard lines */
        guid = new ShapeDrawable(new PathShape(path, board_canvas_w, board_canvas_h));
        guid.getPaint().setColor(0xff0000ff);
        guid.getPaint().setStrokeWidth(6);
        guid.getPaint().setStyle(Paint.Style.STROKE);
        guid.setBounds(board_canvas_x, board_canvas_y,
                board_canvas_x + board_canvas_w, board_canvas_y + board_canvas_h);
        guid.draw(canvas);
    }

    private void draw_rect(Canvas canvas, ShapeDrawable s, int i, int j) {
        int tmpx, tmpy, tmpw, tmph;

        if (go_control == null)
            return;

        tmph = tmpw = board_square_size / 5;

        tmpx = board_canvas_x + start_p + board_square_size * i;
        tmpy = board_canvas_y + start_p + board_square_size * j;
        s.setBounds(tmpx - tmpw, tmpy - tmph, tmpx + tmpw, tmpy + tmph);
        s.draw(canvas);
    }

    private void drawCoordinates(Canvas canvas) {
        paint.setTextSize(round(board_square_size * 0.628));
        float x = board_canvas_x + board_square_size * (float) 1.25;
        float y = board_canvas_y + board_square_size;
        for (int i = 0; i < board_size; i++) {
            canvas.drawText(alphabet, i, i + 1, x, y, paint);
            x += board_square_size;
        }
        x = board_canvas_x + board_square_size * (float) 0.3;
        y = board_canvas_y + board_square_size * board_size + board_square_size * (float) 0.75;
        for (int i = 1; i <= board_size; i++) {
            String tmp = i + "   ";
            if (i < 10) tmp = " " + tmp;
            canvas.drawText(tmp, 0, 2, x, y, paint);
            y -= board_square_size;
        }
    }

    private void draw_reddot(Canvas canvas, ShapeDrawable s, int i, int j) {
        int tmpx, tmpy, tmpw, tmph;

        if (go_control == null)
            return;

        tmph = tmpw = board_square_size / 5;

        tmpx = board_canvas_x + start_p + board_square_size * i;
        tmpy = board_canvas_y + start_p + board_square_size * j;
        s.setBounds(tmpx - tmpw, tmpy - tmph, tmpx + tmpw, tmpy + tmph);
        s.draw(canvas);
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mExampleDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mExampleDrawable = exampleDrawable;
    }

    @Override
    public void callback_board_state_changed() {
        this.invalidate();
    }

    @Override
    public void put_stone_success() {
        /* play sound for putting stone */
        mplayer.start();
    }

    public void release_memory() {
        if (mplayer != null) {
            mplayer.release();
        }
    }
}
