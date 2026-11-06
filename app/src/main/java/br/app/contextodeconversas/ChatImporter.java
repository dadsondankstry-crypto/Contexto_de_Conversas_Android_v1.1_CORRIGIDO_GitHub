package br.app.contextodeconversas;

import android.content.Context;
import android.net.Uri;
import android.util.JsonReader;
import android.util.JsonToken;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Importador de exportacoes oficiais ChatGPT: conversas*.json dentro do ZIP. */
public final class ChatImporter {
    public static class Item {
        public String zipEntry, title;
        public int index;
        public Item(String entry,int index,String title){this.zipEntry=entry;this.index=index;this.title=title;}
        @Override public String toString(){return title;}
    }
    private static class Node {
        String parent,role,text;
        double time;
    }
    private static class Parsed {
        String title="Sem título", current;
        final Map<String,Node> mapping=new LinkedHashMap<>();
    }
    private ChatImporter() {}
    private static boolean isConversationFile(String name){
        String n=name.substring(name.lastIndexOf('/')+1).toLowerCase(Locale.ROOT);
        return n.startsWith("conversations") && n.endsWith(".json");
    }
    // Evita que JsonReader feche o ZipInputStream entre as entradas.
    private static InputStream noClose(InputStream stream) {
        return new FilterInputStream(stream){@Override public void close(){} };
    }
    public static List<Item> list(Context context,Uri uri) throws Exception {
        List<Item> result=new ArrayList<>();
        try(ZipInputStream zip=new ZipInputStream(context.getContentResolver().openInputStream(uri))) {
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) {
                if(!entry.isDirectory() && isConversationFile(entry.getName())) {
                    JsonReader jr=new JsonReader(new InputStreamReader(noClose(zip),StandardCharsets.UTF_8));
                    jr.setLenient(true);
                    jr.beginArray();int index=0;
                    while(jr.hasNext()) {
                        Parsed p=readConversation(jr,false);
                        result.add(new Item(entry.getName(),index++,p.title));
                    }
                    jr.endArray();jr.close();
                }
                zip.closeEntry();
            }
        }
        return result;
    }
    public static List<ConversationDb.Msg> load(Context context,Uri uri,Item target) throws Exception {
        try(ZipInputStream zip=new ZipInputStream(context.getContentResolver().openInputStream(uri))) {
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) {
                if(entry.getName().equals(target.zipEntry)) {
                    JsonReader jr=new JsonReader(new InputStreamReader(noClose(zip),StandardCharsets.UTF_8));
                    jr.setLenient(true);jr.beginArray();int index=0;
                    Parsed parsed=null;
                    while(jr.hasNext()) {
                        if(index++==target.index){parsed=readConversation(jr,true);break;}
                        jr.skipValue();
                    }
                    jr.close();
                    if(parsed==null) throw new IOException("Conversa não encontrada na exportação.");
                    return messages(parsed);
                }
                zip.closeEntry();
            }
        }
        throw new IOException("Arquivo de conversas não encontrado.");
    }
    private static Parsed readConversation(JsonReader jr,boolean full) throws IOException {
        Parsed p=new Parsed();jr.beginObject();
        while(jr.hasNext()) {
            String key=jr.nextName();
            if("title".equals(key)) p.title=string(jr,"Sem título");
            else if("current_node".equals(key)) p.current=string(jr,null);
            else if("mapping".equals(key) && full) {
                if(jr.peek()==JsonToken.NULL) jr.nextNull();
                else {
                    jr.beginObject();
                    while(jr.hasNext()) {
                        String id=jr.nextName(); Node node=readNode(jr); p.mapping.put(id,node);
                    }
                    jr.endObject();
                }
            } else jr.skipValue();
        }
        jr.endObject();return p;
    }
    private static Node readNode(JsonReader jr) throws IOException {
        Node n=new Node();
        if(jr.peek()!=JsonToken.BEGIN_OBJECT){jr.skipValue();return n;}
        jr.beginObject();
        while(jr.hasNext()) {
            String key=jr.nextName();
            if("parent".equals(key)) n.parent=string(jr,null);
            else if("message".equals(key)) readMessage(jr,n);
            else jr.skipValue();
        }
        jr.endObject();return n;
    }
    private static void readMessage(JsonReader jr,Node n) throws IOException {
        if(jr.peek()!=JsonToken.BEGIN_OBJECT){jr.skipValue();return;}
        jr.beginObject();
        while(jr.hasNext()){
            String key=jr.nextName();
            if("author".equals(key) && jr.peek()==JsonToken.BEGIN_OBJECT){
                jr.beginObject();while(jr.hasNext()){String k=jr.nextName();if("role".equals(k))n.role=string(jr,null);else jr.skipValue();}jr.endObject();
            } else if("content".equals(key)) n.text=readContent(jr);
            else if("create_time".equals(key)) {
                try { if(jr.peek()==JsonToken.NULL)jr.nextNull();else n.time=jr.nextDouble(); }
                catch(NumberFormatException e){n.time=0;}
            } else jr.skipValue();
        }
        jr.endObject();
    }
    private static String readContent(JsonReader jr) throws IOException {
        if(jr.peek()!=JsonToken.BEGIN_OBJECT){jr.skipValue();return "";}
        StringBuilder s=new StringBuilder();jr.beginObject();
        while(jr.hasNext()){
            String key=jr.nextName();
            if("parts".equals(key) && jr.peek()==JsonToken.BEGIN_ARRAY){
                jr.beginArray();while(jr.hasNext()){
                    String part=readPart(jr);
                    if(!part.isEmpty()){if(s.length()>0)s.append("\n");s.append(part);}
                }jr.endArray();
            } else if("text".equals(key) && jr.peek()==JsonToken.STRING) {
                String part=jr.nextString();if(s.length()>0)s.append("\n");s.append(part);
            } else jr.skipValue();
        }
        jr.endObject();return s.toString();
    }
    private static String readPart(JsonReader jr) throws IOException {
        if(jr.peek()==JsonToken.STRING)return jr.nextString();
        if(jr.peek()==JsonToken.BEGIN_OBJECT){
            String txt="";jr.beginObject();
            while(jr.hasNext()){String k=jr.nextName();if("text".equals(k) && jr.peek()==JsonToken.STRING)txt=jr.nextString();else jr.skipValue();}
            jr.endObject();return txt;
        }
        jr.skipValue();return "";
    }
    private static String string(JsonReader jr,String otherwise) throws IOException {
        if(jr.peek()==JsonToken.NULL){jr.nextNull();return otherwise;}
        if(jr.peek()==JsonToken.STRING || jr.peek()==JsonToken.NUMBER)return jr.nextString();
        jr.skipValue();return otherwise;
    }
    private static List<ConversationDb.Msg> messages(Parsed p){
        List<ConversationDb.Msg> out=new ArrayList<>();
        List<Node> line=new ArrayList<>();Set<String> visited=new HashSet<>();
        String current=p.current;
        while(current!=null && !visited.contains(current)) {
            visited.add(current);Node n=p.mapping.get(current);
            if(n==null)break;
            line.add(n);current=n.parent;
        }
        if(line.isEmpty()){
            line.addAll(p.mapping.values());
            line.sort(Comparator.comparingDouble(n->n.time));
        } else Collections.reverse(line);
        for(Node n:line){
            if(n.text==null || n.text.trim().isEmpty())continue;
            String role="user".equals(n.role)?"USUÁRIO":"assistant".equals(n.role)?"CHATGPT":null;
            if(role!=null)out.add(new ConversationDb.Msg(0,role,n.text.trim()));
        }
        return out;
    }
    public static String backupJson(String title,List<ConversationDb.Msg> msgs) throws JSONException {
        JSONObject obj=new JSONObject();obj.put("formato","contexto-conversas-v1");obj.put("titulo",title);
        JSONArray arr=new JSONArray();
        for(ConversationDb.Msg m:msgs){JSONObject j=new JSONObject();j.put("autor",m.role);j.put("texto",m.text);arr.put(j);}
        obj.put("mensagens",arr);return obj.toString(2);
    }
    public static List<ConversationDb.Msg> readBackup(String data) throws JSONException {
        JSONObject obj=new JSONObject(data);
        if(!"contexto-conversas-v1".equals(obj.optString("formato")))throw new JSONException("Esse JSON não é um backup do app.");
        JSONArray arr=obj.getJSONArray("mensagens");List<ConversationDb.Msg> out=new ArrayList<>();
        for(int i=0;i<arr.length();i++){JSONObject e=arr.getJSONObject(i);String role=e.optString("autor","USUÁRIO");String body=e.optString("texto","");if(!body.isEmpty())out.add(new ConversationDb.Msg(0,role,body));}
        return out;
    }
}
