package com.manibharathi.activity

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay

private val P=Color(0xFF643AF2); private val B=Color(0xFF159AF2); private val N=Color(0xFF12163A)
private val M=Color(0xFF68708E); private val G=Color(0xFF19B875); private val O=Color(0xFFF08B37); private val C=Color.White

data class A(val id:Long,var name:String,var cat:String,var target:String,var freq:String)
enum class S{HOME,ADD,CALENDAR,REPORT,SETTINGS,SPLASH}
object Done{val set=mutableSetOf<String>()}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}

@Composable fun App(){
 val c=androidx.compose.ui.platform.LocalContext.current
 var s by remember{mutableStateOf(S.SPLASH)};var date by remember{mutableStateOf(today())};var list by remember{mutableStateOf(load(c))}
 var edit by remember{mutableStateOf<A?>(null)};var filter by remember{mutableStateOf("All")};var completionVersion by remember{mutableIntStateOf(0)};var pendingExport by remember{mutableStateOf<String?>(null)};var pendingPdf by remember{mutableStateOf<ByteArray?>(null)}
 val saveFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){u->if(u!=null)pendingExport?.let{c.contentResolver.openOutputStream(u)?.use{os->os.write(it.toByteArray())}}};val savePdf=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){u->if(u!=null)pendingPdf?.let{c.contentResolver.openOutputStream(u)?.use{os->os.write(it)}}};val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){u->if(u!=null){c.contentResolver.openInputStream(u)?.bufferedReader()?.use{br->restoreData(c,br.readText())};list=load(c)}}
 LaunchedEffect(Unit){Done.set.addAll(c.getSharedPreferences("activity",Context.MODE_PRIVATE).getStringSet("done",emptySet())?:emptySet());delay(1200);s=S.HOME}
 BackHandler(enabled=s != S.HOME){s=S.HOME}
 MaterialTheme(colorScheme=lightColorScheme()){Surface(Modifier.fillMaxSize(),color=Color(0xFFF6F6FF)){
  when(s){
   S.SPLASH->Splash()
   S.HOME->Home(list,date,filter,completionVersion,{date=it},{filter=it},{edit=null;s=S.ADD},{id->toggle(c,id,date);completionVersion++},{edit=it;s=S.ADD},{s=S.REPORT},{s=S.SETTINGS})
   S.ADD->Add(c,edit,{s=S.HOME}){x->list=if(edit==null)list+x else list.map{if(it.id==x.id)x else it};save(c,list);s=S.HOME}
   S.CALENDAR->Calendar(list,date,{s=S.HOME}){date=it;s=S.HOME}
   S.REPORT->Report(list,{s=S.HOME},{s=S.SETTINGS},{start,end->pendingPdf=buildPdf(list,start,end);savePdf.launch("Activity-Report.pdf")},{text->share(c,text)})
   S.SETTINGS->Settings({s=S.HOME},{pendingExport=backupData(c,list);saveFile.launch("Activity-Backup.json")},{restore.launch(arrayOf("application/json"))},{share(c,report(list))},{Done.set.clear();c.getSharedPreferences("activity",Context.MODE_PRIVATE).edit().remove("done").apply()})
  }
 }}
}

@Composable fun Splash(){Box(Modifier.fillMaxSize().background(Color(0xFFF6F6FF)),Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Image(painterResource(R.drawable.activity_logo),contentDescription="Activity",Modifier.size(110.dp));Spacer(Modifier.height(18.dp));Text("Activity",color=P,fontSize=40.sp,fontWeight=FontWeight.Bold);Text("Plan • Track • Achieve",color=M,fontSize=16.sp)}}}

@Composable fun Header(settings:()->Unit){Row(Modifier.fillMaxWidth().height(64.dp),verticalAlignment=Alignment.CenterVertically){Image(painterResource(R.drawable.activity_logo),contentDescription="Activity",Modifier.size(48.dp));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text("Activity",color=P,fontSize=25.sp,fontWeight=FontWeight.Bold);Text("Plan • Track • Achieve",color=M,fontSize=11.sp)};Box{var open by remember{mutableStateOf(false)};IconButton({open=true}){Icon(Icons.Default.MoreVert,null,tint=P)};DropdownMenu(open,{open=false}){DropdownMenuItem({Text("Settings")},{open=false;settings()})}}}}

@Composable fun Home(list:List<A>,date:String,filter:String,completionVersion:Int,setDate:(String)->Unit,setFilter:(String)->Unit,add:()->Unit,toggle:(Long)->Unit,edit:(A)->Unit,insights:()->Unit,settings:()->Unit){val c=androidx.compose.ui.platform.LocalContext.current;val refresh=completionVersion;var pickDate by remember{mutableStateOf(false)}
 val d=list.count{done(it.id,date)};val pct=if(list.isEmpty())0 else d*100/list.size;val shown=list.filter{filter=="All"||(filter=="Completed"&&done(it.id,date))||(filter=="Pending"&&!done(it.id,date))}
 Scaffold(bottomBar={Nav(S.HOME,insights,settings)}){p->LazyColumn(Modifier.padding(p).padding(horizontal=18.dp),contentPadding=PaddingValues(top=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Header(settings)};item{Hero(d,list.size,pct)};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
 Surface(Modifier.weight(1f).height(52.dp).clickable{pickDate=true},RoundedCornerShape(16.dp),color=C){
  Row(Modifier.fillMaxSize().padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){
   Icon(Icons.Default.CalendarMonth,null,tint=P,modifier=Modifier.size(23.dp));Spacer(Modifier.width(10.dp));Column{Text("Date",color=M,fontSize=9.sp);Text(display(date),color=N,fontSize=12.sp,fontWeight=FontWeight.Bold)};Spacer(Modifier.weight(1f));Icon(Icons.Default.KeyboardArrowDown,null,tint=M)
  }
 }
 Surface(Modifier.width(118.dp).height(52.dp).clickable{setFilter(if(filter=="All")"Completed" else "All")},RoundedCornerShape(16.dp),color=C){
  Row(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.FilterList,null,tint=P,modifier=Modifier.size(21.dp));Spacer(Modifier.width(6.dp));Text(filter,color=N,fontSize=11.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Icon(Icons.Default.KeyboardArrowDown,null,tint=M)}
 }
}};item{GradButton("＋  Add Activity",add)}
  item{Text("Today",color=N,fontSize=21.sp,fontWeight=FontWeight.Bold);Text(list.size.toString()+" activities  •  "+d+" completed",color=M,fontSize=12.sp)}
  items(shown){x->CardA(x,date,done(x.id,date),toggle,edit)};if(shown.isEmpty())item{Empty(add)};item{Quick(list,date,insights)}
 }}
 if(pickDate) NativeDatePicker(c,date){selected->setDate(selected);pickDate=false}
}


