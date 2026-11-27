package com.manibharathi.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

private val P=Color(0xFF643AF2); private val B=Color(0xFF159AF2); private val N=Color(0xFF12163A)
private val M=Color(0xFF68708E); private val G=Color(0xFF19B875); private val O=Color(0xFFF08B37); private val C=Color.White

data class A(val id:Long,var name:String,var cat:String,var target:String,var freq:String)
enum class S{HOME,ADD,INSIGHTS,CALENDAR,REPORT,SETTINGS}
object Done{val set=mutableStateSetOf<String>()}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}

@Composable fun App(){
 val c=androidx.compose.ui.platform.LocalContext.current
 var s by remember{mutableStateOf(S.HOME)};var date by remember{mutableStateOf(today())};var list by remember{mutableStateOf(load(c))}
 var dark by remember{mutableStateOf(c.getPreferences(0).getBoolean("dark",false))};var edit by remember{mutableStateOf<A?>(null)};var filter by remember{mutableStateOf("All")}
 LaunchedEffect(Unit){Done.set.addAll(c.getPreferences(0).getStringSet("done",emptySet())?:emptySet())}
 MaterialTheme(colorScheme=if(dark)darkColorScheme() else lightColorScheme()){Surface(Modifier.fillMaxSize(),color=if(dark)Color(0xFF0D1022) else Color(0xFFF6F6FF)){
  when(s){
   S.HOME->Home(list,date,filter,{date=it},{filter=it},{edit=null;s=S.ADD},{id->toggle(c,id,date);list=list.toList()},{edit=it;s=S.ADD},{s=S.INSIGHTS},{s=S.SETTINGS})
   S.ADD->Add(edit,{s=S.HOME}){x->list=if(edit==null)list+x else list.map{if(it.id==x.id)x else it};save(c,list);s=S.HOME}
   S.INSIGHTS->Insights(list,date,{s=S.HOME},{s=S.REPORT})
   S.CALENDAR->Calendar(list,date,{s=S.HOME}){date=it;s=S.HOME}
   S.REPORT->Report(list,{s=S.INSIGHTS}){share(c,report(list))}
   S.SETTINGS->Settings(dark,{s=S.HOME},{v->dark=v;c.getPreferences(0).edit().putBoolean("dark",v).apply()},{share(c,report(list))},{Done.set.clear();c.getPreferences(0).edit().remove("done").apply()})
  }
 }}
}

@Composable fun Header(settings:()->Unit){Row(Modifier.fillMaxWidth().height(64.dp),Alignment.CenterVertically){Icon(painterResource(R.drawable.activity_logo),null,Modifier.size(48.dp));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text("Activity",color=P,fontSize=25.sp,fontWeight=FontWeight.Bold);Text("Plan • Track • Achieve",color=M,fontSize=11.sp)};IconButton(settings){Icon(Icons.Default.MoreVert,null,tint=P)}}}

@Composable fun Home(list:List<A>,date:String,filter:String,setDate:(String)->Unit,setFilter:(String)->Unit,add:()->Unit,toggle:(Long)->Unit,edit:(A)->Unit,insights:()->Unit,settings:()->Unit){
 val d=list.count{done(it.id,date)};val pct=if(list.isEmpty())0 else d*100/list.size;val shown=list.filter{filter=="All"||(filter=="Completed"&&done(it.id,date))||(filter=="Pending"&&!done(it.id,date))}
 Scaffold(bottomBar={Nav(S.HOME,insights,settings)}){p->LazyColumn(Modifier.padding(p).padding(horizontal=18.dp),contentPadding=PaddingValues(top=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Header(settings)};item{Hero(d,list.size,pct)};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Pill("▣  "+display(date),Modifier.weight(1f)){setDate(date)};Pill("☷  "+filter,Modifier.width(110.dp)){setFilter(if(filter=="All")"Completed" else "All")}}};item{GradButton("＋  Add Activity",add)}
  item{Text("Today",color=N,fontSize=21.sp,fontWeight=FontWeight.Bold);Text(list.size.toString()+" activities  •  "+d+" completed",color=M,fontSize=12.sp)}
  items(shown){x->CardA(x,date,toggle,edit)};if(shown.isEmpty())item{Empty(add)};item{Quick(list,date,insights)}
 }}
}

