package com.blink.live;

import android.os.Bundle;
import android.view.View;

import com.blink.framelibrary.base.activity.BaseEventActivity;
import com.blink.framelibrary.eventbus.EventMap;

public class LiveActivity extends BaseEventActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
    }

    @Override
    protected void handleEvent(EventMap.BaseEvent event) {

    }

    @Override
    protected boolean isEventActivity() {
        return false;
    }

    @Override
    public void initData(Bundle bundle) {

    }

    @Override
    public int getLayoutId() {
        return 0;
    }

    @Override
    public void onWidgetClick(View view) {

    }

    @Override
    public void init() {

    }

    @Override
    public void initEvents() {

    }

    @Override
    public void initViews() {

    }
}
