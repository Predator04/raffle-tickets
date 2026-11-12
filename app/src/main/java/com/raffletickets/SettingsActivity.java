package com.raffletickets;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsActivity extends Activity {

    private TicketDB db;

    private static final int BG = Color.parseColor("#0A0A0A");
    private static final int CARD = Color.parseColor("#1A1A1A");
    private static final int TEXT = Color.parseColor("#FFFFFF");
    private static final int MUTED = Color.parseColor("#9E9E9E");
    private static final int ACCENT = Color.parseColor("#4FC3F7");
    private static final int LOSE = Color.parseColor("#FF5252");

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = new TicketDB(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(glassBg());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        // header: back + title
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = new Button(this);
        back.setText("\u2190 Back");
        back.setTextColor(TEXT);
        back.setAllCaps(false);
        back.setBackground(glass(12));
        back.setOnClickListener(v -> finish());
        header.addView(back);
        TextView title = label("Settings", 24, true);
        title.setPadding(dp(16), 0, 0, 0);
        header.addView(title);
        root.addView(header);

        root.addView(divider());

        // Tickets
        root.addView(label("Tickets", 18, true));
        root.addView(small("Send your tickets to someone else, or pull in a copy"));
        root.addView(button("Share Tickets", ACCENT, v -> shareTickets()));
        root.addView(button("Copy Tickets", ACCENT, v -> copyTickets()));
        root.addView(button("Import Tickets", ACCENT, v -> importTickets()));
        root.addView(button("Check for Updates", ACCENT, v -> UpdateChecker.check(this, true)));

        root.addView(divider());

        // Danger zone
        root.addView(label("Danger Zone", 18, true));
        root.addView(button("Clear All Tickets", LOSE, v -> confirmClear()));

        scroll.addView(root);
        setContentView(scroll);
    }

    private void shareTickets() {
        List<String> all = db.getAll();
        if (all.isEmpty()) { toast("No tickets to share"); return; }
        StringBuilder sb = new StringBuilder();
        for (String n : all) sb.append(n).append("\n");
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, sb.toString().trim());
        startActivity(Intent.createChooser(send, "Share tickets"));
    }

    private void copyTickets() {
        List<String> all = db.getAll();
        if (all.isEmpty()) { toast("No tickets to copy"); return; }
        StringBuilder sb = new StringBuilder();
        for (String n : all) sb.append(n).append("\n");
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("tickets", sb.toString().trim()));
        toast("Copied " + all.size() + " tickets");
    }

    private void importTickets() {
        final EditText input = new EditText(this);
        input.setHint("Paste ticket numbers here");
        input.setHintTextColor(MUTED);
        input.setTextColor(TEXT);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setGravity(Gravity.TOP);
        input.setMinLines(4);
        input.setMaxLines(8);
        input.setVerticalScrollBarEnabled(true);
        input.setPadding(dp(16), dp(8), dp(16), dp(8));
        input.setBackground(glass(12));

        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("Import tickets")
                .setMessage("Paste ticket numbers — one per line, or comma/space separated.")
                .setView(input)
                .setPositiveButton("Import", null)
                .setNegativeButton("Cancel", null)
                .create();
        d.show();
        if (d.getWindow() != null) {
            d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = input.getText().toString();
            Matcher m = Pattern.compile("\\d+").matcher(text);
            List<String> nums = new ArrayList<>();
            while (m.find()) nums.add(m.group());
            if (nums.isEmpty()) { toast("No numbers found"); return; }
            int added = 0;
            for (String n : nums) added += db.add(n);
            int dup = nums.size() - added;
            toast(added + " imported" + (dup > 0 ? ", " + dup + " already saved" : ""));
            d.dismiss();
        });
    }

    private void confirmClear() {
        final EditText input = new EditText(this);
        input.setHint("Type DELETE to confirm");
        input.setHintTextColor(MUTED);
        input.setTextColor(TEXT);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setPadding(dp(16), dp(8), dp(16), dp(8));
        input.setBackground(glass(12));

        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("Clear all tickets?")
                .setMessage("This permanently deletes all " + db.count() + " tickets. Type DELETE to confirm.")
                .setView(input)
                .setPositiveButton("Delete all", null)
                .setNegativeButton("Cancel", null)
                .create();
        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if ("DELETE".equals(input.getText().toString().trim())) {
                db.deleteAll();
                toast("All tickets deleted");
                d.dismiss();
            } else {
                toast("Type DELETE to confirm");
            }
        });
    }

    // ---- helpers ----
    private TextView label(String text, float sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(TEXT);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private TextView small(String text) {
        TextView t = label(text, 13, false);
        t.setTextColor(MUTED);
        t.setPadding(0, 0, 0, dp(6));
        return t;
    }

    private Button button(String text, int color, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#0A0A0A"));
        btn.setAllCaps(false);
        btn.setBackground(rounded(color, 12));
        btn.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        btn.setLayoutParams(lp);
        return btn;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(Color.parseColor("#2A2A2A"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMargins(0, dp(16), 0, dp(16));
        v.setLayoutParams(lp);
        return v;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable glass(int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.parseColor("#12FFFFFF"));   // frosted translucent fill
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), Color.parseColor("#26FFFFFF")); // subtle glass edge
        return d;
    }

    private GradientDrawable glassBg() {
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#0E1A2E"), Color.parseColor("#0A0A0A")});
    }

    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
