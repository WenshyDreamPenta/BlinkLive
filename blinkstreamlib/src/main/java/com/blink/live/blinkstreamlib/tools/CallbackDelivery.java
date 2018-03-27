package com.blink.live.blinkstreamlib.tools;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/12
 *     desc   : 主线程handler调用类
 * </pre>
 */
public class CallbackDelivery {
    static private CallbackDelivery instance;
    private final Executor mCallbackPoster;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static CallbackDelivery getInstance() {
        return instance == null ? instance = new CallbackDelivery() : instance;
    }

    private CallbackDelivery() {
        mCallbackPoster = new Executor() {
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };
    }

    public void post(Runnable runnable) {
        mCallbackPoster.execute(runnable);
    }

    public void postDelayed(Runnable runnable, long time) {
        handler.postDelayed(runnable,time);
    }
}
