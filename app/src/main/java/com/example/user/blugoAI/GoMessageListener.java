package com.example.user.blugoAI;

import android.os.Handler;

/**
 * Created by user on 2016-06-08.
 */
public interface GoMessageListener {
    int BLUTOOTH_SERVER_SOCKET_ERROR = 2;
    int BLUTOOTH_CLIENT_SOCKET_ERROR = 3;
    int BLUTOOTH_CLIENT_CONNECT_SUCCESS = 4;
    int BLUTOOTH_COMM_ERROR = 5;
    int BLUTOOTH_COMM_MSG = 6;
    int BLUTOOTH_COMM_ACCEPTED_REQUEST = 7;
    int FRONTDOORACTIVITY_MSG_LOAD_END = 8;
    int SAVE_FILE_NAME_INPUT_FINISHED = 9;
    int SINGLE_GAME_SETTING_FINISHED = 10;
    int MSG_LOAD_END = 11;
    int MSG_LOAD_FAIL = 12;
    int MSG_LOAD_BEGIN = 13;
    int MSG_AI_NOT_LOAD = 14;

    String GAME_SETTING_MESSAGE =
            "com.example.user.blugoAI.GoMessageListener.GAME_SETTING_MESSAGE";

    Handler get_go_msg_handler();
}
