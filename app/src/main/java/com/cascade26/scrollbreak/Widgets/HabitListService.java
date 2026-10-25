package com.cascade26.scrollbreak.Widgets;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class HabitListService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new HabitRemoteViewsFactory(this.getApplicationContext());
    }
}
