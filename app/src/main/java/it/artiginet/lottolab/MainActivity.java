package it.artiginet.lottolab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MainActivity extends Activity {
    private static final int REQ_CSV = 9001;
    private static final String PREFS = "lotto_lab_prefs";
    private static final String KEY_DATA = "estrazioni";
    private static final String KEY_HISTORY = "history";

    private final String[] wheels = {
        "Bari","Cagliari","Firenze","Genova","Milano","Napoli",
        "Palermo","Roma","Torino","Venezia","Nazionale"
    };

    private Spinner wheelSpinner;
    private EditText dateInput;
    private final EditText[] numberInputs = new EditText[5];
    private TextView statsText, comboText, historyText;
    private final List<Draw> draws = new ArrayList<>();
    private final List<String> history = new ArrayList<>();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadData();
        buildUi();
        refreshAll();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(18),dp(18),dp(30));
        scroll.addView(root);

        root.addView(text("Lotto Lab Mobile",28,true));
        TextView sub = text("Statistiche, ritardi e combinazioni sul tuo archivio estrazioni.",15,false);
        sub.setPadding(0,dp(4),0,dp(12));
        root.addView(sub);

        TextView warn = text("⚠ Le estrazioni sono casuali: le combinazioni sono solo elaborazioni statistiche.",13,false);
        warn.setPadding(dp(10),dp(10),dp(10),dp(10));
        root.addView(warn);

        root.addView(section("1. Ruota e nuova estrazione"));
        wheelSpinner = new Spinner(this);
        wheelSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, wheels));
        wheelSpinner.setSelection(Arrays.asList(wheels).indexOf("Milano"));
        root.addView(wheelSpinner);

        dateInput = new EditText(this);
        dateInput.setHint("Data (AAAA-MM-GG)");
        dateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(new Date()));
        root.addView(dateInput);

        LinearLayout nums = new LinearLayout(this);
        nums.setOrientation(LinearLayout.HORIZONTAL);
        for (int i=0;i<5;i++) {
            EditText e = new EditText(this);
            e.setHint("N"+(i+1));
            e.setGravity(Gravity.CENTER);
            e.setInputType(InputType.TYPE_CLASS_NUMBER);
            nums.addView(e,new LinearLayout.LayoutParams(0,dp(56),1f));
            numberInputs[i]=e;
        }
        root.addView(nums);

        LinearLayout actions = new LinearLayout(this);
        Button add = button("Aggiungi");
        add.setOnClickListener(v -> addManualDraw());
        actions.addView(add,new LinearLayout.LayoutParams(0,dp(52),1f));
        Button imp = button("Importa CSV");
        imp.setOnClickListener(v -> chooseCsv());
        actions.addView(imp,new LinearLayout.LayoutParams(0,dp(52),1f));
        root.addView(actions);

        root.addView(section("2. Analisi della ruota"));
        statsText = text("",14,false);
        root.addView(statsText);

        Button refresh = button("Aggiorna");
        refresh.setOnClickListener(v -> refreshAll());
        root.addView(refresh);

        root.addView(section("3. Generatore combinazioni"));
        comboText = text("Premi Genera per creare una cinquina.",22,true);
        comboText.setGravity(Gravity.CENTER);
        comboText.setPadding(0,dp(12),0,dp(12));
        root.addView(comboText);

        Button gen = button("Genera combinazione");
        gen.setOnClickListener(v -> generateCombination());
        root.addView(gen);

        root.addView(section("4. Storico combinazioni"));
        historyText = text("",14,false);
        root.addView(historyText);

        Button clear = button("Azzera storico combinazioni");
        clear.setOnClickListener(v -> { history.clear(); saveHistory(); refreshHistory(); });
        root.addView(clear);

        setContentView(scroll);
    }

    private TextView section(String s) {
        TextView t=text(s,19,true);
        t.setPadding(0,dp(20),0,dp(8));
        return t;
    }

    private TextView text(String s,int sp,boolean bold) {
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        if (bold) t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }

    private Button button(String s) {
        Button b=new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    private int dp(int v) { return Math.round(v*getResources().getDisplayMetrics().density); }

    private void addManualDraw() {
        String wheel=String.valueOf(wheelSpinner.getSelectedItem());
        String date=dateInput.getText().toString().trim();
        int[] ns=new int[5];
        boolean[] used=new boolean[91];

        for (int i=0;i<5;i++) {
            String raw=numberInputs[i].getText().toString().trim();
            if(raw.isEmpty()){ toast("Inserisci tutti e 5 i numeri."); return; }
            int n;
            try { n=Integer.parseInt(raw); } catch(Exception e){ toast("Numero non valido."); return; }
            if(n<1||n>90||used[n]){ toast("Numeri diversi da 1 a 90."); return; }
            used[n]=true; ns[i]=n;
        }

        Arrays.sort(ns);
        draws.add(new Draw(date,wheel,ns));
        sortDraws();
        saveData();
        for(EditText e:numberInputs)e.setText("");
        refreshAll();
        toast("Estrazione aggiunta.");
    }

    private void chooseCsv() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("text/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,REQ_CSV);
    }

    @Override
    protected void onActivityResult(int rc,int result,Intent data) {
        super.onActivityResult(rc,result,data);
        if(rc==REQ_CSV && result==RESULT_OK && data!=null && data.getData()!=null) importCsv(data.getData());
    }

    private void importCsv(Uri uri) {
        int imported=0, skipped=0;
        try(BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
            String line;
            while((line=br.readLine())!=null){
                line=line.trim();
                if(line.isEmpty())continue;
                String[] p=line.split("[;,\\t|]");
                if(p.length<7){skipped++;continue;}
                String wheel=normalizeWheel(p[1].trim());
                if(wheel==null){skipped++;continue;}
                int[] ns=new int[5];
                boolean[] used=new boolean[91];
                boolean ok=true;
                for(int i=0;i<5;i++){
                    try{
                        int n=Integer.parseInt(p[i+2].trim());
                        if(n<1||n>90||used[n]){ok=false;break;}
                        used[n]=true; ns[i]=n;
                    }catch(Exception e){ok=false;break;}
                }
                if(!ok){skipped++;continue;}
                Arrays.sort(ns);
                draws.add(new Draw(p[0].trim(),wheel,ns));
                imported++;
            }
            deduplicate();
            sortDraws();
            saveData();
            refreshAll();
            toast("Importate "+imported+" righe. Scartate "+skipped+".");
        } catch(Exception e) {
            new AlertDialog.Builder(this).setTitle("Errore importazione").setMessage(e.getMessage()).setPositiveButton("OK",null).show();
        }
    }

    private String normalizeWheel(String raw){
        for(String w:wheels) if(w.equalsIgnoreCase(raw)) return w;
        return null;
    }

    private void deduplicate(){
        Map<String,Draw> m=new HashMap<>();
        for(Draw d:draws)m.put(d.date+"|"+d.wheel+"|"+Arrays.toString(d.numbers),d);
        draws.clear();
        draws.addAll(m.values());
    }

    private void sortDraws(){ Collections.sort(draws,(a,b)->b.date.compareTo(a.date)); }

    private List<Draw> currentWheelDraws(){
        String w=String.valueOf(wheelSpinner.getSelectedItem());
        List<Draw> out=new ArrayList<>();
        for(Draw d:draws) if(w.equals(d.wheel)) out.add(d);
        return out;
    }

    private void refreshAll(){ refreshStats(); refreshHistory(); }

    private void refreshStats(){
        if(wheelSpinner==null||statsText==null)return;
        String wheel=String.valueOf(wheelSpinner.getSelectedItem());
        List<Draw> wd=currentWheelDraws();
        if(wd.isEmpty()){statsText.setText("Ruota: "+wheel+"\nNessuna estrazione caricata.");return;}

        int[] f=new int[91], d=new int[91];
        Arrays.fill(d,wd.size());
        for(int i=0;i<wd.size();i++)for(int n:wd.get(i).numbers){
            f[n]++;
            if(d[n]==wd.size())d[n]=i;
        }

        List<Integer> nums=new ArrayList<>();
        for(int n=1;n<=90;n++)nums.add(n);

        List<Integer> byF=new ArrayList<>(nums);
        byF.sort((a,b)->Integer.compare(f[b],f[a]));
        List<Integer> byD=new ArrayList<>(nums);
        byD.sort((a,b)->Integer.compare(d[b],d[a]));

        StringBuilder sb=new StringBuilder();
        sb.append("Ruota: ").append(wheel).append("\n");
        sb.append("Estrazioni: ").append(wd.size()).append("\n");
        sb.append("Ultima data: ").append(wd.get(0).date).append("\n\n");
        sb.append("Più frequenti: ");
        for(int i=0;i<10;i++){int n=byF.get(i);if(i>0)sb.append(" · ");sb.append(n).append(" (").append(f[n]).append(")");}
        sb.append("\n\nPiù ritardatari: ");
        for(int i=0;i<10;i++){int n=byD.get(i);if(i>0)sb.append(" · ");sb.append(n).append(" (").append(d[n]).append(")");}
        statsText.setText(sb.toString());
    }

    private void generateCombination(){
        List<Draw> wd=currentWheelDraws();
        if(wd.isEmpty()){toast("Carica prima delle estrazioni.");return;}

        int[] f=new int[91], d=new int[91];
        Arrays.fill(d,wd.size());
        for(int i=0;i<wd.size();i++)for(int n:wd.get(i).numbers){
            f[n]++;
            if(d[n]==wd.size())d[n]=i;
        }

        double maxF=1,maxD=1;
        for(int n=1;n<=90;n++){maxF=Math.max(maxF,f[n]);maxD=Math.max(maxD,d[n]);}

        class Score{int n;double s;Score(int n,double s){this.n=n;this.s=s;}}
        Random r=new Random();
        List<Score> scores=new ArrayList<>();
        for(int n=1;n<=90;n++){
            double score=.55*(f[n]/maxF)+.35*(d[n]/maxD)+r.nextDouble()*.12;
            scores.add(new Score(n,score));
        }
        scores.sort((a,b)->Double.compare(b.s,a.s));
        List<Integer> pool=new ArrayList<>();
        for(int i=0;i<25;i++)pool.add(scores.get(i).n);
        Collections.shuffle(pool,r);
        List<Integer> combo=new ArrayList<>(pool.subList(0,5));
        Collections.sort(combo);

        String line="";
        for(int i=0;i<combo.size();i++) line += (i==0?"":"  ")+String.format(Locale.ITALY,"%02d",combo.get(i));
        comboText.setText(line);

        String stamp=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.ITALY).format(new Date());
        history.add(0,stamp+" · "+wheelSpinner.getSelectedItem()+" · "+line);
        while(history.size()>50)history.remove(history.size()-1);
        saveHistory();
        refreshHistory();
    }

    private void refreshHistory(){
        if(historyText==null)return;
        if(history.isEmpty()){historyText.setText("Nessuna combinazione generata.");return;}
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<Math.min(20,history.size());i++){if(i>0)sb.append("\n");sb.append(history.get(i));}
        historyText.setText(sb.toString());
    }

    private void saveData(){
        StringBuilder sb=new StringBuilder();
        for(Draw d:draws){
            sb.append(d.date).append("|").append(d.wheel);
            for(int n:d.numbers)sb.append("|").append(n);
            sb.append("\n");
        }
        prefs.edit().putString(KEY_DATA,sb.toString()).apply();
    }

    private void loadData(){
        String raw=prefs.getString(KEY_DATA,"");
        if(!raw.isEmpty())for(String line:raw.split("\n")){
            String[] p=line.split("\\|");
            if(p.length!=7)continue;
            try{
                int[] ns=new int[5];
                for(int i=0;i<5;i++)ns[i]=Integer.parseInt(p[i+2]);
                draws.add(new Draw(p[0],p[1],ns));
            }catch(Exception ignored){}
        }
        String h=prefs.getString(KEY_HISTORY,"");
        if(!h.isEmpty())history.addAll(Arrays.asList(h.split("\n")));
        sortDraws();
    }

    private void saveHistory(){
        StringBuilder sb=new StringBuilder();
        for(String s:history){if(sb.length()>0)sb.append("\n");sb.append(s.replace("\n"," "));}
        prefs.edit().putString(KEY_HISTORY,sb.toString()).apply();
    }

    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    private static class Draw{
        final String date,wheel; final int[] numbers;
        Draw(String date,String wheel,int[] numbers){this.date=date;this.wheel=wheel;this.numbers=numbers;}
    }
}