@Composable fun Hero(d:Int,total:Int,pct:Int){Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF512DDB),Color(0xFF159AF2)))).padding(18.dp)){Column{
 Row{Text("TODAY  •  "+display(today()),color=Color(0xFFE5DEFF),fontSize=10.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(pct.toString()+"%",color=C,fontSize=30.sp,fontWeight=FontWeight.Bold)}
 Text(if(pct==100&&total>0)"Day completed. Nice work!" else if(d>0)"Keep going! You're doing great!" else "Ready for a productive day?",color=C,fontSize=18.sp,fontWeight=FontWeight.Bold)
 Spacer(Modifier.height(8.dp));LinearProgressIndicator({pct/100f},Modifier.fillMaxWidth().height(7.dp),color=C,trackColor=C.copy(.25f));Spacer(Modifier.height(7.dp))
 Row{Text(d.toString()+" / "+total+" completed",color=C,fontSize=11.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(streak().toString()+" day streak  ✦",color=C,fontSize=11.sp,fontWeight=FontWeight.Bold)}
}}}

@Composable fun Pill(t:String,m:Modifier,on:()->Unit){Surface(m.height(50.dp).clickable{on()},RoundedCornerShape(16.dp),color=C){Box(Modifier.padding(horizontal=14.dp),Alignment.CenterStart){Text(t,color=N,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}}}
@Composable fun GradButton(t:String,on:()->Unit){Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Brush.horizontalGradient(listOf(P,B))).clickable{on()},Alignment.Center){Text(t,color=C,fontWeight=FontWeight.Bold)}}

@Composable fun CardA(x:A,date:String,toggle:(Long)->Unit,edit:(A)->Unit){val d=done(x.id,date);Surface(shape=RoundedCornerShape(20.dp),color=C,shadowElevation=2.dp){Column(Modifier.clickable{toggle(x.id)}.padding(12.dp)){
 Row(Alignment.CenterVertically){Cat(x.cat);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(x.name,color=N,fontSize=14.sp,fontWeight=FontWeight.Bold);Text(x.target.ifBlank{"No target"}+"  •  "+x.freq,color=M,fontSize=10.sp)}
 Surface(shape=RoundedCornerShape(18.dp),color=if(d)Color(0xFFDDF8E9) else Color(0xFFE8F0FF)){Text(if(d)"✓ Done" else "◷ Pending",Modifier.padding(horizontal=8.dp,vertical=6.dp),color=if(d)G else B,fontSize=9.sp,fontWeight=FontWeight.Bold)}
 IconButton({edit(x)},Modifier.size(32.dp)){Icon(Icons.Default.MoreVert,null,tint=M)}};Row(Modifier.padding(start=58.dp,top=4.dp)){Text(x.cat,color=M,fontSize=10.sp);Spacer(Modifier.weight(1f));Text(if(d)"✓" else "◷",color=if(d)G else B,fontWeight=FontWeight.Bold)}}}}
@Composable fun Cat(cat:String){val q=when(cat){"Fitness"->Triple(Icons.Default.DirectionsRun,Color(0xFFDDF9EC),G);"Health"->Triple(Icons.Default.LocalDrink,Color(0xFFE4F4FF),B);"Personal"->Triple(Icons.Default.Favorite,Color(0xFFFFE6EB),Color(0xFFE64F69));"Study"->Triple(Icons.Default.MenuBook,Color(0xFFE8E3FF),P);else->Triple(Icons.Default.Star,Color(0xFFFFEFD8),O)};Box(Modifier.size(48.dp).clip(CircleShape).background(q.second),Alignment.Center){Icon(q.first,null,tint=q.third,modifier=Modifier.size(25.dp))}}
@Composable fun Empty(add:()->Unit){Surface(shape=RoundedCornerShape(24.dp),color=C){Column(Modifier.fillMaxWidth().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFEAE3FF)),Alignment.Center){Text("+",color=P,fontSize=30.sp)};Text("Build your day",color=N,fontSize=19.sp,fontWeight=FontWeight.Bold);Text("Add your first activity and start tracking.",color=M,fontSize=12.sp);TextButton(add){Text("Add Activity",color=P)}}}}
@Composable fun Quick(l:List<A>,date:String,go:()->Unit){val d=l.count{done(it.id,date)};Surface(shape=RoundedCornerShape(24.dp),color=C){Column(Modifier.padding(16.dp)){Row{Text("Quick Insights",color=N,fontSize=17.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));TextButton(go){Text("View report  →",color=P,fontSize=10.sp)}};Row{Metric(l.size.toString(),"Total",P);Metric(d.toString(),"Completed",G);Metric((l.size-d).toString(),"Pending",O);Metric((if(l.isEmpty())0 else d*100/l.size).toString()+"%","Success",P)}}}}
@Composable fun Metric(v:String,l:String,c:Color){Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text(v,color=c,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(l,color=M,fontSize=9.sp)}}

