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
    static final int BLUE=Color.rgb(43,92,246), CYAN=Color.rgb(0,194,170);
    static final int NAVY=Color.rgb(18,27,52), BG=Color.rgb(244,247,251), WHITE=Color.WHITE, MUTED=Color.rgb(104,116,141);
    LinearLayout root, content;
    SharedPreferences prefs;
    boolean dark;
    String selectedDate, reportFrom;
    ArrayList<Habit> habits=new ArrayList<>();

    static class Habit {
        long id; String name,category,target,frequency;
        Habit(long id,String name,String category,String target,String frequency){
            this.id=id;this.name=name;this.category=category;this.target=target;this.frequency=frequency;
        }
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("tracker",MODE_PRIVATE);
        dark=prefs.getBoolean("dark",false);
        selectedDate=today(); reportFrom=firstOfMonth(); loadHabits(); showHome();
    }

    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    int bg(){return dark?Color.rgb(11,15,27):BG;}
    int surface(){return dark?Color.rgb(22,28,45):WHITE;}
    int primaryText(){return dark?Color.rgb(245,247,255):NAVY;}
    int secondaryText(){return dark?Color.rgb(169,180,204):MUTED;}
    GradientDrawable shape(int color,float radius){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));
        g.setStroke(dp(1),dark?Color.rgb(49,58,79):Color.rgb(226,231,239));return g;
    }
    GradientDrawable grad(int a,int b,float radius){
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});
        g.setCornerRadius(dp(radius));return g;
    }
    TextView tv(String s,float size,boolean bold){
        TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(primaryText());
        v.setTypeface(android.graphics.Typeface.create("sans-serif",bold?1:0));return v;
    }
    TextView muted(String s){TextView v=tv(s,12,false);v.setTextColor(secondaryText());return v;}
    TextView chip(String s,int bg,int fg){
        TextView v=tv(s,12,true);v.setTextColor(fg);v.setGravity(Gravity.CENTER);v.setPadding(dp(12),0,dp(12),0);v.setBackground(shape(bg,30));return v;
    }
    TextView action(String s,int bg,int fg,float size){
        TextView v=tv(s,size,true);v.setTextColor(fg);v.setGravity(Gravity.CENTER);v.setPadding(dp(14),0,dp(14),0);
        v.setBackground(shape(bg,18));v.setClickable(true);return v;
    }
    LinearLayout card(float radius){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(18),dp(16),dp(18),dp(16));l.setBackground(shape(surface(),radius));return l;
    }
    void gap(int h){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}

    void base(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg());
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18),dp(16),dp(18),dp(24));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(12),dp(7),dp(12),dp(7));nav.setBackgroundColor(surface());
        TextView today=navItem("⌂","Today",true), analytics=navItem("◔","Insights",false), settings=navItem("⚙","Settings",false);
        today.setOnClickListener(v->showHome());analytics.setOnClickListener(v->showReport());settings.setOnClickListener(v->settingsDialog());
        nav.addView(today,new LinearLayout.LayoutParams(0,dp(60),1));
        nav.addView(analytics,new LinearLayout.LayoutParams(0,dp(60),1));
        nav.addView(settings,new LinearLayout.LayoutParams(0,dp(60),1));
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(76)));
        setContentView(root);
    }

    TextView navItem(String icon,String name,boolean active){
        TextView v=tv(icon+"\n"+name,11,true);v.setGravity(Gravity.CENTER);
        v.setTextColor(active?BLUE:secondaryText());return v;
    }

    void showHome(){
        base();content.removeAllViews();
        header();gap(18);progressHero();gap(14);quickControls();gap(22);
        sectionHeader();
        for(Habit h:habits)addHabitCard(h);
        if(habits.isEmpty())emptyState();
    }

    void header(){
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_activity_logo);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        h.addView(logo,new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);names.setPadding(dp(12),0,0,0);
        TextView title=tv("Activity",27,true);title.setTextColor(BLUE);
        names.addView(title,new LinearLayout.LayoutParams(-1,dp(34)));
        names.addView(muted("Your daily progress, made simple"),new LinearLayout.LayoutParams(-1,dp(24)));
        h.addView(names,new LinearLayout.LayoutParams(0,dp(60),1));
        TextView menu=action("⋯",surface(),primaryText(),22);menu.setOnClickListener(v->settingsDialog());
        h.addView(menu,new LinearLayout.LayoutParams(dp(52),dp(48)));
        content.addView(h,new LinearLayout.LayoutParams(-1,dp(60)));
    }

    void progressHero(){
        int total=habits.size(),done=completedCount(selectedDate),pct=total==0?0:done*100/total;
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(20),dp(20),dp(20),dp(20));
        hero.setBackground(grad(Color.rgb(30,55,132),Color.rgb(42,104,250),28));

        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView eyebrow=tv(selectedDate.equals(today())?"TODAY • "+new SimpleDateFormat("EEE, d MMM",Locale.US).format(new Date()):displayDate(selectedDate),12,true);
        eyebrow.setTextColor(Color.rgb(213,224,255));top.addView(eyebrow,new LinearLayout.LayoutParams(0,dp(24),1));
        TextView pct=tv(pct+"%",30,true);pct.setTextColor(Color.WHITE);top.addView(pct,new LinearLayout.LayoutParams(-2,dp(40)));
        hero.addView(top);

        TextView title=tv("Daily progress",25,true);title.setTextColor(Color.WHITE);
        hero.addView(title,new LinearLayout.LayoutParams(-1,dp(36)));

        TextView msg=tv(done==0?"Start with one small win.":done==total?"All activities completed. Great work!":(total-done)+" activities left for today.",13,false);
        msg.setTextColor(Color.rgb(224,232,255));hero.addView(msg,new LinearLayout.LayoutParams(-1,dp(30)));

        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(pct);
        bar.setProgressTintList(ColorStateList.valueOf(Color.WHITE));bar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.argb(70,255,255,255)));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(10));bp.setMargins(0,dp(10),0,dp(14));hero.addView(bar,bp);

        LinearLayout stats=new LinearLayout(this);
        TextView c=tv(done+"/"+total+" completed",13,true);c.setTextColor(Color.WHITE);
        TextView s=tv(streak()+" day streak",13,true);s.setTextColor(Color.WHITE);
        stats.addView(c,new LinearLayout.LayoutParams(0,dp(28),1));stats.addView(s,new LinearLayout.LayoutParams(0,dp(28),1));
        hero.addView(stats);
        content.addView(hero,new LinearLayout.LayoutParams(-1,dp(174)));
    }

    void quickControls(){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView date=action("▣  "+displayDate(selectedDate),surface(),primaryText(),13);date.setOnClickListener(v->datePicker());
        row.addView(date,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView filter=action("☷",surface(),primaryText(),18);filter.setOnClickListener(v->filterDialog());
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(dp(52),dp(52));fp.setMargins(dp(8),0,0,0);row.addView(filter,fp);
        TextView add=action("+",BLUE,Color.WHITE,23);add.setOnClickListener(v->addHabitDialog(null));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(dp(58),dp(52));ap.setMargins(dp(8),0,0,0);row.addView(add,ap);
        content.addView(row,new LinearLayout.LayoutParams(-1,dp(52)));
    }

    void sectionHeader(){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout t=new LinearLayout(this);t.setOrientation(LinearLayout.VERTICAL);
        t.addView(tv("Today’s activities",20,true),new LinearLayout.LayoutParams(-1,dp(30)));
        t.addView(muted(habits.size()+" habits • "+completedCount(selectedDate)+" completed"),new LinearLayout.LayoutParams(-1,dp(24)));
        r.addView(t,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView add=action("Add activity",Color.rgb(232,239,255),BLUE,12);add.setOnClickListener(v->addHabitDialog(null));
        r.addView(add,new LinearLayout.LayoutParams(dp(112),dp(42)));
        content.addView(r,new LinearLayout.LayoutParams(-1,dp(54)));
        gap(10);
    }

    void addHabitCard(Habit h){
        boolean done=isDone(h.id,selectedDate);
        LinearLayout c=card(22);c.setPadding(dp(14),dp(14),dp(14),dp(14));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon=action(done?"✓":categoryIcon(h.category),done?Color.rgb(222,249,239):Color.rgb(235,240,255),done?Color.rgb(18,135,88):BLUE,17);
        row.addView(icon,new LinearLayout.LayoutParams(dp(46),dp(46)));

        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,dp(8),0);
        TextView name=tv(h.name,16,true);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(name,new LinearLayout.LayoutParams(-1,-2));
        String meta=(h.target==null||h.target.trim().isEmpty()?"No target":h.target)+"  •  "+h.category;
        TextView sub=muted(meta);sub.setMaxLines(1);sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(sub,new LinearLayout.LayoutParams(-1,dp(24)));
        row.addView(info,new LinearLayout.LayoutParams(0,-2,1));

        TextView status=chip(done?"Done":"Pending",done?Color.rgb(222,249,239):Color.rgb(238,242,248),done?Color.rgb(18,135,88):Color.rgb(65,79,105));
        row.addView(status,new LinearLayout.LayoutParams(-2,dp(34)));
        c.addView(row);

        TextView frequency=muted("↻ "+h.frequency+"   •   Long-press to edit or delete");
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(24));fp.setMargins(dp(58),dp(7),0,0);c.addView(frequency,fp);

        c.setOnClickListener(v->{setDone(h.id,selectedDate,!isDone(h.id,selectedDate));showHome();});
        c.setOnLongClickListener(v->{editDeleteDialog(h);return true;});
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(10));content.addView(c,cp);
    }

    String categoryIcon(String c){
        if("Fitness".equals(c))return "⚡";
        if("Health".equals(c))return "♥";
        if("Study".equals(c))return "▤";
        if("Work".equals(c))return "◆";
        if("Personal".equals(c))return "★";
        if("Finance".equals(c))return "₹";
        return "•";
    }

    void emptyState(){
        LinearLayout e=card(24);e.setGravity(Gravity.CENTER);e.setPadding(dp(22),dp(28),dp(22),dp(28));
        TextView i=action("+",Color.rgb(232,239,255),BLUE,28);e.addView(i,new LinearLayout.LayoutParams(dp(64),dp(64)));
        e.addView(tv("Build your day",20,true),new LinearLayout.LayoutParams(-1,dp(36)));
        TextView m=muted("Create your first activity and start building a streak.");m.setGravity(Gravity.CENTER);
        e.addView(m,new LinearLayout.LayoutParams(-1,dp(38)));
        content.addView(e);
    }

    void datePicker(){
        Calendar c=Calendar.getInstance();
        try{c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(selectedDate));}catch(Exception ignored){}
        new DatePickerDialog(this,(v,y,m,d)->{selectedDate=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);showHome();},
                c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }

    void addHabitDialog(Habit existing){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(22),dp(4),dp(22),0);
        EditText name=field("Activity name",existing==null?"":existing.name);l.addView(name,new LinearLayout.LayoutParams(-1,dp(58)));
        EditText target=field("Target / goal",existing==null?"":existing.target);l.addView(target,new LinearLayout.LayoutParams(-1,dp(58)));
        Spinner cat=new Spinner(this);String[] cats={"Health","Fitness","Study","Work","Personal","Finance","Other"};
        cat.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,cats));
        if(existing!=null)for(int i=0;i<cats.length;i++)if(cats[i].equals(existing.category))cat.setSelection(i);
        l.addView(cat,new LinearLayout.LayoutParams(-1,dp(50)));
        EditText freq=field("Frequency (Daily / Weekly / Custom)",existing==null?"Daily":existing.frequency);l.addView(freq,new LinearLayout.LayoutParams(-1,dp(58)));

        AlertDialog d=new AlertDialog.Builder(this).setTitle(existing==null?"New activity":"Edit activity").setView(l)
                .setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{
            String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Enter a name");return;}
            String f=freq.getText().toString().trim();if(f.isEmpty())f="Daily";
            if(existing==null)habits.add(new Habit(System.currentTimeMillis(),n,cat.getSelectedItem().toString(),target.getText().toString().trim(),f));
            else{existing.name=n;existing.category=cat.getSelectedItem().toString();existing.target=target.getText().toString().trim();existing.frequency=f;}
            saveHabits();d.dismiss();showHome();
        }));d.show();
    }

    EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextColor(primaryText());e.setHintTextColor(secondaryText());return e;}

    void editDeleteDialog(Habit h){
        new AlertDialog.Builder(this).setTitle(h.name).setItems(new String[]{"Edit activity","Delete activity"},(d,w)->{
            if(w==0)addHabitDialog(h);else new AlertDialog.Builder(this).setTitle("Delete activity?")
                    .setMessage("Completion history for this activity will also be removed.")
                    .setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,y)->{habits.remove(h);removeHistory(h.id);saveHabits();showHome();}).show();
        }).show();
    }

    void removeHistory(long id){
        Set<String>s=new HashSet<>(prefs.getStringSet("done",new HashSet<>()));Iterator<String>it=s.iterator();String p=id+"|";
        while(it.hasNext())if(it.next().startsWith(p))it.remove();prefs.edit().putStringSet("done",s).apply();
    }

    void filterDialog(){
        new AlertDialog.Builder(this).setTitle("Show activities").setItems(new String[]{"All","Completed","Pending"},(d,w)->showFilteredHome(w)).show();
    }

    void showFilteredHome(int mode){
        base();content.removeAllViews();header();gap(18);progressHero();gap(14);quickControls();gap(20);
        content.addView(tv(mode==0?"All activities":mode==1?"Completed activities":"Pending activities",20,true),new LinearLayout.LayoutParams(-1,dp(32)));
        gap(8);
        for(Habit h:habits)if(mode==0||(mode==1&&isDone(h.id,selectedDate))||(mode==2&&!isDone(h.id,selectedDate)))addHabitCard(h);
    }

    void showReport(){
        base();content.removeAllViews();header();gap(18);
        LinearLayout intro=card(24);intro.addView(tv("Insights",24,true),new LinearLayout.LayoutParams(-1,dp(36)));
        intro.addView(muted("See how consistently you complete your activities."),new LinearLayout.LayoutParams(-1,dp(30)));
        content.addView(intro);gap(14);

        LinearLayout range=card(22);range.addView(tv("Report period",16,true),new LinearLayout.LayoutParams(-1,dp(30)));
        TextView from=action("From  "+displayDateLong(reportFrom),surface(),primaryText(),13);from.setOnClickListener(v->pickReport(true));
        range.addView(from,new LinearLayout.LayoutParams(-1,dp(50)));gapIn(range,8);
        TextView to=action("To  "+displayDateLong(today()),surface(),primaryText(),13);to.setOnClickListener(v->pickReport(false));
        range.addView(to,new LinearLayout.LayoutParams(-1,dp(50)));gapIn(range,8);
        TextView gen=action("Generate report",BLUE,Color.WHITE,13);gen.setOnClickListener(v->showReport());range.addView(gen,new LinearLayout.LayoutParams(-1,dp(50)));
        content.addView(range);gap(14);renderReport(reportFrom,today());
    }

    void gapIn(LinearLayout l,int h){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}

    void pickReport(boolean from){
        Calendar c=Calendar.getInstance();
        new DatePickerDialog(this,(v,y,m,d)->{String val=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);if(from)reportFrom=val;showReport();},
                c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }

    void renderReport(String from,String to){
        int days=daysBetween(from,to),total=habits.size()*Math.max(days,0),achieved=0;
        Calendar c=Calendar.getInstance();try{c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(from));}catch(Exception ignored){}
        StringBuilder rows=new StringBuilder();
        for(int i=0;i<days;i++){String d=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());int done=completedCount(d);achieved+=done;
            rows.append(displayDateLong(d)).append("   ").append(done).append("/").append(habits.size()).append("   ").append(habits.size()==0?0:done*100/habits.size()).append("%\n");
            c.add(Calendar.DAY_OF_MONTH,1);}
        int score=total==0?0:achieved*100/total;
        LinearLayout scoreCard=card(24);scoreCard.addView(tv("Performance score",16,true),new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);
        TextView sc=tv(score+"%",40,true);sc.setTextColor(BLUE);r.addView(sc,new LinearLayout.LayoutParams(dp(105),dp(60)));
        LinearLayout inf=new LinearLayout(this);inf.setOrientation(LinearLayout.VERTICAL);inf.addView(tv(achieved+" completed",15,true));inf.addView(muted("out of "+total+" scheduled check-ins"));
        r.addView(inf,new LinearLayout.LayoutParams(0,dp(60),1));scoreCard.addView(r);
        ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(100);p.setProgress(score);
        p.setProgressTintList(ColorStateList.valueOf(BLUE));scoreCard.addView(p,new LinearLayout.LayoutParams(-1,dp(10)));
        scoreCard.addView(muted(displayDateLong(from)+"  →  "+displayDateLong(to)),new LinearLayout.LayoutParams(-1,dp(28)));
        content.addView(scoreCard);gap(12);
        LinearLayout details=card(22);details.addView(tv("Daily activity",16,true),new LinearLayout.LayoutParams(-1,dp(30)));
        TextView list=tv(rows.length()==0?"No report data.":rows.toString(),13,false);list.setTextColor(secondaryText());details.addView(list);content.addView(details);
    }

    int daysBetween(String a,String b){try{Date x=new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(a),y=new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(b);return (int)((y.getTime()-x.getTime())/86400000L)+1;}catch(Exception e){return 1;}}
    int completedCount(String date){int n=0;for(Habit h:habits)if(isDone(h.id,date))n++;return n;}
    boolean isDone(long id,String date){return prefs.getStringSet("done",new HashSet<>()).contains(id+"|"+date);}
    void setDone(long id,String date,boolean done){Set<String>s=new HashSet<>(prefs.getStringSet("done",new HashSet<>()));String k=id+"|"+date;if(done)s.add(k);else s.remove(k);prefs.edit().putStringSet("done",s).apply();}
    int streak(){int s=0;Calendar c=Calendar.getInstance();for(int i=0;i<366;i++){String d=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());if(completedCount(d)>0){s++;c.add(Calendar.DAY_OF_MONTH,-1);}else break;}return s;}
    String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    String firstOfMonth(){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
    String displayDate(String d){try{return new SimpleDateFormat("EEE, d MMM",Locale.US).format(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(d));}catch(Exception e){return d;}}
    String displayDateLong(String d){try{return new SimpleDateFormat("dd MMM yyyy",Locale.US).format(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(d));}catch(Exception e){return d;}}

    void loadHabits(){
        habits.clear();String raw=prefs.getString("habits","");
        if(raw.isEmpty()){habits.add(new Habit(1,"Morning Stretching & Yoga","Fitness","20 min","Daily"));habits.add(new Habit(2,"1 Hour Exercise / Gym","Fitness","60 min","Daily"));
            habits.add(new Habit(3,"10,000 Steps Walking","Health","10,000 steps","Daily"));habits.add(new Habit(4,"Drink 3 Litres Water","Health","3 litres","Daily"));
            habits.add(new Habit(5,"Face Care & Sunscreen Routine","Personal","Morning + evening","Daily"));habits.add(new Habit(6,"Healthy Nutrition","Health","Balanced meals","Daily"));saveHabits();return;}
        try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);habits.add(new Habit(o.getLong("id"),o.getString("name"),o.getString("category"),o.optString("target",""),o.optString("frequency","Daily")));}}catch(Exception ignored){}
    }
    void saveHabits(){try{JSONArray a=new JSONArray();for(Habit h:habits){JSONObject o=new JSONObject();o.put("id",h.id);o.put("name",h.name);o.put("category",h.category);o.put("target",h.target);o.put("frequency",h.frequency);a.put(o);}prefs.edit().putString("habits",a.toString()).apply();}catch(Exception ignored){}}

    void settingsDialog(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(22),dp(6),dp(22),0);
        CheckBox darkBox=new CheckBox(this);darkBox.setText("Dark mode");darkBox.setTextColor(primaryText());darkBox.setChecked(dark);l.addView(darkBox,new LinearLayout.LayoutParams(-1,dp(54)));
        Button reset=new Button(this);reset.setText("Reset completion history");reset.setAllCaps(false);l.addView(reset,new LinearLayout.LayoutParams(-1,dp(52)));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Settings").setView(l).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        reset.setOnClickListener(v->{prefs.edit().remove("done").apply();Toast.makeText(this,"Completion history reset",Toast.LENGTH_SHORT).show();});
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{dark=darkBox.isChecked();prefs.edit().putBoolean("dark",dark).apply();d.dismiss();showHome();}));d.show();
    }

    void printReport(){
        PrintManager pm=(PrintManager)getSystemService(PRINT_SERVICE);
        pm.print("Activity Report",new PrintDocumentAdapter(){
            public void onLayout(PrintAttributes a,PrintAttributes b,CancellationSignal c,LayoutResultCallback cb,Bundle e){cb.onLayoutFinished(new PrintDocumentInfo.Builder("activity-report.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(),true);}
            public void onWrite(PageRange[] p,android.os.ParcelFileDescriptor dest,CancellationSignal c,WriteResultCallback cb){try{
                java.io.FileOutputStream out=new java.io.FileOutputStream(dest.getFileDescriptor());StringBuilder s=new StringBuilder("ACTIVITY\nPerformance Report\n\n");
                for(Habit h:habits)s.append(h.name).append(" — ").append(isDone(h.id,today())?"Completed":"Pending").append("\n");
                out.write(s.toString().getBytes());out.flush();cb.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            }catch(Exception e){cb.onWriteFailed(e.toString());}}
        },null);
    }
}