@Composable fun Hero(d:Int,total:Int,pct:Int){
 Box(
  Modifier.fillMaxWidth()
   .clip(RoundedCornerShape(28.dp))
   .background(Brush.linearGradient(listOf(Color(0xFF4E2AD8),Color(0xFF159AF2))))
   .padding(18.dp)
 ){
  Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){
     Surface(shape=RoundedCornerShape(20.dp),color=C.copy(alpha=.16f)){
      Text("TODAY  •  "+display(today()),color=Color(0xFFEDE8FF),fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=11.dp,vertical=7.dp))
     }
     Spacer(Modifier.height(9.dp))
     Text(if(pct==100&&total>0)"Day completed. Nice work!" else if(d>0)"Keep going! You're doing great!" else "Ready for a productive day?",color=C,fontSize=17.sp,fontWeight=FontWeight.Bold)
     Spacer(Modifier.height(4.dp))
     Text("Your progress for today",color=Color(0xFFD9E8FF),fontSize=10.sp)
    }
    Box(Modifier.size(82.dp).clip(CircleShape).background(C.copy(alpha=.14f)),Alignment.Center){
     Box(Modifier.size(68.dp).clip(CircleShape).background(C.copy(alpha=.95f)),Alignment.Center){
      Text(pct.toString()+"%",color=P,fontSize=20.sp,fontWeight=FontWeight.Bold)
     }
    }
   }
   LinearProgressIndicator({pct/100f},Modifier.fillMaxWidth().height(8.dp),color=C,trackColor=C.copy(.22f))
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    Surface(Modifier.weight(1f),shape=RoundedCornerShape(16.dp),color=C.copy(alpha=.13f)){
     Row(Modifier.padding(horizontal=12.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
      Icon(Icons.Default.CheckCircle,null,tint=C,modifier=Modifier.size(18.dp));Spacer(Modifier.width(7.dp))
      Column{Text(d.toString()+" / "+total,color=C,fontSize=13.sp,fontWeight=FontWeight.Bold);Text("Completed",color=Color(0xFFD9E8FF),fontSize=8.sp)}
     }
    }
    Surface(Modifier.weight(1f),shape=RoundedCornerShape(16.dp),color=C.copy(alpha=.13f)){
     Row(Modifier.padding(horizontal=12.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
      Icon(Icons.Default.LocalFireDepartment,null,tint=Color(0xFFFFD166),modifier=Modifier.size(18.dp));Spacer(Modifier.width(7.dp))
      Column{Text(streak().toString()+" days",color=C,fontSize=13.sp,fontWeight=FontWeight.Bold);Text("Current streak",color=Color(0xFFD9E8FF),fontSize=8.sp)}
     }
    }
   }
  }
 }
}

@Composable fun Pill(t:String,m:Modifier,on:()->Unit){Surface(m.height(50.dp).clickable{on()},RoundedCornerShape(16.dp),color=C){Box(Modifier.padding(horizontal=14.dp),Alignment.CenterStart){Text(t,color=N,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}}}
@Composable fun GradButton(t:String,on:()->Unit){Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Brush.horizontalGradient(listOf(P,B))).clickable{on()},Alignment.Center){Text(t,color=C,fontWeight=FontWeight.Bold)}}