@Composable fun Nav(active:S,insights:()->Unit,settings:()->Unit){NavigationBar(containerColor=C){NavigationBarItem(active==S.HOME,{},{Icon(Icons.Default.Home,null)},label={Text("Home",fontSize=10.sp)});NavigationBarItem(active==S.INSIGHTS,insights,{Icon(Icons.Default.BarChart,null)},label={Text("Insights",fontSize=10.sp)});NavigationBarItem(active==S.SETTINGS,settings,{Icon(Icons.Default.Settings,null)},label={Text("Settings",fontSize=10.sp)})}}

@Composable fun Top(t:String,back:()->Unit){Row(Modifier.fillMaxWidth().height(58.dp),Alignment.CenterVertically){IconButton(back){Icon(Icons.Default.ArrowBack,null,tint=N)};Text(t,color=N,fontSize=20.sp,fontWeight=FontWeight.Bold)}}
@Composable fun Add(edit:A?,back:()->Unit,save:(A)->Unit){var name by remember{mutableStateOf(edit?.name?:"")};var cat by remember{mutableStateOf(edit?.cat?:"Fitness")};var target by remember{mutableStateOf(edit?.target?:"")};var freq by remember{mutableStateOf(edit?.freq?:"Daily")};Column(Modifier.fillMaxSize().padding(18.dp)){Top(if(edit==null)"Add Activity" else "Edit Activity",back);LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){item{Box(Modifier.fillMaxWidth(),Alignment.Center){Box(Modifier.size(86.dp).clip(CircleShape).background(Color(0xFFDDF7F5)),Alignment.Center){Icon(Icons.Default.DirectionsRun,null,tint=Color(0xFF124E68),modifier=Modifier.size(48.dp))}}};item{Field("Activity Name *",name,"e.g. Morning Exercise"){name=it}};item{Select("Category",cat,listOf("Fitness","Health","Personal","Study","Work","Finance","Other")){cat=it}};item{Select("Input Type","Checkbox",listOf("Checkbox","Duration","Number","Text")){}};item{Field("Target / Goal",target,"e.g. 30 min"){target=it}};item{Select("Frequency",freq,listOf("Daily","Weekly","Custom")){freq=it}};item{Field("Start Date",today(),"Nov 26, 2025"){} };item{Row(Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Reminder",color=N,fontWeight=FontWeight.Bold,fontSize=13.sp);Text("Daily reminder time",color=M,fontSize=10.sp)};Text("8:00 AM",color=N,fontWeight=FontWeight.Bold);Switch(true,{})}};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(back,Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(26.dp)){Text("Cancel",color=P)};Button({if(name.isNotBlank())save(A(edit?.id?:System.currentTimeMillis(),name,cat,target,freq))},Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(26.dp),colors=ButtonDefaults.buttonColors(containerColor=P)){Text("Save")}}}}}}

@Composable fun Field(l:String,v:String,h:String,on:(String)->Unit){Column{Text(l,color=N,fontSize=12.sp,fontWeight=FontWeight.Bold);OutlinedTextField(v,on,Modifier.fillMaxWidth(),placeholder={Text(h,color=M,fontSize=12.sp)},singleLine=true,shape=RoundedCornerShape(14.dp))}}
@Composable fun Select(l:String,v:String,opts:List<String>,on:(String)->Unit){var open by remember{mutableStateOf(false)};Column{Text(l,color=N,fontSize=12.sp,fontWeight=FontWeight.Bold);Box{OutlinedButton({open=true},Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)){Text(v,Modifier.weight(1f),color=N);Icon(Icons.Default.KeyboardArrowDown,null,tint=M)};DropdownMenu(open,{open=false}){opts.forEach{DropdownMenuItem({Text(it)},{on(it);open=false})}}}}}

@Composable fun Insights(l:List<A>,date:String,back:()->Unit,reportGo:()->Unit){val d=l.count{done(it.id,date)};val r=if(l.isEmpty())0 else d*100/l.size;Column(Modifier.fillMaxSize().padding(18.dp)){Top("Insights",back);Row{listOf("Overview","Reports","Analytics").forEachIndexed{i,t->Text(t,Modifier.padding(14.dp),color=if(i==0)P else M,fontSize=11.sp,fontWeight=FontWeight.Bold)}};LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)){item{Surface(shape=RoundedCornerShape(22.dp),color=C){Row(Modifier.padding(18.dp),Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Overall Performance",color=N,fontSize=13.sp,fontWeight=FontWeight.Bold);Text(r.toString()+"%",color=N,fontSize=34.sp,fontWeight=FontWeight.Bold);Text("↑ 5%",color=G,fontWeight=FontWeight.Bold)};Box(Modifier.size(96.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(P,Color(0xFF18D7BF),B,P))),Alignment.Center){Box(Modifier.size(72.dp).clip(CircleShape).background(C),Alignment.Center){Icon(Icons.Default.BarChart,null,tint=B,modifier=Modifier.size(38.dp))}}}}};item{Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){BM(l.size.toString(),"Total",O);BM(d.toString(),"Completed",G);BM((l.size-d).toString(),"Pending",B);BM(streak().toString(),"Streak",O)}};item{Surface(shape=RoundedCornerShape(22.dp),color=C){Column(Modifier.padding(16.dp)){Text("Weekly Progress",color=N,fontWeight=FontWeight.Bold);Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.Bottom){listOf(70,80,90,85,95,89,67).forEach{v->Box(Modifier.width(25.dp).height((v/2).dp).clip(RoundedCornerShape(7.dp)).background(Brush.verticalGradient(listOf(B,P))))}};Text("Mon   Tue   Wed   Thu   Fri   Sat   Sun",color=M,fontSize=8.sp)}}};item{GradButton("Open Reports  →",reportGo)}}}}
@Composable fun BM(v:String,l:String,c:Color){Surface(Modifier.weight(1f),shape=RoundedCornerShape(17.dp),color=C){Column(Modifier.padding(vertical=11.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(v,color=c,fontWeight=FontWeight.Bold,fontSize=18.sp);Text(l,color=M,fontSize=9.sp)}}}

@Composable fun Calendar(l:List<A>,date:String,back:()->Unit,choose:(String)->Unit){Column(Modifier.fillMaxSize().padding(18.dp)){Top("Calendar",back);Text(SimpleDateFormat("MMMM yyyy",Locale.US).format(Date()),Modifier.fillMaxWidth().padding(12.dp),color=N,fontWeight=FontWeight.Bold);LazyColumn{item{Surface(shape=RoundedCornerShape(22.dp),color=C){Column(Modifier.padding(16.dp)){Text("Selected date",color=M,fontSize=10.sp);Text(display(date),color=N,fontSize=18.sp,fontWeight=FontWeight.Bold);Text("Tap the date selector on Home to change the day.",color=M,fontSize=10.sp);l.take(5).forEach{Row(Modifier.padding(vertical=5.dp),Alignment.CenterVertically){Cat(it.cat);Spacer(Modifier.width(8.dp));Text(it.name,color=N,fontSize=11.sp);Spacer(Modifier.weight(1f));Text(if(done(it.id,date))"✓" else "◷",color=if(done(it.id,date))G else B,fontWeight=FontWeight.Bold)}}}}}}}}

@Composable fun Report(l:List<A>,back:()->Unit,share:()->Unit){Column(Modifier.fillMaxSize().padding(18.dp)){Top("Report",back);Row{listOf("Weekly","Monthly","Custom").forEachIndexed{i,t->Surface(shape=RoundedCornerShape(20.dp),color=if(i==0)P else C){Text(t,Modifier.padding(horizontal=16.dp,vertical=9.dp),color=if(i==0)C else N,fontSize=11.sp,fontWeight=FontWeight.Bold)}}};Spacer(Modifier.height(10.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)){item{Surface(shape=RoundedCornerShape(22.dp),color=C){Column(Modifier.padding(18.dp)){Text("Nov 23 – Nov 29, 2025",color=N,fontWeight=FontWeight.Bold);Row(Modifier.padding(top=12.dp),Alignment.CenterVertically){Box(Modifier.size(88.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(B,Color(0xFF18D7BF),P))),Alignment.Center){Box(Modifier.size(70.dp).clip(CircleShape).background(C),Alignment.Center){Text("85%",color=N,fontWeight=FontWeight.Bold,fontSize=20.sp)}};Spacer(Modifier.width(15.dp));Column{Text("Performance",color=M,fontSize=10.sp);Text("85%  ↑ 5%",color=N,fontWeight=FontWeight.Bold,fontSize=17.sp);Text("✓ Achieved  5 / "+maxOf(1,l.size),color=G,fontSize=10.sp);Text("🔥 Streak  7 Days",color=O,fontSize=10.sp)}}}}};item{Surface(shape=RoundedCornerShape(22.dp),color=C){Column(Modifier.padding(16.dp)){Text("Daily Performance",color=N,fontWeight=FontWeight.Bold);Text("70%   80%   90%   85%   95%   89%   67%",Modifier.padding(top=12.dp),color=P,fontWeight=FontWeight.Bold,fontSize=11.sp)}}};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Button(share,Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=P)){Icon(Icons.Default.Print,null);Spacer(Modifier.width(5.dp));Text("Print")};OutlinedButton(share,Modifier.weight(1f)){Icon(Icons.Default.Download,null);Spacer(Modifier.width(5.dp));Text("Save PDF")}}}}}}

@Composable fun Settings(dark:Boolean,back:()->Unit,toggle:(Boolean)->Unit,export:()->Unit,reset:()->Unit){Column(Modifier.fillMaxSize().padding(18.dp)){Top("Settings",back);LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){item{Setting(Icons.Default.DarkMode,"Appearance",if(dark)"Dark Mode" else "Light / Dark Mode"){toggle(!dark)}};item{Setting(Icons.Default.Notifications,"Notifications","Reminder Settings"){} };item{Setting(Icons.Default.Storage,"Data Management","Backup & Restore"){} };item{Setting(Icons.Default.Share,"Export Data","Export Your Data",export)};item{Setting(Icons.Default.Info,"About App","Version 1.0.0"){} };item{Setting(Icons.Default.Security,"Privacy Policy","Read Our Policy"){} };item{Setting(Icons.Default.Star,"Rate Us","Give 5 Stars"){} };item{OutlinedButton(reset,Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Text("Reset Completion History",color=Color(0xFFB63F55))}}}}}
@Composable fun Setting(i:ImageVector,t:String,s:String,click:()->Unit){Surface(shape=RoundedCornerShape(18.dp),color=C){Row(Modifier.fillMaxWidth().clickable{click()}.padding(14.dp),Alignment.CenterVertically){Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE9E4FF)),Alignment.Center){Icon(i,null,tint=P)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(t,color=N,fontWeight=FontWeight.Bold,fontSize=13.sp);Text(s,color=M,fontSize=10.sp)};Icon(Icons.Default.ChevronRight,null,tint=M)}}}

