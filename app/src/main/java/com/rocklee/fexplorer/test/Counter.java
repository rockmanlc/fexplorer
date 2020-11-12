package com.rocklee.fexplorer.test;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.rocklee.fexplorer.R;

public class Counter extends Activity implements View.OnClickListener, ICounterCallback {
    private static final String TAG = "rocklee";
    private CounterService mCounterService = null;
    private Button startButton = null;
    private Button stopButton = null;
    private TextView counterText = null;

    public static void changed() {
        Log.d(TAG, "C++ call java");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Counter Created");
        setContentView(R.layout.test);
        startButton = (Button)findViewById(R.id.button_start);
        stopButton = (Button)findViewById(R.id.button_stop);
        counterText = (TextView)findViewById(R.id.textView_counter);
        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);

        Intent intent = new Intent(this, CounterService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        String helloJni = stringFromJNI();
        Log.d(TAG, helloJni);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter counterActionFilter = new IntentFilter(CounterService.BROADCAST_COUNTER_ACTION);
        registerReceiver(counterActionReceiver, counterActionFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(startButton)) {
            if (mCounterService != null) {
                Log.d(TAG, "random num is " + mCounterService.getRandomNumber());
                mCounterService.startCounter(0, this);
                //mCounterService.startCounter(0);
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            }
        } else if (v.equals(stopButton)) {
            if (mCounterService != null) {
                mCounterService.stopCounter();
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        }

    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CounterService.CounterBinder binder = (CounterService.CounterBinder) service;
            mCounterService = binder.getService();
            Log.d(TAG, "Counter Service Connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCounterService = null;
            Log.d(TAG, "Counter Service Disconnected");
        }
    };

    @Override
    public void count(int val) {
        String text = String.valueOf(val);
        counterText.setText(text);
    }

    private BroadcastReceiver counterActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int counter = intent.getIntExtra(CounterService.COUNTER_VALUE, 0);
            String text = String.valueOf(counter);
            counterText.setText(text);
            Log.d(TAG, "Receive counter broadcast ACTION");
        }
    };

    public native String stringFromJNI();

    static {
        System.loadLibrary("native-lib");
    }
}
