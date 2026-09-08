package kr.co.juhyun.musicplaylist;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
    private final ArrayList<Track> tracks = new ArrayList<>();
    private LinearLayout list, detail;
    private TextView title, now;
    private Button play;
    private MediaPlayer player;
    private int trackIndex = -1, songIndex = -1;

    static class Track { String name; ArrayList<String> uris = new ArrayList<>(); Track(String n){name=n;} }

    @Override public void onCreate(Bundle b){ super.onCreate(b); load(); showHome(); }

    private int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private TextView text(String s, int size){ TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(Color.rgb(23,53,42)); v.setPadding(dp(12),dp(10),dp(12),dp(10)); return v; }
    private GradientDrawable bg(int color,int radius){ GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g; }
    private Button button(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(15); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setBackground(bg(Color.rgb(46,125,91),16)); b.setPadding(dp(12),dp(10),dp(12),dp(10)); return b; }
    private LinearLayout root(){ LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); r.setPadding(dp(18),dp(18),dp(18),dp(16)); r.setBackgroundColor(Color.rgb(247,249,248)); return r; }

    private void showHome(){
        stop(); trackIndex=-1;
        LinearLayout r=root(); title=text("나의 음악",30); title.setTypeface(null,Typeface.BOLD); title.setGravity(Gravity.CENTER_HORIZONTAL); r.addView(title);
        TextView sub=text("듣고 싶은 트랙을 선택하세요",15); sub.setTextColor(Color.rgb(92,112,103));sub.setGravity(Gravity.CENTER_HORIZONTAL);r.addView(sub);
        ScrollView sv=new ScrollView(this); GridLayout grid=new GridLayout(this); grid.setColumnCount(2); grid.setPadding(0,dp(12),0,dp(18)); sv.addView(grid); r.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<10;i++){ final int p=i; Track t=tracks.get(i); LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER);card.setPadding(dp(12),dp(18),dp(12),dp(14));card.setBackground(bg(Color.WHITE,20));card.setElevation(dp(3));
            TextView icon=text("♫",34);icon.setTextColor(Color.rgb(46,125,91));icon.setGravity(Gravity.CENTER);card.addView(icon);
            TextView n=text(t.name,18);n.setTypeface(null,Typeface.BOLD);n.setGravity(Gravity.CENTER);n.setMaxLines(1);card.addView(n,new LinearLayout.LayoutParams(-1,-2));
            TextView count=text(t.uris.size()+"곡",14);count.setTextColor(Color.rgb(112,130,122));count.setGravity(Gravity.CENTER);card.addView(count);
            TextView hint=text("눌러서 열기",12);hint.setTextColor(Color.rgb(46,125,91));hint.setGravity(Gravity.CENTER);card.addView(hint);
            card.setOnClickListener(v->showTrack(p)); GridLayout.LayoutParams gp=new GridLayout.LayoutParams();gp.width=0;gp.height=-2;gp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);gp.setMargins(dp(6),dp(7),dp(6),dp(7));grid.addView(card,gp); }
        LinearLayout tip=new LinearLayout(this);tip.setGravity(Gravity.CENTER);tip.setPadding(dp(10),dp(8),dp(10),dp(8));tip.setBackground(bg(Color.rgb(232,242,237),16));TextView tv=text("각 트랙의 이름과 음악은 자유롭게 바꿀 수 있습니다",13);tv.setTextColor(Color.rgb(46,125,91));tip.addView(tv);r.addView(tip);
        setContentView(r);
    }

    private void showTrack(int p){
        trackIndex=p; songIndex=-1; stop(); Track t=tracks.get(p); LinearLayout r=root();
        Button back=button("←  나의 음악"); back.setOnClickListener(v->showHome()); r.addView(back,new LinearLayout.LayoutParams(-1,dp(52)));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL); title=text(t.name,26); title.setTypeface(null,Typeface.BOLD); head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button rename=button("✎ 이름 변경"); rename.setOnClickListener(v->rename()); head.addView(rename,new LinearLayout.LayoutParams(dp(112),dp(48))); r.addView(head);
        Button add=button("＋  스마트폰에서 음악 추가"); add.setTextSize(17);add.setOnClickListener(v->pick()); LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(58));ap.setMargins(0,dp(8),0,dp(12));r.addView(add,ap);
        ScrollView sv=new ScrollView(this); detail=new LinearLayout(this); detail.setOrientation(LinearLayout.VERTICAL); sv.addView(detail); r.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        refreshSongs();
        LinearLayout playerBox=new LinearLayout(this);playerBox.setOrientation(LinearLayout.VERTICAL);playerBox.setPadding(dp(12),dp(8),dp(12),dp(12));playerBox.setBackground(bg(Color.rgb(23,53,42),20));
        now=text("♫  재생할 음악을 선택하세요",15);now.setTextColor(Color.WHITE);now.setGravity(Gravity.CENTER);playerBox.addView(now);
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER); String[] labels={"◀ 이전","▶ 재생","다음 ▶"};
        for(String s:labels){ Button b=button(s); b.setBackground(bg(Color.rgb(46,125,91),14));if(s.startsWith("◀")) b.setOnClickListener(v->previous()); else if(s.startsWith("다음")) b.setOnClickListener(v->next()); else {play=b; b.setOnClickListener(v->toggle());} LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(50),1);cp.setMargins(dp(4),0,dp(4),0);controls.addView(b,cp); }
        playerBox.addView(controls);r.addView(playerBox); setContentView(r);
    }

    private void refreshSongs(){
        detail.removeAllViews(); Track t=tracks.get(trackIndex);
        if(t.uris.isEmpty()){TextView empty=text("아직 등록된 음악이 없습니다.\n위의 초록색 버튼을 눌러 음악을 추가하세요.",16);empty.setGravity(Gravity.CENTER);empty.setTextColor(Color.rgb(92,112,103));empty.setPadding(dp(18),dp(48),dp(18),dp(48));detail.addView(empty);}
        for(int i=0;i<t.uris.size();i++){ final int p=i; LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),dp(6),dp(6),dp(6));row.setBackground(bg(Color.WHITE,16));row.setElevation(dp(2));
            Button song=button("▶  "+(i+1)+". "+name(Uri.parse(t.uris.get(i))));song.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);song.setTextColor(Color.rgb(23,53,42));song.setBackgroundColor(Color.TRANSPARENT);song.setOnClickListener(v->start(p));row.addView(song,new LinearLayout.LayoutParams(0,dp(58),1));
            Button del=button("삭제");del.setTextColor(Color.rgb(170,60,60));del.setBackground(bg(Color.rgb(250,235,235),12));del.setOnClickListener(v->{ if(songIndex==p) stop();t.uris.remove(p);save();refreshSongs();});row.addView(del,new LinearLayout.LayoutParams(dp(68),dp(44)));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.setMargins(0,dp(5),0,dp(5));detail.addView(row,rp); }
    }

    private void rename(){ final EditText e=new EditText(this); e.setText(tracks.get(trackIndex).name); new AlertDialog.Builder(this).setTitle("트랙 이름 변경").setView(e).setPositiveButton("저장",(d,w)->{String n=e.getText().toString().trim(); if(!n.isEmpty()){tracks.get(trackIndex).name=n; title.setText(n); save();}}).setNegativeButton("취소",null).show(); }
    private void pick(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("audio/*"); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,50); }
    @Override protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(req!=50||res!=RESULT_OK||data==null)return; Track t=tracks.get(trackIndex); if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++) addUri(t,data.getClipData().getItemAt(i).getUri());} else if(data.getData()!=null)addUri(t,data.getData()); save(); refreshSongs(); }
    private void addUri(Track t,Uri u){ try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){} if(!t.uris.contains(u.toString()))t.uris.add(u.toString()); }

    private String name(Uri u){ try(android.database.Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int x=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(x>=0)return c.getString(x);}}catch(Exception ignored){} return "음악 파일"; }
    private void start(int p){ stop(); if(trackIndex<0||tracks.get(trackIndex).uris.isEmpty())return; songIndex=p; try{player=MediaPlayer.create(this,Uri.parse(tracks.get(trackIndex).uris.get(p))); if(player==null)throw new Exception(); player.setOnCompletionListener(m->next()); player.start(); now.setText("재생 중: "+name(Uri.parse(tracks.get(trackIndex).uris.get(p)))); play.setText("일시정지");}catch(Exception e){Toast.makeText(this,"음악 파일을 열 수 없습니다.",Toast.LENGTH_SHORT).show();} }
    private void toggle(){ if(player==null){if(trackIndex>=0&&!tracks.get(trackIndex).uris.isEmpty())start(songIndex<0?0:songIndex);return;} if(player.isPlaying()){player.pause();play.setText("▶ 재생");}else{player.start();play.setText("Ⅱ 일시정지");} }
    private void next(){ if(trackIndex>=0&&!tracks.get(trackIndex).uris.isEmpty())start((songIndex+1)%tracks.get(trackIndex).uris.size()); }
    private void previous(){ if(trackIndex>=0&&!tracks.get(trackIndex).uris.isEmpty())start((songIndex-1+tracks.get(trackIndex).uris.size())%tracks.get(trackIndex).uris.size()); }
    private void stop(){ if(player!=null){try{player.stop();}catch(Exception ignored){} player.release();player=null;} }
    @Override protected void onDestroy(){stop();super.onDestroy();}

    private void load(){ tracks.clear(); try{JSONArray a=new JSONArray(getPreferences(0).getString("tracks","[]")); for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Track t=new Track(o.getString("name"));JSONArray u=o.getJSONArray("uris");for(int j=0;j<u.length();j++)t.uris.add(u.getString(j));tracks.add(t);}}catch(Exception ignored){} while(tracks.size()<10)tracks.add(new Track("트랙 "+(tracks.size()+1))); }
    private void save(){ try{JSONArray a=new JSONArray();for(Track t:tracks){JSONObject o=new JSONObject();o.put("name",t.name);o.put("uris",new JSONArray(t.uris));a.put(o);}getPreferences(0).edit().putString("tracks",a.toString()).apply();}catch(Exception ignored){} }
}
