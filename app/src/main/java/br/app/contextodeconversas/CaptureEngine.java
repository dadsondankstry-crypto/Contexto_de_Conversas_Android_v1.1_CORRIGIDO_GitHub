package br.app.contextodeconversas;

import android.content.*;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;

/** Dados ficam apenas no SQLite local. Nenhuma rede, log ou servidor. */
public final class CaptureEngine {
    private CaptureEngine() {}
    public static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("opcoes",Context.MODE_PRIVATE); }
    public static long activeChat(Context c) { return prefs(c).getLong("active_chat",-1); }
    public static void setActive(Context c,long id) { prefs(c).edit().putLong("active_chat",id).apply(); }
    public static boolean auto(Context c) { return prefs(c).getBoolean("auto",false); }
    public static boolean bubble(Context c) { return prefs(c).getBoolean("bubble",true); }
    public static String clipboard(Context c) {
        try {
            android.content.ClipboardManager cm=(android.content.ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE);
            if(cm==null || !cm.hasPrimaryClip()) return null;
            ClipData clip=cm.getPrimaryClip();
            if(clip==null || clip.getItemCount()<1) return null;
            CharSequence text=clip.getItemAt(0).getText();
            return text==null ? null : text.toString();
        } catch(Exception e) { return null; }
    }
    public static String store(Context c,String value) {
        if(value==null || value.trim().isEmpty()) return "Área de transferência vazia ou inacessível.";
        if(value.length()>500000) return "Texto muito grande para captura direta (limite 500 mil caracteres).";
        long chat=activeChat(c);
        ConversationDb db=new ConversationDb(c);
        if(db.getChat(chat)==null) return "Selecione uma conversa primeiro.";
        String role=db.nextRole(chat);
        long saved=db.add(chat,role,value);
        return saved>0 ? "Salvo como "+role : "Não foi possível salvar.";
    }
    public static boolean isDefaultKeyboard(Context c) {
        String configured=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);
        return configured!=null && configured.contains(c.getPackageName()+"/");
    }
}