@Composable fun CardA(x:A,date:String,isDone:Boolean,toggle:(Long)->Unit,edit:(A)->Unit){
 val d=isDone
 Surface(shape=RoundedCornerShape(20.dp),color=C,shadowElevation=2.dp){
  Row(Modifier.fillMaxWidth().clickable{toggle(x.id)}.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
   Cat(x.cat);Spacer(Modifier.width(11.dp))
   Column(Modifier.weight(1f)){Text(x.name,color=N,fontSize=14.sp,fontWeight=FontWeight.Bold);Text(x.target.ifBlank{"No target"}+"  •  "+x.freq,color=M,fontSize=10.sp)}
   Surface(shape=RoundedCornerShape(20.dp),color=if(d)Color(0xFFDDF8E9) else Color(0xFFE8F0FF)){
    Row(Modifier.padding(horizontal=10.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
     Icon(if(d) Icons.Default.CheckCircle else Icons.Default.Schedule,null,tint=if(d)G else B,modifier=Modifier.size(20.dp));Spacer(Modifier.width(5.dp));Text(if(d)"Done" else "Pending",color=if(d)G else B,fontSize=10.sp,fontWeight=FontWeight.Bold)
    }
   }
   IconButton({edit(x)},Modifier.size(42.dp)){Icon(Icons.Default.MoreVert,null,tint=M,modifier=Modifier.size(25.dp))}
  }
 }
}
@Composable fun Cat(cat:String){val q=when(cat){"Fitness"->Triple(Icons.Default.DirectionsRun,Color(0xFFDDF9EC),G);"Health"->Triple(Icons.Default.LocalDrink,Color(0xFFE4F4FF),B);"Personal"->Triple(Icons.Default.Favorite,Color(0xFFFFE6EB),Color(0xFFE64F69));"Study"->Triple(Icons.Default.MenuBook,Color(0xFFE8E3FF),P);else->Triple(Icons.Default.Star,Color(0xFFFFEFD8),O)};Box(Modifier.size(48.dp).clip(CircleShape).background(q.second),Alignment.Center){Icon(q.first,null,tint=q.third,modifier=Modifier.size(25.dp))}}
@Composable fun Empty(add:()->Unit){Surface(shape=RoundedCornerShape(24.dp),color=C){Column(Modifier.fillMaxWidth().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFEAE3FF)),Alignment.Center){Text("+",color=P,fontSize=30.sp)};Text("Build your day",color=N,fontSize=19.sp,fontWeight=FontWeight.Bold);Text("Add your first activity and start tracking.",color=M,fontSize=12.sp);TextButton(add){Text("Add Activity",color=P)}}}}
@Composable fun Quick(l:List<A>,date:String,go:()->Unit){
 val d=l.count{done(it.id,date)};val pending=l.size-d;val success=if(l.isEmpty())0 else d*100/l.size
 Surface(shape=RoundedCornerShape(24.dp),color=C){
  Column(Modifier.padding(16.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Quick Insights",color=N,fontSize=18.sp,fontWeight=FontWeight.Bold);Text("Today's activity snapshot",color=M,fontSize=10.sp)};Button(go,Modifier.height(46.dp),shape=RoundedCornerShape(23.dp),colors=ButtonDefaults.buttonColors(containerColor=P),contentPadding=PaddingValues(horizontal=18.dp)){Text("View Report  →",color=C,fontSize=12.sp,fontWeight=FontWeight.Bold)}}
   Spacer(Modifier.height(12.dp))
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    InsightMini(Modifier.weight(1f),Icons.Default.EmojiEvents,l.size.toString(),"Total",O);InsightMini(Modifier.weight(1f),Icons.Default.CheckCircle,d.toString(),"Completed",G);InsightMini(Modifier.weight(1f),Icons.Default.Schedule,pending.toString(),"Pending",B);InsightMini(Modifier.weight(1f),Icons.Default.TrendingUp,success.toString()+"%","Success",P)
   }
  }
 }
}
@Composable fun InsightMini(modifier:Modifier,i:ImageVector,v:String,l:String,c:Color){
 Surface(modifier,shape=RoundedCornerShape(16.dp),color=Color(0xFFF7F6FF)){
  Column(Modifier.padding(vertical=10.dp,horizontal=7.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Box(Modifier.size(34.dp).clip(CircleShape).background(c.copy(alpha=.12f)),Alignment.Center){Icon(i,null,tint=c,modifier=Modifier.size(20.dp))}
   Spacer(Modifier.height(4.dp));Text(v,color=N,fontSize=17.sp,fontWeight=FontWeight.Bold);Text(l,color=M,fontSize=8.sp,fontWeight=FontWeight.SemiBold)
  }
 }
}
@Composable fun Nav(active:S,report:()->Unit,settings:()->Unit){Surface(color=C,shadowElevation=3.dp){Row(Modifier.fillMaxWidth().height(78.dp).padding(horizontal=70.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){NavItem(Icons.Default.Home,"Home",active==S.HOME){};NavItem(Icons.Default.Assessment,"Report",active==S.REPORT,report)}}}
@Composable fun NavItem(i:ImageVector,t:String,selected:Boolean,on:()->Unit){Column(Modifier.width(96.dp).clickable{on()},horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.width(76.dp).height(34.dp).clip(RoundedCornerShape(20.dp)).background(if(selected)Color(0xFFE8DEFF) else Color.Transparent),Alignment.Center){Icon(i,null,tint=if(selected)N else M,modifier=Modifier.size(25.dp))};Text(t,color=if(selected)N else M,fontSize=10.sp,fontWeight=FontWeight.Bold)}}

@Composable fun Top(t:String,back:()->Unit){Row(Modifier.fillMaxWidth().height(58.dp),verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.Default.ArrowBack,null,tint=N)};Text(t,color=N,fontSize=20.sp,fontWeight=FontWeight.Bold)}}
@Composable fun Add(c:Context,edit:A?,back:()->Unit,save:(A)->Unit){
 var name by remember{mutableStateOf(edit?.name?:"")};var cat by remember{mutableStateOf(edit?.cat?:"Fitness")};var target by remember{mutableStateOf(edit?.target?:"")};var freq by remember{mutableStateOf(edit?.freq?:"Daily")}
 val pref=c.getSharedPreferences("activity",Context.MODE_PRIVATE);var reminderHour by remember{mutableStateOf(pref.getInt("reminderHour",8))};var reminderMinute by remember{mutableStateOf(pref.getInt("reminderMinute",0))}
 val timeText=String.format(Locale.US,"%02d:%02d %s",((reminderHour+11)%12)+1,reminderMinute,if(reminderHour<12)"AM" else "PM")
 Column(Modifier.fillMaxSize().padding(18.dp)){Top(if(edit==null)"Add Activity" else "Edit Activity",back);LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Box(Modifier.fillMaxWidth(),Alignment.Center){Box(Modifier.size(86.dp).clip(CircleShape).background(Color(0xFFDDF7F5)),Alignment.Center){Icon(Icons.Default.DirectionsRun,null,tint=Color(0xFF124E68),modifier=Modifier.size(48.dp))}}}
  item{Field("Activity Name *",name,"e.g. Morning Exercise"){name=it}}
  item{Select("Category",cat,listOf("Fitness","Health","Personal","Study","Work","Finance","Other")){cat=it}}
  item{Select("Input Type","Checkbox",listOf("Checkbox","Duration","Number","Text")){}}
  item{Field("Target / Goal",target,"e.g. 30 min"){target=it}}
  item{Select("Frequency",freq,listOf("Daily","Weekly","Custom")){freq=it}}
  item{Field("Start Date",today(),"Nov 26, 2025"){}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(back,Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(26.dp)){Text("Cancel",color=P)};Button({if(name.isNotBlank()){save(A(edit?.id?:System.currentTimeMillis(),name,cat,target,freq))}},Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(26.dp),colors=ButtonDefaults.buttonColors(containerColor=P)){Text("Save")}}}
 }}
}
@Composable fun Field(l:String,v:String,h:String,on:(String)->Unit){Column{Text(l,color=N,fontSize=12.sp,fontWeight=FontWeight.Bold);OutlinedTextField(v,on,Modifier.fillMaxWidth(),placeholder={Text(h,color=M,fontSize=12.sp)},singleLine=true,shape=RoundedCornerShape(14.dp))}}
@Composable fun Select(l:String,v:String,opts:List<String>,on:(String)->Unit){var open by remember{mutableStateOf(false)};Column{Text(l,color=N,fontSize=12.sp,fontWeight=FontWeight.Bold);Box{OutlinedButton({open=true},Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)){Text(v,Modifier.weight(1f),color=N);Icon(Icons.Default.KeyboardArrowDown,null,tint=M)};DropdownMenu(open,{open=false}){opts.forEach{DropdownMenuItem({Text(it)},{on(it);open=false})}}}}}

