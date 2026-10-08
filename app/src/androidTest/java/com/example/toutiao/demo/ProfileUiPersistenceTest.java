package com.example.toutiao.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Map;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class ProfileUiPersistenceTest {
    @Test public void realProfileEditorAndThreeStorageButtonsPersistAndCaptureEvidence() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assumeTrue("UI acceptance runs only in isolated package", context.getPackageName().endsWith(".acceptance"));
        ProfileStore store = new ProfileStore(context);
        Map<String, String> original = store.snapshot();
        File[] files = {new File(context.getFilesDir(), ProfileBackup.TEXT_FILE), new File(context.getFilesDir(), ProfileBackup.XML_FILE)};
        byte[][] prior = new byte[files.length][];
        for (int i = 0; i < files.length; i++) if (files[i].exists()) prior[i] = Files.readAllBytes(files[i].toPath());
        try {
            try (ActivityScenario<MineActivity> mine = ActivityScenario.launch(MineActivity.class)) {
                onView(withId(R.id.tv_apply_auth)).perform(click());
                onView(withId(R.id.item_username)).perform(scrollTo(), click());
                onView(isAssignableFrom(android.widget.EditText.class)).perform(replaceText("   "), closeSoftKeyboard());
                onView(withId(android.R.id.button1)).perform(invokeClick());
                onView(isAssignableFrom(android.widget.EditText.class)).check(matches(hasErrorText("昵称不能为空")));
                onView(withId(android.R.id.button2)).perform(invokeClick());
                assertEquals(original.get("nickname"), store.get("nickname"));
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                onView(withId(R.id.item_username)).perform(scrollTo(), invokeClick());
                onView(isAssignableFrom(android.widget.EditText.class)).perform(replaceText("配置实验同学"), closeSoftKeyboard());
                onView(withId(android.R.id.button1)).perform(invokeClick());
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                onView(withId(R.id.item_bg)).perform(scrollTo(), invokeClick());
                onView(withText("渐变晚霞")).perform(click());
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                onView(withId(R.id.item_avatar_frame)).perform(scrollTo(), invokeClick());
                onView(withText("技术专家")).perform(click());
                pressBack();
                onView(withId(R.id.tv_profile_name)).check(matches(withText("配置实验同学")));
                onView(withId(R.id.profile_avatar_badge)).check(matches(withText("★ 技术专家")));
                mine.recreate();
                onView(withId(R.id.tv_profile_name)).check(matches(withText("配置实验同学")));
                mine.onActivity(activity -> {
                    View header = activity.findViewById(R.id.profile_header);
                    assertTrue(header.getBackground() instanceof android.graphics.drawable.GradientDrawable);
                    assertEquals("渐变晚霞", new ProfileStore(activity).get("profile_bg"));
                    assertEquals(View.VISIBLE, activity.findViewById(R.id.profile_avatar_badge).getVisibility());
                });
                screenshot(context, "profile-home.png");
            }
            try (ActivityScenario<StorageDemoActivity> storage = ActivityScenario.launch(StorageDemoActivity.class)) {
                onView(withId(R.id.storage_prefs_write)).perform(scrollTo(), invokeClick());
                onView(isAssignableFrom(android.widget.EditText.class)).perform(replaceText("存储演示同学"), closeSoftKeyboard());
                onView(withId(android.R.id.button1)).perform(invokeClick());
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                onView(withId(R.id.storage_prefs_read)).perform(scrollTo(), invokeClick());
                awaitResult(storage, "SharedPreferences读取成功");
                onView(withId(R.id.storage_text_write)).perform(scrollTo(), invokeClick());
                awaitResult(storage, "UTF-8文本写入成功");
                onView(withId(R.id.storage_text_read)).perform(scrollTo(), invokeClick());
                awaitResult(storage, "UTF-8文本读取成功");
                onView(withId(R.id.storage_xml_write)).perform(scrollTo(), invokeClick());
                awaitResult(storage, "XML写入成功");
                onView(withId(R.id.storage_xml_read)).perform(scrollTo(), invokeClick());
                awaitResult(storage, "XML读取成功");
                onView(withId(R.id.storage_result)).perform(scrollTo()).check(matches(withSubstring("存储演示同学")));
                screenshot(context, "storage-demo.png");
            }
        } finally {
            store.restore(original);
            for (int i = 0; i < files.length; i++) {
                if (prior[i] == null) files[i].delete();
                else Files.write(files[i].toPath(), prior[i]);
            }
        }
    }
    /** Calls the displayed button's real listener, avoiding preview SDK touch/window movement races. */
    private static androidx.test.espresso.ViewAction invokeClick() {
        return new androidx.test.espresso.ViewAction() {
            @Override public org.hamcrest.Matcher<View> getConstraints() {
                return org.hamcrest.Matchers.allOf(isDisplayed(), isEnabled());
            }
            @Override public String getDescription() { return "invoke displayed dialog button click listener"; }
            @Override public void perform(androidx.test.espresso.UiController ui, View view) {
                assertTrue("Button did not handle click", view.performClick());
                ui.loopMainThreadUntilIdle();
            }
        };
    }
    private static void awaitResult(ActivityScenario<StorageDemoActivity> scenario, String expected) throws Exception {
        long until = System.currentTimeMillis() + 5000;
        String[] current = {""};
        while (System.currentTimeMillis() < until) {
            scenario.onActivity(activity -> current[0] = ((TextView) activity.findViewById(R.id.storage_result)).getText().toString());
            if (current[0].startsWith(expected)) return;
            if (current[0].contains("失败")) fail(current[0]);
            Thread.sleep(50);
        }
        fail("Timed out waiting for " + expected + ": " + current[0]);
    }
    private static void screenshot(Context context, String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File directory = new File(context.getExternalFilesDir(null), "lab-evidence");
        assertTrue(directory.exists() || directory.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(directory, name))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { bitmap.recycle(); }
    }
}
