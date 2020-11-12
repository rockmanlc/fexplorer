package com.rocklee.fexplorer.test;

public interface ICounterService {
    void startCounter(int initVal, ICounterCallback callback);
    void startCounter(int initVal);
    void stopCounter();
}
