package com.example.toutiao.demo;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small local repositories share one worker and transactional SQLite connection per operation.
 * JSON retains the complete article/order snapshot; indexed columns hold queryable identities.
 * Legacy files are never deleted. A migration marker commits with the migrated rows.
 */
public final class DemoDatabase {
    public static final String NAME = "course_demo.db";
    private static volatile Thread worker;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "course-sqlite"); worker = thread; return thread;
    });
    private static final ThreadLocal<SQLiteDatabase> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<String> ACTIVE_PATH = new ThreadLocal<>();
    private final Context context;
    private final String path;
    private volatile String migrationError;

    public DemoDatabase(Context context) {
        this.context = context.getApplicationContext();
        path = this.context.getDatabasePath(NAME).getAbsolutePath();
    }

    /** Execute database work off the UI thread; synchronous adapters only wait for bounded rows. */
    private <T> T io(Callable<T> action) {
        if (Thread.currentThread() == worker) return call(action);
        try { return IO.submit(action).get(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("存储操作被中断", e); }
        catch (ExecutionException e) { Throwable cause=e.getCause(); if(cause instanceof RuntimeException)throw (RuntimeException)cause; throw new IllegalStateException("存储操作失败",cause); }
    }
    private static <T> T call(Callable<T> action) {
        try { return action.call(); } catch(RuntimeException e){throw e;} catch(Exception e){throw new IllegalStateException("存储操作失败",e);}
    }
    private <T> T connection(CallableWithDb<T> action) {
        return io(() -> {
            SQLiteDatabase active=ACTIVE.get();
            if(active!=null){
                if(!path.equals(ACTIVE_PATH.get()))throw new IllegalStateException("事务不能混用数据库");
                return action.run(active);
            }
            try(Helper helper=new Helper(context)) {
                SQLiteDatabase db=helper.getWritableDatabase();
                migrate(db);
                ACTIVE.set(db); ACTIVE_PATH.set(path);
                try{return action.run(db);}finally{ACTIVE.remove();ACTIVE_PATH.remove();}
            }
        });
    }
    private interface CallableWithDb<T> { T run(SQLiteDatabase db) throws Exception; }
    public <T> T transaction(Callable<T> action) {
        return connection(db -> {
            if(db.inTransaction()) return action.call();
            db.beginTransaction();
            try { T result=action.call();db.setTransactionSuccessful();return result; }
            finally {db.endTransaction();}
        });
    }
    private static String table(String value) {
        if(!"history".equals(value)&&!"saved".equals(value)&&!"cart".equals(value)&&!"orders".equals(value))throw new IllegalArgumentException("未知数据表");
        return value;
    }
    public JSONArray list(String name) {
        String safe=table(name);
        return connection(db -> {
            JSONArray rows=new JSONArray();
            try(Cursor cursor=db.query(safe,new String[]{"json"},null,null,null,null,"updated_at DESC, rowid DESC")) {
                while(cursor.moveToNext()) rows.put(new JSONObject(cursor.getString(0)));
            }
            return rows;
        });
    }
    public void put(String name,JSONObject item) {
        String safe=table(name);
        connection(db -> { upsert(db,safe,item, System.currentTimeMillis(),SQLiteDatabase.CONFLICT_REPLACE); return null; });
    }
    public void remove(String name,String idOrTitle) {
        String safe=table(name);
        connection(db -> { db.delete(safe,"id=?",new String[]{idOrTitle});return null; });
    }
    public void replace(String name,JSONArray rows) {
        String safe=table(name);
        transaction(() -> {
            SQLiteDatabase db=ACTIVE.get(); db.delete(safe,null,null);
            long now=System.currentTimeMillis();
            for(int i=0;i<rows.length();i++) upsert(db,safe,rows.getJSONObject(i),now-i,SQLiteDatabase.CONFLICT_REPLACE);
            return null;
        });
    }
    public String migrationStatus() {
        return connection(db -> {
            try(Cursor cursor=db.rawQuery("SELECT value FROM metadata WHERE key='legacy_json_v1'",null)) {
                return cursor.moveToFirst()?"旧JSON迁移完成（原文件保留）":"迁移未完成，旧数据保留："+(migrationError==null?"等待迁移":migrationError);
            }
        });
    }
    public String summary() {
        return connection(db -> {
            StringBuilder result=new StringBuilder("SQLiteOpenHelper / "+NAME+" / v"+db.getVersion()+"\n");
            for(String name:new String[]{"history","saved","cart","orders"}) {
                try(Cursor cursor=db.rawQuery("SELECT COUNT(*) FROM "+name,null)) {cursor.moveToFirst();result.append(name).append("：").append(cursor.getLong(0)).append(" 条\n");}
            }
            return result.append(migrationStatus()).toString();
        });
    }
    private static String identity(String table,JSONObject row) throws Exception {
        if("history".equals(table)||"saved".equals(table)) {
            String id=row.optString("news_id");
            if(id.isEmpty()){id=LegacyIds.news(row.optString("title"));row.put("news_id",id);}
            return id;
        }
        String id=row.optString("id");
        if(id.isEmpty() || ("orders".equals(table) && row.optString("orderId").isEmpty()))throw new IllegalArgumentException("记录缺少稳定ID: "+table);
        return id;
    }
    private static void upsert(SQLiteDatabase db,String table,JSONObject row,long time,int conflict) throws Exception {
        ContentValues values=new ContentValues();
        values.put("id",identity(table,row)); values.put("title",row.optString("title"));
        values.put("updated_at",time);
        if("history".equals(table))row.put("viewed_at",time);
        if("cart".equals(table)) {
            values.put("product_id",row.optString("productId"));values.put("sku",row.optString("sku"));
            values.put("quantity",row.optInt("quantity",1));values.put("selected",row.optBoolean("selected",true)?1:0);
            values.put("price_cents",row.optLong("priceCents"));
        }
        values.put("json",row.toString());
        if(db.insertWithOnConflict(table,null,values,conflict)==-1 && conflict!=SQLiteDatabase.CONFLICT_IGNORE)throw new IllegalStateException("数据库写入失败");
    }
    private void migrate(SQLiteDatabase db) throws Exception {
        try(Cursor marker=db.rawQuery("SELECT value FROM metadata WHERE key='legacy_json_v1'",null)){if(marker.moveToFirst())return;}
        SharedPreferences content=context.getSharedPreferences("content",Context.MODE_PRIVATE);
        SharedPreferences shop=context.getSharedPreferences("shop_store_v2",Context.MODE_PRIVATE);
        // Parse and validate before touching any row; malformed sources remain intact and retryable.
        JSONArray history,saved,cart,orders,legacyCart;
        try {
            history=new JSONArray(content.getString("history","[]"));
            saved=new JSONArray(content.getString("saved","[]"));
            cart=new JSONArray(shop.getString("cart_items","[]"));
            orders=new JSONArray(shop.getString("order_items","[]"));
            legacyCart=new JSONArray(content.getString("cart","[]"));
        } catch(Exception damaged) { migrationError="旧JSON解析失败";android.util.Log.e("CourseDatabase",migrationError,damaged);return; }
        db.beginTransaction();
        try {
            migrateRows(db,"history",history);migrateRows(db,"saved",saved);
            migrateRows(db,"cart",cart);migrateRows(db,"orders",orders);
            if(cart.length()==0) {
                JSONArray normalized=new JSONArray();
                for(int i=0;i<legacyCart.length();i++) {
                    JSONObject old=legacyCart.getJSONObject(i);
                    normalized.put(new ShopStore.CartItem(old.optString("title"),old.optLong("cents"),old.optInt("quantity",1),old.optInt("img"),"推荐",true).toJson());
                }
                migrateRows(db,"cart",normalized);
            }
            ContentValues flag=new ContentValues();flag.put("key","legacy_json_v1");flag.put("value","complete");
            db.insertOrThrow("metadata",null,flag);db.setTransactionSuccessful();
        } catch(Exception failure) {
            migrationError="旧数据校验失败"; android.util.Log.e("CourseDatabase",migrationError,failure);
        } finally {db.endTransaction();}
    }
    private static void migrateRows(SQLiteDatabase db,String table,JSONArray rows) throws Exception {
        long now=System.currentTimeMillis();
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.getJSONObject(i);
            if("cart".equals(table))row=ShopStore.CartItem.fromJson(row).toJson();
            if("orders".equals(table))row=ShopStore.OrderItem.fromJson(row).toJson();
            upsert(db,table,row,now-i,SQLiteDatabase.CONFLICT_IGNORE);
        }
    }
    private static final class Helper extends SQLiteOpenHelper {
        Helper(Context context){super(context,NAME,null,2);}
        @Override public void onConfigure(SQLiteDatabase db){db.setForeignKeyConstraintsEnabled(true);}
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            for(String table:new String[]{"history","saved","orders"}) {
                db.execSQL("CREATE TABLE "+table+" (id TEXT PRIMARY KEY, title TEXT NOT NULL, json TEXT NOT NULL, updated_at INTEGER NOT NULL)");
                db.execSQL("CREATE INDEX idx_"+table+"_recent ON "+table+"(updated_at DESC)");
            }
            db.execSQL("CREATE TABLE cart (id TEXT PRIMARY KEY, product_id TEXT NOT NULL, sku TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity BETWEEN 1 AND 999), selected INTEGER NOT NULL CHECK(selected IN (0,1)), price_cents INTEGER NOT NULL CHECK(price_cents>=0), title TEXT NOT NULL, json TEXT NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(product_id,sku))");
            db.execSQL("CREATE INDEX idx_cart_selected ON cart(selected,updated_at DESC)");
            createVersion2Indexes(db);
        }
        @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
            if(oldVersion<2)createVersion2Indexes(db);
        }
        private static void createVersion2Indexes(SQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_title ON history(title)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_saved_title ON saved(title)");
        }
    }
}
