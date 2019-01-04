package com.example.user.blugo;

import android.Manifest;
import android.support.annotation.NonNull;
import android.widget.ProgressBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PlayRequestActivity extends AppCompatActivity implements GoMessageListener,
        Handler.Callback, AdapterView.OnItemSelectedListener {
    public final static int REQUEST_COARSE_LOCATION = 2;

    private ListView dev_listview;
    private ArrayList<BluetoothDeviceWrap> device_array;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<BluetoothDeviceWrap> arrayAdapter;
    private SingleReceiver mReceiver = null;
    private ProgressBar pbar_discover;
    public Handler msg_handler = new Handler(this);

    private Spinner sp_rule, sp_board_size, sp_wb, sp_handicap;
    private EditText komi;

    private ProgressBar load_progress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_request);

        dev_listview = (ListView) findViewById(R.id.device_list);
        device_array = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, device_array);
        dev_listview.setAdapter(arrayAdapter);
        dev_listview.setOnItemClickListener(
                (a, v, position, id) -> {
                    Object o = dev_listview.getItemAtPosition(position);
                    BluetoothDevice device = ((BluetoothDeviceWrap) o).getBluetoothDevice();

                    BlutoothClientThread client = BlutoothClientThread.getInstance();

                    if (client != null) {
                        client.cancel();
                        try {
                            client.join();
                        } catch (InterruptedException e) {
                        }
                    }

                    client = BlutoothClientThread.getInstance(mBluetoothAdapter, device,
                            PlayRequestActivity.this);
                    client.start();

                    /* pop-up wait dialog */
                    load_progress = new ProgressBar(PlayRequestActivity.this);
                    load_progress.setProgress(0);
                    load_progress.setMax(100);
                    load_progress.setIndeterminate(true);
                    load_progress.setVisibility(View.VISIBLE);
                }
        );

        pbar_discover = (ProgressBar) findViewById(R.id.discover_on_progress);
        pbar_discover.setVisibility(View.VISIBLE);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listBluetoothDevice();

        /* rule */
        sp_rule = (Spinner) findViewById(R.id.sp_rule);
        List<String> rules = new ArrayList<>();
        rules.add(getString(R.string.rule_japanese).toUpperCase());
        rules.add(getString(R.string.rule_chinese).toUpperCase());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rules);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        sp_rule.setAdapter(adapter);
        sp_rule.setOnItemSelectedListener(this);

        /* size : 19, 17, 15, 13, 11, 9, 7, 5, 3 */
        sp_board_size = (Spinner) findViewById(R.id.sp_board_size);
        List<Integer> bd_size = new ArrayList<>();

        for (int i = 19; i >= 3; i -= 2) {
            bd_size.add(i);
        }

        ArrayAdapter<Integer> bd_size_adapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, bd_size);
        bd_size_adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        sp_board_size.setAdapter(bd_size_adapter);
        sp_board_size.setOnItemSelectedListener(this);

        /* w/b : random, black, white */
        sp_wb = (Spinner) findViewById(R.id.sp_wb);
        List<String> wb_choose = new ArrayList<>();
        wb_choose.add(getString(R.string.random));
        wb_choose.add(getString(R.string.black));
        wb_choose.add(getString(R.string.white));
        ArrayAdapter<String> wb_choose_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, wb_choose);
        wb_choose_adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        sp_wb.setAdapter(wb_choose_adapter);

        /* Handicap : 2, 3, 4, 5, 6, 7, 8, 9, 13, 16, 25 */
        sp_handicap = (Spinner) findViewById(R.id.sp_handicap);
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

        komi = (EditText) findViewById(R.id.num_komi);

        reset_default(null);
    }

    public void reset_default(View view) {
        sp_rule.setSelection(0, false);

        /* default 19x19 */
        sp_board_size.setSelection(0);

        /* Random */
        sp_wb.setSelection(0);

        /* Handicap 0 */
        sp_handicap.setSelection(0);

        komi.setText("6.5");
    }

    private GoPlaySetting get_play_setting() {
        GoPlaySetting setting = new GoPlaySetting();

        setting.rule = sp_rule.getSelectedItemPosition();
        try {
            setting.komi = Float.parseFloat(komi.getText().toString());
        } catch (NumberFormatException e) {
            Log.d("EXP", "'" + komi.getText().toString() + "'" +
                    " cannot be converted to float");
            setting.komi = (setting.rule == 0) ? 6.5f : 7.5f;
        }
        setting.size = (Integer) sp_board_size.getSelectedItem();
        setting.wb = sp_wb.getSelectedItemPosition();
        setting.handicap = (Integer) sp_handicap.getSelectedItem();

        return setting;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /* Make sure client closed */
        BlutoothClientThread client = BlutoothClientThread.getInstance();

        if (client != null) {
            client.cancel();
            try {
                client.join();
            } catch (InterruptedException e) {
            }
            client = null;
        }

        /* We must execute below codes */
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        if (mReceiver != null) {
            try {
                unregisterReceiver(mReceiver);
            } catch (IllegalArgumentException e) {
                Log.d("DEBUG", e.toString());
            }
            mReceiver = null;
        }
    }

    public void listBluetoothDevice() {
        BluetoothDeviceWrap wrap;

        device_array.clear();

        /* List already found devices */
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d("MYTAG", "Device : " + String.format("%s, %s", device.getName(), device.getAddress()));
                wrap = new BluetoothDeviceWrap(device);

                if (!device_array.contains(wrap)) {
                    device_array.add(wrap);
                }
            }
        }

        arrayAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    proceedDiscovery();
                }
                break;
        }
    }

    protected void proceedDiscovery() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        if (mReceiver == null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            mReceiver = new SingleReceiver();
            registerReceiver(mReceiver, filter);
        }

        Log.d("TAG", "Result:" + mBluetoothAdapter.startDiscovery());
    }

    public void discover_bluetooth_devices(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION);
        } else
            proceedDiscovery();
    }

    @Override
    public Handler get_go_msg_handler() {
        return msg_handler;
    }

    @Override
    public boolean handleMessage(Message msg) {
        String m;
        switch (msg.what) {
            case GoMessageListener.BLUTOOTH_CLIENT_SOCKET_ERROR:
                if (load_progress != null)
                    load_progress.setVisibility(View.GONE);     // To Hide ProgressBar
                Toast.makeText(this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                break;
            case GoMessageListener.BLUTOOTH_CLIENT_CONNECT_SUCCESS:
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }

                if (mReceiver != null) {
                    try {
                        unregisterReceiver(mReceiver);
                    } catch (IllegalArgumentException e) {
                        Log.d("DEBUG", e.toString());
                    }
                    mReceiver = null;
                }

                Toast.makeText(this, (String) msg.obj, Toast.LENGTH_SHORT).show();

                /* send game request */
                m = BlutoothMsgParser.make_message(BlutoothMsgParser.MsgType.REQUEST_PLAY,
                        this.get_play_setting());

                BlutoothCommThread connected = BlutoothCommThread.getInstance();
                if (connected != null)
                    connected.write(m);
                break;

            case GoMessageListener.BLUTOOTH_COMM_MSG:
                BlutoothMsgParser.MsgParsed parsed = (BlutoothMsgParser.MsgParsed) msg.obj;
                handle_comm_message(parsed);
                break;
        }
        return false;
    }

    private void handle_comm_message(BlutoothMsgParser.MsgParsed msg) {
        String m;
        Integer tmp;
        switch (msg.type) {
            case REQUEST_PLAY_ACK:
                if (load_progress != null)
                    load_progress.setVisibility(View.GONE);     // To Hide ProgressBar

                Log.d("CLIENT", "PLAY_ACK RECEIVED");
                tmp = (Integer) msg.content;

                if (tmp < 0) {
                    Toast.makeText(this, (String) "Requested game setting was refused",
                            Toast.LENGTH_SHORT).show();
                    break;
                }

                GoPlaySetting setting = this.get_play_setting();
                setting.wb = tmp;

                Intent intent = new Intent(this, BluetoothGameActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(GoMessageListener.GAME_SETTING_MESSAGE, setting);
                intent.putExtras(bundle);

                startActivity(intent);
                break;
        }
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

    private class BluetoothDeviceWrap {
        public BluetoothDevice device;

        public BluetoothDeviceWrap(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public String toString() {
            return String.format("%s (%s)", device.getAddress(), device.getName());
        }

        @Override
        public boolean equals(Object o) {
            BluetoothDeviceWrap target = (BluetoothDeviceWrap) o;
            return device.equals(target.device);
        }

        public BluetoothDevice getBluetoothDevice() {
            return this.device;
        }
    }

    private class SingleReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDeviceWrap wrap;

            Log.d("MYTAG", "Received : " + action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("FOUND", String.format("%s, %s", device.getName(), device.getAddress()));

                wrap = new BluetoothDeviceWrap(device);

                if (!device_array.contains(wrap)) {
                    device_array.add(wrap);
                    arrayAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                pbar_discover.setVisibility(View.VISIBLE);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                pbar_discover.setVisibility(View.INVISIBLE);
            }
        }
    }
}
