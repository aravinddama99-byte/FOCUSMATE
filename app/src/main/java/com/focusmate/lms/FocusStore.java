package com.focusmate.lms;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/** Persistent state shared by the UI bridge and the blocker service. */
public class FocusStore {
    private final SharedPreferences sp;
    public FocusStore(Context c) { sp = c.getSharedPreferences("focus", Context.MODE_PRIVATE); }

    public Set<String> blocked() { return new HashSet<>(sp.getStringSet("blocked", new HashSet<>())); }
    public void setBlocked(Set<String> s) { sp.edit().putStringSet("blocked", s).apply(); }

    public long endAt() { return sp.getLong("endAt", 0); }
    public boolean strict() { return sp.getBoolean("strict", false); }
    public boolean active() { return endAt() > System.currentTimeMillis(); }

    public void start(long minutes, boolean strict) {
        sp.edit().putLong("endAt", System.currentTimeMillis() + minutes * 60_000L).putBoolean("strict", strict).apply();
    }
    /** Returns false if strict mode forbids stopping early. */
    public boolean stop() {
        if (active() && strict()) return false;
        sp.edit().putLong("endAt", 0).apply();
        return true;
    }
}
