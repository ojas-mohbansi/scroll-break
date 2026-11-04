package com.cascade26.scrollbreak.Activities;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.cascade26.scrollbreak.Fragments.AddTaskBottomSheet;
import com.cascade26.scrollbreak.Models.Task;
import com.cascade26.scrollbreak.R;
import com.cascade26.scrollbreak.Utils.TaskManager;
import com.cascade26.scrollbreak.Widgets.TasksWidgetProvider;

public class WidgetQuickAddActivity extends AppCompatActivity implements AddTaskBottomSheet.OnTaskActionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_widget_quick_add);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        // Show the AddTaskBottomSheet immediately
        if (savedInstanceState == null) {
            AddTaskBottomSheet bottomSheet = AddTaskBottomSheet.newInstance();
            bottomSheet.setOnTaskActionListener(this);
            bottomSheet.show(getSupportFragmentManager(), "AddTaskBottomSheet");
        }
        getSupportFragmentManager().setFragmentResultListener("dismiss", this, (requestKey, result) -> finish());

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewDestroyed(
                            @androidx.annotation.NonNull androidx.fragment.app.FragmentManager fm,
                            @androidx.annotation.NonNull androidx.fragment.app.Fragment f) {
                        super.onFragmentViewDestroyed(fm, f);
                        if (f instanceof AddTaskBottomSheet) {
                            if (!isFinishing())
                                finish();
                        }
                    }
                }, false);
    }

    @Override
    public void onTaskCreated(Task task) {
        TaskManager taskManager = new TaskManager(this);
        taskManager.addTask(task);

        // Update widgets
        updateWidgets();

        // Activity will close via lifecycle callback
    }

    @Override
    public void onTaskUpdated(Task task) {
        TaskManager taskManager = new TaskManager(this);
        taskManager.updateTask(task);

        updateWidgets();
    }

    private void updateWidgets() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(this, TasksWidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);
    }
}
