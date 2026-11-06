package br.app.contextodeconversas;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private static final int OPEN_ZIP=11, OPEN_JSON=12, SAVE_TXT=13, SAVE_JSON=14;
    private static final int BG=0xFF0C0C16, CARD=0xFF1D1B2D, PURPLE=0xFFAB91FF, LIGHT=0xFFEEEAFB, MUTED=0xFFAAA7BF;
    private ConversationDb db;
    private LinearLayout body;
    private TextView currentTitle, currentStatus;
    private Uri pendingZip;
    private String pendingShared;
    private boolean building=false;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        db=new ConversationDb(this);
        if(db.getChat(CaptureEngine.activeChat(this))==null){
            List<ConversationDb.Chat> chats=db.chats();
            CaptureEngine.setActive(this,chats.isEmpty()?db.newChat("Minha primeira conversa"):chats.get(0).id);
        }
        if(Intent.ACTION_SEND.equals(getIntent().getAction()) && "text/plain".equals(getIntent().getType()))pendingShared=getIntent().getStringExtra(Intent.EXTRA_TEXT);
        screen();
        if(pendingShared!=null && !pendingShared.trim().isEmpty())new Handler(Looper.getMainLooper()).postDelayed(()->showShared(pendingShared),250);
    }
    @Override public void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(Intent.ACTION_SEND.equals(intent.getAction()))showShared(intent.getStringExtra(Intent.EXTRA_TEXT));}
    @Override protected void onResume(){super.onResume();if(body!=null && !building)refreshMessageArea();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    private void addGap(LinearLayout parent,int h){View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout box(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(16),dp(16),dp(16));l.setBackground(round(CARD,18));return l;}
    private void card(LinearLayout child){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(14));body.addView(child,p);}
    private TextView button(String label,boolean major,Runnable onclick){
        TextView b=text(label,15,major?BG:LIGHT);b.setGravity(Gravity.CENTER);b.setTypeface(null,1);
        b.setPadding(dp(13),dp(14),dp(13),dp(14));b.setBackground(round(major?PURPLE:0xFF34304B,13));
        b.setOnClickListener(v->onclick.run());return b;
    }
    private void fullButton(LinearLayout l,String label,boolean major,Runnable callback){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(9));l.addView(button(label,major,callback),p);}
    private void twoButtons(LinearLayout l,String a,Runnable ac,String b,Runnable bc){LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
        TextView x=button(a,false,ac),y=button(b,false,bc);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1);left.setMargins(0,0,dp(5),0);
        LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-2,1);right.setMargins(dp(5),0,0,0);
        line.addView(x,left);line.addView(y,right);l.addView(line);addGap(l,9);
    }
    private void label(LinearLayout l,String title){TextView t=text(title,12,PURPLE);t.setLetterSpacing(.06f);t.setPadding(0,0,0,dp(9));l.addView(t);}
    private void screen(){
        building=true;
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);scroll.setClipToPadding(false);
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(28),dp(18),dp(32));body.setFitsSystemWindows(true);
        scroll.addView(body);setContentView(scroll);
        TextView title=text("📚  Contexto de Conversas",25,LIGHT);title.setTypeface(null,1);body.addView(title);
        TextView sub=text("Salve diálogos inteiros para continuar em outro chat.",14,MUTED);sub.setPadding(0,dp(6),0,dp(20));body.addView(sub);
        LinearLayout active=box();label(active,"CONVERSA ATIVA");currentTitle=text("",22,LIGHT);currentTitle.setTypeface(null,1);active.addView(currentTitle);
        currentStatus=text("",13,MUTED);currentStatus.setPadding(0,dp(6),0,dp(16));active.addView(currentStatus);
        fullButton(active,"📂 Escolher outra conversa",false,this::chooseChat);
        twoButtons(active,"＋ Nova conversa",this::newChat,"✏️ Renomear",this::renameChat);
        card(active);
        LinearLayout actions=box();label(actions,"SALVAR MENSAGENS");
        fullButton(actions,"📋 Colar texto copiado agora",true,this::copyFromClipboard);
        twoButtons(actions,"＋ Texto manual",this::manualText,"↩ Desfazer última",this::undo);
        TextView hint=text("A ordem alterna automaticamente: USUÁRIO → CHATGPT. Você pode corrigir o autor e editar qualquer mensagem.",13,MUTED);actions.addView(hint);card(actions);
        LinearLayout auto=box();label(auto,"CAPTURA AUTOMÁTICA (OPCIONAL)");
        Switch autoSwitch=new Switch(this);autoSwitch.setText("Salvar cópias automaticamente");autoSwitch.setTextSize(15);autoSwitch.setTextColor(LIGHT);autoSwitch.setChecked(CaptureEngine.auto(this));
        autoSwitch.setOnCheckedChangeListener((v,on)->{
            CaptureEngine.prefs(this).edit().putBoolean("auto",on).apply();
            if(on && !CaptureEngine.isDefaultKeyboard(this))new AlertDialog.Builder(this).setTitle("Ativar teclado auxiliar").setMessage("O Android 10+ bloqueia a leitura automática de cópias por aplicativos comuns. Para testar o automático, habilite e escolha o teclado auxiliar Contexto como teclado padrão. Você pode voltar ao teclado antigo quando quiser. O serviço de acessibilidade mantém o botão flutuante disponível.").setPositiveButton("Configurar",(d,w)->openKeyboardSettings()).setNegativeButton("Depois",null).show();
        });
        auto.addView(autoSwitch);addGap(auto,5);
        CheckBox scope=new CheckBox(this);scope.setText("Capturar também de outros apps (menos privacidade)");scope.setTextSize(13);scope.setTextColor(MUTED);scope.setButtonTintList(android.content.res.ColorStateList.valueOf(PURPLE));scope.setChecked(CaptureEngine.prefs(this).getBoolean("all_apps",false));
        scope.setOnCheckedChangeListener((v,on)->CaptureEngine.prefs(this).edit().putBoolean("all_apps",on).apply());auto.addView(scope);
        CheckBox bubble=new CheckBox(this);bubble.setText("Mostrar botão flutuante 📋");bubble.setTextSize(13);bubble.setTextColor(MUTED);bubble.setButtonTintList(android.content.res.ColorStateList.valueOf(PURPLE));bubble.setChecked(CaptureEngine.bubble(this));
        bubble.setOnCheckedChangeListener((v,on)->CaptureEngine.prefs(this).edit().putBoolean("bubble",on).apply());auto.addView(bubble);
        twoButtons(auto,"♿ Acessibilidade",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)),"⌨️ Teclado",this::openKeyboardSettings);
        TextView privacy=text("Acesso automático: apenas quando ativado, com este teclado definido como padrão e, normalmente, apenas no app ChatGPT. Os textos ficam armazenados no celular. O Android pode restringir o funcionamento conforme o aparelho.",12,MUTED);auto.addView(privacy);card(auto);
        LinearLayout importBox=box();label(importBox,"IMPORTAR CONVERSAS ANTIGAS");
        fullButton(importBox,"📦 Importar exportação ChatGPT (ZIP)",false,()->openDocument(OPEN_ZIP,new String[]{"application/zip","application/x-zip-compressed","application/octet-stream"}));
        fullButton(importBox,"📥 Importar backup do app (JSON)",false,()->openDocument(OPEN_JSON,new String[]{"application/json","text/plain","application/octet-stream"}));
        TextView note=text("Com o ZIP oficial do ChatGPT, você escolhe uma conversa e o aplicativo organiza suas mensagens sem copiar uma por uma. Exportações muito grandes podem demorar.",12,MUTED);importBox.addView(note);card(importBox);
        LinearLayout exports=box();label(exports,"EXPORTAR / LEVAR PARA OUTRO CHAT");
        twoButtons(exports,"💾 Salvar TXT",()->saveDocument(SAVE_TXT),"🧩 Salvar JSON",()->saveDocument(SAVE_JSON));
        twoButtons(exports,"📄 Copiar tudo",this::copyAll,"↗ Compartilhar",this::shareAll);
        fullButton(exports,"🗑 Excluir conversa ativa",false,this::deleteChat);card(exports);
        LinearLayout history=box();label(history,"HISTÓRICO — TOQUE PARA EDITAR");
        TextView historyCount=text("",13,MUTED);historyCount.setTag("count");history.addView(historyCount);
        LinearLayout messages=new LinearLayout(this);messages.setOrientation(LinearLayout.VERTICAL);messages.setTag("messages");history.addView(messages);card(history);
        refreshMessageArea();building=false;
    }
    private void refreshMessageArea(){
        if(body==null || currentTitle==null)return;
        ConversationDb.Chat chat=db.getChat(CaptureEngine.activeChat(this));
        if(chat==null)return;
        currentTitle.setText(chat.title);
        List<ConversationDb.Msg> msgs=db.messages(chat.id);
        currentStatus.setText(msgs.size()+" mensagens • Próxima: "+db.nextRole(chat.id));
        LinearLayout container=null;TextView count=null;
        for(int i=0;i<body.getChildCount();i++){
            View c=body.getChildAt(i);
            if(c instanceof LinearLayout){
                LinearLayout box=(LinearLayout)c;
                View found=box.findViewWithTag("messages");
                if(found instanceof LinearLayout){container=(LinearLayout)found;count=box.findViewWithTag("count");break;}
            }
        }
        if(container==null)return;
        container.removeAllViews();if(count!=null)count.setText(msgs.size()+" mensagens salvas");
        if(msgs.isEmpty()){
            TextView t=text("Nenhuma mensagem ainda. Copie o texto no ChatGPT e use 'Colar texto copiado agora'.",13,MUTED);t.setPadding(0,dp(14),0,dp(4));container.addView(t);return;
        }
        for(int i=0;i<msgs.size();i++){
            ConversationDb.Msg m=msgs.get(i);
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(12),dp(12),dp(12),dp(12));
            row.setBackground(round("USUÁRIO".equals(m.role)?0xFF302446:0xFF243445,12));
            TextView who=text(String.format(Locale.getDefault(),"%03d  •  %s",i+1,m.role),12,PURPLE);who.setTypeface(null,1);row.addView(who);
            String preview=m.text.length()>310?m.text.substring(0,310)+"…":m.text;
            TextView detail=text(preview,14,LIGHT);detail.setPadding(0,dp(7),0,0);row.addView(detail);
            row.setOnClickListener(v->editMessage(m));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(10),0,0);container.addView(row,p);
        }
    }
    private void chooseChat(){
        List<ConversationDb.Chat> chats=db.chats();String[] titles=new String[chats.size()];
        for(int i=0;i<chats.size();i++)titles[i]=chats.get(i).title+"   ("+chats.get(i).total+")";
        new AlertDialog.Builder(this).setTitle("Escolha uma conversa").setItems(titles,(d,i)->{CaptureEngine.setActive(this,chats.get(i).id);refreshMessageArea();}).setPositiveButton("Nova conversa",(d,w)->newChat()).setNegativeButton("Cancelar",null).show();
    }
    private void askText(String title,String initial,java.util.function.Consumer<String> done){
        EditText input=new EditText(this);input.setText(initial);input.setTextColor(LIGHT);input.setHintTextColor(MUTED);input.setSingleLine(true);input.setSelectAllOnFocus(true);
        LinearLayout box=new LinearLayout(this);box.setPadding(dp(22),dp(12),dp(22),0);box.addView(input);
        new AlertDialog.Builder(this).setTitle(title).setView(box).setPositiveButton("Salvar",(d,w)->{String value=input.getText().toString().trim();if(!value.isEmpty())done.accept(value);}).setNegativeButton("Cancelar",null).show();
    }
    private void newChat(){askText("Nova conversa","Minha conversa",text->{long id=db.newChat(text);CaptureEngine.setActive(this,id);refreshMessageArea();});}
    private void renameChat(){long id=CaptureEngine.activeChat(this);ConversationDb.Chat chat=db.getChat(id);if(chat!=null)askText("Renomear",chat.title,s->{db.rename(id,s);refreshMessageArea();});}
    private void copyFromClipboard(){
        String copied=CaptureEngine.clipboard(this);
        Toast.makeText(this,CaptureEngine.store(this,copied),Toast.LENGTH_SHORT).show();refreshMessageArea();
    }
    private void undo(){new AlertDialog.Builder(this).setMessage("Remover a última mensagem dessa conversa?").setPositiveButton("Remover",(d,w)->{db.undo(CaptureEngine.activeChat(this));refreshMessageArea();}).setNegativeButton("Cancelar",null).show();}
    private void manualText(){messageDialog(null);}
    private void editMessage(ConversationDb.Msg msg){messageDialog(msg);}
    private void messageDialog(ConversationDb.Msg msg){
        String[] roles={"USUÁRIO","CHATGPT"};
        String existingRole=msg==null?db.nextRole(CaptureEngine.activeChat(this)):msg.role;
        final int[] chosen={"CHATGPT".equals(existingRole)?1:0};
        LinearLayout area=new LinearLayout(this);area.setOrientation(LinearLayout.VERTICAL);area.setPadding(dp(20),dp(7),dp(20),dp(8));
        TextView hint=text("Quem falou?",14,MUTED);area.addView(hint);
        RadioGroup radio=new RadioGroup(this);radio.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=0;i<roles.length;i++){RadioButton r=new RadioButton(this);r.setText(roles[i]);r.setTextColor(LIGHT);r.setButtonTintList(android.content.res.ColorStateList.valueOf(PURPLE));r.setId(100+i);radio.addView(r);}
        radio.check(100+chosen[0]);radio.setOnCheckedChangeListener((g,id)->chosen[0]=id-100);area.addView(radio);
        EditText input=new EditText(this);input.setTextColor(LIGHT);input.setHintTextColor(MUTED);input.setHint("Cole ou digite o texto da mensagem");input.setGravity(Gravity.TOP);input.setMinLines(6);input.setMaxLines(14);
        if(msg!=null)input.setText(msg.text);area.addView(input);
        ScrollView sc=new ScrollView(this);sc.addView(area);
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(msg==null?"Adicionar mensagem":"Editar mensagem").setView(sc)
            .setPositiveButton("Salvar",(d,w)->{String content=input.getText().toString().trim();if(content.isEmpty()){Toast.makeText(this,"Texto vazio",Toast.LENGTH_SHORT).show();return;}
                if(msg==null)db.add(CaptureEngine.activeChat(this),roles[chosen[0]],content);else db.updateMessage(msg.id,roles[chosen[0]],content);
                refreshMessageArea();}).setNegativeButton("Cancelar",null);
        if(msg!=null)builder.setNeutralButton("Excluir",(d,w)->new AlertDialog.Builder(this).setMessage("Excluir esta mensagem?").setPositiveButton("Excluir",(dd,ww)->{db.deleteMessage(msg.id);refreshMessageArea();}).setNegativeButton("Cancelar",null).show());
        builder.show();
    }
    private void showShared(String payload){
        if(payload==null || payload.trim().isEmpty())return;
        pendingShared=null;
        new AlertDialog.Builder(this).setTitle("Texto recebido").setMessage("Salvar o texto compartilhado como "+db.nextRole(CaptureEngine.activeChat(this))+"?\n\n"+payload.substring(0,Math.min(350,payload.length())))
            .setPositiveButton("Salvar",(d,w)->{db.add(CaptureEngine.activeChat(this),db.nextRole(CaptureEngine.activeChat(this)),payload);refreshMessageArea();}).setNegativeButton("Cancelar",null).show();
    }
    private void deleteChat(){new AlertDialog.Builder(this).setTitle("Apagar conversa?").setMessage("Todas as mensagens desta conversa serão excluídas permanentemente. Exporte antes de apagar.")
        .setPositiveButton("Apagar",(d,w)->{db.deleteChat(CaptureEngine.activeChat(this));List<ConversationDb.Chat> chats=db.chats();CaptureEngine.setActive(this,chats.isEmpty()?db.newChat("Minha primeira conversa"):chats.get(0).id);refreshMessageArea();})
        .setNegativeButton("Cancelar",null).show();}
    private String transcript(){
        ConversationDb.Chat chat=db.getChat(CaptureEngine.activeChat(this));if(chat==null)return "";
        StringBuilder s=new StringBuilder("CONVERSA COMPLETA\nProjeto: ").append(chat.title).append("\n\n");
        List<ConversationDb.Msg> msgs=db.messages(chat.id);
        for(int i=0;i<msgs.size();i++){ConversationDb.Msg m=msgs.get(i);s.append("================================\nMENSAGEM ").append(i+1).append("\nAUTOR: ").append(m.role).append("\n\n").append(m.text).append("\n\n");}
        return s.toString();
    }
    private void copyAll(){String text=transcript();if(text.length()>900000){toast("Texto muito grande para copiar: use Salvar TXT.");return;}
        android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText("Conversa completa",text));toast("Conversa copiada. Pode colar em outro chat.");}
    private void shareAll(){Intent intent=new Intent(Intent.ACTION_SEND);intent.setType("text/plain");intent.putExtra(Intent.EXTRA_TEXT,transcript());startActivity(Intent.createChooser(intent,"Enviar conversa para..."));}
    private void openDocument(int request,String[] types){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,types);i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,request);}
    private void saveDocument(int request){
        ConversationDb.Chat chat=db.getChat(CaptureEngine.activeChat(this));String base=chat==null?"Conversa":chat.title.replaceAll("[^a-zA-Z0-9_-]","_");
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(request==SAVE_TXT?"text/plain":"application/json");i.putExtra(Intent.EXTRA_TITLE,base+(request==SAVE_TXT?".txt":".json"));startActivityForResult(i,request);
    }
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(result!=RESULT_OK || data==null || data.getData()==null)return;
        Uri uri=data.getData();
        if(req==OPEN_ZIP)importZip(uri);
        else if(req==OPEN_JSON)importBackup(uri);
        else if(req==SAVE_TXT || req==SAVE_JSON)exportFile(uri,req);
    }
    private void exportFile(Uri uri,int req){
        try(OutputStream stream=getContentResolver().openOutputStream(uri,"wt")){
            if(stream==null)throw new IOException("Não foi possível abrir o arquivo.");
            String data;
            if(req==SAVE_TXT)data=transcript();
            else {ConversationDb.Chat c=db.getChat(CaptureEngine.activeChat(this));data=ChatImporter.backupJson(c.title,db.messages(c.id));}
            stream.write(data.getBytes(StandardCharsets.UTF_8));toast("Arquivo salvo com sucesso.");
        }catch(Exception ex){error("Falha ao exportar: "+ex.getMessage());}
    }
    private void importZip(Uri uri){
        ProgressDialog progress=ProgressDialog.show(this,"Lendo exportação do ChatGPT","Buscando conversas...",true,false);
        new Thread(()->{
            try {List<ChatImporter.Item> found=ChatImporter.list(this,uri);runOnUiThread(()->{
                progress.dismiss();if(found.isEmpty()){error("Não encontrei conversations.json no ZIP. Verifique se é a exportação oficial do ChatGPT.");return;}
                String[] choices=new String[found.size()];for(int i=0;i<found.size();i++)choices[i]=found.get(i).title;
                new AlertDialog.Builder(this).setTitle("Selecionar conversa ("+found.size()+")").setItems(choices,(d,index)->importSelected(uri,found.get(index))).setNegativeButton("Cancelar",null).show();
            });}
            catch(Exception ex){runOnUiThread(()->{progress.dismiss();error("Erro ao abrir ZIP: "+ex.getMessage());});}
        }).start();
    }
    private void importSelected(Uri uri,ChatImporter.Item item){
        ProgressDialog progress=ProgressDialog.show(this,"Importando conversa","Organizando mensagens...",true,false);
        new Thread(()->{
            try{List<ConversationDb.Msg> msgs=ChatImporter.load(this,uri,item);
                if(msgs.isEmpty())throw new IOException("Nenhuma mensagem de texto encontrada. Áudios, imagens e ferramentas podem não ser exportados como texto.");
                long id=db.newChat(item.title==null?"Conversa importada":item.title);db.addAll(id,msgs);CaptureEngine.setActive(this,id);
                runOnUiThread(()->{progress.dismiss();refreshMessageArea();toast("Importadas "+msgs.size()+" mensagens!");});
            } catch(Exception ex){runOnUiThread(()->{progress.dismiss();error("Falha ao importar: "+ex.getMessage());});}
        }).start();
    }
    private void importBackup(Uri uri){
        try(InputStream stream=getContentResolver().openInputStream(uri)){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
            while((n=stream.read(buf))!=-1){if(bytes.size()>25_000_000)throw new IOException("Backup muito grande (limite 25 MB).");bytes.write(buf,0,n);}
            String json=new String(bytes.toByteArray(),StandardCharsets.UTF_8);
            org.json.JSONObject obj=new org.json.JSONObject(json);
            List<ConversationDb.Msg> messages=ChatImporter.readBackup(json);
            long id=db.newChat(obj.optString("titulo","Conversa restaurada"));db.addAll(id,messages);CaptureEngine.setActive(this,id);refreshMessageArea();toast("Backup importado: "+messages.size()+" mensagens.");
        }catch(Exception ex){error("Não foi possível importar: "+ex.getMessage());}
    }
    private void openKeyboardSettings(){
        new AlertDialog.Builder(this).setTitle("Ativar teclado auxiliar")
            .setMessage("1. Toque em 'Habilitar teclados' e ative 'Teclado auxiliar Contexto'.\n2. Volte e toque em 'Escolher teclado'.\n3. Selecione nosso teclado.\n4. Para captura automática de cópias, habilite também 'Acessibilidade' (botão correspondente).\n\nEste teclado é simples, sem corretor. Volte ao Gboard quando acabar a coleta.")
            .setPositiveButton("Habilitar teclados",(d,w)->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)))
            .setNeutralButton("Escolher teclado",(d,w)->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker())
            .setNegativeButton("Voltar",null).show();
    }
    private void toast(String t){Toast.makeText(this,t,Toast.LENGTH_LONG).show();}
    private void error(String t){new AlertDialog.Builder(this).setTitle("Atenção").setMessage(t).setPositiveButton("OK",null).show();}
}