@Composable fun SegmentedTabs(labels:List<String>,selected:Int,onSelected:(Int)->Unit){
 Surface(Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(18.dp),color=Color(0xFFEDEBFA)){
  Row(Modifier.fillMaxSize().padding(5.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
   labels.forEachIndexed{index,label->
    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(if(index==selected)C else Color.Transparent).clickable{onSelected(index)},Alignment.Center){
     Text(label,color=if(index==selected)N else M,fontSize=11.sp,fontWeight=if(index==selected)FontWeight.Bold else FontWeight.SemiBold)
    }
   }
  }
 }
}

@Composable fun OverviewPerformance(l:List<A>,date:String){
 val d=l.count{done(it.id,date)}
 val pct=if(l.isEmpty())0 else d*100/l.size
 Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=C){
  Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){
   Column(Modifier.weight(1f)){
    Text("Overall Performance",color=N,fontSize=14.sp,fontWeight=FontWeight.Bold)
    Text(pct.toString()+"%",color=N,fontSize=38.sp,fontWeight=FontWeight.Bold)
    Text(if(d==l.size&&l.isNotEmpty())"All activities completed for this date." else d.toString()+" of "+l.size+" activities completed",color=M,fontSize=10.sp)
   }
   Box(Modifier.size(92.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(P,B,Color(0xFF18D7BF),P))),Alignment.Center){
    Box(Modifier.size(68.dp).clip(CircleShape).background(C),Alignment.Center){
     Icon(Icons.Default.BarChart,null,tint=P,modifier=Modifier.size(34.dp))
    }
   }
  }
 }
}

@Composable fun OverviewMetrics(l:List<A>,date:String){
 val total=l.size
 val completed=l.count{done(it.id,date)}
 val pending=total-completed
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  MetricCard(Icons.Default.Assignment,"Total",total.toString(),O)
  MetricCard(Icons.Default.CheckCircle,"Completed",completed.toString(),G)
  MetricCard(Icons.Default.Schedule,"Pending",pending.toString(),B)
 }
}

@Composable fun MetricCard(icon:ImageVector,label:String,value:String,tint:Color){
 Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),color=C){
  Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){
   Box(Modifier.size(42.dp).clip(CircleShape).background(tint.copy(alpha=.12f)),Alignment.Center){
    Icon(icon,null,tint=tint,modifier=Modifier.size(22.dp))
   }
   Spacer(Modifier.width(12.dp))
   Text(label,color=M,fontSize=11.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
   Text(value,color=N,fontSize=23.sp,fontWeight=FontWeight.Bold)
  }
 }
}

@Composable fun OverviewDaily(l:List<A>,date:String){
 Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=C){
  Column(Modifier.padding(16.dp)){
   Text("Selected Date Performance",color=N,fontSize=16.sp,fontWeight=FontWeight.Bold)
   Text(display(date),color=M,fontSize=10.sp)
   Spacer(Modifier.height(10.dp))
   DailyChart(l,date,date)
  }
 }
}

@Composable fun Analytical(l:List<A>,date:String){
 Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=C){
  Column(Modifier.padding(18.dp)){
   Text("Analytical",color=N,fontSize=17.sp,fontWeight=FontWeight.Bold)
   Text("Activity distribution and selected-date completion",color=M,fontSize=10.sp)
   Spacer(Modifier.height(12.dp))
   val grouped=l.groupBy{it.cat}
   grouped.entries.forEach{entry->
    val categoryDone=entry.value.count{done(it.id,date)}
    val categoryPct=categoryDone*100/maxOf(1,entry.value.size)
    Row(Modifier.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){
     Text(entry.key,Modifier.width(82.dp),color=N,fontSize=10.sp,fontWeight=FontWeight.Bold)
     Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE9E7F5))){
      Box(Modifier.fillMaxWidth(categoryDone.toFloat()/maxOf(1,entry.value.size)).fillMaxHeight().background(P))
     }
     Spacer(Modifier.width(8.dp))
     Text(categoryPct.toString()+"%",color=M,fontSize=9.sp)
    }
   }
  }
 }
}

