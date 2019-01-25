package com.example.user.blugoAI;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class FrontDoorActivity extends AppCompatActivity implements FileChooser.FileSelectedListener,
        Handler.Callback, DialogInterface.OnDismissListener, GoMessageListener,
        AdapterView.OnItemSelectedListener {
    public Handler msg_handler = new Handler(this);

    public final static int REQUEST_ENABLE_BT = 1;
    public final static int REQUEST_READ_EXTERNAL_STORAGE = 1;

    public final static String EXTRA_MESSAGE = "com.example.user.blugoAI.FrontDoorActivity.MESSAGE";

    private BluetoothAdapter mBluetoothAdapter = null;

    private Dialog dialog, dialog_rq_confirm;

    private boolean connection_established = false;

    private GoPlaySetting setting;

    private TextView komi;
    private Spinner sp_rule;
    private Spinner sp_board_size;
    private Spinner sp_handicap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front_door);

        Toolbar my_tool_bar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(my_tool_bar);

        dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_Light_Dialog);
        dialog_rq_confirm = new Dialog(this,
                android.R.style.Theme_DeviceDefault_Light_Dialog);

        connection_established = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.front_door_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
            case R.id.action_about:
                // User chose the "Settings" item, show the app settings UI...
                //Toast.makeText(this, "menu test ...", Toast.LENGTH_SHORT).show();
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void load_SGF(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
            return;
        }

        FileChooser f = new FileChooser(this);

        f.setExtension("sgf");
        f.setFileListener(this);
        f.showDialog();
    }

    public void start_single_game(View view) {

        AlertDialog alert;
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = Objects.requireNonNull(inflater).inflate(R.layout.new_single_game, null);

        komi = (TextView) layout.findViewById(R.id.num_komi);
        komi.setText("6.5");

        /* rule */
        sp_rule = (Spinner) layout.findViewById(R.id.sp_rule);
        List<String> rules = new ArrayList<>();
        rules.add(getString(R.string.rule_japanese).toUpperCase());
        rules.add(getString(R.string.rule_chinese).toUpperCase());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rules);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        sp_rule.setAdapter(adapter);
        sp_rule.setOnItemSelectedListener(this);

        /* size : 19, 17, 15, 13, 11, 9, 7, 5, 3 */
        sp_board_size = (Spinner) layout.findViewById(R.id.sp_board_size);
        List<Integer> bd_size = new ArrayList<>();

        for (int i = 19; i >= 3; i -= 2) {
            bd_size.add(i);
        }

        ArrayAdapter<Integer> bd_size_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bd_size);
        bd_size_adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        sp_board_size.setAdapter(bd_size_adapter);
        sp_board_size.setOnItemSelectedListener(this);


        /* Handicap : 2, 3, 4, 5, 6, 7, 8, 9, 13, 16, 25 */
        sp_handicap = (Spinner) layout.findViewById(R.id.sp_handicap);
        List<Integer> handicap = new ArrayList<>();
        handicap.add(0);
        for (int i = 2; i <= 9; i++)
            handicap.add(i);
        handicap.add(13);
        handicap.add(16);
        handicap.add(25);
        ArrayAdapter<Integer> handicap_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, handicap);
        bd_size_adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        sp_handicap.setAdapter(handicap_adapter);
        sp_handicap.setOnItemSelectedListener(this);

        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        builder
                .setView(layout)
                .setTitle(getString(R.string.game_setting))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Message msg;
                    GoPlaySetting setting = new GoPlaySetting();

                    setting.handicap = (Integer) sp_handicap.getSelectedItem();
                    setting.size = (Integer) sp_board_size.getSelectedItem();
                    setting.rule = sp_rule.getSelectedItemPosition();
                    try {
                        setting.komi = Float.parseFloat(komi.getText().toString());
                    } catch (NumberFormatException e) {
                        Log.d("EXP", "'" + komi.getText().toString() + "'" +
                                " cannot be converted to float");
                        setting.komi = (setting.rule == 0) ? 6.5f : 7.5f;
                    }

                    if (setting.handicap > 0 && setting.size < 19) {
                        setting.handicap = 0;
                    }

                    msg = Message.obtain(FrontDoorActivity.this.msg_handler,
                            SINGLE_GAME_SETTING_FINISHED, setting);
                    FrontDoorActivity.this.msg_handler.sendMessage(msg);
                })
                .setNegativeButton(android.R.string.cancel, null);

        alert = builder.create();
        alert.show();
    }

    private Boolean enableBluetooth() {
        if (mBluetoothAdapter == null)
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Log.e("", "Bluetooth isn't supported.");
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else
            return true;

        return false;
    }

    public void wait_game_request(View view) {
        if (!enableBluetooth())
            return;

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = Objects.requireNonNull(inflater).inflate(R.layout.wait_dialog, null);

        TextView t = (TextView) layout.findViewById(R.id.text_b_addr);

        String macAddress;
        /*
        After marshmallow,
        getAddress() method returns only 00:02:00:00:00:00 (deliberated false address).
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            macAddress = android.provider.Settings.Secure.getString(this.getContentResolver(),
                    "bluetooth_address");
        } else {
            macAddress = mBluetoothAdapter.getAddress();
        }

        t.setText(macAddress);
        t = (TextView) layout.findViewById(R.id.text_b_name);
        t.setText(mBluetoothAdapter.getName());

        BlutoothServerThread server = BlutoothServerThread.getInstance(mBluetoothAdapter, this);
        server.start();

        dialog.setContentView(layout);
        dialog.setTitle(getString(R.string.waiting_request) + "...");
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    public void enable_discover(View view) {
        /* goto discoverable mode */
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    public void request_play(View view) {
        if (!enableBluetooth())
            return;

        Intent intent = new Intent(this, PlayRequestActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean handleMessage(Message msg) {
        Random r;
        String m;
        BlutoothServerThread server;
        int my_color;
        Intent intent;
        GoPlaySetting game_setting;
        NewBoardState state;

        switch (msg.what) {
            case GoMessageListener.FRONTDOORACTIVITY_MSG_LOAD_END:
                intent = new Intent(this, ReviewGameActivity.class);
                intent.putExtra(EXTRA_MESSAGE, (String) msg.obj);
                startActivity(intent);
                /* sgf_string = null;*/
                return true;

            case GoMessageListener.BLUTOOTH_SERVER_SOCKET_ERROR:
                server = BlutoothServerThread.getInstance();
                if (server != null) {
                    server.cancel();
                    try {
                        server.join();
                    } catch (InterruptedException e) {
                    }
                    server = null;
                }
                break;

            case GoMessageListener.BLUTOOTH_COMM_ACCEPTED_REQUEST:
                if (setting.wb == 0) {
                    /* random */
                    r = new Random();
                    my_color = r.nextInt(100) % 2;
                } else if (setting.wb == 1) {
                    /* opponent want black */
                    my_color = 1;
                } else {
                    /* opponent want white */
                    my_color = 0;
                }

                Log.d("SERVER", "REQUEST_PLAY RECEIVED");
                server = BlutoothServerThread.getInstance();
                m = BlutoothMsgParser.make_message(BlutoothMsgParser.MsgType.REQUEST_PLAY_ACK,
                        my_color == 0 ? 1 : 0);
                server.get_connected().write(m);

                setting.wb = my_color;

                intent = new Intent(this, BluetoothGameActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(GoMessageListener.GAME_SETTING_MESSAGE, setting);
                intent.putExtras(bundle);

                startActivity(intent);

                break;

            case GoMessageListener.BLUTOOTH_COMM_MSG:
                BlutoothMsgParser.MsgParsed parsed = (BlutoothMsgParser.MsgParsed) msg.obj;
                handle_comm_message(parsed);
                break;

            case GoMessageListener.SINGLE_GAME_SETTING_FINISHED:
                game_setting = (GoPlaySetting) msg.obj;

                intent = new Intent(this, GoBoardActivity.class);
                bundle = new Bundle();

                if (game_setting.handicap > 0)
                    state = NewBoardState.build_handicapped_game(game_setting.handicap);
                else
                    state = new NewBoardState(game_setting.size);

                bundle.putParcelable(ReviewGameActivity.MSG_BOARD_STATE, state);
                bundle.putParcelable(ReviewGameActivity.MSG_SETTING, game_setting);
                bundle.putInt(ReviewGameActivity.MSG_CURRENT_TURN, game_setting.handicap > 0 ? 1 : 0);
                bundle.putInt(ReviewGameActivity.MSG_START_TURNNO, 0);
                bundle.putBoolean(ReviewGameActivity.MSG_ENABLE_SAVE, true);

                intent.putExtras(bundle);

                startActivity(intent);
                break;
        }
        return false;
    }

    private void handle_comm_message(BlutoothMsgParser.MsgParsed msg) {
        switch (msg.type) {
            case REQUEST_PLAY:
                BlutoothServerThread server = BlutoothServerThread.getInstance();

                if (server == null)
                    break;

                setting = (GoPlaySetting) msg.content;


                dialog.dismiss();

                dialog_rq_confirm.setContentView(R.layout.request_confirm);
                dialog_rq_confirm.setTitle(getString(R.string.game_request_arrived));
                dialog_rq_confirm.setCanceledOnTouchOutside(false);

                Button accept_button = (Button) dialog_rq_confirm.findViewById(R.id.btn_accept);
                Button reject_button = (Button) dialog_rq_confirm.findViewById(R.id.btn_reject);

                TextView tmp = (TextView) dialog_rq_confirm.findViewById(R.id.txt_rule);
                tmp.setText(setting.rule == 0 ?
                        getString(R.string.rule_japanese) : getString(R.string.rule_chinese));

                tmp = (TextView) dialog_rq_confirm.findViewById(R.id.txt_handicap);
                tmp.setText(setting.handicap + "");

                tmp = (TextView) dialog_rq_confirm.findViewById(R.id.txt_komi);
                tmp.setText(setting.komi + "");

                tmp = (TextView) dialog_rq_confirm.findViewById(R.id.txt_size);
                tmp.setText(setting.size + "x" + setting.size);

                tmp = (TextView) dialog_rq_confirm.findViewById(R.id.txt_your_color);

                if (setting.wb == 0) {
                    tmp.setText(getString(R.string.random));
                } else if (setting.wb == 1) {
                    tmp.setText(getString(R.string.white));
                } else if (setting.wb == 2) {
                    tmp.setText(getString(R.string.black));
                }

                accept_button.setOnClickListener(v -> {
                    Message msg1 = Message.obtain(msg_handler,
                            GoMessageListener.BLUTOOTH_COMM_ACCEPTED_REQUEST,
                            "connection success");
                    msg_handler.sendMessage(msg1);
                    dialog_rq_confirm.dismiss();
                });

                reject_button.setOnClickListener(v -> {
                    BlutoothServerThread server1 = BlutoothServerThread.getInstance();
                    String m;
                    m = BlutoothMsgParser.make_message(BlutoothMsgParser.MsgType.REQUEST_PLAY_ACK,
                            -1);
                    server1.get_connected().write(m);

                    BlutoothClientThread client;

                    /* stop communicator */
                    BlutoothCommThread comm;
                    comm = BlutoothCommThread.getInstance();
                    if (comm != null) {
                        comm.cancel();
                        try {
                            comm.join();
                        } catch (InterruptedException e) {
                        }
                    }

                    /* stop server */
                    server1 = BlutoothServerThread.getInstance();
                    if (server1 != null) {
                        server1.cancel();
                        try {
                            server1.join();
                        } catch (InterruptedException e) {
                        }
                    }
                    dialog_rq_confirm.dismiss();
                });

                dialog_rq_confirm.show();

                break;
        }
    }

    @Override
    public void fileSelected(File file) {
        Message msg;
        msg = Message.obtain(FrontDoorActivity.this.msg_handler, FRONTDOORACTIVITY_MSG_LOAD_END, file.getPath());
        FrontDoorActivity.this.msg_handler.sendMessage(msg);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        BlutoothServerThread server = BlutoothServerThread.getInstance();

        if (server == null)
            return;

        if (server.get_connected() != null) {
            /* There are established connection */
            return;
        }

        server.cancel();
        try {
            server.join();
        } catch (InterruptedException e) {
        }
    }

    @Override
    public Handler get_go_msg_handler() {
        return this.msg_handler;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Integer value;

        /* Handicapped game normally has no komi */
        if (parent.equals(this.sp_rule) && (Integer) this.sp_handicap.getSelectedItem() < 1) {
            if (position == 0) {
                komi.setText("6.5");
            } else {
                komi.setText("7.5");
            }
        } else if (parent.equals(this.sp_board_size)) {
            value = (Integer) parent.getItemAtPosition(position);
            if (value != 19) {
                sp_handicap.setSelection(0);
            }
        } else if (parent.equals(this.sp_handicap)) {
            value = (Integer) parent.getItemAtPosition(position);
            if (value != 0) {
                /* Choose 19x19 */
                sp_board_size.setSelection(0);
                /* Set komi to 0 */
                komi.setText("0");
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
