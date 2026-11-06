package br.app.contextodeconversas;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

public class ClipboardAccessibilityService extends AccessibilityService {
    private WindowManager manager;
    private View bubbleView;
    private android.content.ClipboardManager clipboard;
    private final android.content.ClipboardManager.OnPrimaryClipChangedListener listener=()->AutomaticCapture.attempt(this);
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener prefsListener=(p,key)->{
        if("bubble".equals(key)) refreshBubble();
    };
    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        manager=(WindowManager)getSystemService(WINDOW_SERVICE);
        clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(clipboard!=null)clipboard.addPrimaryClipChangedListener(listener);
        CaptureEngine.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener);
        refreshBubble();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        if(e==null || e.getPackageName()==null)return;
        int type=e.getEventType();
        if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || type==AccessibilityEvent.TYPE_WINDOWS_CHANGED){
            String pkg=e.getPackageName().toString();
            if(!pkg.contains("inputmethod") && !pkg.equals("com.google.android.inputmethod.latin") && !pkg.equals("com.android.systemui")){
                CaptureEngine.prefs(this).edit().putString("foreground",pkg).apply();
            }
        }
    }
    private void refreshBubble(){
        if(manager==null)return;
        if(!CaptureEngine.bubble(this)) { removeBubble();return; }
        if(bubbleView!=null)return;
        TextView v=new TextView(this);
        v.setText("📋");v.setTextSize(27);v.setGravity(Gravity.CENTER);
        v.setContentDescription("Salvar texto copiado na conversa ativa");
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(77,52,130));bg.setCornerRadius(36);bg.setStroke(2,Color.rgb(174,145,255));v.setBackground(bg);
        int flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(dp(56),dp(56), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,flags,PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL; lp.x=12;lp.y=90;
        v.setOnClickListener(view->{
            Intent intent=new Intent(this,CaptureActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
        });
        try{manager.addView(v,lp);bubbleView=v;}catch(Exception ignored){}
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private void removeBubble(){if(bubbleView!=null){try{manager.removeView(bubbleView);}catch(Exception ignored){}bubbleView=null;}}
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){
        removeBubble();
        CaptureEngine.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener);
        if(clipboard!=null)clipboard.removePrimaryClipChangedListener(listener);
        super.onDestroy();
    }
}
