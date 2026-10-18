package com.example.toutiao.demo;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.anything;

@RunWith(AndroidJUnit4.class)
public class WeatherIntegrationTest {
    @Test public void existingWeatherEntryDefaultsToWuhanAndSupportsCitySearch() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        android.content.SharedPreferences prefs = context.getSharedPreferences("native_weather_v1", 0);
        String previous = prefs.getString("city", null);
        prefs.edit().remove("city").commit();
        try (ActivityScenario<WeatherActivity> scenario = ActivityScenario.launch(new Intent(context, WeatherActivity.class))) {
            AtomicReference<String> city = new AtomicReference<>("");
            for (int i = 0; i < 100; i++) {
                scenario.onActivity(a -> city.set(((TextView)a.findViewById(R.id.weather_city)).getText().toString()));
                if ("101200101".equals(prefs.getString("city", ""))) break;
                Thread.sleep(100);
            }
            assertTrue(city.get(), city.get().contains("武汉"));
            assertEquals("101200101", prefs.getString("city", ""));
            onView(withId(R.id.weather_city_container)).perform(click());
            onView(isAssignableFrom(EditText.class)).perform(replaceText("上海"), closeSoftKeyboard());
            onData(anything()).inAdapterView(isAssignableFrom(ListView.class)).atPosition(0).perform(click());
            scenario.onActivity(a -> {
                assertTrue(((TextView)a.findViewById(R.id.weather_city)).getText().toString().contains("上海"));
                assertEquals("101020100", prefs.getString("city", ""));
                a.findViewById(R.id.weather_back).performClick();
                assertTrue(a.isFinishing());
            });
        } finally {
            if (previous == null) prefs.edit().remove("city").commit();
            else prefs.edit().putString("city", previous).commit();
        }
    }
}