fun done(id:Long,d:String)=Done.set.contains(id.toString()+"|"+d)
fun toggle(c:Context,id:Long,d:String){val k=id.toString()+"|"+d;if(!Done.set.add(k))Done.set.remove(k);c.getPreferences(0).edit().putStringSet("done",Done.set).apply()}
fun streak():Int{var n=0;val c=Calendar.getInstance();while(n<366){val d=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.time);if(Done.set.any{it.endsWith("|"+d)}){n++;c.add(Calendar.DAY_OF_MONTH,-1)}else break};return n}
fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
fun display(s:String)=runCatching{SimpleDateFormat("EEE, MMM d",Locale.US).format(SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(s)!!)}.getOrDefault(s)
fun load(c:Context):List<A>{val raw=c.getPreferences(0).getString("items",null)?:return listOf(A(1,"Morning Exercise","Fitness","30 min","Daily"),A(2,"Drink 3 Litres Water","Health","3 L","Daily"),A(3,"10000 Steps Walking","Health","10,000 steps","Daily"),A(4,"Healthy Nutrition","Health","5 servings","Daily"),A(5,"Face Care & Sunscreen","Personal","Daily","Daily"));return runCatching{val a=JSONArray(raw);(0 until a.length()).map{val o=a.getJSONObject(it);A(o.getLong("id"),o.getString("name"),o.getString("category"),o.optString("target"),o.optString("frequency","Daily"))}}.getOrDefault(emptyList())}
fun save(c:Context,l:List<A>){val a=JSONArray();l.forEach{val o=JSONObject();o.put("id",it.id);o.put("name",it.name);o.put("category",it.cat);o.put("target",it.target);o.put("frequency",it.freq);a.put(o)};c.getPreferences(0).edit().putString("items",a.toString()).apply()}
fun report(l:List<A>)="ACTIVITY REPORT\n\n"+l.joinToString("\n"){it.name+" — "+if(done(it.id,today()))"Completed" else "Pending"}
fun share(c:Context,t:String){c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,t)},"Activity"))}
