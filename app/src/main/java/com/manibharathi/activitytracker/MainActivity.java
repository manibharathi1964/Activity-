package com.manibharathi.activitytracker;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.print.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int PURPLE=Color.rgb(108,92,231), TEAL=Color.rgb(32,211,194), NAVY=Color.rgb(24,30,55);
    static final int BG=Color.rgb(246,247,251), WHITE=Color.WHITE, MUTED=Color.rgb(111,119,143);
    LinearLayout root, content; SharedPreferences prefs; boolean dark;
    String selectedDate, reportFrom; ArrayList<Habit> habits=new ArrayList<>();

    static class Habit {
        long id; String name,category,target,frequency;
        Habit(long id,String name,String category,String target,String frequency){this.id=id;this.name=name;this.category=category;this.target=target;this.frequency=frequency;}
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs=getSharedPreferences("tracker",MODE_PRIVATE); dark=prefs.getBoolean("dark",false);
        selectedDate=today(); reportFrom=firstOfMonth(); loadHabits(); showHome();
    }

    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    int bg(){return dark?Color.rgb(13,16,28):BG;} int surface(){return dark?Color.rgb(23,28,44):WHITE;}
    int text(){return dark?Color.rgb(245,247,255):NAVY;} int mutedColor(){return dark?Color.rgb(168,177,199):MUTED;}
    GradientDrawable rounded(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    GradientDrawable bordered(int c,float r){GradientDrawable g=rounded(c,r);g.setStroke(dp(1),dark?Color.rgb(48,56,76):Color.rgb(228,231,239));return g;}
    GradientDrawable gradient(int a,int b,float r){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});g.setCornerRadius(dp(r));return g;}
    TextView tv(String s,float size,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(text());v.setTypeface(android.graphics.Typeface.create("sans-serif",bold?1:0));return v;}
    TextView small(String s){TextView v=tv(s,12,false);v.setTextColor(mutedColor());return v;}
    TextView button(String s,int c,int tc,float size,float radius){TextView v=tv(s,size,true);v.setTextColor(tc);v.setGravity(Gravity.CENTER);v.setBackground(rounded(c,radius));v.setPadding(dp(12),0,dp(12),0);v.setClickable(true);return v;}
    LinearLayout card(float radius){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(18),dp(16),dp(18),dp(16));l.setBackground(bordered(surface(),radius));return l;}
    void gap(int h){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}

    void base(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg());
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(12),dp(18),dp(28));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(16),dp(8),dp(16),dp(10));nav.setBackgroundColor(surface());
        TextView home=navItem("Today",true), insights=navItem("Insights",false);
        home.setOnClickListener(v->showHome()); insights.setOnClickListener(v->showReport());
        nav.addView(home,new LinearLayout.LayoutParams(0,dp(58),1));
        TextView add=button("+",PURPLE,WHITE,24,22); add.setElevation(dp(8)); add.setOnClickListener(v->addHabitDialog(null));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(dp(58),dp(58));ap.setMargins(dp(10),0,dp(10),0);nav.addView(add,ap);
        nav.addView(insights,new LinearLayout.LayoutParams(0,dp(58),1));
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(78))); setContentView(root);
    }

    TextView navItem(String name,boolean active){
        TextView v=tv((active?"●  ":"○  ")+name,12,true);v.setGravity(Gravity.CENTER);v.setTextColor(active?PURPLE:mutedColor());return v;
    }

    void header(){
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(0,dp(8),0,dp(4));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_activity_logo);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        h.addView(logo,new LinearLayout.LayoutParams(dp(62),dp(62)));
        LinearLayout nameBox=new LinearLayout(this);nameBox.setOrientation(LinearLayout.VERTICAL);nameBox.setPadding(dp(12),0,0,0);
        TextView title=tv("Activity",29,true);title.setTextColor(PURPLE);nameBox.addView(title,new LinearLayout.LayoutParams(-1,dp(36)));
        nameBox.addView(small("Plan less. Do more."));
        h.addView(nameBox,new LinearLayout.LayoutParams(0,dp(62),1));
        TextView menu=button("⋯",surface(),text(),23,18);menu.setContentDescription("Settings");menu.setOnClickListener(v->settingsDialog());
        h.addView(menu,new LinearLayout.LayoutParams(dp(50),dp(46)));
        content.addView(h,new LinearLayout.LayoutParams(-1,dp(70)));
    }

    void showHome(){
        base();content.removeAllViews();header();gap(14);progressHero();gap(14);dateRow();gap(22);sectionHeader();
        for(Habit h:habits)addHabitCard(h); if(habits.isEmpty())emptyState();
    }

    void progressHero(){
        int total=habits.size(), done=completedCount(selectedDate), percent=total==0?0:done*100/total;
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(20),dp(19),dp(20),dp(18));
        hero.setBackground(gradient(Color.rgb(39,32,91),Color.rgb(108,92,231),28));hero.setElevation(dp(2));

        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=tv(selectedDate.equals(today())?"TODAY  •  "+displayDate(selectedDate):displayDate(selectedDate),11,true);label.setTextColor(Color.rgb(220,216,255));
        top.addView(label,new LinearLayout.LayoutParams(0,dp(24),1));
        TextView pctView=tv(percent+"%",30,true);pctView.setTextColor(WHITE);top.addView(pctView,new LinearLayout.LayoutParams(-2,dp(38)));
        hero.addView(top);
        TextView title=tv(done==0?"Ready for a productive day?":done==total?"Day completed. Nice work!":"Keep the momentum going.",22,true);title.setTextColor(WHITE);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(32));tp.setMargins(0,dp(3),0,0);hero.addView(title,tp);
        TextView msg=tv(done==0?"Complete one activity to start your streak.":(total-done)+" activities remaining today.",12,false);msg.setTextColor(Color.rgb(224,221,255));
        hero.addView(msg,new LinearLayout.LayoutParams(-1,dp(26)));
        ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(100);p.setProgress(percent);
        p.setProgressTintList(ColorStateList.valueOf(WHITE));p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.argb(65,255,255,255)));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(8));pp.setMargins(0,dp(9),0,dp(12));hero.addView(p,pp);
        LinearLayout stats=new LinearLayout(this);
        TextView a=tv(done+"/"+total+" completed",12,true);a.setTextColor(WHITE);stats.addView(a,new LinearLayout.LayoutParams(0,dp(24),1));
        TextView b=tv(streak()+" day streak",12,true);b.setTextColor(WHITE);b.setGravity(Gravity.RIGHT);stats.addView(b,new LinearLayout.LayoutParams(0,dp(24),1));
        hero.addView(stats);content.addView(hero,new LinearLayout.LayoutParams(-1,dp(170)));
    }

    void dateRow(){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView date=button("▣   "+displayDate(selectedDate),surface(),text(),13,17);date.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);date.setOnClickListener(v->datePicker());
        row.addView(date,new LinearLayout.LayoutParams(0,dp(50),1));
        TextView filter=button("☷",surface(),text(),19,17);filter.setOnClickListener(v->filterDialog());
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(dp(50),dp(50));fp.setMargins(dp(8),0,0,0);row.addView(filter,fp);
        content.addView(row,new LinearLayout.LayoutParams(-1,dp(50)));
    }

    void sectionHeader(){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(tv("Today",21,true),new LinearLayout.LayoutParams(-1,dp(30)));
        labels.addView(small(habits.size()+" activities  •  "+completedCount(selectedDate)+" completed"),new LinearLayout.LayoutParams(-1,dp(24)));
        row.addView(labels,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView add=button("Add",Color.rgb(236,233,255),PURPLE,12,16);add.setOnClickListener(v->addHabitDialog(null));
        row.addView(add,new LinearLayout.LayoutParams(dp(76),dp(42)));content.addView(row,new LinearLayout.LayoutParams(-1,dp(54)));gap(10);
    }

    void addHabitCard(Habit h){
        boolean done=isDone(h.id,selectedDate);
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(12),dp(12));
        c.setBackground(bordered(surface(),22));c.setElevation(dp(1));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);

        TextView mark=button(done?"✓":categoryIcon(h.category),done?Color.rgb(221,248,239):Color.rgb(237,234,255),done?Color.rgb(18,133,86):PURPLE,18,16);
        row.addView(mark,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,dp(8),0);
        TextView name=tv(h.name,16,true);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(name,new LinearLayout.LayoutParams(0,dp(40),1));
        TextView meta=small((h.target==null||h.target.isEmpty()?"No target":h.target)+"   •   "+h.category);
        meta.setMaxLines(1);meta.setEllipsize(TextUtils.TruncateAt.END);info.addView(meta,new LinearLayout.LayoutParams(-1,dp(22)));
        row.addView(info,new LinearLayout.LayoutParams(0,dp(66),1));

        TextView more=button("⋮",surface(),mutedColor(),20,14);more.setContentDescription("Activity options");more.setOnClickListener(v->editDeleteDialog(h));
        row.addView(more,new LinearLayout.LayoutParams(dp(40),dp(44)));
        c.addView(row);

        LinearLayout bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER_VERTICAL);bottom.setPadding(dp(60),dp(5),dp(2),0);
        TextView freq=small("↻  "+h.frequency);bottom.addView(freq,new LinearLayout.LayoutParams(0,dp(24),1));
        TextView status=tv(done?"DONE":"PENDING",10,true);status.setTextColor(done?Color.rgb(18,133,86):Color.rgb(94,105,127));status.setGravity(Gravity.CENTER);
        status.setBackground(rounded(done?Color.rgb(226,248,239):Color.rgb(240,242,247),30));status.setPadding(dp(10),0,dp(10),0);
        bottom.addView(status,new LinearLayout.LayoutParams(-2,dp(28)));c.addView(bottom);

        c.setOnClickListener(v->{setDone(h.id,selectedDate,!isDone(h.id,selectedDate));showHome();});
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(104));cp.setMargins(0,0,0,dp(10));content.addView(c,cp);
    }

    String categoryIcon(String c){if("Fitness".equals(c))return "⚡";if("Health".equals(c))return "♥";if("Study".equals(c))return "▤";if("Work".equals(c))return "◆";if("Personal".equals(c))return "★";if("Finance".equals(c))return "₹";return "•";}

    void emptyState(){
        LinearLayout e=card(24);e.setGravity(Gravity.CENTER);e.setPadding(dp(24),dp(30),dp(24),dp(30));
        TextView i=button("+",Color.rgb(236,233,255),PURPLE,28,20);e.addView(i,new LinearLayout.LayoutParams(dp(66),dp(66)));
        TextView t=tv("Your day starts here",20,true);t.setGravity(Gravity.CENTER);e.addView(t,new LinearLayout.LayoutParams(-1,dp(34)));
        TextView m=small("Add an activity and make your first win.");m.setGravity(Gravity.CENTER);e.addView(m,new LinearLayout.LayoutParams(-1,dp(30)));
        content.addView(e);
    }

    void datePicker(){
        Calendar c=Calendar.getInstance();try{c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(selectedDate));}catch(Exception ignored){}
        new DatePickerDialog(this,(v,y,m,d)->{selectedDate=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);showHome();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }

    void addHabitDialog(Habit existing){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(6),dp(22),0);
        TextView hint=small(existing==null?"Keep it clear and measurable.":"Update the activity details.");box.addView(hint,new LinearLayout.LayoutParams(-1,dp(28)));
        EditText name=field("Activity name",existing==null?"":existing.name);box.addView(name,new LinearLayout.LayoutParams(-1,dp(56)));
        EditText target=field("Target  •  e.g. 30 min / 2 litres",existing==null?"":existing.target);box.addView(target,new LinearLayout.LayoutParams(-1,dp(56)));
        Spinner cat=new Spinner(this);String[] cats={"Health","Fitness","Study","Work","Personal","Finance","Other"};cat.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,cats));
        if(existing!=null)for(int i=0;i<cats.length;i++)if(cats[i].equals(existing.category))cat.setSelection(i);
        box.addView(cat,new LinearLayout.LayoutParams(-1,dp(52)));
        EditText freq=field("Frequency  •  Daily / Weekly / Custom",existing==null?"Daily":existing.frequency);box.addView(freq,new LinearLayout.LayoutParams(-1,dp(56)));
        AlertDialog d=new AlertDialog.Builder(this).setTitle(existing==null?"Add activity":"Edit activity").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Enter a name");return;}String f=freq.getText().toString().trim();if(f.isEmpty())f="Daily";
            if(existing==null)habits.add(new Habit(System.currentTimeMillis(),n,cat.getSelectedItem().toString(),target.getText().toString().trim(),f));
            else{existing.name=n;existing.category=cat.getSelectedItem().toString();existing.target=target.getText().toString().trim();existing.frequency=f;}
            saveHabits();d.dismiss();showHome();}));d.show();
    }

    EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextColor(text());e.setHintTextColor(mutedColor());e.setSingleLine(true);return e;}

    void editDeleteDialog(Habit h){
        new AlertDialog.Builder(this).setTitle(h.name).setItems(new String[]{"Edit activity","Delete activity"},(d,w)->{
            if(w==0)addHabitDialog(h);else new AlertDialog.Builder(this).setTitle("Delete activity?").setMessage("Its completion history will also be removed.")
                .setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,y)->{habits.remove(h);removeHistory(h.id);saveHabits();showHome();}).show();
        }).show();
    }

    void filterDialog(){new AlertDialog.Builder(this).setTitle("Filter").setItems(new String[]{"All activities","Completed","Pending"},(d,w)->showFilteredHome(w)).show();}
    void showFilteredHome(int mode){base();content.removeAllViews();header();gap(14);progressHero();gap(14);dateRow();gap(20);content.addView(tv(mode==0?"All activities":mode==1?"Completed":"Pending",21,true),new LinearLayout.LayoutParams(-1,dp(32)));gap(8);
        for(Habit h:habits)if(mode==0||(mode==1&&isDone(h.id,selectedDate))||(mode==2&&!isDone(h.id,selectedDate)))addHabitCard(h);
    }

    void showReport(){
        base();content.removeAllViews();header();gap(14);
        LinearLayout intro=card(24);intro.addView(tv("Insights",24,true),new LinearLayout.LayoutParams(-1,dp(34)));intro.addView(small("A clear view of your consistency and progress."),new LinearLayout.LayoutParams(-1,dp(28)));content.addView(intro);gap(12);
        LinearLayout stats=new LinearLayout(this);stats.setGravity(Gravity.CENTER_VERTICAL);
        statCard(stats,""+habits.size(),"Activities");statCard(stats,""+completedCount(today()),"Done today");statCard(stats,""+streak(),"Day streak");content.addView(stats,new LinearLayout.LayoutParams(-1,dp(92)));gap(12);
        LinearLayout range=card(22);range.addView(tv("Report period",16,true),new LinearLayout.LayoutParams(-1,dp(30)));
        TextView from=button("From   "+displayDateLong(reportFrom),surface(),text(),12,15);from.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);from.setOnClickListener(v->pickReport(true));range.addView(from,new LinearLayout.LayoutParams(-1,dp(48)));
        Space sp=new Space(this);range.addView(sp,new LinearLayout.LayoutParams(1,dp(8)));
        TextView to=button("To   "+displayDateLong(today()),surface(),text(),12,15);to.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);to.setOnClickListener(v->pickReport(false));range.addView(to,new LinearLayout.LayoutParams(-1,dp(48)));
        Space sp2=new Space(this);range.addView(sp2,new LinearLayout.LayoutParams(1,dp(8)));
        TextView gen=button("Generate report",PURPLE,WHITE,12,16);gen.setOnClickListener(v->showReport());range.addView(gen,new LinearLayout.LayoutParams(-1,dp(48)));
        content.addView(range);gap(12);renderReport(reportFrom,today());
    }

    void statCard(LinearLayout parent,String value,String label){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(12),dp(12),dp(8),dp(8));c.setBackground(bordered(surface(),18));TextView v=tv(value,22,true);v.setTextColor(PURPLE);c.addView(v,new LinearLayout.LayoutParams(-1,dp(30)));c.addView(small(label),new LinearLayout.LayoutParams(-1,dp(24)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(82),1);p.setMargins(0,0,dp(7),0);parent.addView(c,p);}
    void pickReport(boolean from){Calendar c=Calendar.getInstance();new DatePickerDialog(this,(v,y,m,d)->{String val=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);if(from)reportFrom=val;showReport();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}
    void renderReport(String from,String to){
        int days=daysBetween(from,to),total=habits.size()*Math.max(days,0),achieved=0;Calendar c=Calendar.getInstance();try{c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(from));}catch(Exception ignored){}
        StringBuilder rows=new StringBuilder();for(int i=0;i<days;i++){String d=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());int done=completedCount(d);achieved+=done;rows.append(displayDateLong(d)).append("    ").append(done).append("/").append(habits.size()).append("    ").append(habits.size()==0?0:done*100/habits.size()).append("%\n");c.add(Calendar.DAY_OF_MONTH,1);}
        int scoreValue=total==0?0:achieved*100/total;LinearLayout scoreCard=card(22);scoreCard.addView(tv("Performance",16,true),new LinearLayout.LayoutParams(-1,dp(30)));
        TextView big=tv(scoreValue+"%",38,true);big.setTextColor(PURPLE);scoreCard.addView(big,new LinearLayout.LayoutParams(-1,dp(50)));
        ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(100);p.setProgress(scoreValue);p.setProgressTintList(ColorStateList.valueOf(PURPLE));scoreCard.addView(p,new LinearLayout.LayoutParams(-1,dp(8)));
        scoreCard.addView(small(achieved+" completed  •  "+total+" scheduled check-ins"),new LinearLayout.LayoutParams(-1,dp(26)));content.addView(scoreCard);gap(12);
        LinearLayout detail=card(22);detail.addView(tv("Daily activity",16,true),new LinearLayout.LayoutParams(-1,dp(30)));TextView rowsV=small(rows.length()==0?"No report data.":rows.toString());detail.addView(rowsV);content.addView(detail);
    }

    void settingsDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(6),dp(20),0);
        CheckBox darkBox=new CheckBox(this);darkBox.setText("Dark mode");darkBox.setTextColor(text());darkBox.setChecked(dark);box.addView(darkBox,new LinearLayout.LayoutParams(-1,dp(54)));
        TextView reset=button("Reset completion history",Color.rgb(246,238,238),Color.rgb(170,60,70),12,16);box.addView(reset,new LinearLayout.LayoutParams(-1,dp(48)));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Settings").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        reset.setOnClickListener(v->{prefs.edit().remove("done").apply();Toast.makeText(this,"Completion history reset",Toast.LENGTH_SHORT).show();});
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{dark=darkBox.isChecked();prefs.edit().putBoolean("dark",dark).apply();d.dismiss();showHome();}));d.show();
    }

    void removeHistory(long id){Set<String>s=new HashSet<>(prefs.getStringSet("done",new HashSet<>()));Iterator<String>it=s.iterator();String p=id+"|";while(it.hasNext())if(it.next().startsWith(p))it.remove();prefs.edit().putStringSet("done",s).apply();}
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
        if(raw.isEmpty()){habits.add(new Habit(1,"Morning Stretching & Yoga","Fitness","20 min","Daily"));habits.add(new Habit(2,"1 Hour Exercise / Gym","Fitness","60 min","Daily"));habits.add(new Habit(3,"10,000 Steps Walking","Health","10,000 steps","Daily"));habits.add(new Habit(4,"Drink 3 Litres Water","Health","3 litres","Daily"));habits.add(new Habit(5,"Face Care & Sunscreen Routine","Personal","Morning + evening","Daily"));habits.add(new Habit(6,"Healthy Nutrition","Health","Balanced meals","Daily"));saveHabits();return;}
        try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);habits.add(new Habit(o.getLong("id"),o.getString("name"),o.getString("category"),o.optString("target",""),o.optString("frequency","Daily")));}}catch(Exception ignored){}
    }
    void saveHabits(){try{JSONArray a=new JSONArray();for(Habit h:habits){JSONObject o=new JSONObject();o.put("id",h.id);o.put("name",h.name);o.put("category",h.category);o.put("target",h.target);o.put("frequency",h.frequency);a.put(o);}prefs.edit().putString("habits",a.toString()).apply();}catch(Exception ignored){}}

    void printReport(){
        PrintManager pm=(PrintManager)getSystemService(PRINT_SERVICE);
        pm.print("Activity Report",new PrintDocumentAdapter(){
            public void onLayout(PrintAttributes a,PrintAttributes b,CancellationSignal c,LayoutResultCallback cb,Bundle e){cb.onLayoutFinished(new PrintDocumentInfo.Builder("activity-report.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(),true);}
            public void onWrite(PageRange[] p,android.os.ParcelFileDescriptor dest,CancellationSignal c,WriteResultCallback cb){try{java.io.FileOutputStream out=new java.io.FileOutputStream(dest.getFileDescriptor());StringBuilder s=new StringBuilder("ACTIVITY\nPerformance Report\n\n");for(Habit h:habits)s.append(h.name).append(" — ").append(isDone(h.id,today())?"Completed":"Pending").append("\n");out.write(s.toString().getBytes());out.flush();cb.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});}catch(Exception e){cb.onWriteFailed(e.toString());}}
        },null);
    }
}