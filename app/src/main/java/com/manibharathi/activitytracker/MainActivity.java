package com.manibharathi.activitytracker;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.print.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int PURPLE=Color.rgb(106,72,255), BLUE=Color.rgb(47,145,255), TEAL=Color.rgb(39,211,184);
    static final int BG=Color.rgb(244,244,252), WHITE=Color.WHITE, NAVY=Color.rgb(25,28,52), MUTED=Color.rgb(100,105,132);
    LinearLayout root, content; SharedPreferences prefs; boolean dark; String selectedDate,reportFrom; ArrayList<Habit> habits=new ArrayList<>();

    static class Habit { long id; String name,category,target,frequency;
        Habit(long i,String n,String c,String t,String f){id=i;name=n;category=c;target=t;frequency=f;} }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs=getSharedPreferences("tracker",MODE_PRIVATE); dark=prefs.getBoolean("dark",false);
        selectedDate=today(); reportFrom=firstOfMonth(); loadHabits(); showHome();
    }

    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    int bg(){return dark?Color.rgb(12,14,25):BG;} int surface(){return dark?Color.rgb(25,28,43):WHITE;}
    int text(){return dark?Color.rgb(245,247,255):NAVY;} int muted(){return dark?Color.rgb(171,177,198):MUTED;}
    GradientDrawable solid(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    GradientDrawable glass(float r){GradientDrawable g=solid(dark?Color.rgb(25,28,43):Color.argb(218,255,255,255),r);g.setStroke(dp(1),dark?Color.rgb(53,58,79):Color.rgb(229,230,242));return g;}
    GradientDrawable grad(int a,int b,float r){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});g.setCornerRadius(dp(r));return g;}
    TextView tv(String s,float z,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(text());v.setTypeface(android.graphics.Typeface.create("sans-serif",b?1:0));return v;}
    TextView sub(String s){TextView v=tv(s,12,false);v.setTextColor(muted());return v;}
    TextView pill(String s,int bgc,int tc,float z){TextView v=tv(s,z,true);v.setTextColor(tc);v.setGravity(Gravity.CENTER);v.setPadding(dp(13),0,dp(13),0);v.setBackground(solid(bgc,30));return v;}
    LinearLayout card(float r){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(18),dp(16),dp(18),dp(16));l.setBackground(glass(r));l.setElevation(dp(1));return l;}
    void gap(int h){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}

    void base(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg());
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(24),dp(18),dp(30));
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);sc.addView(content);
        root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);nav.setPadding(dp(10),dp(7),dp(10),dp(9));nav.setBackground(glass(28));
        TextView home=nav("⌂","Home",true), ins=nav("▥","Insights",false);home.setOnClickListener(v->showHome());ins.setOnClickListener(v->showReport());
        nav.addView(home,new LinearLayout.LayoutParams(0,dp(60),1));
        TextView add=pill("+",PURPLE,WHITE,27);add.setGravity(Gravity.CENTER);add.setElevation(dp(7));add.setOnClickListener(v->addHabitDialog(null));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(dp(58),dp(58));ap.setMargins(dp(7),0,dp(7),0);nav.addView(add,ap);
        nav.addView(ins,new LinearLayout.LayoutParams(0,dp(60),1));
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(82)));setContentView(root);
    }

    TextView nav(String icon,String label,boolean active){TextView v=tv(icon+"\n"+label,11,true);v.setGravity(Gravity.CENTER);v.setTextColor(active?PURPLE:muted());return v;}

    void header(){
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(0,0,0,dp(3));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_activity_logo);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        h.addView(logo,new LinearLayout.LayoutParams(dp(58),dp(58)));
        LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(11),0,0,0);
        TextView title=tv("Activity",28,true);title.setTextColor(PURPLE);b.addView(title,new LinearLayout.LayoutParams(-1,dp(35)));
        b.addView(sub("Plan  •  Track  •  Achieve"),new LinearLayout.LayoutParams(-1,dp(22)));
        h.addView(b,new LinearLayout.LayoutParams(0,dp(60),1));
        TextView menu=pill("⋮",Color.argb(40,106,72,255),PURPLE,23);menu.setContentDescription("Settings");menu.setOnClickListener(v->settingsDialog());
        h.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));content.addView(h,new LinearLayout.LayoutParams(-1,dp(72)));
    }

    void showHome(){
        base();content.removeAllViews();header();gap(15);hero();gap(14);dateFilter();gap(14);addButton();gap(19);
        TextView t=tv("Today",21,true);content.addView(t,new LinearLayout.LayoutParams(-1,dp(30)));
        TextView s=sub(habits.size()+" activities  •  "+completedCount(selectedDate)+" completed");content.addView(s,new LinearLayout.LayoutParams(-1,dp(24)));gap(9);
        for(Habit h:habits)activityCard(h);if(habits.isEmpty())emptyState();
        gap(10);quickInsights();
    }

    void hero(){
        int total=habits.size(),done=completedCount(selectedDate),pct=total==0?0:done*100/total;
        LinearLayout h=new LinearLayout(this);h.setOrientation(LinearLayout.VERTICAL);h.setPadding(dp(20),dp(17),dp(20),dp(16));h.setBackground(grad(Color.rgb(81,52,220),Color.rgb(42,147,250),28));h.setElevation(dp(3));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView l=tv(selectedDate.equals(today())?"TODAY  •  "+displayDate(selectedDate):displayDate(selectedDate),11,true);l.setTextColor(Color.rgb(225,220,255));top.addView(l,new LinearLayout.LayoutParams(0,dp(24),1));
        TextView p=tv(pct+"%",32,true);p.setTextColor(WHITE);p.setGravity(Gravity.RIGHT);top.addView(p,new LinearLayout.LayoutParams(-2,dp(40)));h.addView(top);
        TextView title=tv(done==total&&total>0?"Day completed. Nice work!":done>0?"Keep the momentum going!":"Ready for a productive day?",21,true);title.setTextColor(WHITE);h.addView(title,new LinearLayout.LayoutParams(-1,dp(32)));
        TextView m=tv(done==0?"Complete one activity to start your streak.":(total-done)+" activities remaining today.",12,false);m.setTextColor(Color.rgb(231,229,255));h.addView(m,new LinearLayout.LayoutParams(-1,dp(24)));
        ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(100);pb.setProgress(pct);pb.setProgressTintList(ColorStateList.valueOf(WHITE));pb.setProgressBackgroundTintList(ColorStateList.valueOf(Color.argb(55,255,255,255)));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(8));pp.setMargins(0,dp(6),0,dp(8));h.addView(pb,pp);
        LinearLayout stats=new LinearLayout(this);TextView a=tv(done+"/"+total+" completed",12,true);a.setTextColor(WHITE);stats.addView(a,new LinearLayout.LayoutParams(0,dp(24),1));TextView st=tv(streak()+" day streak  ✦",12,true);st.setTextColor(WHITE);st.setGravity(Gravity.RIGHT);stats.addView(st,new LinearLayout.LayoutParams(0,dp(24),1));h.addView(stats);
        content.addView(h,new LinearLayout.LayoutParams(-1,dp(165)));
    }

    void dateFilter(){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);
        TextView d=pill("▣   "+displayDate(selectedDate)+"   ⌄",surface(),text(),13);d.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);d.setOnClickListener(v->datePicker());r.addView(d,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView f=pill("☷  All",surface(),text(),13);f.setOnClickListener(v->filterDialog());LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(dp(106),dp(52));fp.setMargins(dp(8),0,0,0);r.addView(f,fp);content.addView(r,new LinearLayout.LayoutParams(-1,dp(52)));
    }

    void addButton(){
        TextView add=pill("＋   Add Activity",PURPLE,WHITE,14);add.setGravity(Gravity.CENTER);add.setElevation(dp(2));add.setOnClickListener(v->addHabitDialog(null));content.addView(add,new LinearLayout.LayoutParams(-1,dp(52)));
    }

    void activityCard(Habit h){
        boolean done=isDone(h.id,selectedDate);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(12),dp(11));c.setBackground(glass(23));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon=pill(done?"✓":categoryIcon(h.category),done?Color.rgb(222,248,238):Color.rgb(237,233,255),done?Color.rgb(18,145,88):PURPLE,18);icon.setGravity(Gravity.CENTER);row.addView(icon,new LinearLayout.LayoutParams(dp(50),dp(50)));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,dp(5),0);
        TextView name=tv(h.name,17,true);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);info.addView(name,new LinearLayout.LayoutParams(0,dp(42),1));
        TextView meta=sub("◷  "+(h.target.isEmpty()?"No target":h.target)+"   •   "+h.frequency);meta.setMaxLines(1);meta.setEllipsize(TextUtils.TruncateAt.END);info.addView(meta,new LinearLayout.LayoutParams(-1,dp(22)));
        row.addView(info,new LinearLayout.LayoutParams(0,dp(66),1));
        TextView more=pill("⋮",Color.argb(30,106,72,255),PURPLE,20);more.setContentDescription("Activity options");more.setOnClickListener(v->editDeleteDialog(h));row.addView(more,new LinearLayout.LayoutParams(dp(40),dp(42)));c.addView(row);
        LinearLayout bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER_VERTICAL);bottom.setPadding(dp(62),dp(4),0,0);
        TextView cat=sub(h.category);bottom.addView(cat,new LinearLayout.LayoutParams(0,dp(25),1));
        TextView status=pill(done?"✓  DONE":"◷  PENDING",done?Color.rgb(221,248,237):Color.rgb(232,239,255),done?Color.rgb(16,143,85):Color.rgb(42,86,180),10);bottom.addView(status,new LinearLayout.LayoutParams(-2,dp(28)));c.addView(bottom);
        c.setOnClickListener(v->{setDone(h.id,selectedDate,!isDone(h.id,selectedDate));showHome();});
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(106));cp.setMargins(0,0,0,dp(10));content.addView(c,cp);
    }

    String categoryIcon(String c){if("Fitness".equals(c))return "⚡";if("Health".equals(c))return "♥";if("Study".equals(c))return "▤";if("Work".equals(c))return "◆";if("Personal".equals(c))return "★";if("Finance".equals(c))return "₹";return "•";}

    void quickInsights(){
        LinearLayout c=card(24);LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.addView(tv("Quick Insights",18,true),new LinearLayout.LayoutParams(0,dp(32),1));TextView v=pill("View report  →",Color.argb(28,106,72,255),PURPLE,11);v.setOnClickListener(x->showReport());top.addView(v,new LinearLayout.LayoutParams(dp(118),dp(34)));c.addView(top);
        LinearLayout row=new LinearLayout(this);metric(row,""+habits.size(),"Total",PURPLE);metric(row,""+completedCount(today()),"Completed",Color.rgb(24,154,96));metric(row,""+(habits.size()-completedCount(today())),"Pending",Color.rgb(235,126,50));metric(row,(habits.size()==0?0:completedCount(today())*100/habits.size())+"%","Success",Color.rgb(119,73,220));c.addView(row,new LinearLayout.LayoutParams(-1,dp(86)));content.addView(c);
    }

    void metric(LinearLayout p,String value,String label,int c){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setBackground(solid(Color.argb(dark?35:70,Math.min(255,c+20)%256,Math.min(255,c+35)%256,255),18));TextView v=tv(value,18,true);v.setTextColor(c);box.addView(v,new LinearLayout.LayoutParams(-1,dp(30)));TextView l=tv(label,10,true);l.setTextColor(muted());box.addView(l,new LinearLayout.LayoutParams(-1,dp(22)));LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(0,dp(72),1);q.setMargins(dp(3),0,dp(3),0);p.addView(box,q);}

    void emptyState(){LinearLayout e=card(26);e.setGravity(Gravity.CENTER);TextView i=pill("+",Color.rgb(235,231,255),PURPLE,28);e.addView(i,new LinearLayout.LayoutParams(dp(68),dp(68)));TextView t=tv("Build your day",20,true);t.setGravity(Gravity.CENTER);e.addView(t,new LinearLayout.LayoutParams(-1,dp(35)));TextView m=sub("Add your first activity and start tracking.");m.setGravity(Gravity.CENTER);e.addView(m,new LinearLayout.LayoutParams(-1,dp(28)));content.addView(e);}

    void addHabitDialog(Habit existing){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(3),dp(22),0);
        TextView intro=tv(existing==null?"Create a clear, measurable activity.":"Refine your activity details.",13,false);intro.setTextColor(muted());box.addView(intro,new LinearLayout.LayoutParams(-1,dp(30)));
        EditText name=field("Activity name",existing==null?"":existing.name);box.addView(name,new LinearLayout.LayoutParams(-1,dp(58)));
        EditText target=field("Target  •  30 min / 2 litres / 10,000 steps",existing==null?"":existing.target);box.addView(target,new LinearLayout.LayoutParams(-1,dp(58)));
        Spinner cat=new Spinner(this);String[] cats={"Health","Fitness","Study","Work","Personal","Finance","Other"};cat.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,cats));if(existing!=null)for(int i=0;i<cats.length;i++)if(cats[i].equals(existing.category))cat.setSelection(i);box.addView(cat,new LinearLayout.LayoutParams(-1,dp(52)));
        EditText freq=field("Frequency  •  Daily / Weekly / Custom",existing==null?"Daily":existing.frequency);box.addView(freq,new LinearLayout.LayoutParams(-1,dp(58)));
        AlertDialog d=new AlertDialog.Builder(this).setTitle(existing==null?"New activity":"Edit activity").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Enter an activity name");return;}String f=freq.getText().toString().trim();if(f.isEmpty())f="Daily";if(existing==null)habits.add(new Habit(System.currentTimeMillis(),n,cat.getSelectedItem().toString(),target.getText().toString().trim(),f));else{existing.name=n;existing.category=cat.getSelectedItem().toString();existing.target=target.getText().toString().trim();existing.frequency=f;}saveHabits();d.dismiss();showHome();}));d.show();
    }

    EditText field(String hint,String val){EditText e=new EditText(this);e.setHint(hint);e.setText(val);e.setTextColor(text());e.setHintTextColor(muted());e.setSingleLine(true);e.setTextSize(14);return e;}

    void editDeleteDialog(Habit h){new AlertDialog.Builder(this).setTitle(h.name).setItems(new String[]{"Edit activity","Delete activity"},(d,w)->{if(w==0)addHabitDialog(h);else new AlertDialog.Builder(this).setTitle("Delete activity?").setMessage("Completion history for this activity will be removed.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,y)->{habits.remove(h);removeHistory(h.id);saveHabits();showHome();}).show();}).show();}

    void filterDialog(){new AlertDialog.Builder(this).setTitle("Show activities").setItems(new String[]{"All activities","Completed","Pending"},(d,w)->showFilteredHome(w)).show();}
    void showFilteredHome(int mode){base();content.removeAllViews();header();gap(15);hero();gap(14);dateFilter();gap(18);content.addView(tv(mode==0?"All activities":mode==1?"Completed":"Pending",21,true),new LinearLayout.LayoutParams(-1,dp(31)));gap(8);for(Habit h:habits)if(mode==0||(mode==1&&isDone(h.id,selectedDate))||(mode==2&&!isDone(h.id,selectedDate)))activityCard(h);}

    void showReport(){
        base();content.removeAllViews();header();gap(15);
        TextView title=tv("Insights",28,true);content.addView(title,new LinearLayout.LayoutParams(-1,dp(38)));TextView intro=tv("See your consistency, momentum and performance at a glance.",13,false);intro.setTextColor(muted());content.addView(intro,new LinearLayout.LayoutParams(-1,dp(28)));gap(14);
        reportHero();gap(14);insightGrid();gap(14);reportControls();gap(14);dailyReport();gap(10);
    }

    void reportHero(){
        int total=habits.size(),done=completedCount(today()),rate=total==0?0:done*100/total;
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(20),dp(18),dp(20),dp(18));c.setBackground(grad(Color.rgb(54,35,150),Color.rgb(45,155,235),28));
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.addView(tv("PERFORMANCE",11,true),new LinearLayout.LayoutParams(0,dp(26),1));TextView score=tv(rate+"%",38,true);score.setTextColor(WHITE);r.addView(score);c.addView(r);
        TextView msg=tv(rate>=80?"Excellent consistency!":rate>=50?"Good momentum — keep building.":"Start small and build your streak.",16,true);msg.setTextColor(WHITE);c.addView(msg,new LinearLayout.LayoutParams(-1,dp(34)));
        ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(100);p.setProgress(rate);p.setProgressTintList(ColorStateList.valueOf(WHITE));p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.argb(55,255,255,255)));c.addView(p,new LinearLayout.LayoutParams(-1,dp(8)));
        TextView m=tv(done+" completed today  •  "+total+" total activities",12,false);m.setTextColor(Color.rgb(228,228,255));c.addView(m,new LinearLayout.LayoutParams(-1,dp(26)));content.addView(c,new LinearLayout.LayoutParams(-1,dp(145)));
    }

    void insightGrid(){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);metric(row,""+habits.size(),"Activities",PURPLE);metric(row,""+completedCount(today()),"Completed",Color.rgb(24,154,96));metric(row,""+(habits.size()-completedCount(today())),"Pending",Color.rgb(235,126,50));metric(row,""+streak(),"Streak",Color.rgb(52,118,221));content.addView(row,new LinearLayout.LayoutParams(-1,dp(88)));
    }

    void reportControls(){
        LinearLayout c=card(23);c.addView(tv("Report period",17,true),new LinearLayout.LayoutParams(-1,dp(32)));
        LinearLayout r=new LinearLayout(this);TextView from=pill("From\n"+displayDateLong(reportFrom),surface(),text(),11);from.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);from.setOnClickListener(v->pickReport(true));r.addView(from,new LinearLayout.LayoutParams(0,dp(54),1));TextView to=pill("To\n"+displayDateLong(today()),surface(),text(),11);to.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(54),1);tp.setMargins(dp(8),0,0,0);r.addView(to,tp);to.setOnClickListener(v->pickReport(false));c.addView(r);
        TextView generate=pill("Generate detailed report  →",PURPLE,WHITE,12);generate.setGravity(Gravity.CENTER);generate.setOnClickListener(v->showReport());LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(48));gp.setMargins(0,dp(10),0,0);c.addView(generate,gp);content.addView(c);
    }

    void dailyReport(){
        int days=daysBetween(reportFrom,today());Calendar c=Calendar.getInstance();try{c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(reportFrom));}catch(Exception ignored){}
        LinearLayout card=card(23);LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.addView(tv("Daily performance",17,true),new LinearLayout.LayoutParams(0,dp(30),1));head.addView(sub(days+" days"),new LinearLayout.LayoutParams(-2,dp(30)));card.addView(head);
        for(int i=0;i<Math.min(days,10);i++){String d=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());int done=completedCount(d),rate=habits.size()==0?0:done*100/habits.size();LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView date=tv(displayDate(d),12,true);r.addView(date,new LinearLayout.LayoutParams(dp(100),dp(38)));ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(100);p.setProgress(rate);p.setProgressTintList(ColorStateList.valueOf(rate>0?TEAL:Color.rgb(222,225,235)));r.addView(p,new LinearLayout.LayoutParams(0,dp(8),1));TextView val=tv(rate+"%",11,true);val.setTextColor(rate>0?TEAL:muted());val.setGravity(Gravity.RIGHT);r.addView(val,new LinearLayout.LayoutParams(dp(48),dp(38)));card.addView(r);c.add(Calendar.DAY_OF_MONTH,1);}
        content.addView(card);
    }

    void statCard(LinearLayout p,String v,String l){metric(p,v,l,PURPLE);}
    void pickReport(boolean from){Calendar c=Calendar.getInstance();try{String seed=from?reportFrom:today();c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(seed));}catch(Exception ignored){}new DatePickerDialog(this,(v,y,m,d)->{String val=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);if(from)reportFrom=val;showReport();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}

    void settingsDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(4),dp(20),0);
        CheckBox darkBox=new CheckBox(this);darkBox.setText("Dark mode");darkBox.setTextSize(15);darkBox.setTextColor(text());darkBox.setChecked(dark);box.addView(darkBox,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView reset=pill("Reset completion history",Color.rgb(249,235,238),Color.rgb(177,60,74),12);box.addView(reset,new LinearLayout.LayoutParams(-1,dp(48)));reset.setOnClickListener(v->{prefs.edit().remove("done").apply();Toast.makeText(this,"Completion history reset",Toast.LENGTH_SHORT).show();});
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Settings").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{dark=darkBox.isChecked();prefs.edit().putBoolean("dark",dark).apply();d.dismiss();showHome();}));d.show();
    }

    void datePicker(){Calendar c=Calendar.getInstance();try{c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(selectedDate));}catch(Exception ignored){}new DatePickerDialog(this,(v,y,m,d)->{selectedDate=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);showHome();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}
    void removeHistory(long id){Set<String>s=new HashSet<>(prefs.getStringSet("done",new HashSet<>()));Iterator<String>it=s.iterator();String p=id+"|";while(it.hasNext())if(it.next().startsWith(p))it.remove();prefs.edit().putStringSet("done",s).apply();}
    int daysBetween(String a,String b){try{Date x=new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(a),y=new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(b);return Math.max(1,(int)((y.getTime()-x.getTime())/86400000L)+1);}catch(Exception e){return 1;}}
    int completedCount(String d){int n=0;for(Habit h:habits)if(isDone(h.id,d))n++;return n;}
    boolean isDone(long id,String d){return prefs.getStringSet("done",new HashSet<>()).contains(id+"|"+d);}
    void setDone(long id,String d,boolean done){Set<String>s=new HashSet<>(prefs.getStringSet("done",new HashSet<>()));String k=id+"|"+d;if(done)s.add(k);else s.remove(k);prefs.edit().putStringSet("done",s).apply();}
    int streak(){int s=0;Calendar c=Calendar.getInstance();for(int i=0;i<366;i++){String d=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());if(completedCount(d)>0){s++;c.add(Calendar.DAY_OF_MONTH,-1);}else break;}return s;}
    String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    String firstOfMonth(){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
    String displayDate(String d){try{return new SimpleDateFormat("EEE, d MMM",Locale.US).format(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(d));}catch(Exception e){return d;}}
    String displayDateLong(String d){try{return new SimpleDateFormat("dd MMM yyyy",Locale.US).format(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(d));}catch(Exception e){return d;}}

    void loadHabits(){habits.clear();String raw=prefs.getString("habits","");if(raw.isEmpty()){habits.add(new Habit(1,"Morning Stretching & Yoga","Fitness","20 min","Daily"));habits.add(new Habit(2,"1 Hour Exercise / Gym","Fitness","60 min","Daily"));habits.add(new Habit(3,"10,000 Steps Walking","Health","10,000 steps","Daily"));habits.add(new Habit(4,"Drink 3 Litres Water","Health","3 litres","Daily"));habits.add(new Habit(5,"Face Care & Sunscreen Routine","Personal","Morning + evening","Daily"));habits.add(new Habit(6,"Healthy Nutrition","Health","Balanced meals","Daily"));saveHabits();return;}try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);habits.add(new Habit(o.getLong("id"),o.getString("name"),o.getString("category"),o.optString("target",""),o.optString("frequency","Daily")));}}catch(Exception ignored){}}
    void saveHabits(){try{JSONArray a=new JSONArray();for(Habit h:habits){JSONObject o=new JSONObject();o.put("id",h.id);o.put("name",h.name);o.put("category",h.category);o.put("target",h.target);o.put("frequency",h.frequency);a.put(o);}prefs.edit().putString("habits",a.toString()).apply();}catch(Exception ignored){}}
}
    void printReport(){
        PrintManager pm=(PrintManager)getSystemService(PRINT_SERVICE);
        pm.print("Activity Report",new PrintDocumentAdapter(){
            public void onLayout(PrintAttributes a,PrintAttributes b,android.os.CancellationSignal c,LayoutResultCallback cb,Bundle e){cb.onLayoutFinished(new PrintDocumentInfo.Builder("activity-report.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(),true);}
            public void onWrite(PageRange[] p,android.os.ParcelFileDescriptor dest,android.os.CancellationSignal c,WriteResultCallback cb){try{java.io.FileOutputStream out=new java.io.FileOutputStream(dest.getFileDescriptor());StringBuilder s=new StringBuilder("ACTIVITY\nPerformance Report\n\n");for(Habit h:habits)s.append(h.name).append(" — ").append(isDone(h.id,today())?"Completed":"Pending").append("\n");out.write(s.toString().getBytes());out.flush();cb.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});}catch(Exception e){cb.onWriteFailed(e.toString());}}
        },null);
    }
