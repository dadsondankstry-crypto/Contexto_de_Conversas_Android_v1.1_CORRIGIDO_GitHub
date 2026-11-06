package br.app.contextodeconversas;

import android.app.Activity;
import android.os.*;
import android.widget.Toast;

/** Recebe foco por um instante para acessar legitimamente a área de transferência. */
public class CaptureActivity extends Activity {
    private boolean captured=false;
    @Override protected void onResume(){
        super.onResume();
        if(!captured){
            captured=true;
            new Handler(Looper.getMainLooper()).postDelayed(()->{
                String content=CaptureEngine.clipboard(this);
                String status=CaptureEngine.store(this,content);
                Toast.makeText(this,status,Toast.LENGTH_SHORT).show();finish();
            },220);
        }
    }
}
