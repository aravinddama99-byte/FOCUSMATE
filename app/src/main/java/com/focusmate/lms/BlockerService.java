package com.focusmate.lms;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import java.util.Set;

/** Watches the foreground app; during a focus session, bounces blocked apps to the block screen. */
public class BlockerService extends AccessibilityService {
    private FocusStore store;
    private long lastBlock = 0;

    @Override public void onServiceConnected() { store = new FocusStore(this); }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getPackageName() == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (store == null) store = new FocusStore(this);
        if (!store.active()) return;
        String pkg = e.getPackageName().toString();
        if (pkg.equals(getPackageName())) return;
        Set<String> blocked = store.blocked();
        if (!blocked.contains(pkg)) return;
        long now = System.currentTimeMillis();
        if (now - lastBlock < 400) return;
        lastBlock = now;
        performGlobalAction(GLOBAL_ACTION_HOME);
        Intent i = new Intent(this, BlockActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION)
            .putExtra("pkg", pkg);
        startActivity(i);
    }

    @Override public void onInterrupt() {}
}
