package com.focusmate.lms;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Full-screen "stay focused" wall shown over a blocked app. */
public class BlockActivity extends Activity {
    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView time;
    private FocusStore store;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long left = store.endAt() - System.currentTimeMillis();
            if (left <= 0) { finish(); return; }
            long s = left / 1000;
            time.setText(String.format("%02d:%02d remaining", s / 60, s % 60));
            h.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new FocusStore(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(60, 60, 60, 60);
        root.setBackgroundColor(Color.parseColor("#0A0E23"));

        TextView icon = tv("\uD83C\uDF31", 64, Color.WHITE, false);
        TextView title = tv("Stay focused", 30, Color.WHITE, true);
        TextView sub = tv("This app is blocked during your focus session.\nYou've got this.", 16, Color.parseColor("#B7BEDB"), false);
        time = tv("", 22, Color.parseColor("#7CF2B0"), true);
        Button back = new Button(this);
        back.setText("Back to FocusMate");
        back.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
        root.addView(icon); root.addView(title); root.addView(sub); root.addView(time); root.addView(back);
        setContentView(root);
    }

    private TextView tv(String t, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(sp); v.setTextColor(color); v.setGravity(Gravity.CENTER);
        v.setPadding(0, 16, 0, 16);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        return v;
    }

    @Override protected void onResume() { super.onResume(); h.post(tick); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(tick); }

    /** Back button sends the user home rather than back into the blocked app. */
    @Override public void onBackPressed() {
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}
