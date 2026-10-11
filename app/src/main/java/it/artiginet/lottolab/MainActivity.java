package it.artiginet.lottolab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.util.*;
import java.util.zip.*;

public class MainActivity extends Activity {
    static final int REQ_CSV=9001;
    static final String PREFS="lotto_lab_prefs", KEY_DATA="estrazioni", KEY_HISTORY="history", KEY_LAST_UPDATE="last_update";
    static final String ARCHIVE_URL="https://www.brightstarlottery.it/STORICO_ESTRAZIONI_LOTTO/storico01-oggi.zip";
    static final int BG=Color.rgb(245,247,250), CARD=Color.WHITE, TEXT=Color.rgb(28,31,38),
            MUTED=Color.rgb(100,108,120), PURPLE=Color.rgb(94,53,177),
            PURPLE_LIGHT=Color.rgb(238,233,250), GOLD=Color.rgb(246,183,38);

    final String[] wheels={"Tutte le ruote","Bari","Cagliari","Firenze","Genova","Milano","Napoli","Palermo","Roma","Torino","Venezia","Nazionale"};
    final List<Draw> draws=new ArrayList<>();
    final List<String> history=new ArrayList<>();

    LinearLayout page;
    Spinner wheelSpinner;
    TextView updateText,statsText,historyText,latestTitle;
    LinearLayout latestContainer,lottoContainer,system8Container,superContainer;
    EditText dateInput;
    EditText[] numberInputs=new EditText[5];
    SharedPreferences prefs;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        loadData();
        buildShell();
        showHome();
        autoUpdate();
    }

    void buildShell(){
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16),dp(18),dp(16),dp(32));
        scroll.addView(page);
        setContentView(scroll);
    }

    void showHome(){
        page.removeAllViews();
        TextView title=text("Lotto Lab",30,true); title.setTextColor(TEXT); page.addView(title);
        TextView ver=text("Mobile 1.4",13,true); ver.setTextColor(PURPLE); page.addView(ver);
        TextView sub=text("Scegli la sezione che vuoi usare.",15,false); sub.setTextColor(MUTED); sub.setPadding(0,0,0,dp(14)); page.addView(sub);

        LinearLayout hero=card();
        hero.addView(cardTitle("Cosa vuoi fare?"));
        hero.addView(navButton("🎯  LOTTO","Cinquina, ruota e Sistema 8",v->showLotto()));
        hero.addView(navButton("⭐  SUPERENALOTTO","Generatore sestina dedicato",v->showSuper()));
        hero.addView(navButton("📊  STATISTICHE","Frequenze e ritardi per ruota",v->showStats()));
        hero.addView(navButton("🗓️  ULTIME ESTRAZIONI","Visualizza le estrazioni più recenti",v->showLatest()));
        hero.addView(navButton("🕘  STORICO","Combinazioni generate",v->showHistory()));
        page.addView(hero);

        LinearLayout a=card(); a.addView(cardTitle("Archivio estrazioni"));
        updateText=text("",14,false); updateText.setTextColor(MUTED); a.addView(updateText);
        Button upd=primary("Aggiorna estrazioni"); upd.setOnClickListener(v->downloadArchive(true)); a.addView(upd);
        page.addView(a);
        refreshUpdateText();

        TextView warn=text("Le estrazioni sono casuali. Statistiche e sistemi non possono prevedere numeri vincenti.",13,false);
        warn.setTextColor(MUTED); warn.setPadding(dp(12),dp(10),dp(12),dp(10)); warn.setBackground(bg(PURPLE_LIGHT,16)); page.addView(warn);
    }

    void addHeader(String title){
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        Button back=secondary("← Indietro"); back.setOnClickListener(v->showHome());
        row.addView(back,new LinearLayout.LayoutParams(dp(120),dp(48)));
        TextView t=text(title,24,true); t.setTextColor(TEXT); t.setPadding(dp(12),0,0,0);
        row.addView(t,new LinearLayout.LayoutParams(0,-2,1));
        page.addView(row);
    }

    void showLotto(){
        page.removeAllViews(); addHeader("Lotto");
        TextView intro=text("Scegli una ruota e genera una cinquina o un Sistema 8.",14,false); intro.setTextColor(MUTED); intro.setPadding(0,dp(10),0,dp(6)); page.addView(intro);

        LinearLayout w=card(); w.addView(cardTitle("Ruota"));
        wheelSpinner=makeWheelSpinner(false,()->{});
        w.addView(wheelSpinner); page.addView(w);

        LinearLayout g=card(); g.addView(cardTitle("Genera cinquina"));
        TextView gh=text("La cinquina usa frequenza, ritardo e una componente casuale sui dati della ruota selezionata.",13,false); gh.setTextColor(MUTED); g.addView(gh);
        lottoContainer=new LinearLayout(this); lottoContainer.setOrientation(LinearLayout.VERTICAL); g.addView(lottoContainer);
        Button gb=primary("Genera cinquina"); gb.setOnClickListener(v->generateLotto()); g.addView(gb); page.addView(g);

        LinearLayout e=card(); e.addView(cardTitle("Sistema 8"));
        TextView eh=text("Genera 8 numeri base e li distribuisce in 8 cinquine. È un modo per organizzare la giocata, non una previsione.",13,false); eh.setTextColor(MUTED); e.addView(eh);
        system8Container=new LinearLayout(this); system8Container.setOrientation(LinearLayout.VERTICAL); e.addView(system8Container);
        Button eb=primary("Crea sistema da 8 numeri"); eb.setOnClickListener(v->generateSystem8()); e.addView(eb); page.addView(e);
    }

    void showSuper(){
        page.removeAllViews(); addHeader("SuperEnalotto");
        LinearLayout se=card(); se.addView(cardTitle("Generatore SuperEnalotto"));
        TextView seh=text("Genera 6 numeri su 90. Puoi scegliere casuale puro oppure una distribuzione equilibrata.",14,false); seh.setTextColor(MUTED); se.addView(seh);
        superContainer=new LinearLayout(this); superContainer.setOrientation(LinearLayout.VERTICAL); se.addView(superContainer);
        Button sr=secondary("Genera casuale"); sr.setOnClickListener(v->generateSuper(false)); se.addView(sr);
        Button sb=primary("Genera equilibrata"); sb.setOnClickListener(v->generateSuper(true)); se.addView(sb);
        page.addView(se);

        LinearLayout info=card(); info.addView(cardTitle("Modalità"));
        TextView txt=text("Casuale: 6 numeri scelti in modo casuale.\n\nEquilibrata: cerca una distribuzione più uniforme tra pari/dispari, bassi/alti e decine. Non aumenta la probabilità matematica di vincita.",13,false);
        txt.setTextColor(MUTED); info.addView(txt); page.addView(info);
    }

    void showStats(){
        page.removeAllViews(); addHeader("Statistiche");
        LinearLayout w=card(); w.addView(cardTitle("Seleziona ruota"));
        wheelSpinner=makeWheelSpinner(true,()->refreshStats());
        w.addView(wheelSpinner); page.addView(w);

        LinearLayout s=card(); s.addView(cardTitle("Analisi Lotto"));
        statsText=text("",14,false); statsText.setTextColor(TEXT); s.addView(statsText);
        Button rec=secondary("Ricalcola statistiche"); rec.setOnClickListener(v->refreshStats()); s.addView(rec); page.addView(s);
        refreshStats();
    }

    void showLatest(){
        page.removeAllViews(); addHeader("Ultime estrazioni");
        LinearLayout w=card(); w.addView(cardTitle("Seleziona ruota"));
        wheelSpinner=makeWheelSpinner(true,()->refreshLatest());
        w.addView(wheelSpinner); page.addView(w);

        LinearLayout l=card(); latestTitle=cardTitle("Ultime estrazioni"); l.addView(latestTitle);
        latestContainer=new LinearLayout(this); latestContainer.setOrientation(LinearLayout.VERTICAL); l.addView(latestContainer); page.addView(l);
        refreshLatest();
    }

    void showHistory(){
        page.removeAllViews(); addHeader("Storico");
        LinearLayout h=card(); h.addView(cardTitle("Combinazioni generate"));
        historyText=text("",13,false); historyText.setTextColor(MUTED); h.addView(historyText);
        Button hc=secondary("Azzera storico"); hc.setOnClickListener(v->{history.clear();saveHistory();refreshHistory();}); h.addView(hc); page.addView(h);
        refreshHistory();

        LinearLayout m=card(); m.addView(cardTitle("Inserimento manuale / CSV"));
        TextView hint=text("Questa sezione serve per aggiungere estrazioni manuali o importare un archivio CSV.",13,false); hint.setTextColor(MUTED); m.addView(hint);
        wheelSpinner=makeWheelSpinner(false,()->{});
        m.addView(wheelSpinner);
        dateInput=new EditText(this); dateInput.setHint("Data AAAA-MM-GG"); dateInput.setText(new SimpleDateFormat("yyyy-MM-dd",Locale.ITALY).format(new Date())); m.addView(dateInput);
        LinearLayout nr=new LinearLayout(this); nr.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=0;i<5;i++){EditText in=new EditText(this); in.setHint("N"+(i+1)); in.setGravity(Gravity.CENTER); in.setInputType(InputType.TYPE_CLASS_NUMBER); nr.addView(in,new LinearLayout.LayoutParams(0,dp(56),1)); numberInputs[i]=in;}
        m.addView(nr);
        Button add=secondary("Aggiungi estrazione"); add.setOnClickListener(v->addManualDraw()); m.addView(add);
        Button csv=secondary("Importa CSV"); csv.setOnClickListener(v->chooseCsv()); m.addView(csv);
        page.addView(m);
    }

    Spinner makeWheelSpinner(boolean allowAll, final Runnable changed){
        Spinner sp=new Spinner(this);
        List<String> list=new ArrayList<>();
        for(String w:wheels) if(allowAll || !"Tutte le ruote".equals(w)) list.add(w);
        sp.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,list));
        int idx=list.indexOf("Milano"); if(idx>=0) sp.setSelection(idx);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){ if(changed!=null)changed.run(); }
            public void onNothingSelected(AdapterView<?> p){}
        });
        return sp;
    }

    Button navButton(String title,String subtitle,View.OnClickListener click){
        Button b=button(title+"\n"+subtitle);
        b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        b.setTextColor(TEXT); b.setTextSize(16); b.setPadding(dp(16),0,dp(12),0);
        b.setBackground(bg(PURPLE_LIGHT,16)); b.setOnClickListener(click);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(76)); lp.setMargins(0,dp(7),0,0); b.setLayoutParams(lp);
        return b;
    }

    LinearLayout card(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(16),dp(16),dp(16),dp(16));x.setBackground(bg(CARD,18));x.setElevation(dp(2));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(7),0,dp(7));x.setLayoutParams(lp);return x;}
    TextView cardTitle(String s){TextView t=text(s,20,true);t.setTextColor(TEXT);t.setPadding(0,0,0,dp(10));return t;}
    GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    Button primary(String s){Button b=button(s);b.setTextColor(Color.WHITE);b.setBackground(bg(PURPLE,14));return sized(b);}
    Button secondary(String s){Button b=button(s);b.setTextColor(PURPLE);b.setBackground(bg(PURPLE_LIGHT,14));return sized(b);}
    Button sized(Button b){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(dp(3),dp(10),dp(3),0);b.setLayoutParams(lp);return b;}
    TextView text(String s,int sp,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    TextView ball(int n){TextView t=text(String.format(Locale.ITALY,"%02d",n),16,true);t.setTextColor(TEXT);t.setGravity(Gravity.CENTER);t.setBackground(bg(GOLD,100));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(46),dp(46));lp.setMargins(dp(3),dp(3),dp(3),dp(3));t.setLayoutParams(lp);return t;}
    void showNumbers(LinearLayout box,List<Integer> nums){box.removeAllViews();appendNumbers(box,nums);}
    void appendNumbers(LinearLayout box,List<Integer> nums){HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER);for(int n:nums)r.addView(ball(n));hs.addView(r);box.addView(hs);}

    void autoUpdate(){long last=prefs.getLong(KEY_LAST_UPDATE,0), now=System.currentTimeMillis();if(draws.isEmpty()||now-last>6L*60*60*1000)downloadArchive(false);}

    void downloadArchive(boolean show){
        if(updateText!=null)updateText.setText("Aggiornamento in corso...");
        new Thread(()->{
            HttpURLConnection c=null;
            try{
                c=(HttpURLConnection)new URL(ARCHIVE_URL).openConnection();
                c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","LottoLabMobile/1.4");c.connect();
                if(c.getResponseCode()!=200)throw new Exception("Errore HTTP "+c.getResponseCode());
                ZipInputStream zip=new ZipInputStream(c.getInputStream());ZipEntry e;List<Draw> online=new ArrayList<>();int count=0;
                while((e=zip.getNextEntry())!=null){if(e.isDirectory())continue;BufferedReader br=new BufferedReader(new InputStreamReader(zip,StandardCharsets.UTF_8));String line;while((line=br.readLine())!=null){Draw d=parseOnlineLine(line);if(d!=null){online.add(d);count++;}}}
                zip.close();if(online.isEmpty())throw new Exception("Nessuna estrazione riconosciuta.");
                synchronized(draws){draws.clear();draws.addAll(online);deduplicate();sortDraws();saveData();}
                prefs.edit().putLong(KEY_LAST_UPDATE,System.currentTimeMillis()).apply();
                int total=count;runOnUiThread(()->{refreshVisible();if(show)toast("Archivio aggiornato: "+total+" righe.");});
            }catch(Exception ex){runOnUiThread(()->{refreshUpdateText();if(show||draws.isEmpty())new AlertDialog.Builder(this).setTitle("Aggiornamento non riuscito").setMessage(ex.getMessage()+"\n\nPuoi riprovare o usare il CSV.").setPositiveButton("OK",null).show();});}
            finally{if(c!=null)c.disconnect();}
        }).start();
    }

    void refreshVisible(){refreshUpdateText(); if(latestContainer!=null&&latestContainer.getParent()!=null)refreshLatest(); if(statsText!=null&&statsText.getParent()!=null)refreshStats(); if(historyText!=null&&historyText.getParent()!=null)refreshHistory();}

    Draw parseOnlineLine(String raw){
        if(raw==null)return null;raw=raw.trim();if(raw.isEmpty())return null;String[] p=raw.split("\\s+");if(p.length<7)return null;
        String wheel=wheelFromCode(p[1]);if(wheel==null)return null;int[] ns=new int[5];boolean[] used=new boolean[91];
        try{for(int i=0;i<5;i++){int n=Integer.parseInt(p[i+2]);if(n<1||n>90||used[n])return null;used[n]=true;ns[i]=n;}}catch(Exception e){return null;}
        return new Draw(normalizeDate(p[0]),wheel,ns);
    }

    String wheelFromCode(String c){
        if(c==null)return null;switch(c.trim().toUpperCase(Locale.ITALY)){
            case "BA":return "Bari";case "CA":return "Cagliari";case "FI":return "Firenze";case "GE":return "Genova";
            case "MI":return "Milano";case "NA":return "Napoli";case "PA":return "Palermo";case "RM":case "RO":return "Roma";
            case "TO":return "Torino";case "VE":return "Venezia";case "RN":case "NZ":return "Nazionale";default:return null;}
    }

    String normalizeDate(String raw){
        if(raw==null)return "";String c=raw.trim().replace("/","-");String[] fs={"yyyy-MM-dd","dd-MM-yyyy","yyyy-M-d","d-M-yyyy"};
        for(String f:fs)try{SimpleDateFormat in=new SimpleDateFormat(f,Locale.ITALY);in.setLenient(false);Date d=in.parse(c);if(d!=null)return new SimpleDateFormat("yyyy-MM-dd",Locale.ITALY).format(d);}catch(Exception ignored){}
        return c;
    }
    long dateValue(String s){try{Date d=new SimpleDateFormat("yyyy-MM-dd",Locale.ITALY).parse(normalizeDate(s));return d==null?0:d.getTime();}catch(Exception e){return 0;}}
    String displayDate(String s){try{Date d=new SimpleDateFormat("yyyy-MM-dd",Locale.ITALY).parse(normalizeDate(s));if(d!=null)return new SimpleDateFormat("dd/MM/yyyy",Locale.ITALY).format(d);}catch(Exception ignored){}return s;}

    void refreshUpdateText(){if(updateText==null)return;long last=prefs.getLong(KEY_LAST_UPDATE,0);if(last==0){updateText.setText("Archivio locale: "+draws.size()+" righe.\nMai aggiornato automaticamente.");return;}String w=new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.ITALY).format(new Date(last));updateText.setText("Archivio: "+draws.size()+" righe\nUltimo aggiornamento: "+w);}

    List<Draw> currentWheelDraws(){
        if(wheelSpinner==null)return new ArrayList<>();
        String w=String.valueOf(wheelSpinner.getSelectedItem());List<Draw> out=new ArrayList<>();for(Draw d:draws)if("Tutte le ruote".equals(w)||w.equals(d.wheel))out.add(d);return out;
    }

    void refreshLatest(){
        if(latestContainer==null||wheelSpinner==null)return;latestContainer.removeAllViews();List<Draw> cur=currentWheelDraws();if(cur.isEmpty()){latestContainer.addView(text("Nessuna estrazione disponibile.",14,false));return;}
        String sel=String.valueOf(wheelSpinner.getSelectedItem());
        if("Tutte le ruote".equals(sel)){String date=cur.get(0).date;latestTitle.setText("Ultima estrazione completa · "+displayDate(date));for(String w:wheels){if("Tutte le ruote".equals(w))continue;for(Draw d:cur)if(date.equals(d.date)&&w.equals(d.wheel)){latestContainer.addView(drawView(d,true));break;}}}
        else{latestTitle.setText("Ultime estrazioni · "+sel);for(int i=0;i<Math.min(10,cur.size());i++)latestContainer.addView(drawView(cur.get(i),false));}
    }

    View drawView(Draw d,boolean showWheel){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(6),dp(8),dp(6),dp(8));
        TextView m=text((showWheel?d.wheel+" · ":"")+displayDate(d.date),13,true);m.setTextColor(MUTED);box.addView(m);
        HorizontalScrollView hs=new HorizontalScrollView(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);for(int n:d.numbers)r.addView(ball(n));hs.addView(r);box.addView(hs);return box;
    }

    void refreshStats(){
        if(statsText==null||wheelSpinner==null)return;String wheel=String.valueOf(wheelSpinner.getSelectedItem());List<Draw> list=currentWheelDraws();
        if(list.isEmpty()){statsText.setText("Ruota: "+wheel+"\nNessuna estrazione disponibile.");return;}
        int[] f=new int[91],d=new int[91];Arrays.fill(d,list.size());
        for(int i=0;i<list.size();i++)for(int n:list.get(i).numbers){f[n]++;if(d[n]==list.size())d[n]=i;}
        List<Integer> nums=new ArrayList<>();for(int n=1;n<=90;n++)nums.add(n);
        List<Integer> byF=new ArrayList<>(nums),byD=new ArrayList<>(nums);byF.sort((a,b)->Integer.compare(f[b],f[a]));byD.sort((a,b)->Integer.compare(d[b],d[a]));
        StringBuilder s=new StringBuilder("Ruota: "+wheel+"\nEstrazioni analizzate: "+list.size()+"\nUltima data: "+displayDate(list.get(0).date)+"\n\nPiù frequenti\n");
        for(int i=0;i<10;i++){if(i>0)s.append(" · ");int n=byF.get(i);s.append(n).append("(").append(f[n]).append(")");}
        s.append("\n\nPiù ritardatari\n");for(int i=0;i<10;i++){if(i>0)s.append(" · ");int n=byD.get(i);s.append(n).append("(").append(d[n]).append(")");}
        statsText.setText(s.toString());
    }

    List<Integer> rankedNumbers(int count){
        List<Draw> list=currentWheelDraws();if(list.isEmpty())return randomUnique(count);
        int[] f=new int[91],d=new int[91];Arrays.fill(d,list.size());for(int i=0;i<list.size();i++)for(int n:list.get(i).numbers){f[n]++;if(d[n]==list.size())d[n]=i;}
        double mf=1,md=1;for(int n=1;n<=90;n++){mf=Math.max(mf,f[n]);md=Math.max(md,d[n]);}
        Random r=new Random();
        final class Scored{int n;double s;Scored(int n,double s){this.n=n;this.s=s;}}
        List<Scored> scored=new ArrayList<>();
        for(int n=1;n<=90;n++)scored.add(new Scored(n,.5*(f[n]/mf)+.3*(d[n]/md)+r.nextDouble()*.2));
        scored.sort((a,b)->Double.compare(b.s,a.s));
        List<Integer> pool=new ArrayList<>();for(int i=0;i<Math.min(30,scored.size());i++)pool.add(scored.get(i).n);
        Collections.shuffle(pool,r);List<Integer> out=new ArrayList<>(pool.subList(0,count));Collections.sort(out);return out;
    }

    void generateLotto(){
        if(wheelSpinner==null)return;
        List<Integer> x=rankedNumbers(5);
        lottoContainer.removeAllViews();
        TextView lbl=text("Cinquina per: "+wheelSpinner.getSelectedItem(),14,true); lbl.setTextColor(PURPLE); lbl.setPadding(0,dp(8),0,dp(4)); lottoContainer.addView(lbl);
        appendNumbers(lottoContainer,x);
        addHistory("Lotto · "+wheelSpinner.getSelectedItem(),x);
    }

    void generateSystem8(){
        List<Integer> base=rankedNumbers(8);int[][] p={{0,1,2,3,4},{0,1,3,5,6},{0,2,4,5,7},{0,3,4,6,7},{1,2,3,6,7},{1,2,4,5,6},{1,3,4,5,7},{2,3,5,6,7}};
        system8Container.removeAllViews();TextView t=text("8 numeri base · "+wheelSpinner.getSelectedItem(),13,true);t.setTextColor(MUTED);system8Container.addView(t);appendNumbers(system8Container,base);
        for(int i=0;i<p.length;i++){List<Integer> line=new ArrayList<>();for(int idx:p[i])line.add(base.get(idx));Collections.sort(line);TextView tv=text("Cinquina "+(i+1)+" · "+join(line),14,false);tv.setPadding(0,dp(4),0,dp(4));system8Container.addView(tv);}
        addHistory("Sistema 8 · "+wheelSpinner.getSelectedItem(),base);
    }

    void generateSuper(boolean balanced){List<Integer> x=balanced?balancedSuper():randomUnique(6);showNumbers(superContainer,x);addHistory(balanced?"SuperEnalotto equilibrata":"SuperEnalotto casuale",x);}
    List<Integer> randomUnique(int count){List<Integer> a=new ArrayList<>();for(int n=1;n<=90;n++)a.add(n);Collections.shuffle(a);List<Integer> o=new ArrayList<>(a.subList(0,count));Collections.sort(o);return o;}
    List<Integer> balancedSuper(){for(int k=0;k<500;k++){List<Integer>x=randomUnique(6);int even=0,low=0;Set<Integer>dec=new HashSet<>();for(int n:x){if(n%2==0)even++;if(n<=45)low++;dec.add((n-1)/10);}if(even>=2&&even<=4&&low>=2&&low<=4&&dec.size()>=4)return x;}return randomUnique(6);}
    String join(List<Integer>x){StringBuilder s=new StringBuilder();for(int i=0;i<x.size();i++){if(i>0)s.append("  ");s.append(String.format(Locale.ITALY,"%02d",x.get(i)));}return s.toString();}
    void addHistory(String label,List<Integer>x){String stamp=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.ITALY).format(new Date());history.add(0,stamp+" · "+label+" · "+join(x));while(history.size()>50)history.remove(history.size()-1);saveHistory();if(historyText!=null&&historyText.getParent()!=null)refreshHistory();}
    void refreshHistory(){if(historyText==null)return;if(history.isEmpty()){historyText.setText("Nessuna combinazione generata.");return;}StringBuilder s=new StringBuilder();for(int i=0;i<Math.min(20,history.size());i++){if(i>0)s.append("\n");s.append(history.get(i));}historyText.setText(s.toString());}

    void addManualDraw(){
        if(wheelSpinner==null)return;
        String wheel=String.valueOf(wheelSpinner.getSelectedItem());if("Tutte le ruote".equals(wheel)){toast("Seleziona una ruota specifica.");return;}
        int[] ns=new int[5];boolean[] used=new boolean[91];
        for(int i=0;i<5;i++){String v=numberInputs[i].getText().toString().trim();if(v.isEmpty()){toast("Inserisci tutti e 5 i numeri.");return;}try{int n=Integer.parseInt(v);if(n<1||n>90||used[n]){toast("Usa numeri diversi da 1 a 90.");return;}used[n]=true;ns[i]=n;}catch(Exception e){toast("Numero non valido.");return;}}
        draws.add(new Draw(normalizeDate(dateInput.getText().toString()),wheel,ns));deduplicate();sortDraws();saveData();for(EditText e:numberInputs)e.setText("");toast("Estrazione aggiunta.");
    }

    void chooseCsv(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("text/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQ_CSV);}
    @Override protected void onActivityResult(int r,int res,Intent data){super.onActivityResult(r,res,data);if(r==REQ_CSV&&res==RESULT_OK&&data!=null&&data.getData()!=null)importCsv(data.getData());}

    void importCsv(Uri uri){
        int imported=0,skipped=0;
        try(BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),StandardCharsets.UTF_8))){
            String line;while((line=br.readLine())!=null){line=line.trim();if(line.isEmpty())continue;String[] p=line.split("[;,\\t|]");if(p.length<7){skipped++;continue;}String w=normalizeWheel(p[1].trim());if(w==null){skipped++;continue;}int[]ns=new int[5];boolean[]used=new boolean[91];boolean ok=true;for(int i=0;i<5;i++)try{int n=Integer.parseInt(p[i+2].trim());if(n<1||n>90||used[n]){ok=false;break;}used[n]=true;ns[i]=n;}catch(Exception e){ok=false;break;}if(!ok){skipped++;continue;}draws.add(new Draw(normalizeDate(p[0]),w,ns));imported++;}
            deduplicate();sortDraws();saveData();toast("Importate "+imported+" righe. Scartate "+skipped+".");
        }catch(Exception e){new AlertDialog.Builder(this).setTitle("Errore importazione").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}
    }

    String normalizeWheel(String x){for(String w:wheels)if(!"Tutte le ruote".equals(w)&&w.equalsIgnoreCase(x))return w;return wheelFromCode(x);}
    void deduplicate(){Map<String,Draw>m=new HashMap<>();for(Draw d:draws)m.put(d.date+"|"+d.wheel+"|"+Arrays.toString(d.numbers),d);draws.clear();draws.addAll(m.values());}
    void sortDraws(){draws.sort((a,b)->Long.compare(dateValue(b.date),dateValue(a.date)));}

    void saveData(){StringBuilder s=new StringBuilder();for(Draw d:draws){s.append(normalizeDate(d.date)).append("|").append(d.wheel);for(int n:d.numbers)s.append("|").append(n);s.append("\n");}prefs.edit().putString(KEY_DATA,s.toString()).apply();}
    void loadData(){String raw=prefs.getString(KEY_DATA,"");if(!raw.isEmpty())for(String line:raw.split("\n")){String[]p=line.split("\\|");if(p.length!=7)continue;try{int[]ns=new int[5];for(int i=0;i<5;i++)ns[i]=Integer.parseInt(p[i+2]);draws.add(new Draw(normalizeDate(p[0]),p[1],ns));}catch(Exception ignored){}}String h=prefs.getString(KEY_HISTORY,"");if(!h.isEmpty())history.addAll(Arrays.asList(h.split("\n")));sortDraws();}
    void saveHistory(){StringBuilder s=new StringBuilder();for(String x:history){if(s.length()>0)s.append("\n");s.append(x.replace("\n"," "));}prefs.edit().putString(KEY_HISTORY,s.toString()).apply();}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    static class Draw{
        final String date,wheel;final int[]numbers;
        Draw(String d,String w,int[]n){date=d;wheel=w;numbers=n;}
    }
}
