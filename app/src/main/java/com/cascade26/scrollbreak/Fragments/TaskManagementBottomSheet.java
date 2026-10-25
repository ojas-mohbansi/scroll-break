package com.cascade26.scrollbreak.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cascade26.scrollbreak.Activities.HomeActivity;
import com.cascade26.scrollbreak.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/** Full-height task management surface launched from the Home dashboard. */
public class TaskManagementBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_TASK_ID = "task_id";
    private static final String ARG_OPEN_ADD = "open_add_task";

    public static TaskManagementBottomSheet newInstance(@Nullable String taskId, boolean openAddTask) {
        TaskManagementBottomSheet sheet = new TaskManagementBottomSheet();
        Bundle args = new Bundle();
        if (taskId != null) args.putString(ARG_TASK_ID, taskId);
        args.putBoolean(ARG_OPEN_ADD, openAddTask);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_task_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) return;
        Bundle source = getArguments();
        TasksFragment tasks = new TasksFragment();
        Bundle args = new Bundle();
        if (source != null) {
            args.putString(HomeActivity.EXTRA_TASK_ID, source.getString(ARG_TASK_ID));
            args.putBoolean(ARG_OPEN_ADD, source.getBoolean(ARG_OPEN_ADD));
        }
        tasks.setArguments(args);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.taskManagementContent, tasks)
                .commit();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!(getDialog() instanceof BottomSheetDialog)) return;
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            sheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }
}