@Composable
fun Calendar(l:List<A>,date:String,back:()->Unit,choose:(String)->Unit){
 var shown by remember{mutableStateOf(java.util.Calendar.getInstance())}
 Column(Modifier.fillMaxSize().padding(18.dp)){
  Top("Calendar",back)
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
   IconButton({shown.add(java.util.Calendar.MONTH,-1)}){Icon(Icons.Default.ChevronLeft,null,tint=N)}
   Text(SimpleDateFormat("MMMM yyyy",Locale.US).format(shown.time),Modifier.weight(1f),color=N,fontSize=19.sp,fontWeight=FontWeight.Bold)
   IconButton({shown.add(java.util.Calendar.MONTH,1)}){Icon(Icons.Default.ChevronRight,null,tint=N)}
  }
  Surface(shape=RoundedCornerShape(22.dp),color=C){
   Column(Modifier.padding(10.dp)){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
     listOf("S","M","T","W","T","F","S").forEach{Text(it,Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=M,fontSize=10.sp,fontWeight=FontWeight.Bold)}
    }
    val first=(shown.clone() as java.util.Calendar).apply{set(java.util.Calendar.DAY_OF_MONTH,1)}
    val offset=first.get(java.util.Calendar.DAY_OF_WEEK)-1
    val max=shown.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    for(row in 0..5){
     Row(Modifier.fillMaxWidth()){
      for(col in 0..6){
       val n=row*7+col-offset
       if(n in 1..max){
        val cal=(shown.clone() as java.util.Calendar).apply{set(java.util.Calendar.DAY_OF_MONTH,n)}
        val ds=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(cal.time)
        val selected=ds==date
        Box(Modifier.weight(1f).padding(3.dp),contentAlignment=Alignment.Center){
         Surface(Modifier.size(34.dp).clickable{choose(ds)},shape=CircleShape,color=if(selected)P else Color.Transparent){
          Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(n.toString(),color=if(selected)C else N,fontSize=11.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal)}
         }
        }
       }else{Spacer(Modifier.weight(1f).height(40.dp))}
      }
     }
    }
   }
  }
  Spacer(Modifier.height(12.dp))
  Surface(shape=RoundedCornerShape(22.dp),color=C){
   Column(Modifier.padding(16.dp)){
    Text("Activities",color=N,fontSize=16.sp,fontWeight=FontWeight.Bold)
    l.forEach{item->
     Row(Modifier.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
      Cat(item.cat);Spacer(Modifier.width(8.dp));Text(item.name,color=N,fontSize=11.sp);Spacer(Modifier.weight(1f));Text(if(done(item.id,date))"✓ Done" else "◷ Pending",color=if(done(item.id,date))G else B,fontSize=9.sp,fontWeight=FontWeight.Bold)
     }
    }
   }
  }
 }
}

