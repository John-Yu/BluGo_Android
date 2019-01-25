package com.example.user.blugoAI;

import android.os.Handler;

/**
 * Created by user on 2016-06-07.
 */
public interface GoBoardViewListener {
    int MSG_VIEW_FULLY_DRAWN = 1;

    Handler get_view_msg_handler();
}
