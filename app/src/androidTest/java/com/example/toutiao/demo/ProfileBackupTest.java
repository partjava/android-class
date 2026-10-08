package com.example.toutiao.demo;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.io.FileOutputStream;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class ProfileBackupTest {
 @Test public void roundTripAndCorruptBackupNeverPartiallyRestore() throws Exception {
  Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
  String name = "profile_backup_test_" + System.nanoTime();
  ProfileStore store = new ProfileStore(context, name);
  File dir = new File(context.getCacheDir(), name); assertTrue(dir.mkdir());
  ProfileBackup backup = new ProfileBackup(dir);
  try {
   store.set("nickname", "中文昵称🌟"); store.set("school", "晴川学院");
   backup.writeText(store.snapshot()); backup.writeXml(store.snapshot());
   assertEquals("中文昵称🌟", backup.readText().get("nickname"));
   assertEquals("image3", backup.readXml().get("avatar_res"));
   store.set("nickname", "后来修改"); store.restore(backup.readXml());
   assertEquals("中文昵称🌟", new ProfileStore(context, name).get("nickname"));
   try (FileOutputStream out = new FileOutputStream(new File(dir, ProfileBackup.XML_FILE))) {
    out.write("<profile><field key=\"nickname\">坏数据</field></profile>".getBytes(StandardCharsets.UTF_8));
   }
   try { store.restore(backup.readXml()); fail("Incomplete backup accepted"); } catch (java.io.IOException expected) { }
   assertEquals("中文昵称🌟", store.get("nickname"));
   try { store.set("birth", "2099-01-01"); fail("Future date accepted"); } catch (IllegalArgumentException expected) { }
  } finally {
   context.getSharedPreferences(name, 0).edit().clear().commit();
   for (File f : dir.listFiles()) f.delete(); dir.delete();
  }
 }
 @Test public void invalidFieldsAndOldAvatarAreRejectedOrSafelyRecovered() {
  Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
  String name = "profile_validation_test_" + System.nanoTime();
  ProfileStore store = new ProfileStore(context, name);
  try {
   String[][] invalid = {{"nickname", new String(new char[25]).replace('\0', '字')}, {"gender", "损坏"}, {"profile_bg", "坏背景"}, {"avatar_frame", "坏挂件"}, {"birth", "2025-02-30"}};
   for (String[] pair : invalid) {
    try { store.set(pair[0], pair[1]); fail("Accepted " + pair[0]); } catch (IllegalArgumentException expected) { }
   }
   context.getSharedPreferences(name, 0).edit().putInt("avatar_res", -123).commit();
   assertEquals(R.drawable.image3, store.getAvatarRes());
  } finally { context.getSharedPreferences(name, 0).edit().clear().commit(); }
 }
 @Test public void missingBackupIsExplicit() throws Exception {
  File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir();
  ProfileBackup backup = new ProfileBackup(new File(dir, "missing_" + System.nanoTime()));
  try { backup.readText(); fail("Missing file accepted"); } catch (java.io.IOException expected) { assertTrue(expected.getMessage().contains("不存在")); }
 }
}