@Composable fun Report(l:List<A>,back:()->Unit,settings:()->Unit,savePdf:(String,String)->Unit,share:(String)->Unit){
 val c=androidx.compose.ui.platform.LocalContext.current
 var tab by remember{mutableStateOf(0)}
 var customStart by remember{mutableStateOf(today())}
 var customEnd by remember{mutableStateOf(today())}
 var pickingStart by remember{mutableStateOf(false)}
 var pickingEnd by remember{mutableStateOf(false)}
 var generatedAt by remember{mutableStateOf("")};var generatedReport by remember{mutableStateOf("")}
 val range=when(tab){0->Pair(today(),today());1->weekBounds();2->monthBounds();else->normalizeRange(customStart,customEnd)}
 val stats=rangeStats(l,range.first,range.second)
 val title=when(tab){0->"Today Report";1->"Weekly Report";2->"Monthly Report";else->"Custom Report"}
 Scaffold(bottomBar={Nav(S.REPORT,back,settings)},containerColor=Color(0xFFF6F6FF)){p->
  LazyColumn(Modifier.fillMaxSize().padding(p).padding(horizontal=18.dp),contentPadding=PaddingValues(top=14.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   item{Header(settings)}
   item{ReportSegmentedTabs(tab){tab=it}}
   item{if(tab==3){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){DateRangeButton("Start Date",customStart,Modifier.weight(1f)){pickingStart=true};DateRangeButton("End Date",customEnd,Modifier.weight(1f)){pickingEnd=true}}}}
   item{
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=C,shadowElevation=2.dp){
     Column(Modifier.padding(18.dp)){
      Row(verticalAlignment=Alignment.CenterVertically){
       Column(Modifier.weight(1f)){Text(title,color=N,fontSize=20.sp,fontWeight=FontWeight.Bold);Text(formatRange(range.first,range.second),color=M,fontSize=10.sp)}
       Surface(shape=RoundedCornerShape(14.dp),color=Color(0xFFE9E4FF)){Text(if(generatedAt.isBlank())"Ready" else "Generated "+generatedAt,color=P,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp))}
      }
      Spacer(Modifier.height(16.dp))
      Row(verticalAlignment=Alignment.CenterVertically){
       Box(Modifier.size(94.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(P,B,Color(0xFF18D7BF),P))),Alignment.Center){
        Box(Modifier.size(72.dp).clip(CircleShape).background(C),Alignment.Center){Text(stats.third.toString()+"%",color=N,fontWeight=FontWeight.Bold,fontSize=20.sp)}
       }
       Spacer(Modifier.width(16.dp))
       Column{Text("Overall Performance",color=M,fontSize=10.sp);Text(stats.third.toString()+"%",color=N,fontWeight=FontWeight.Bold,fontSize=18.sp);Text("✓ "+stats.first+" completed of "+stats.second,color=G,fontSize=10.sp);Text("🔥 "+streak()+" day streak",color=O,fontSize=10.sp)}
      }
      Spacer(Modifier.height(14.dp))
      Button({generatedAt=SimpleDateFormat("hh:mm a",Locale.US).format(Date());generatedReport=report(l,range.first,range.second)},Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(24.dp),colors=ButtonDefaults.buttonColors(containerColor=P)){Icon(Icons.Default.Assessment,null);Spacer(Modifier.width(6.dp));Text("Generate "+title,fontWeight=FontWeight.Bold)}
     }
    }
   }
   item{if(generatedReport.isNotBlank()){Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=Color(0xFFF7F6FF)){Column(Modifier.padding(16.dp)){Text("Generated Report",color=N,fontSize=16.sp,fontWeight=FontWeight.Bold);Text("Generated at "+generatedAt,color=M,fontSize=9.sp);Spacer(Modifier.height(8.dp));Text(generatedReport,color=N,fontSize=10.sp,lineHeight=16.sp)}}}}\n   item{DailyChart(l,range.first,range.second)}
   item{
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=C){
     Column(Modifier.padding(16.dp)){
      Text(when(tab){0->"Daily Performance";1->"Weekly Performance";2->"Monthly Performance";else->"Custom Range Performance"},color=N,fontSize=16.sp,fontWeight=FontWeight.Bold)
      Text(formatRange(range.first,range.second),color=M,fontSize=10.sp);Spacer(Modifier.height(10.dp));PerformanceRows(l,range.first,range.second)
     }
    }
   }
   item{
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
     Button({savePdf(range.first,range.second)},Modifier.weight(1f).height(50.dp),shape=RoundedCornerShape(25.dp),colors=ButtonDefaults.buttonColors(containerColor=P)){Icon(Icons.Default.PictureAsPdf,null);Spacer(Modifier.width(5.dp));Text("Save Report")}
     OutlinedButton({share(report(l,range.first,range.second))},Modifier.weight(1f).height(50.dp),shape=RoundedCornerShape(25.dp)){Icon(Icons.Default.Share,null);Spacer(Modifier.width(5.dp));Text("Share")}
    }
   }
  }
  if(pickingStart) NativeDatePicker(c,customStart){customStart=it;pickingStart=false}
  if(pickingEnd) NativeDatePicker(c,customEnd){customEnd=it;pickingEnd=false}
 }
}
@Composable fun PerformanceRows(l:List<A>,start:String,end:String){
 val dates=rangeDates(start,end);val totalPossible=l.size*dates.size;val completed=dates.sumOf{d->l.count{done(it.id,d)}};val pending=totalPossible-completed;val pct=if(totalPossible==0)0 else completed*100/totalPossible
 ReportMetricRow(Icons.Default.CheckCircle,"Completed",completed.toString(),G);ReportMetricRow(Icons.Default.Schedule,"Pending",pending.toString(),B);ReportMetricRow(Icons.Default.TrendingUp,"Completion Rate",pct.toString()+"%",P);ReportMetricRow(Icons.Default.CalendarMonth,"Days in Range",dates.size.toString(),O)
}
@Composable fun ReportMetricRow(icon:ImageVector,label:String,value:String,tint:Color){Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha=.12f)),Alignment.Center){Icon(icon,null,tint=tint,modifier=Modifier.size(19.dp))};Spacer(Modifier.width(10.dp));Text(label,color=M,fontSize=10.sp,modifier=Modifier.weight(1f));Text(value,color=N,fontSize=14.sp,fontWeight=FontWeight.Bold)}}

@Composable fun ReportSegmentedTabs(selected:Int,onSelected:(Int)->Unit){
 SegmentedTabs(listOf("Today","Weekly","Monthly","Custom"),selected,onSelected)
}

@Composable fun DateRangeButton(label:String,value:String,modifier:Modifier,onClick:()->Unit){
 Surface(modifier.height(64.dp).clickable{onClick()},shape=RoundedCornerShape(18.dp),color=C){
  Row(Modifier.fillMaxSize().padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){
   Icon(Icons.Default.DateRange,null,tint=P,modifier=Modifier.size(20.dp))
   Spacer(Modifier.width(8.dp))
   Column(Modifier.weight(1f)){
    Text(label,color=M,fontSize=9.sp)
    Text(display(value),color=N,fontSize=11.sp,fontWeight=FontWeight.Bold)
   }
   Icon(Icons.Default.KeyboardArrowDown,null,tint=M)
  }
 }
}

@Composable fun NativeDatePicker(c:Context,initial:String,onDone:(String)->Unit){
 LaunchedEffect(Unit){
  val cal=Calendar.getInstance()
  runCatching{cal.time=SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(initial)!!}
  android.app.DatePickerDialog(c,{_,y,m,d->onDone(String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d))},cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show()
 }
}

fun parseDay(s:String):Calendar=Calendar.getInstance().apply{
 time=SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(s)?:Date()
 set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)
}

fun normalizeRange(start:String,end:String):Pair<String,String>{
 return if(start<=end)Pair(start,end) else Pair(end,start)
}

fun weekBounds():Pair<String,String>{
 val c=Calendar.getInstance()
 c.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY)
 val start=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.time)
 c.add(Calendar.DAY_OF_MONTH,6)
 val end=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.time)
 return Pair(start,end)
}

fun monthBounds():Pair<String,String>{
 val c=Calendar.getInstance()
 c.set(Calendar.DAY_OF_MONTH,1)
 val start=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.time)
 c.set(Calendar.DAY_OF_MONTH,c.getActualMaximum(Calendar.DAY_OF_MONTH))
 val end=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.time)
 return Pair(start,end)
}

