package com.example.toutiao.demo;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.File;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class DatabasePersistenceTest {
    private Context isolated;
    @Before public void setup() {
        Context base = ApplicationProvider.getApplicationContext();
        String suffix = "db-test-" + java.util.UUID.randomUUID();
        isolated = new ContextWrapper(base) {
            @Override public Context getApplicationContext(){ return this; }
            @Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){return base.getSharedPreferences(suffix+name,mode);}
            @Override public File getDatabasePath(String name){return new File(base.getCacheDir(),suffix+name);}
            @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),factory);}
            @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,android.database.DatabaseErrorHandler handler){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(),factory,handler);}
        };
    }
    @Test public void historyDeduplicatesByIdAndSameTitlesRemainDistinct() throws Exception {
        ContentStore store = new ContentStore(isolated);
        JSONObject a = ContentStore.article("同名新闻","来源","正文",0,1); a.put("news_id","news-a");
        JSONObject b = ContentStore.article("同名新闻","来源","另一正文",0,1); b.put("news_id","news-b");
        store.put("history",a);store.put("history",b);store.put("history",a);
        assertEquals(2,new ContentStore(isolated).list("history").length());
        store.put("saved",a);assertTrue(new ContentStore(isolated).contains("saved","news-a"));
        store.remove("saved","news-a");assertFalse(store.contains("saved","news-a"));
    }
    @Test public void legacyMigrationKeepsSourceAndIsIdempotent() throws Exception {
        String source = new JSONArray().put(ContentStore.article("旧新闻","旧来源","正文",0,1)).toString();
        isolated.getSharedPreferences("content",0).edit().putString("history",source).commit();
        assertEquals(1,new ContentStore(isolated).list("history").length());
        assertEquals(1,new ContentStore(isolated).list("history").length());
        assertEquals(source,isolated.getSharedPreferences("content",0).getString("history",""));
        assertTrue(isolated.getDatabasePath("course_demo.db").exists());
    }
    @Test public void failedTransactionRollsBack() throws Exception {
        DemoDatabase db = new DemoDatabase(isolated);
        try { db.transaction(()->{db.replace("cart",new JSONArray().put(new JSONObject().put("id","sku-a")));throw new IllegalStateException("test rollback");});fail(); }
        catch(IllegalStateException expected){}
        assertEquals(0,db.list("cart").length());
    }
    @Test public void corruptedLegacySourceIsPreservedAndRetryable() throws Exception {
        isolated.getSharedPreferences("content",0).edit().putString("history","broken-json").commit();
        DemoDatabase db=new DemoDatabase(isolated);
        assertEquals(0,db.list("history").length());
        assertTrue(db.migrationStatus().contains("未完成"));
        assertEquals("broken-json",isolated.getSharedPreferences("content",0).getString("history",""));
        isolated.getSharedPreferences("content",0).edit().putString("history",new JSONArray().put(ContentStore.article("修复源","","正文",0,1)).toString()).commit();
        assertEquals(1,db.list("history").length());assertTrue(db.migrationStatus().contains("完成"));
    }
    @Test public void legacyCartAndOrderMigrationPreservesSourcesAndSku() throws Exception {
        JSONObject old=new JSONObject().put("title","无线蓝牙耳机").put("priceCents",12900).put("quantity",2).put("selected",false).put("imgRes",R.drawable.shop_1).put("category","数码");
        String source=new JSONArray().put(old).toString();
        isolated.getSharedPreferences("shop_store_v2",0).edit().putString("cart_items",source).commit();
        ShopStore shop=new ShopStore(isolated);assertEquals(2,shop.getCartCount());
        assertFalse(shop.getCartItems().get(0).selected);assertFalse(shop.getCartItems().get(0).sku.isEmpty());
        assertEquals(source,isolated.getSharedPreferences("shop_store_v2",0).getString("cart_items",""));
        shop.updateQuantity(shop.getCartItems().get(0).key(),3);
        assertEquals(3,new ShopStore(isolated).getCartCount());
    }
}
