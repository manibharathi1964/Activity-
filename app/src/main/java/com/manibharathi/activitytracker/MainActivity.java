package com.manibharathi.activitytracker;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.print.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int PRIMARY = Color.rgb(48, 86, 255);
    static final int SECONDARY = Color.rgb(0, 196, 170);
    static final int NAVY = Color.rgb(18, 27, 54);
    static final int BG = Color.rgb(246, 248, 252);
    static final int CARD = Color.WHITE;
    static final int MUTED = Color.rgb(102, 113, 137);

    LinearLayout root, content;
    SharedPreferences prefs;
    boolean dark = false;
    String selectedDate;
    String reportFrom;
    ArrayList<Habit> habits = new ArrayList<>();

    static class Habit {
        long id;
        String name, category, target, frequency;
        Habit(long i, String n, String c, String t, String f) {
            id = i; name = n; category = c; target = t; frequency = f;
        }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("tracker", MODE_PRIVATE);
        dark = prefs.getBoolean("dark", false);
        selectedDate = today();
        reportFrom = firstOfMonth();
        loadHabits();
        showHome();
    }

    int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    int bg() { return dark ? Color.rgb(13,16,28) : BG; }
    int card() { return dark ? Color.rgb(25,30,47) : CARD; }
    int text() { return dark ? Color.rgb(244,247,255) : NAVY; }
    int muted() { return dark ? Color.rgb(174,184,207) : MUTED; }

    String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
    String displayDate(String d) {
        try {
            return new SimpleDateFormat("EEE, dd MMM", Locale.US)
                    .format(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d));
        } catch (Exception e) { return d; }
    }
    String displayDateLong(String d) {
        try {
            return new SimpleDateFormat("dd MMM yyyy", Locale.US)
                    .format(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d));
        } catch (Exception e) { return d; }
    }

    GradientDrawable bgShape(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), dark ? Color.rgb(54,61,82) : Color.rgb(225,230,240));
        return g;
    }

    GradientDrawable gradient(int start, int end, float radius) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{start, end});
        g.setCornerRadius(dp(radius));
        return g;
    }

    TextView label(String s, float size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(text());
        v.setTypeface(android.graphics.Typeface.create("sans-serif", bold ? 1 : 0));
        return v;
    }

    TextView small(String s) {
        TextView v = label(s, 13, false);
        v.setTextColor(muted());
        return v;
    }

    LinearLayout cardBox(float radius) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(16), dp(18), dp(16));
        l.setBackground(bgShape(card(), radius));
        return l;
    }

    LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w == -2 ? -2 : w, h == -2 ? -2 : h);
    }

    void margin(View v, int l, int t, int r, int b) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        if (p instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams x = (LinearLayout.LayoutParams)p;
            x.setMargins(dp(l), dp(t), dp(r), dp(b));
            v.setLayoutParams(x);
        }
    }

    TextView pill(String s, int color, int textColor) {
        TextView v = label(s, 12, true);
        v.setTextColor(textColor);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(12), 0, dp(12), 0);
        v.setBackground(bgShape(color, 30));
        return v;
    }

    Button button(String text, int color, int textColor, float radius) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(textColor);
        b.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 1));
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setBackground(bgShape(color, radius));
        return b;
    }

    void base() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg());

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(14), dp(18), dp(18));

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setClipToPadding(false);
        sv.addView(content);

        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(12), dp(8), dp(12), dp(8));
        nav.setBackgroundColor(card());

        Button home = navButton("⌂", "Today");
        Button analytics = navButton("◔", "Analytics");
        Button settings = navButton("⚙", "Settings");
        home.setOnClickListener(v -> showHome());
        analytics.setOnClickListener(v -> showReport());
        settings.setOnClickListener(v -> settingsDialog());

        nav.addView(home, new LinearLayout.LayoutParams(0, dp(64), 1));
        nav.addView(analytics, new LinearLayout.LayoutParams(0, dp(64), 1));
        nav.addView(settings, new LinearLayout.LayoutParams(0, dp(64), 1));
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(82)));

        setContentView(root);
    }

    Button navButton(String icon, String name) {
        Button b = button(icon + "\n" + name, Color.TRANSPARENT, PRIMARY, 18);
        b.setTextSize(12);
        return b;
    }

    void addGap(int h) {
        Space s = new Space(this);
        content.addView(s, new LinearLayout.LayoutParams(1, dp(h)));
    }

    void showHome() {
        base();
        content.removeAllViews();
        header();
        addGap(14);
        hero();
        addGap(14);
        dateToolbar();
        addGap(18);

        TextView section = label("Today’s activities", 19, true);
        content.addView(section, new LinearLayout.LayoutParams(-1, dp(34)));
        TextView hint = small("Tap the checkbox when you complete an activity");
        content.addView(hint, new LinearLayout.LayoutParams(-1, dp(28)));

        for (Habit h : habits) addHabitRow(h);

        if (habits.isEmpty()) {
            LinearLayout empty = cardBox(24);
            empty.setGravity(Gravity.CENTER);
            TextView e1 = label("Your day starts here", 18, true);
            e1.setGravity(Gravity.CENTER);
            empty.addView(e1, new LinearLayout.LayoutParams(-1, dp(40)));
            TextView e2 = small("Create your first activity and build your streak.");
            e2.setGravity(Gravity.CENTER);
            empty.addView(e2, new LinearLayout.LayoutParams(-1, dp(34)));
            Button add = button("+  Add Activity", PRIMARY, Color.WHITE, 20);
            add.setOnClickListener(v -> addHabitDialog(null));
            empty.addView(add, new LinearLayout.LayoutParams(-1, dp(54)));
            margin(add, 0, 12, 0, 0);
            content.addView(empty);
        }
    }

    void header() {
        LinearLayout h = new LinearLayout(this);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(2), dp(4), dp(2), dp(4));

        ImageView icon = new ImageView(this);
        icon.setImageResource(com.manibharathi.activitytracker.R.drawable.ic_activity_logo);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        h.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(58)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(12), 0, 0, 0);
        TextView title = label("Activity", 26, true);
        title.setTextColor(PRIMARY);
        names.addView(title, new LinearLayout.LayoutParams(-1, dp(34)));
        TextView sub = small("Plan • Track • Complete");
        names.addView(sub, new LinearLayout.LayoutParams(-1, dp(25)));
        h.addView(names, new LinearLayout.LayoutParams(0, dp(62), 1));

        Button menu = button("⋮", dark ? Color.rgb(30,36,55) : Color.WHITE, text(), 20);
        menu.setTextSize(24);
        menu.setOnClickListener(v -> settingsDialog());
        h.addView(menu, new LinearLayout.LayoutParams(dp(50), dp(50)));

        content.addView(h, new LinearLayout.LayoutParams(-1, dp(70)));
    }

    void hero() {
        int total = habits.size();
        int done = completedCount(selectedDate);
        int pct = total == 0 ? 0 : done * 100 / total;

        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setPadding(dp(20), dp(20), dp(20), dp(18));
        h.setBackground(gradient(Color.rgb(28,49,110), Color.rgb(44,96,255), 28));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView left = label(selectedDate.equals(today()) ? "Good day. Keep going!" : displayDate(selectedDate), 16, true);
        left.setTextColor(Color.WHITE);
        top.addView(left, new LinearLayout.LayoutParams(0, dp(32), 1));
        TextView pctText = label(pct + "%", 28, true);
        pctText.setTextColor(Color.WHITE);
        top.addView(pctText, new LinearLayout.LayoutParams(-2, dp(40)));
        h.addView(top);

        TextView title = label("Today's Progress", 25, true);
        title.setTextColor(Color.WHITE);
        h.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(pct);
        bar.setProgressTintList(ColorStateList.valueOf(Color.WHITE));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.argb(65,255,255,255)));
        h.addView(bar, new LinearLayout.LayoutParams(-1, dp(14)));

        LinearLayout stats = new LinearLayout(this);
        stats.setGravity(Gravity.CENTER_VERTICAL);
        TextView c = label("Completed  " + done + "/" + total, 14, true);
        c.setTextColor(Color.WHITE);
        TextView s = label("Streak  " + streak() + " days", 14, true);
        s.setTextColor(Color.WHITE);
        stats.addView(c, new LinearLayout.LayoutParams(0, dp(30), 1));
        stats.addView(s, new LinearLayout.LayoutParams(0, dp(30), 1));
        h.addView(stats);

        content.addView(h, new LinearLayout.LayoutParams(-1, dp(178)));
    }

    void dateToolbar() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button date = button("▣  " + displayDate(selectedDate), card(), text(), 18);
        date.setTextSize(14);
        date.setOnClickListener(v -> datePicker());
        row.addView(date, new LinearLayout.LayoutParams(0, dp(58), 1));

        Button filter = button("☷", card(), text(), 18);
        filter.setTextSize(19);
        filter.setOnClickListener(v -> filterDialog());
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(dp(58), dp(58));
        fp.setMargins(dp(8),0,0,0);
        row.addView(filter, fp);

        Button add = button("+", PRIMARY, Color.WHITE, 18);
        add.setTextSize(24);
        add.setOnClickListener(v -> addHabitDialog(null));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(58), dp(58));
        ap.setMargins(dp(8),0,0,0);
        row.addView(add, ap);

        content.addView(row, new LinearLayout.LayoutParams(-1, dp(58)));
    }

    void addHabitRow(Habit h) {
        boolean done = isDone(h.id, selectedDate);
        LinearLayout c = cardBox(22);
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(10), dp(12), dp(12), dp(12));

        CheckBox cb = new CheckBox(this);
        cb.setChecked(done);
        cb.setButtonTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{PRIMARY, Color.rgb(178,188,208)}));
        cb.setOnClickListener(v -> {
            setDone(h.id, selectedDate, cb.isChecked());
            showHome();
        });
        c.addView(cb, new LinearLayout.LayoutParams(dp(52), dp(62)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(2), 0, dp(8), 0);

        TextView name = label(h.name, 16, true);
        name.setMaxLines(3);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(name, new LinearLayout.LayoutParams(-1, -2));

        String meta = (h.target == null || h.target.trim().isEmpty() ? "No target" : h.target)
                + "  •  " + h.category + "  •  " + h.frequency;
        TextView sub = small(meta);
        sub.setMaxLines(2);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(sub, new LinearLayout.LayoutParams(-1, dp(36)));

        c.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        TextView status = pill(done ? "Completed" : "Pending",
                done ? Color.rgb(220,248,236) : (dark ? Color.rgb(44,53,73) : Color.rgb(239,243,249)),
                done ? Color.rgb(20,125,82) : (dark ? Color.rgb(215,222,240) : Color.rgb(62,76,101)));
        status.setMinWidth(dp(82));
        c.addView(status, new LinearLayout.LayoutParams(-2, dp(42)));

        c.setOnLongClickListener(v -> {
            editDeleteDialog(h);
            return true;
        });

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(10));
        content.addView(c, cp);
    }

    void datePicker() {
        Calendar cal = Calendar.getInstance();
        try { cal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDate)); }
        catch (Exception ignored) {}
        DatePickerDialog d = new DatePickerDialog(this,
                (v,y,m,day) -> {
                    selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y,m+1,day);
                    showHome();
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        d.show();
    }

    void addHabitDialog(Habit existing) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(24), dp(8), dp(24), 0);

        EditText name = new EditText(this);
        name.setHint("Activity name");
        name.setText(existing == null ? "" : existing.name);
        l.addView(name, new LinearLayout.LayoutParams(-1, dp(58)));

        EditText target = new EditText(this);
        target.setHint("Target / goal (e.g. 10,000 steps)");
        target.setText(existing == null ? "" : existing.target);
        l.addView(target, new LinearLayout.LayoutParams(-1, dp(58)));

        Spinner cat = new Spinner(this);
        String[] cats = {"Health","Fitness","Study","Work","Personal","Finance","Other"};
        cat.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, cats));
        if (existing != null) {
            for (int i=0;i<cats.length;i++) if (cats[i].equals(existing.category)) cat.setSelection(i);
        }
        l.addView(cat, new LinearLayout.LayoutParams(-1, dp(52)));

        EditText freq = new EditText(this);
        freq.setHint("Frequency (Daily / Weekly / Custom)");
        freq.setText(existing == null ? "Daily" : existing.frequency);
        l.addView(freq, new LinearLayout.LayoutParams(-1, dp(58)));

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Create Activity" : "Edit Activity")
                .setView(l)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dlg.setOnShowListener(x -> dlg.getButton(-1).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if (n.isEmpty()) { name.setError("Enter an activity name"); return; }
            String t = target.getText().toString().trim();
            String f = freq.getText().toString().trim();
            if (f.isEmpty()) f = "Daily";

            if (existing == null)
                habits.add(new Habit(System.currentTimeMillis(), n, cat.getSelectedItem().toString(), t, f));
            else {
                existing.name=n; existing.target=t; existing.category=cat.getSelectedItem().toString(); existing.frequency=f;
            }
            saveHabits();
            dlg.dismiss();
            showHome();
        }));
        dlg.show();
    }

    void editDeleteDialog(Habit h) {
        new AlertDialog.Builder(this)
                .setTitle(h.name)
                .setItems(new String[]{"Edit activity","Delete activity"}, (d,w) -> {
                    if (w == 0) addHabitDialog(h);
                    else new AlertDialog.Builder(this)
                            .setTitle("Delete activity?")
                            .setMessage("This removes the activity from your list. Completion history is also removed.")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete", (x,y) -> {
                                habits.remove(h);
                                removeHistory(h.id);
                                saveHabits();
                                showHome();
                            }).show();
                }).show();
    }

    void removeHistory(long id) {
        Set<String> s = new HashSet<>(prefs.getStringSet("done", new HashSet<>()));
        String prefix = id + "|";
        Iterator<String> it = s.iterator();
        while (it.hasNext()) if (it.next().startsWith(prefix)) it.remove();
        prefs.edit().putStringSet("done", s).apply();
    }

    void filterDialog() {
        String[] opts = {"All activities","Completed only","Pending only"};
        new AlertDialog.Builder(this).setTitle("Filter activities").setItems(opts, (d,w) -> {
            showFilteredHome(w);
        }).show();
    }

    void showFilteredHome(int mode) {
        base();
        content.removeAllViews();
        header(); addGap(14); hero(); addGap(14); dateToolbar(); addGap(18);
        TextView section = label(mode == 0 ? "All activities" : mode == 1 ? "Completed activities" : "Pending activities", 19, true);
        content.addView(section, new LinearLayout.LayoutParams(-1, dp(34)));
        for (Habit h : habits)
            if (mode == 0 || (mode == 1 && isDone(h.id, selectedDate) || mode == 2 && !isDone(h.id, selectedDate)))
                addHabitRow(h);
    }

    void showReport() {
        base();
        content.removeAllViews();
        header();
        addGap(14);

        LinearLayout intro = cardBox(26);
        TextView title = label("Analytics & Reports", 22, true);
        intro.addView(title, new LinearLayout.LayoutParams(-1, dp(34)));
        intro.addView(small("Review your consistency and performance over any date range."),
                new LinearLayout.LayoutParams(-1, dp(38)));
        content.addView(intro);

        addGap(12);

        LinearLayout range = cardBox(22);
        range.addView(label("Report range", 16, true), new LinearLayout.LayoutParams(-1, dp(32)));

        Button from = button("From  " + displayDateLong(reportFrom), card(), text(), 16);
        from.setOnClickListener(v -> pickReport(true));
        range.addView(from, new LinearLayout.LayoutParams(-1, dp(52)));
        margin(from,0,8,0,0);

        Button to = button("To  " + displayDateLong(today()), card(), text(), 16);
        to.setOnClickListener(v -> pickReport(false));
        range.addView(to, new LinearLayout.LayoutParams(-1, dp(52)));
        margin(to,0,8,0,0);

        Button gen = button("Generate detailed report", PRIMARY, Color.WHITE, 18);
        gen.setOnClickListener(v -> showReport());
        range.addView(gen, new LinearLayout.LayoutParams(-1, dp(54)));
        margin(gen,0,8,0,0);

        Button print = button("Print / Save report", dark ? Color.rgb(39,46,68) : Color.rgb(239,243,250), text(), 18);
        print.setOnClickListener(v -> printReport());
        range.addView(print, new LinearLayout.LayoutParams(-1, dp(54)));

        content.addView(range);

        addGap(12);
        renderReport(reportFrom, today());
    }

    void pickReport(boolean from) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog d = new DatePickerDialog(this, (v,y,m,day) -> {
            String val = String.format(Locale.US, "%04d-%02d-%02d", y,m+1,day);
            if (from) reportFrom = val;
            else if (val.compareTo(reportFrom) >= 0) {}
            showReport();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        d.show();
    }

    void renderReport(String from, String to) {
        int days = daysBetween(from,to);
        int total = habits.size() * Math.max(days,0);
        int achieved = 0;

        Calendar c = Calendar.getInstance();
        try { c.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(from)); }
        catch (Exception ignored) {}

        StringBuilder table = new StringBuilder();
        for (int i=0;i<days;i++) {
            String d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
            int done = completedCount(d);
            achieved += done;
            int pct = habits.size() == 0 ? 0 : done * 100 / habits.size();
            table.append(displayDateLong(d)).append("   •   ").append(done).append("/")
                    .append(habits.size()).append("   •   ").append(pct).append("%\n");
            c.add(Calendar.DAY_OF_MONTH,1);
        }
        int score = total == 0 ? 0 : achieved * 100 / total;

        LinearLayout scoreCard = cardBox(26);
        scoreCard.addView(label("Performance score", 16, true), new LinearLayout.LayoutParams(-1, dp(30)));

        LinearLayout scoreRow = new LinearLayout(this);
        scoreRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView scoreText = label(score + "%", 40, true);
        scoreText.setTextColor(PRIMARY);
        scoreRow.addView(scoreText, new LinearLayout.LayoutParams(dp(110), dp(60)));
        LinearLayout scoreInfo = new LinearLayout(this);
        scoreInfo.setOrientation(LinearLayout.VERTICAL);
        scoreInfo.addView(label(achieved + " completed", 16, true));
        scoreInfo.addView(small("out of " + total + " scheduled check-ins"));
        scoreRow.addView(scoreInfo, new LinearLayout.LayoutParams(0, dp(64), 1));
        scoreCard.addView(scoreRow);

        ProgressBar bar = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100); bar.setProgress(score);
        bar.setProgressTintList(ColorStateList.valueOf(PRIMARY));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(dark ? Color.rgb(48,56,79) : Color.rgb(229,234,243)));
        scoreCard.addView(bar, new LinearLayout.LayoutParams(-1, dp(12)));

        TextView rangeText = small(displayDateLong(from) + "  →  " + displayDateLong(to));
        scoreCard.addView(rangeText, new LinearLayout.LayoutParams(-1, dp(28)));
        content.addView(scoreCard);

        addGap(12);

        LinearLayout details = cardBox(22);
        details.addView(label("Daily activity", 17, true), new LinearLayout.LayoutParams(-1, dp(34)));
        TextView rows = label(table.length() == 0 ? "No report data." : table.toString(), 13, false);
        rows.setTextColor(muted());
        details.addView(rows);
        content.addView(details);
    }

    int daysBetween(String a, String b) {
        try {
            Date x = new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(a);
            Date y = new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(b);
            return (int)((y.getTime()-x.getTime())/86400000L)+1;
        } catch(Exception e) { return 1; }
    }

    int completedCount(String date) {
        int n=0;
        for (Habit h : habits) if (isDone(h.id,date)) n++;
        return n;
    }

    boolean isDone(long id,String date) {
        return prefs.getStringSet("done",new HashSet<>()).contains(id+"|"+date);
    }

    void setDone(long id,String date,boolean done) {
        Set<String> s = new HashSet<>(prefs.getStringSet("done",new HashSet<>()));
        String k=id+"|"+date;
        if(done) s.add(k); else s.remove(k);
        prefs.edit().putStringSet("done",s).apply();
    }

    int streak() {
        int s=0;
        Calendar c=Calendar.getInstance();
        for(int i=0;i<366;i++) {
            String d=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());
            if(completedCount(d)>0) { s++; c.add(Calendar.DAY_OF_MONTH,-1); }
            else break;
        }
        return s;
    }

    String firstOfMonth() {
        Calendar c=Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH,1);
        return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());
    }

    void loadHabits() {
        habits.clear();
        String raw=prefs.getString("habits","");
        if(raw.isEmpty()) {
            habits.add(new Habit(1,"Morning Stretching & Yoga","Fitness","20 min","Daily"));
            habits.add(new Habit(2,"1 Hour Exercise / Gym","Fitness","60 min","Daily"));
            habits.add(new Habit(3,"10,000 Steps Walking","Health","10,000 steps","Daily"));
            habits.add(new Habit(4,"Drink 3 Litres Water","Health","3 litres","Daily"));
            habits.add(new Habit(5,"Face Care & Sunscreen Routine","Personal","Morning + evening","Daily"));
            habits.add(new Habit(6,"Healthy Nutrition","Health","Balanced meals","Daily"));
            saveHabits();
            return;
        }
        try {
            JSONArray a=new JSONArray(raw);
            for(int i=0;i<a.length();i++) {
                JSONObject o=a.getJSONObject(i);
                habits.add(new Habit(o.getLong("id"),o.getString("name"),o.getString("category"),
                        o.optString("target",""),o.optString("frequency","Daily")));
            }
        } catch(Exception ignored) {}
    }

    void saveHabits() {
        try {
            JSONArray a=new JSONArray();
            for(Habit h:habits) {
                JSONObject o=new JSONObject();
                o.put("id",h.id); o.put("name",h.name); o.put("category",h.category);
                o.put("target",h.target); o.put("frequency",h.frequency); a.put(o);
            }
            prefs.edit().putString("habits",a.toString()).apply();
        } catch(Exception ignored) {}
    }

    void settingsDialog() {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(24),dp(8),dp(24),0);

        CheckBox darkBox=new CheckBox(this);
        darkBox.setText("Dark mode");
        darkBox.setTextColor(text());
        darkBox.setChecked(dark);
        l.addView(darkBox,new LinearLayout.LayoutParams(-1,dp(54)));

        Button reset=new Button(this);
        reset.setText("Reset completion history");
        reset.setAllCaps(false);
        l.addView(reset,new LinearLayout.LayoutParams(-1,dp(54)));

        AlertDialog d=new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(l)
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Save",null).create();

        reset.setOnClickListener(v -> {
            prefs.edit().remove("done").apply();
            Toast.makeText(this,"Completion history reset",Toast.LENGTH_SHORT).show();
        });

        d.setOnShowListener(x -> d.getButton(-1).setOnClickListener(v -> {
            dark=darkBox.isChecked();
            prefs.edit().putBoolean("dark",dark).apply();
            d.dismiss();
            showHome();
        }));
        d.show();
    }

    void printReport() {
        PrintManager pm=(PrintManager)getSystemService(PRINT_SERVICE);
        pm.print("Activity Report",new PrintDocumentAdapter() {
            public void onLayout(PrintAttributes oldA,PrintAttributes newA,CancellationSignal cs,
                                 LayoutResultCallback cb,Bundle extras) {
                cb.onLayoutFinished(new PrintDocumentInfo.Builder("activity-report.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(),true);
            }
            public void onWrite(PageRange[] pages,android.os.ParcelFileDescriptor dest,
                                CancellationSignal cs,WriteResultCallback cb) {
                try {
                    java.io.FileOutputStream out=new java.io.FileOutputStream(dest.getFileDescriptor());
                    StringBuilder s=new StringBuilder();
                    s.append("ACTIVITY\n");
                    s.append("Performance Report\n");
                    s.append("Range: ").append(displayDateLong(reportFrom)).append(" to ").append(displayDateLong(today())).append("\n\n");
                    for(Habit h:habits)
                        s.append(h.name).append("  —  ")
                         .append(isDone(h.id,today()) ? "Completed" : "Pending").append("\n");
                    out.write(s.toString().getBytes());
                    out.flush();
                    cb.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                } catch(Exception e) { cb.onWriteFailed(e.toString()); }
            }
        },null);
    }
}
