package com.example.toutiao.demo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;

/** 一套导航和四个 Fragment；切换只更新内容区，不创建新的主 Activity。 */
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView navigation;
    private int selected = R.id.nav_home;
    private ArrayList<Integer> history = new ArrayList<>();

    protected int initialPage() { return R.id.nav_home; }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_main);
        navigation = findViewById(R.id.bottom_nav);
        if (state != null) {
            selected = state.getInt("selected", R.id.nav_home);
            ArrayList<Integer> saved = state.getIntegerArrayList("history");
            if (saved != null) history = saved;
        } else selected = initialPage();
        navigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_add) {
                ContentLibraryActivity.compose(this);
                return false;
            }
            showPage(item.getItemId(), true);
            return true;
        });
        showPage(selected, false);
    }

    public void showPage(int id, boolean remember) {
        if (getSupportFragmentManager().isStateSaved()) return;
        if (id != R.id.nav_home && id != R.id.nav_video && id != R.id.nav_shop && id != R.id.nav_mine) return;
        if (remember && selected != id) {
            history.add(selected);
            if (history.size() > 30) history.remove(0);
        }
        String tag = "page:" + id;
        Fragment target = getSupportFragmentManager().findFragmentByTag(tag);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment.isAdded() && fragment != target) {
                transaction.hide(fragment).setMaxLifecycle(fragment, Lifecycle.State.STARTED);
            }
        }
        if (target == null) {
            target = id == R.id.nav_video ? new VideoFragment() : id == R.id.nav_shop ? new ShopFragment()
                    : id == R.id.nav_mine ? new MineFragment() : new HomeFragment();
            transaction.add(R.id.main_content, target, tag);
        }
        transaction.show(target).setMaxLifecycle(target, Lifecycle.State.RESUMED);
        transaction.setPrimaryNavigationFragment(target).commitNow();
        selected = id;
        navigation.getMenu().findItem(id).setChecked(true);
        getWindow().setStatusBarColor(ContextCompat.getColor(this,
                id == R.id.nav_video ? R.color.bg_player : R.color.brand_red));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
    }

    @Override public void onBackPressed() {
        if (!history.isEmpty()) showPage(history.remove(history.size() - 1), false);
        else if (selected != R.id.nav_home) showPage(R.id.nav_home, false);
        else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("selected", selected);
        out.putIntegerArrayList("history", history);
        super.onSaveInstanceState(out);
    }
}
