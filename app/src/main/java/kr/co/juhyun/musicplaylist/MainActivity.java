package kr.co.juhyun.musicplaylist;

import android.app.*;
import android.content.*;
import android.graphics.Color;
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

    private TextView text(String s, int size){ TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(Color.rgb(23,53,42)); v.setPadding(16,16,16,16); return v; }
    private Button button(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }
    private LinearLayout root(){ LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); r.setPadding(20,20,20,20); r.setBackgroundColor(Color.rgb(245,248,246)); return r; }

    private void showHome(){
        stop(); trackIndex=-1;
        LinearLayout r=root(); title=text("음악 플레이리스트",28); title.setTypeface(null,1); r.addView(title);
        r.addView(text("10개 트랙에 내 음악을 등록하고 연속 재생할 수 있습니다.",15));
        ScrollView sv=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); sv.addView(list); r.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<10;i++){ final int p=i; Button x=button(tracks.get(i).name+"   ·   "+tracks.get(i).uris.size()+"곡"); x.setOnClickListener(v->showTrack(p)); list.addView(x); }
        setContentView(r);
    }

    private void showTrack(int p){
        trackIndex=p; songIndex=-1; stop(); Track t=tracks.get(p); LinearLayout r=root();
        Button back=button("← 트랙 목록"); back.setOnClickListener(v->showHome()); r.addView(back);
        LinearLayout head=new LinearLayout(this); title=text(t.name,25); title.setTypeface(null,1); head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button rename=button("이름 변경"); rename.setOnClickListener(v->rename()); head.addView(rename); r.addView(head);
        Button add=button("＋ 스마트폰에서 음악 추가"); add.setOnClickListener(v->pick()); r.addView(add);
        ScrollView sv=new ScrollView(this); detail=new LinearLayout(this); detail.setOrientation(LinearLayout.VERTICAL); sv.addView(detail); r.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        refreshSongs();
        now=text("♫ 재생할 음악을 선택하세요",15); r.addView(now);
        LinearLayout controls=new LinearLayout(this); String[] labels={"이전 곡","재생","다음 곡"};
        for(String s:labels){ Button b=button(s); if(s.equals("이전 곡")) b.setOnClickListener(v->previous()); else if(s.equals("다음 곡")) b.setOnClickListener(v->next()); else {play=b; b.setOnClickListener(v->toggle());} controls.addView(b,new LinearLayout.LayoutParams(0,-2,1)); }
        r.addView(controls); setContentView(r);
    }

    private void refreshSongs(){
        detail.removeAllViews(); Track t=tracks.get(trackIndex);
        if(t.uris.isEmpty()) detail.addView(text("등록된 음악이 없습니다.",16));
        for(int i=0;i<t.uris.size();i++){ final int p=i; LinearLayout row=new LinearLayout(this); Button song=button((i+1)+". "+name(Uri.parse(t.uris.get(i)))); song.setOnClickListener(v->start(p)); row.addView(song,new LinearLayout.LayoutParams(0,-2,1)); Button del=button("삭제"); del.setOnClickListener(v->{ if(songIndex==p) stop(); t.uris.remove(p); save(); refreshSongs(); }); row.addView(del); detail.addView(row); }
    }

    private void rename(){ final EditText e=new EditText(this); e.setText(tracks.get(trackIndex).name); new AlertDialog.Builder(this).setTitle("트랙 이름 변경").setView(e).setPositiveButton("저장",(d,w)->{String n=e.getText().toString().trim(); if(!n.isEmpty()){tracks.get(trackIndex).name=n; title.setText(n); save();}}).setNegativeButton("취소",null).show(); }
    private void pick(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("audio/*"); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,50); }
    @Override protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(req!=50||res!=RESULT_OK||data==null)return; Track t=tracks.get(trackIndex); if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++) addUri(t,data.getClipData().getItemAt(i).getUri());} else if(data.getData()!=null)addUri(t,data.getData()); save(); refreshSongs(); }
    private void addUri(Track t,Uri u){ try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){} if(!t.uris.contains(u.toString()))t.uris.add(u.toString()); }

    private String name(Uri u){ try(android.database.Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int x=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(x>=0)return c.getString(x);}}catch(Exception ignored){} return "음악 파일"; }
    private void start(int p){ stop(); if(trackIndex<0||tracks.get(trackIndex).uris.isEmpty())return; songIndex=p; try{player=MediaPlayer.create(this,Uri.parse(tracks.get(trackIndex).uris.get(p))); if(player==null)throw new Exception(); player.setOnCompletionListener(m->next()); player.start(); now.setText("재생 중: "+name(Uri.parse(tracks.get(trackIndex).uris.get(p)))); play.setText("일시정지");}catch(Exception e){Toast.makeText(this,"음악 파일을 열 수 없습니다.",Toast.LENGTH_SHORT).show();} }
    private void toggle(){ if(player==null){if(trackIndex>=0&&!tracks.get(trackIndex).uris.isEmpty())start(songIndex<0?0:songIndex);return;} if(player.isPlaying()){player.pause();play.setText("재생");}else{player.start();play.setText("일시정지");} }
    private void next(){ if(trackIndex>=0&&!tracks.get(trackIndex).uris.isEmpty())start((songIndex+1)%tracks.get(trackIndex).uris.size()); }
    private void previous(){ if(trackIndex>=0&&!tracks.get(trackIndex).uris.isEmpty())start((songIndex-1+tracks.get(trackIndex).uris.size())%tracks.get(trackIndex).uris.size()); }
    private void stop(){ if(player!=null){try{player.stop();}catch(Exception ignored){} player.release();player=null;} }
    @Override protected void onDestroy(){stop();super.onDestroy();}

    private void load(){ tracks.clear(); try{JSONArray a=new JSONArray(getPreferences(0).getString("tracks","[]")); for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Track t=new Track(o.getString("name"));JSONArray u=o.getJSONArray("uris");for(int j=0;j<u.length();j++)t.uris.add(u.getString(j));tracks.add(t);}}catch(Exception ignored){} while(tracks.size()<10)tracks.add(new Track("트랙 "+(tracks.size()+1))); }
    private void save(){ try{JSONArray a=new JSONArray();for(Track t:tracks){JSONObject o=new JSONObject();o.put("name",t.name);o.put("uris",new JSONArray(t.uris));a.put(o);}getPreferences(0).edit().putString("tracks",a.toString()).apply();}catch(Exception ignored){} }
}
