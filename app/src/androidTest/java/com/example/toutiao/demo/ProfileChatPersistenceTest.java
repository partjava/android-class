package com.example.toutiao.demo;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ProfileChatPersistenceTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    @Test public void profileSurvivesNewStoreAndRejectsBlankNickname() {
        ProfileStore store = new ProfileStore(context);
        String old = store.get("nickname");
        try {
            store.set("nickname", "  测试昵称  ");
            assertEquals("测试昵称", new ProfileStore(context).get("nickname"));
            try { store.set("nickname", "  "); fail("blank nickname accepted"); }
            catch (IllegalArgumentException expected) { }
            assertEquals("测试昵称", store.get("nickname"));
        } finally { store.set("nickname", old); }
    }
    @Test public void conversationsPersistSeparatelyAndReadingClearsOnlyOwnUnread() {
        ChatStore store = new ChatStore(context, "chat_test");
        context.getSharedPreferences("chat_test", 0).edit().clear().commit();
        store.append("甲", new ChatMessage("甲的消息", false));
        store.append("乙", new ChatMessage("乙的消息", false));
        ChatStore reopened = new ChatStore(context, "chat_test");
        assertEquals("甲的消息", reopened.messages("甲").get(0).getText());
        assertEquals(1, reopened.unread("甲"));
        reopened.markRead("甲");
        assertEquals(0, reopened.unread("甲"));
        assertEquals(1, reopened.unread("乙"));
        assertEquals("乙的消息", reopened.preview("乙"));
        reopened.append("甲", new ChatMessage("  ", true));
        assertEquals(1, reopened.messages("甲").size());
        context.getSharedPreferences("chat_test", 0).edit().clear().commit();
    }
}