fun formatRange(start:String,end:String):String=display(start)+" – "+display(end)

fun rangeDates(start:String,end:String):List<String>{
 val out=mutableListOf<String>()
 val c=parseDay(start)
 val e=parseDay(end)
 while(!c.after(e)){
  out.add(SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.time))
  c.add(Calendar.DAY_OF_MONTH,1)
 }
 return out
}

fun rangeStats(l:List<A>,start:String,end:String):Triple<Int,Int,Int>{
 val dates=rangeDates(start,end)
 val completed=dates.sumOf{d->l.count{done(it.id,d)}}
 val possible=l.size*dates.size
 val pct=if(possible==0)0 else completed*100/possible
 return Triple(completed,possible,pct)
}

@Composable fun DailyChart(l:List<A>,start:String,end:String){
 val dates=rangeDates(start,end)
 val points=dates.map{d->if(l.isEmpty())0 else l.count{done(it.id,d)}*100/l.size}
 val visible=if(points.size<=14)points else points.mapIndexed{index,_->val from=(index*points.size/14);val to=minOf(points.size,(index+1)*points.size/14);points.subList(from,to).average().toInt()}
 val labels=if(dates.size<=14)dates else visible.indices.map{dates[minOf(dates.lastIndex,it*dates.size/visible.size)]}
 Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=C){
  Column(Modifier.padding(16.dp)){
   Text("Daily Performance",color=N,fontSize=16.sp,fontWeight=FontWeight.Bold)
   Text(formatRange(start,end),color=M,fontSize=10.sp)
   Spacer(Modifier.height(8.dp))
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){visible.forEach{Text(it.toString()+"%",color=M,fontSize=8.sp)}}
   Canvas(Modifier.fillMaxWidth().height(140.dp).padding(horizontal=4.dp)){
    val w=size.width
    val h=size.height
    if(visible.size==1){
     val y=h-(visible[0]/100f*h)
     drawCircle(B,7f,androidx.compose.ui.geometry.Offset(w/2f,y))
    }else if(visible.size>1){
     val pts=visible.mapIndexed{idx,v->androidx.compose.ui.geometry.Offset(idx*(w/(visible.size-1)),h-(v/100f*h))}
     for(i in 0 until pts.lastIndex)drawLine(P,pts[i],pts[i+1],strokeWidth=5f)
     pts.forEach{drawCircle(B,6f,it)}
    }
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){labels.forEach{Text(display(it),color=M,fontSize=7.sp,fontWeight=FontWeight.Bold)}}
  }
 }
}

@Composable fun Settings(back:()->Unit,export:()->Unit,restore:()->Unit,share:()->Unit,reset:()->Unit){Column(Modifier.fillMaxSize().padding(18.dp)){Top("Settings",back);LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){item{Setting(Icons.Default.Storage,"Data Management","Backup & Restore"){export()}};item{Setting(Icons.Default.Restore,"Restore Backup","Import Activity JSON"){restore()}};item{Setting(Icons.Default.Share,"Export Data","Share Your Data",share)};item{Setting(Icons.Default.Info,"About App","Version 1.0.0"){} };item{OutlinedButton(reset,Modifier.fillMaxWidth().height(50.dp),shape=RoundedCornerShape(25.dp)){Text("Reset Completion History",color=Color(0xFFB63F55))}}}}}

@Composable fun Setting(i:ImageVector,t:String,s:String,click:()->Unit){Surface(shape=RoundedCornerShape(18.dp),color=C){Row(Modifier.fillMaxWidth().clickable{click()}.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE9E4FF)),Alignment.Center){Icon(i,null,tint=P)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(t,color=N,fontWeight=FontWeight.Bold,fontSize=13.sp);Text(s,color=M,fontSize=10.sp)};Icon(Icons.Default.ChevronRight,null,tint=M)}}}

