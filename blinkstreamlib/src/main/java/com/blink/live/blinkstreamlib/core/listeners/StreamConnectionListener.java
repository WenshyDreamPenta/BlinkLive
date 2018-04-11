package com.blink.live.blinkstreamlib.core.listeners;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/12
 *     desc   : RESConnection 监听接口
 * </pre>
 */
public interface StreamConnectionListener {
    void onOpenConnectionResult(int result);

    void onWriteError(int errno);

    void onCloseConnectionResult(int result);

    class RESWriteErrorRunable implements Runnable {
        StreamConnectionListener connectionListener;
        int errno;

        public RESWriteErrorRunable(StreamConnectionListener connectionListener, int errno) {
            this.connectionListener = connectionListener;
            this.errno = errno;
        }

        @Override
        public void run() {
            if (connectionListener != null) {
                connectionListener.onWriteError(errno);
            }
        }
    }
}