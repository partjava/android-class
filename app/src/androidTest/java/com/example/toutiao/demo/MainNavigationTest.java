package com.example.toutiao.demo;

import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MainNavigationTest {
    @Test public void mainPagesUseFragmentsAndRestoreSelection() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), HomeActivity.class);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                activity.getSupportFragmentManager().executePendingTransactions();
                assertFalse("主页面必须由 Fragment 承载", activity.getSupportFragmentManager().getFragments().isEmpty());
                BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
                nav.setSelectedItemId(R.id.nav_video);
                activity.getSupportFragmentManager().executePendingTransactions();
                assertEquals(R.id.nav_video, nav.getSelectedItemId());
                nav.setSelectedItemId(R.id.nav_shop);
                activity.getSupportFragmentManager().executePendingTransactions();
                activity.onBackPressed();
                activity.getSupportFragmentManager().executePendingTransactions();
                assertEquals(R.id.nav_video, nav.getSelectedItemId());
                long visible = activity.getSupportFragmentManager().getFragments().stream()
                        .filter(f -> f.isAdded() && !f.isHidden()).count();
                assertEquals(1L, visible);
            });
            scenario.recreate();
            scenario.onActivity(activity -> assertEquals(R.id.nav_video,
                    ((BottomNavigationView) activity.findViewById(R.id.bottom_nav)).getSelectedItemId()));
        }
    }

    @Test public void mineTabAllFeaturesAreClickable() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), HomeActivity.class);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                activity.showPage(R.id.nav_mine, false);
                activity.getSupportFragmentManager().executePendingTransactions();

                int[] clickableIds = new int[]{
                    R.id.btn_publish,
                    R.id.iv_setting,
                    R.id.iv_friend,
                    R.id.iv_msg,
                    R.id.iv_avatar,
                    R.id.tv_profile_name,
                    R.id.iv_edit_name,
                    R.id.tv_profile_intro,
                    R.id.tv_apply_auth,
                    R.id.tv_stat_follow,
                    R.id.tv_stat_fans,
                    R.id.tv_stat_likes,
                    R.id.tv_tag_ip,
                    R.id.tv_tag_edit,
                    R.id.tv_all_func,
                    R.id.ll_msg_private,
                    R.id.ll_history,
                    R.id.ll_create,
                    R.id.ll_book,
                    R.id.ll_shop,
                    R.id.ll_collect,
                    R.id.ll_service,
                    R.id.ll_refund,
                    R.id.tv_chatroom_title,
                    R.id.tv_chatroom_more,
                    R.id.ll_chatroom_card,
                    R.id.btn_go_comment,
                    R.id.tv_works_title,
                    R.id.tv_collect_tab,
                    R.id.tv_like_tab,
                    R.id.iv_work_search,
                    R.id.btn_all_work,
                    R.id.btn_video_work,
                    R.id.btn_micro_work,
                    R.id.tv_works_count
                };

                for (int id : clickableIds) {
                    android.view.View v = activity.findViewById(id);
                    assertNotNull("View should exist: " + activity.getResources().getResourceEntryName(id), v);
                    assertTrue("View should have onClickListener set: " + activity.getResources().getResourceEntryName(id), v.hasOnClickListeners());
                }
            });
        }
    }
}