fun backupData(c:Context,l:List<A>):String{val o=JSONObject();val a=JSONArray();l.forEach{val x=JSONObject();x.put("id",it.id);x.put("name",it.name);x.put("category",it.cat);x.put("target",it.target);x.put("frequency",it.freq);a.put(x)};o.put("activities",a);o.put("done",JSONArray(Done.set.toList()));return o.toString(2)}
fun restoreData(c:Context,text:String){runCatching{val o=JSONObject(text);val a=o.optJSONArray("activities")?:JSONArray();c.getSharedPreferences("activity",Context.MODE_PRIVATE).edit().putString("items",a.toString()).apply();val d=o.optJSONArray("done");val set=mutableSetOf<String>();if(d!=null)for(i in 0 until d.length())set.add(d.getString(i));Done.set.clear();Done.set.addAll(set);c.getSharedPreferences("activity",Context.MODE_PRIVATE).edit().putStringSet("done",set).apply()}}
fun buildPdf(l:List<A>,start:String=today(),end:String=today()):ByteArray{
 val doc=PdfDocument();val pageW=595;val pageH=842;var pageNo=1
 var page=doc.startPage(PdfDocument.PageInfo.Builder(pageW,pageH,pageNo).create());var canvas=page.canvas
 val p=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
 fun text(v:String,x:Float,y:Float,size:Float,bold:Boolean=false,color:Int=android.graphics.Color.rgb(18,22,58)){p.color=color;p.textSize=size;p.typeface=if(bold)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT;canvas.drawText(v,x,y,p)}
 fun header(){p.color=android.graphics.Color.rgb(100,58,242);canvas.drawRect(0f,0f,pageW.toFloat(),96f,p);text("Activity",42f,42f,27f,true,android.graphics.Color.WHITE);text("Plan • Track • Achieve",42f,68f,12f,false,android.graphics.Color.rgb(235,232,255))}
 fun newPage(){doc.finishPage(page);pageNo++;page=doc.startPage(PdfDocument.PageInfo.Builder(pageW,pageH,pageNo).create());canvas=page.canvas}
 header();text("Performance Report",42f,140f,23f,true);text(formatRange(start,end),42f,162f,11f,false,android.graphics.Color.rgb(104,112,142))
 val dates=rangeDates(start,end);val completed=dates.sumOf{d->l.count{done(it.id,d)}};val possible=l.size*dates.size;val pending=possible-completed;val pct=if(possible==0)0 else completed*100/possible
 p.color=android.graphics.Color.rgb(247,246,252);canvas.drawRoundRect(42f,190f,553f,315f,18f,18f,p)
 text("OVERALL PERFORMANCE",60f,218f,10f,true,android.graphics.Color.rgb(104,112,142));text(pct.toString()+"%",60f,258f,34f,true)
 text(completed.toString()+" completed",60f,285f,10f,false,android.graphics.Color.rgb(25,184,117));text(pending.toString()+" pending",180f,285f,10f,false,android.graphics.Color.rgb(21,154,242));text(dates.size.toString()+" day range",290f,285f,10f,false,android.graphics.Color.rgb(240,139,55))
 p.color=android.graphics.Color.rgb(230,228,245);canvas.drawRoundRect(60f,298f,535f,306f,4f,4f,p);p.color=android.graphics.Color.rgb(100,58,242);canvas.drawRoundRect(60f,298f,60f+475f*(pct/100f),306f,4f,4f,p)
 text("Performance Breakdown",42f,355f,17f,true);var y=385f
 listOf("Completed" to completed.toString(),"Pending" to pending.toString(),"Completion Rate" to pct.toString()+"%","Current Streak" to streak().toString()+" days").forEach{(label,value)->p.color=android.graphics.Color.rgb(247,246,252);canvas.drawRoundRect(42f,y-18f,553f,y+18f,9f,9f,p);text(label,58f,y+4f,10f,false,android.graphics.Color.rgb(104,112,142));text(value,450f,y+4f,11f,true);y+=46f}
 y+=8f;text("Activity Details",42f,y,17f,true);y+=28f
 l.forEachIndexed{i,x->if(y>790f){newPage();header();y=125f};val doneDays=dates.count{d->done(x.id,d)};val status=if(doneDays>0)"COMPLETED" else "PENDING";val statusColor=if(doneDays>0)android.graphics.Color.rgb(25,184,117) else android.graphics.Color.rgb(21,154,242);p.color=android.graphics.Color.rgb(247,246,252);canvas.drawRoundRect(42f,y-19f,553f,y+23f,10f,10f,p);text((i+1).toString()+". "+x.name,57f,y,11f,true);text(x.cat+"  •  "+x.target+"  •  "+x.freq,57f,y+15f,8.5f,false,android.graphics.Color.rgb(104,112,142));text(status,456f,y,8.5f,true,statusColor);if(doneDays>0)text(doneDays.toString()+" day(s)",456f,y+14f,7.5f,false,statusColor);y+=52f}
 text("Activity • Plan • Track • Achieve",42f,820f,9f,false,android.graphics.Color.rgb(100,58,242));doc.finishPage(page);val out=ByteArrayOutputStream();doc.writeTo(out);doc.close();return out.toByteArray()
}
fun done(id:Long,d:String)=Done.set.contains(id.toString()+"|"+d)
fun toggle(c:Context,id:Long,d:String){val k=id.toString()+"|"+d;if(!Done.set.add(k))Done.set.remove(k);c.getSharedPreferences("activity",Context.MODE_PRIVATE).edit().putStringSet("done",Done.set).apply()}
fun streak():Int{var n=0;val c=Calendar.getInstance();while(n<366){val d=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.time);if(Done.set.any{it.endsWith("|"+d)}){n++;c.add(Calendar.DAY_OF_MONTH,-1)}else break};return n}
fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
fun display(s:String)=runCatching{SimpleDateFormat("EEE, MMM d",Locale.US).format(SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(s)!!)}.getOrDefault(s)
fun load(c:Context):List<A>{val raw=c.getSharedPreferences("activity",Context.MODE_PRIVATE).getString("items",null)?:return listOf(A(1,"Morning Exercise","Fitness","30 min","Daily"),A(2,"Drink 3 Litres Water","Health","3 L","Daily"),A(3,"10000 Steps Walking","Health","10,000 steps","Daily"),A(4,"Healthy Nutrition","Health","5 servings","Daily"),A(5,"Face Care & Sunscreen","Personal","Daily","Daily"));return runCatching{val a=JSONArray(raw);(0 until a.length()).map{val o=a.getJSONObject(it);A(o.getLong("id"),o.getString("name"),o.getString("category"),o.optString("target"),o.optString("frequency","Daily"))}}.getOrDefault(emptyList())}
fun save(c:Context,l:List<A>){val a=JSONArray();l.forEach{val o=JSONObject();o.put("id",it.id);o.put("name",it.name);o.put("category",it.cat);o.put("target",it.target);o.put("frequency",it.freq);a.put(o)};c.getSharedPreferences("activity",Context.MODE_PRIVATE).edit().putString("items",a.toString()).apply()}
fun report(l:List<A>,start:String=today(),end:String=today()):String{
 val dates=rangeDates(start,end)
 return "ACTIVITY REPORT\n"+formatRange(start,end)+"\n\n"+l.joinToString("\n"){a->
  val completed=dates.count{d->done(a.id,d)}
  a.name+" — "+if(completed>0)"Completed on "+completed+" day(s)" else "Pending"
 }
}
fun share(c:Context,t:String){c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,t)},"Activity"))}
