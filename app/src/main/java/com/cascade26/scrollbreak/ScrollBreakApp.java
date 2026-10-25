package com.cascade26.scrollbreak;

import android.app.Application;
import com.cascade26.scrollbreak.Utils.BlockerMigrationManager;
import com.cascade26.scrollbreak.Utils.Utils;

/** Application entry point for local-only app initialization. */
public class ScrollBreakApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.applyAppThemeFromPrefs(this);

        BlockerMigrationManager.migrateAndSeed(this);
    }
}
