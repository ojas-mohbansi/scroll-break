package com.cascade26.scrollbreak.Models;

public class UpdateLogItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    private final int type;
    private final String text;

    public UpdateLogItem(int type, String text) {
        this.type = type;
        this.text = text;
    }

    public int getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}
