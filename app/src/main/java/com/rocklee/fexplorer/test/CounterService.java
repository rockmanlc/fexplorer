package com.rocklee.fexplorer.test;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.Random;

public class CounterService extends Service  implements ICounterService {
    private static final String TAG = "rocklee";
    private boolean mStop = false;
    public final static String BROADCAST_COUNTER_ACTION = "broadcounter.COUNTER_ACTION";
    public final static String COUNTER_VALUE = "broadcounter.counter.value";
    private final IBinder binder = new CounterBinder();
    private final Random mGenerator = new Random();
    private ICounterCallback mCounterCallback = null;

    @Override
    public void startCounter(int initVal, ICounterCallback callback) {
        mCounterCallback = callback;
        mStop = false;
        AsyncTask<Integer, Integer, Integer> task = new AsyncTask<Integer, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Integer... integers) {
                Integer initCounter = integers[0];

                while (!mStop) {
                   publishProgress(initCounter);

                   try {
                       Thread.sleep(1000);
                   } catch (InterruptedException e) {
                       e.printStackTrace();
                   }
                   initCounter++;
                }
                return initCounter;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate();
                int val = values[0];
                mCounterCallback.count(val);
                Log.d(TAG, "service onProgressUpdate");
            }

            @Override
            protected void onPostExecute(Integer val) {
                mCounterCallback.count(val);
                Log.d(TAG, "service onPostExecute");
            }
        };
        task.execute(initVal);
    }

    @Override
    public void startCounter(final int initVal) {
        mStop = false;
        final AsyncTask<Integer, Integer, Integer> task = new AsyncTask<Integer, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Integer... integers) {
                Integer initCounter = integers[0];
                while (!mStop) {
                    publishProgress(initCounter);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    initCounter++;
                }
                return initCounter;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate();
                int val = values[0];
                Intent intent = new Intent(BROADCAST_COUNTER_ACTION);
                intent.putExtra(COUNTER_VALUE, val);
                sendBroadcast(intent);
                Log.d(TAG, "sendBroadcast onProgressUpdate");
            }

            @Override
            protected void onPostExecute(Integer val) {
                Intent intent = new Intent(BROADCAST_COUNTER_ACTION);
                intent.putExtra(COUNTER_VALUE, val);
                sendBroadcast(intent);
                Log.d(TAG, "sendBroadcast onPostExecute");
            }
        };
        task.execute(initVal);
    }

    @Override
    public void stopCounter() {
        mStop = true;
    }

    public class CounterBinder extends Binder {
        CounterService getService() {
            return CounterService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Counter Service Created");
    }
    public int getRandomNumber() {
        return mGenerator.nextInt(100);
    }
}
