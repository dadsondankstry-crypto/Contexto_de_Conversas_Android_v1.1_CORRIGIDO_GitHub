package br.app.contextodeconversas;

import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.view.*;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

/** Teclado alternativo opcional. IME padrão tem direito legal à leitura do clipboard Android 10+. */
public class CaptureKeyboard extends InputMethodService {
    private android.content.ClipboardManager clipboard;
    private final android.content.ClipboardManager.OnPrimaryClipChangedListener listener=()->AutomaticCapture.attempt(this);
    private boolean upper=false;
    @Override public void onCreate(){
        super.onCreate();clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(clipboard!=null)clipboard.addPrimaryClipChangedListener(listener);
    }
    @Override public View onCreateInputView(){
        return keyboard();
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout keyboard(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(4),dp(6),dp(4),dp(8));root.setBackgroundColor(Color.rgb(24,23,35));
        TextView title=new TextView(this);title.setText("CONTEXTO • Teclado auxiliar    ↔  Trocar teclado");title.setTextColor(Color.rgb(192,178,245));title.setPadding(dp(8),dp(5),dp(8),dp(12));
        title.setTextSize(12);title.setOnClickListener(v->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker());root.addView(title);
        addRow(root,"1234567890");addRow(root,upper?"QWERTYUIOP":"qwertyuiop");addRow(root,upper?"ASDFGHJKL":"asdfghjkl");
        LinearLayout row=new LinearLayout(this);row.addView(key("⇧",()->{upper=!upper;setInputView(keyboard());},1.4f));
        for(char ch:(upper?"ZXCVBNM":"zxcvbnm").toCharArray())row.addView(key(String.valueOf(ch),()->insert(String.valueOf(ch)),1));
        row.addView(key("⌫",()->{InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.deleteSurroundingText(1,0);},1.4f));root.addView(row);
        LinearLayout bottom=new LinearLayout(this);bottom.addView(key(",",()->insert(","),1));bottom.addView(key("?",()->insert("?"),1));
        bottom.addView(key("espaço",()->insert(" "),4));bottom.addView(key(".",()->insert("."),1));bottom.addView(key("↵",()->insert("\n"),1.5f));root.addView(bottom);
        LinearLayout accent=new LinearLayout(this);for(char ch:"áéíóúãõç!".toCharArray())accent.addView(key(String.valueOf(ch),()->insert(String.valueOf(ch)),1));root.addView(accent);
        return root;
    }
    private void addRow(LinearLayout root,String keys){LinearLayout row=new LinearLayout(this);for(char c:keys.toCharArray())row.addView(key(String.valueOf(c),()->insert(String.valueOf(c)),1));root.addView(row);}
    private View key(String label,Runnable action,float weight){
        TextView v=new TextView(this);v.setText(label);v.setTextColor(Color.WHITE);v.setTextSize(17);v.setGravity(Gravity.CENTER);
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(62,55,82));bg.setCornerRadius(dp(6));v.setBackground(bg);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(42),weight);p.setMargins(dp(2),dp(3),dp(2),dp(3));v.setLayoutParams(p);v.setOnClickListener(w->action.run());return v;
    }
    private void insert(String text){InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(text,1);}
    @Override public void onDestroy(){if(clipboard!=null)clipboard.removePrimaryClipChangedListener(listener);super.onDestroy();}
}
