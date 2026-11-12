package com.raffletickets;

import android.app.Activity;
import android.app.AlertDialog;
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

public class MainActivity extends Activity {

    private TicketDB db;
    private TextView countLabel;
    private LinearLayout ticketList;
    private EditText addInput, checkInput;
    private TextView resultLabel;

    private static final int BG = Color.parseColor("#0A0A0A");
    private static final int CARD = Color.parseColor("#1A1A1A");
    private static final int TEXT = Color.parseColor("#FFFFFF");
    private static final int MUTED = Color.parseColor("#9E9E9E");
    private static final int ACCENT = Color.parseColor("#4FC3F7");
    private static final int WIN = Color.parseColor("#00E676");
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

        // header: title + settings
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("Raffle Tickets", 26, true);
        title.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(title);
        Button settings = new Button(this);
        settings.setText("Settings");
        settings.setTextColor(TEXT);
        settings.setAllCaps(false);
        settings.setBackground(glass(12));
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        header.addView(settings);
        root.addView(header);

        // prominent ticket count card
        LinearLayout countCard = cardLayout();
        countLabel = label("0", 60, true);
        countLabel.setGravity(Gravity.CENTER);
        countCard.addView(countLabel);
        TextView countSub = small("tickets saved");
        countSub.setGravity(Gravity.CENTER);
        countCard.addView(countSub);
        root.addView(countCard);

        root.addView(divider());

        // ---- Add ----
        root.addView(label("Add Tickets", 18, true));
        root.addView(small("Single number or a range like 374129-374199"));
        addInput = rangeInput("Number or range, e.g. 374129-374199");
        root.addView(addInput);
        root.addView(button("Add Tickets", ACCENT, v -> doAdd()));

        root.addView(divider());

        // ---- Check ----
        root.addView(label("Check Winning Number", 18, true));
        root.addView(small("Type the full number or the last 4 digits"));
        checkInput = input("Winning number (full or last 4)");
        root.addView(checkInput);
        root.addView(button("Check", ACCENT, v -> doCheck()));
        resultLabel = label("", 16, false);
        resultLabel.setGravity(Gravity.CENTER);
        resultLabel.setPadding(0, dp(8), 0, dp(8));
        root.addView(resultLabel);

        root.addView(divider());

        // ---- List ----
        root.addView(label("Your Tickets", 18, true));
        root.addView(small("Long-press a ticket to delete it"));
        ticketList = new LinearLayout(this);
        ticketList.setOrientation(LinearLayout.VERTICAL);
        root.addView(ticketList);

        scroll.addView(root);
        setContentView(scroll);
        refresh();
        UpdateChecker.check(this, false);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------
    private void doAdd() {
        String raw = addInput.getText().toString().trim();
        if (raw.isEmpty()) { toast("Enter a ticket number"); return; }

        String[] parts = raw.split("[-–—\\s]+");
        List<String> toAdd = new ArrayList<>();
        if (parts.length == 2 && isDigits(parts[0]) && isDigits(parts[1])) {
            long start = Long.parseLong(parts[0]);
            long end = Long.parseLong(parts[1]);
            if (start > end) { long t = start; start = end; end = t; }
            if (end - start > 100000) { toast("Range too big (max 100,000)"); return; }
            for (long n = start; n <= end; n++) toAdd.add(String.valueOf(n));
        } else {
            String num = raw.replaceAll("\\D", "");
            if (num.isEmpty()) { toast("Invalid ticket number"); return; }
            toAdd.add(num);
        }

        int added = 0;
        for (String n : toAdd) added += db.add(n);
        int dup = toAdd.size() - added;
        toast(added + " added" + (dup > 0 ? ", " + dup + " already saved" : ""));
        addInput.setText("");
        refresh();
    }

    private void doCheck() {
        String raw = checkInput.getText().toString().trim();
        String num = raw.replaceAll("\\D", "");
        if (num.isEmpty()) { toast("Enter a number to check"); return; }

        List<String> matches = db.findSuffix(num);
        if (matches.isEmpty()) {
            resultLabel.setText("NO MATCH");
            resultLabel.setTextColor(LOSE);
        } else {
            StringBuilder sb = new StringBuilder("WINNER!\n");
            int shown = 0;
            for (String m : matches) {
                if (shown++ >= 20) { sb.append("+ ").append(matches.size() - 20).append(" more"); break; }
                sb.append(m).append("\n");
            }
            resultLabel.setText(sb.toString().trim());
            resultLabel.setTextColor(WIN);
        }
    }

    private void confirmDeleteSingle(String num) {
        new AlertDialog.Builder(this)
                .setTitle("Delete ticket " + num + "?")
                .setMessage("This removes this one ticket.")
                .setPositiveButton("Delete", (d, w) -> {
                    db.delete(num);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------
    private void refresh() {
        countLabel.setText(String.valueOf(db.count()));

        ticketList.removeAllViews();
        for (final String num : db.getAll()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));

            TextView tv = label(num, 16, false);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(tv);

            row.setOnLongClickListener(v -> {
                confirmDeleteSingle(num);
                return true;
            });

            ticketList.addView(row);
        }
    }

    // ------------------------------------------------------------------
    // UI helpers
    // ------------------------------------------------------------------
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

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setSingleLine(true);
        e.setBackground(glass(12));
        e.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        e.setLayoutParams(lp);
        return e;
    }

    private EditText rangeInput(String hint) {
        EditText e = input(hint);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789- "));
        return e;
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

    private LinearLayout cardLayout() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(20), dp(20), dp(20), dp(20));
        c.setBackground(glass(20));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(12), 0, dp(4));
        c.setLayoutParams(lp);
        return c;
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

    private boolean isDigits(String s) {
        return s != null && s.matches("\\d+");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
