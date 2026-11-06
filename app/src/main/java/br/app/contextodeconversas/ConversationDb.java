package br.app.contextodeconversas;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.text.TextUtils;
import java.util.*;

public class ConversationDb extends SQLiteOpenHelper {
    public static class Chat {
        public long id;
        public String title;
        public int total;
        public Chat(long id, String title, int total) { this.id=id; this.title=title; this.total=total; }
    }
    public static class Msg {
        public long id;
        public String role, text;
        public Msg(long id, String role, String text) { this.id=id; this.role=role; this.text=text; }
    }
    public ConversationDb(Context ctx) { super(ctx.getApplicationContext(), "conversas.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chats (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE messages (_id INTEGER PRIMARY KEY AUTOINCREMENT, chat_id INTEGER NOT NULL, role TEXT NOT NULL, body TEXT NOT NULL, created INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX chat_idx ON messages(chat_id, _id)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
    public synchronized long newChat(String title) {
        ContentValues v=new ContentValues(); v.put("title", title.trim()); v.put("created", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("chats", null, v);
    }
    public synchronized List<Chat> chats() {
        List<Chat> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT c._id,c.title,(SELECT COUNT(*) FROM messages m WHERE m.chat_id=c._id) FROM chats c ORDER BY c._id DESC",null)) {
            while(c.moveToNext()) out.add(new Chat(c.getLong(0),c.getString(1),c.getInt(2)));
        }
        return out;
    }
    public synchronized Chat getChat(long id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT title FROM chats WHERE _id=?",new String[]{String.valueOf(id)})) {
            if(c.moveToFirst()) return new Chat(id,c.getString(0),0);
        }
        return null;
    }
    public synchronized String nextRole(long chatId) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT role FROM messages WHERE chat_id=? ORDER BY _id DESC LIMIT 1",new String[]{String.valueOf(chatId)})) {
            if(c.moveToFirst()) return "USUÁRIO".equals(c.getString(0)) ? "CHATGPT" : "USUÁRIO";
        }
        return "USUÁRIO";
    }
    public synchronized long add(long chatId, String role, String text) {
        if (getChat(chatId)==null || TextUtils.isEmpty(text) || TextUtils.isEmpty(text.trim())) return -1;
        ContentValues v=new ContentValues(); v.put("chat_id",chatId);v.put("role",role);v.put("body",text.trim());v.put("created",System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("messages",null,v);
    }
    public synchronized void addAll(long chatId, List<Msg> items) {
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try {
            for(Msg m:items) { ContentValues v=new ContentValues();v.put("chat_id",chatId);v.put("role",m.role);v.put("body",m.text);v.put("created",System.currentTimeMillis());db.insertOrThrow("messages",null,v); }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    public synchronized List<Msg> messages(long chatId) {
        List<Msg> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT _id,role,body FROM messages WHERE chat_id=? ORDER BY _id",new String[]{String.valueOf(chatId)})) {
            while(c.moveToNext()) out.add(new Msg(c.getLong(0),c.getString(1),c.getString(2)));
        }
        return out;
    }
    public synchronized void updateMessage(long id,String role,String text) {
        ContentValues v=new ContentValues();v.put("role",role);v.put("body",text);
        getWritableDatabase().update("messages",v,"_id=?",new String[]{String.valueOf(id)});
    }
    public synchronized void deleteMessage(long id) { getWritableDatabase().delete("messages","_id=?",new String[]{String.valueOf(id)}); }
    public synchronized void undo(long chatId) { getWritableDatabase().execSQL("DELETE FROM messages WHERE _id=(SELECT MAX(_id) FROM messages WHERE chat_id=?)",new Object[]{chatId}); }
    public synchronized void rename(long chatId, String title) {
        ContentValues v=new ContentValues();v.put("title",title.trim());getWritableDatabase().update("chats",v,"_id=?",new String[]{String.valueOf(chatId)});
    }
    public synchronized void deleteChat(long chatId) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try { db.delete("messages","chat_id=?",new String[]{String.valueOf(chatId)});db.delete("chats","_id=?",new String[]{String.valueOf(chatId)});db.setTransactionSuccessful(); }
        finally { db.endTransaction(); }
    }
}
