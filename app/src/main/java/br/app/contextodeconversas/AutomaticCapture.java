package br.app.contextodeconversas;

import android.content.Context;
import android.content.ClipboardManager;
import android.content.ClipDescription;
import android.os.Build;
import android.os.SystemClock;

public final class AutomaticCapture {
    private static String lastText="";
    private static long lastTime=0;
    private AutomaticCapture(){}
    /** Só salva quando o usuário ativou a captura E escolheu nosso teclado como IME. */
    public static synchronized void attempt(Context context){
        if(!CaptureEngine.auto(context) || !CaptureEngine.isDefaultKeyboard(context))return;
        boolean any=CaptureEngine.prefs(context).getBoolean("all_apps",false);
        String pkg=CaptureEngine.prefs(context).getString("foreground","");
        if(!any && !"com.openai.chatgpt".equals(pkg))return;
        // Nunca armazena automaticamente uma copia marcada como sensivel pelo app de origem.
        ClipboardManager cm=(ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        if(cm!=null && Build.VERSION.SDK_INT>=33){
            ClipDescription cd=cm.getPrimaryClipDescription();
            if(cd!=null && cd.getExtras()!=null && cd.getExtras().getBoolean(ClipDescription.EXTRA_IS_SENSITIVE,false))return;
        }
        String text=CaptureEngine.clipboard(context);
        if(text==null || text.trim().isEmpty() || text.length()>500000)return;
        long now=SystemClock.elapsedRealtime();
        // Evita que IME e acessibilidade salvem o mesmo evento em duplicidade.
        if(text.equals(lastText) && now-lastTime<1500)return;
        String response=CaptureEngine.store(context,text);
        if(response.startsWith("Salvo")){lastTime=now;lastText=text;}
    }
}
