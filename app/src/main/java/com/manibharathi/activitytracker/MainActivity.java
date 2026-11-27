package com.manibharathi.activitytracker;

import android.app.*;
import android.content.*;
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
    static final int PURPLE=Color.rgb(81,70,229), BLUE=Color.rgb(55,174,225), TEXT=Color.rgb(18,27,48), BG=Color.rgb(246,248,253);
    LinearLayout root, content; SharedPreferences prefs; boolean dark=false; String selectedDate;
    ArrayList<Habit> habits=new ArrayList<>();

    static class Habit {
        long id; String name, category, target, frequency;
        Habit(long i,String n,String c,String t,String f){id=i;name=n;category=c;target=t;frequency=f;}
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("tracker",MODE_PRIVATE);
        dark=prefs.getBoolean("dark",false);
        selectedDate=today();
        loadHabits();
        showHome();
    }

    String today(){ return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()); }
    String displayDate(String d){ try{return new SimpleDateFormat("dd/MM/yyyy",Locale.US).format(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(d));}catch(Exception e){return d;} }
    int bg(){return dark?Color.rgb(18,20,30):BG;} int card(){return dark?Color.rgb(30,33,46):Color.WHITE;} int text(){return dark?Color.WHITE:TEXT;} int muted(){return dark?Color.rgb(190,196,210):Color.rgb(85,95,115);}

    void base(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg());
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); sv.addView(content);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(8,8,8,8); nav.setBackgroundColor(card());
        Button home=navButton("⌂","Home"); Button report=navButton("◔","Report");
        home.setOnClickListener(v->showHome()); report.setOnClickListener(v->showReport());
        nav.addView(home,new LinearLayout.LayoutParams(0,72,1)); nav.addView(report,new LinearLayout.LayoutParams(0,72,1));
        root.addView(nav,new LinearLayout.LayoutParams(-1,88));
        setContentView(root);
    }

    Button navButton(String icon,String label){ Button b=new Button(this); b.setText(icon+"\n"+label); b.setTextSize(13); b.setAllCaps(false); b.setTextColor(PURPLE); b.setBackgroundColor(Color.TRANSPARENT); return b; }

    TextView tv(String s,float size,boolean bold){
        TextView v=new TextView(this); v.setText(s); v.setTextColor(text()); v.setTextSize(size); v.setTypeface(android.graphics.Typeface.DEFAULT,bold?1:0); v.setGravity(Gravity.CENTER_VERTICAL); return v;
    }
    GradientDrawable rounded(int color,float r){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(r); g.setStroke(1,dark?Color.rgb(70,75,95):Color.rgb(215,221,235)); return g; }
    LinearLayout box(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(28,24,28,24); l.setBackground(rounded(card(),28)); return l; }
    void gap(int h){ Space s=new Space(this); content.addView(s,new LinearLayout.LayoutParams(1,h)); }
    void addText(String s,float size,boolean bold){ content.addView(tv(s,size,bold),new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT)); }

    void showHome(){
        base(); content.removeAllViews(); header();
        gap(18); progressCard(); gap(18); dateRow(); gap(16);
        for(Habit h:habits) addHabitRow(h);
        if(habits.isEmpty()){ LinearLayout e=box(); TextView t=tv("No activities yet. Tap + Add to create your first habit.",16,false); e.addView(t); content.addView(e,new LinearLayout.LayoutParams(-1,120)); }
    }

    void header(){
        LinearLayout h=box(); h.setOrientation(LinearLayout.HORIZONTAL); h.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon=tv("☑",30,true); icon.setGravity(Gravity.CENTER); icon.setTextColor(Color.WHITE); icon.setBackground(rounded(PURPLE,32));
        h.addView(icon,new LinearLayout.LayoutParams(80,80));
        LinearLayout names=new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL); names.setPadding(20,0,0,0);
        TextView title=tv("My Activity Tracker",24,true); title.setTextColor(BLUE); names.addView(title);
        names.addView(tv("Advanced Habit & Goal Manager",15,true)); h.addView(names,new LinearLayout.LayoutParams(0,80,1));
        Button settings=new Button(this); settings.setText("⚙"); settings.setTextSize(24); settings.setAllCaps(false); settings.setBackground(rounded(dark?Color.rgb(40,44,58):Color.rgb(248,249,253),24)); settings.setOnClickListener(v->settingsDialog());
        h.addView(settings,new LinearLayout.LayoutParams(76,76));
        content.addView(h,new LinearLayout.LayoutParams(-1,120));
    }

    void progressCard(){
        int total=habits.size(), done=completedCount(selectedDate), pct=total==0?0:(done*100/total);
        LinearLayout p=box(); TextView row=tv("Today's Progress                         "+pct+"% Completed",19,true); p.addView(row,new LinearLayout.LayoutParams(-1,55));
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); bar.setMax(100); bar.setProgress(pct); bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE)); p.addView(bar,new LinearLayout.LayoutParams(-1,28));
        TextView stats=tv("Completed: "+done+"/"+total+"                              🔥 Streak: "+streak()+" Days",15,true); stats.setTextColor(text()); p.addView(stats,new LinearLayout.LayoutParams(-1,55));
        content.addView(p,new LinearLayout.LayoutParams(-1,190));
    }

    void dateRow(){
        LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL);
        Button date=new Button(this); date.setText(displayDate(selectedDate)+"  ▼"); date.setTextSize(16); date.setAllCaps(false); date.setTextColor(text()); date.setBackground(rounded(card(),24)); date.setOnClickListener(v->datePicker());
        r.addView(date,new LinearLayout.LayoutParams(0,76,1));
        Button filter=new Button(this); filter.setText("⚑"); filter.setTextSize(20); filter.setAllCaps(false); filter.setBackground(rounded(card(),24)); filter.setOnClickListener(v->filterDialog());
        r.addView(filter,new LinearLayout.LayoutParams(78,76));
        Button add=new Button(this); add.setText("+ Add"); add.setTextSize(17); add.setAllCaps(false); add.setTextColor(Color.WHITE); add.setBackground(rounded(PURPLE,24)); add.setOnClickListener(v->addHabitDialog(null));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(145,76); ap.setMargins(12,0,0,0); r.addView(add,ap);
        content.addView(r);
    }

    void addHabitRow(Habit h){
        LinearLayout c=box(); c.setOrientation(LinearLayout.HORIZONTAL); c.setGravity(Gravity.CENTER_VERTICAL);
        CheckBox cb=new CheckBox(this); cb.setChecked(isDone(h.id,selectedDate)); cb.setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{PURPLE,Color.rgb(180,190,205)}));
        cb.setOnClickListener(v->{setDone(h.id,selectedDate,cb.isChecked()); showHome();});
        c.addView(cb,new LinearLayout.LayoutParams(72,90));
        LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL);
        TextView n=tv(h.name,17,true); info.addView(n); TextView sub=tv("Target: "+h.frequency+" • ",14,true); sub.append(h.category); sub.setTextColor(muted()); info.addView(sub);
        c.addView(info,new LinearLayout.LayoutParams(0,100,1));
        TextView status=tv(cb.isChecked()?"Completed":"Pending",14,true); status.setGravity(Gravity.CENTER); status.setTextColor(cb.isChecked()?Color.rgb(20,125,82):Color.rgb(60,72,92)); status.setBackground(rounded(cb.isChecked()?Color.rgb(220,248,234):Color.rgb(231,237,245),22)); c.addView(status,new LinearLayout.LayoutParams(120,52));
        c.setOnLongClickListener(v->{editDeleteDialog(h);return true;});
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,120); cp.setMargins(0,0,0,14); content.addView(c,cp);
    }

    void datePicker(){
        Calendar cal=Calendar.getInstance(); try{cal.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(selectedDate));}catch(Exception ignored){}
        DatePickerDialog d=new DatePickerDialog(this,(v,y,m,day)->{selectedDate=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,day);showHome();},cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)); d.show();
    }

    void addHabitDialog(Habit existing){
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(30,10,30,0);
        EditText name=new EditText(this); name.setHint("Habit name"); name.setText(existing==null?"":existing.name); l.addView(name);
        EditText target=new EditText(this); target.setHint("Target / Goal (e.g. 10000 steps)"); target.setText(existing==null?"":existing.target); l.addView(target);
        Spinner cat=new Spinner(this); String[] cats={"Health","Fitness","Study","Work","Personal","Finance","Other"}; cat.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,cats)); l.addView(cat);
        EditText freq=new EditText(this); freq.setHint("Frequency (Daily / Weekly / Custom)"); freq.setText(existing==null?"Daily":existing.frequency); l.addView(freq);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(existing==null?"Add New Habit":"Edit Habit").setView(l).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dlg.setOnShowListener(x->dlg.getButton(-1).setOnClickListener(v->{
            String n=name.getText().toString().trim(); if(n.isEmpty()){name.setError("Enter habit name");return;}
            String t=target.getText().toString().trim(); String f=freq.getText().toString().trim(); if(f.isEmpty())f="Daily";
            if(existing==null) habits.add(new Habit(System.currentTimeMillis(),n,cat.getSelectedItem().toString(),t,f)); else {existing.name=n;existing.target=t;existing.category=cat.getSelectedItem().toString();existing.frequency=f;}
            saveHabits(); dlg.dismiss(); showHome();
        })); dlg.show();
    }

    void editDeleteDialog(Habit h){
        new AlertDialog.Builder(this).setTitle(h.name).setItems(new String[]{"Edit","Delete"},(d,w)->{if(w==0)addHabitDialog(h);else{habits.remove(h);saveHabits();showHome();}}).show();
    }

    void filterDialog(){
        String[] opts={"All","Completed","Pending"}; new AlertDialog.Builder(this).setTitle("Filter Activities").setItems(opts,(d,w)->{
            ArrayList<Habit> copy=new ArrayList<>(habits); content.removeAllViews(); header();gap(18);progressCard();gap(18);dateRow();gap(16);
            for(Habit h:copy){if(w==0||(w==1&&isDone(h.id,selectedDate))||(w==2&&!isDone(h.id,selectedDate)))addHabitRow(h);}
        }).show();
    }

    void showReport(){
        base(); content.removeAllViews(); header(); gap(20);
        LinearLayout c=box(); TextView title=tv("Executive Analytics & Reports",21,true); c.addView(title,new LinearLayout.LayoutParams(0,60,1));
        Button print=new Button(this); print.setText("▣ Print"); print.setAllCaps(false); print.setTextColor(Color.WHITE); print.setBackground(rounded(PURPLE,22)); c.addView(print,new LinearLayout.LayoutParams(-1,64));
        TextView range=tv("Select Report Date Range",16,true); c.addView(range); gapIn(c,8);
        Button from=new Button(this); from.setText("From: "+displayDate(reportFrom)); from.setAllCaps(false); from.setBackground(rounded(card(),18)); c.addView(from,new LinearLayout.LayoutParams(-1,60));
        Button to=new Button(this); to.setText("To: "+displayDate(today())); to.setAllCaps(false); to.setBackground(rounded(card(),18)); c.addView(to,new LinearLayout.LayoutParams(-1,60));
        Button gen=new Button(this); gen.setText("Generate Detailed Report"); gen.setTextColor(Color.WHITE); gen.setAllCaps(false); gen.setBackground(rounded(PURPLE,22)); c.addView(gen,new LinearLayout.LayoutParams(-1,68));
        content.addView(c,new LinearLayout.LayoutParams(-1,380));
        gap(18); renderReport(reportFrom,today(),print);
        from.setOnClickListener(v->pickReport(true,from,to,print)); to.setOnClickListener(v->pickReport(false,from,to,print)); gen.setOnClickListener(v->{showReport();});
        print.setOnClickListener(v->printReport());
    }

    String reportFrom;
    { reportFrom=firstOfMonth(); }
    String firstOfMonth(){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
    void gapIn(LinearLayout l,int h){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,h));}

    void pickReport(boolean from,Button fb,Button tb,Button print){
        Calendar c=Calendar.getInstance(); DatePickerDialog d=new DatePickerDialog(this,(v,y,m,day)->{String val=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,day);if(from)reportFrom=val;showReport();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));d.show();
    }

    void renderReport(String from,String to,Button print){
        int days=daysBetween(from,to), total=habits.size()*Math.max(days,0), achieved=0; StringBuilder table=new StringBuilder();
        Calendar c=Calendar.getInstance(); try{c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(from));}catch(Exception ignored){}
        for(int i=0;i<days;i++){String d=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());int done=completedCount(d);achieved+=done;table.append(d).append("   ").append(done).append(" / ").append(habits.size()).append("   ").append(habits.size()==0?0:done*100/habits.size()).append("% completed\n");c.add(Calendar.DAY_OF_MONTH,1);}
        int score=total==0?0:achieved*100/total;
        LinearLayout summary=box(); summary.addView(tv("My Activity Tracker",21,true)); summary.addView(tv("Comprehensive Executive Performance Summary     Status: Verified",14,true));
        gapIn(summary,12); summary.addView(tv("RANGE\n"+from+"  to  "+to+"\n\nSCORE\n"+score+"% Performance\n\nACHIEVED\n"+achieved+" out of "+total+" total",17,true));
        content.addView(summary,new LinearLayout.LayoutParams(-1,260)); gap(16);
        LinearLayout details=box(); details.addView(tv("DETAILED DAILY ACTIVITY TABLE",18,true)); details.addView(tv("Date              Completed Habits       Performance",15,true)); TextView rows=tv(table.toString(),14,true); details.addView(rows); content.addView(details,new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    int daysBetween(String a,String b){try{Date x=new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(a),y=new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(b);return (int)((y.getTime()-x.getTime())/86400000L)+1;}catch(Exception e){return 1;}}
    int completedCount(String date){int n=0;for(Habit h:habits)if(isDone(h.id,date))n++;return n;}
    boolean isDone(long id,String date){return prefs.getStringSet("done",new HashSet<>()).contains(id+"|"+date);}
    void setDone(long id,String date,boolean done){Set<String>s=new HashSet<>(prefs.getStringSet("done",new HashSet<>()));String k=id+"|"+date;if(done)s.add(k);else s.remove(k);prefs.edit().putStringSet("done",s).apply();}
    int streak(){int s=0;Calendar c=Calendar.getInstance();for(int i=0;i<366;i++){String d=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());if(completedCount(d)>0){s++;c.add(Calendar.DAY_OF_MONTH,-1);}else break;}return s;}

    void loadHabits(){habits.clear();String raw=prefs.getString("habits","");if(raw.isEmpty()){habits.add(new Habit(1,"Morning Stretching & Yoga & Surya Namaskar","Fitness","Daily","Daily"));habits.add(new Habit(2,"1 Hour Exercise / Gym","Fitness","Daily","Daily"));habits.add(new Habit(3,"10000 Steps Walking","Health","10000 steps","Daily"));habits.add(new Habit(4,"Drink 3 Litres Water","Health","3 litres","Daily"));habits.add(new Habit(5,"Face Care & Ice Water Dip & Sunscreen & Skincare Routine","Health","Daily","Daily"));habits.add(new Habit(6,"Healthy Nutrition / Hair Drink","Health","Daily","Daily"));saveHabits();return;}try{JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);habits.add(new Habit(o.getLong("id"),o.getString("name"),o.getString("category"),o.optString("target",""),o.optString("frequency","Daily")));}}catch(Exception ignored){}}
    void saveHabits(){try{JSONArray a=new JSONArray();for(Habit h:habits){JSONObject o=new JSONObject();o.put("id",h.id);o.put("name",h.name);o.put("category",h.category);o.put("target",h.target);o.put("frequency",h.frequency);a.put(o);}prefs.edit().putString("habits",a.toString()).apply();}catch(Exception ignored){}}

    void settingsDialog(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(30,10,30,0);
        CheckBox darkBox=new CheckBox(this);darkBox.setText("Dark mode");darkBox.setChecked(dark);l.addView(darkBox);
        Button reset=new Button(this);reset.setText("Reset all completed history");reset.setAllCaps(false);l.addView(reset);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Settings").setView(l).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
        reset.setOnClickListener(v->{prefs.edit().remove("done").apply();Toast.makeText(this,"Completion history reset",Toast.LENGTH_SHORT).show();});
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{dark=darkBox.isChecked();prefs.edit().putBoolean("dark",dark).apply();d.dismiss();showHome();}));d.show();
    }

    void printReport(){
        PrintManager pm=(PrintManager)getSystemService(PRINT_SERVICE);pm.print("Activity Tracker Report",new PrintDocumentAdapter(){
            public void onLayout(PrintAttributes oldA,PrintAttributes newA,CancellationSignal cs,LayoutResultCallback cb,Bundle extras){cb.onLayoutFinished(new PrintDocumentInfo.Builder("activity-report.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(),true);}
            public void onWrite(android.print.PageRange[] pages,android.os.ParcelFileDescriptor dest,CancellationSignal cs,WriteResultCallback cb){try{java.io.FileOutputStream out=new java.io.FileOutputStream(dest.getFileDescriptor());String s="My Activity Tracker\nGenerated: "+today()+"\n\n";for(Habit h:habits)s+=h.name+" - "+(isDone(h.id,today() )?"Completed":"Pending")+"\n";out.write(s.getBytes());out.flush();cb.onWriteFinished(new android.print.PageRange[]{android.print.PageRange.ALL_PAGES});}catch(Exception e){cb.onWriteFailed(e.toString());}}
        },null);
    }
}
