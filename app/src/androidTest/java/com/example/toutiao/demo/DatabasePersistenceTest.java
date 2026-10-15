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
    @Test public void cartImageSurvivesJsonSnapshot() throws Exception {
        JSONObject json = new JSONObject().put("productId", "yanxuan-image")
                .put("sku", "默认规格").put("title", "官网商品").put("priceCents", 1999)
                .put("imageUrl", "https://example.com/product.jpg");
        ShopStore.CartItem item = ShopStore.CartItem.fromJson(json);
        assertEquals("https://example.com/product.jpg", item.toJson().optString("imageUrl"));
    }
    @Test public void readdingSameProductUpdatesMetadataWithoutDuplicating() {
        ShopStore store = new ShopStore(isolated);
        store.addToCart("yanxuan-metadata", "默认规格", "旧名称", 1999, 0, "其他", 1);
        store.addToCart("yanxuan-metadata", "默认规格", "新名称", 1499, 0, "家具", 1);
        assertEquals(1, store.getCartItems().size());
        ShopStore.CartItem item = store.getCartItems().get(0);
        assertEquals("新名称", item.title);
        assertEquals(1499, item.priceCents);
        assertEquals(2, item.quantity);
    }
    @Test public void cartRemoteImagePersistsAndRestoreDoesNotIncreaseQuantity() {
        ShopStore store = new ShopStore(isolated);
        store.addToCart("yanxuan-image-save", "默认规格", "官网商品", 1999, 0, "家具", 2,
                "https://example.com/product.jpg");
        ShopStore.CartItem saved = new ShopStore(isolated).getCartItems().get(0);
        assertEquals("https://example.com/product.jpg", saved.imageUrl);
        ShopStore.CartItem incoming = ShopStore.CartItem.fromJson(saved.toJson());
        incoming.quantity = 9;
        assertFalse(store.restoreCartSnapshot(incoming));
        assertEquals(2, store.getCartItems().get(0).quantity);
        store.saveCart(java.util.Collections.emptyList());
        assertTrue(store.restoreCartSnapshot(incoming));
        assertEquals(9, store.getCartItems().get(0).quantity);
        assertFalse(store.restoreCartSnapshot(incoming));
        assertEquals(1, store.getCartItems().size());
    }
    @Test public void restoreCorrectsOldMetadataButPreservesLocalChoices() {
        ShopStore store = new ShopStore(isolated);
        ShopStore.CartItem old = new ShopStore.CartItem("cart-old", "已选规格", "错误名称", 28, 2, 0, "其他", false);
        store.saveCart(java.util.Collections.singletonList(old));
        ShopStore.CartItem incoming = new ShopStore.CartItem("cart-old", "默认规格", "官网名称", 29, 9, 0, "家具", true);
        incoming.imageUrl = "https://example.com/correct.jpg";
        assertTrue(store.restoreCartSnapshot(incoming));
        ShopStore.CartItem item = store.getCartItems().get(0);
        assertEquals("官网名称", item.title);
        assertEquals(29, item.priceCents);
        assertEquals("https://example.com/correct.jpg", item.imageUrl);
        assertEquals(2, item.quantity);
        assertEquals("已选规格", item.sku);
        assertFalse(item.selected);
    }
    @Test public void deletedItemCannotBeResurrectedByStaleServerResponse() {
        ShopStore store = new ShopStore(isolated);
        ShopStore.CartItem item = new ShopStore.CartItem("cart-deleted", "默认规格", "商品", 100, 1, 0, "其他", true);
        store.saveCart(java.util.Collections.singletonList(item));
        store.removeFromCart(item.key());
        assertFalse(new ShopStore(isolated).restoreCartSnapshot(item));
        assertTrue(store.getCartItems().isEmpty());
        store.addToCart(item.productId, item.sku, item.title, item.priceCents, 0, "其他", 1);
        assertEquals(1, store.getCartItems().size());
    }
    @Test public void batchDeletionDoesNotRestoreSelectedItems() {
        ShopStore store = new ShopStore(isolated);
        ShopStore.CartItem selected = new ShopStore.CartItem("cart-selected", "默认规格", "选中", 100, 1, 0, "其他", true);
        ShopStore.CartItem kept = new ShopStore.CartItem("cart-kept", "默认规格", "保留", 200, 1, 0, "其他", false);
        store.saveCart(java.util.Arrays.asList(selected, kept));
        store.removeSelectedFromCart();
        assertFalse(store.restoreCartSnapshot(selected));
        assertEquals("cart-kept", store.getCartItems().get(0).productId);
    }
    @Test public void skuEditMergesSameSkuWithoutDuplicateRows() {
        ShopStore store = new ShopStore(isolated);
        ShopStore.CartItem a = new ShopStore.CartItem("cart-edit", "白色", "商品", 100, 2, 0, "其他", true);
        ShopStore.CartItem b = new ShopStore.CartItem("cart-edit", "黑色", "商品", 100, 3, 0, "其他", false);
        store.saveCart(java.util.Arrays.asList(a, b));
        ShopItem product = new ShopItem("cart-edit", "商品", 1, 0, 0, "其他");
        assertTrue(store.updateCartOptions(a.key(), "黑色", 4, product));
        assertEquals(1, store.getCartItems().size());
        assertEquals(7, store.getCartItems().get(0).quantity);
        assertEquals("黑色", store.getCartItems().get(0).sku);
    }
    @Test public void selectedSkuPriceSurvivesBasePriceRestore() {
        ShopStore store = new ShopStore(isolated);
        ShopStore.CartItem original = new ShopStore.CartItem("cart-sku-price", "白色", "商品", 1000, 1, 0, "其他", true);
        store.saveCart(java.util.Collections.singletonList(original));
        ShopItem priced = new ShopItem("cart-sku-price", "商品", 12.34, 0, 0, "其他");
        assertTrue(store.updateCartOptions(original.key(), "黑色", 2, priced));
        store.restoreCartSnapshot(original);
        assertEquals(1234, new ShopStore(isolated).getCartItems().get(0).priceCents);
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
