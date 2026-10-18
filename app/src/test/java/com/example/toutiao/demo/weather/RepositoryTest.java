package com.example.toutiao.demo.weather;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.OkHttpClient;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class RepositoryTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private String raw() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/beijing.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[4096]; int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    @Test public void realGetWritesRawThenFallsBackWhenServerUnavailable() throws Exception {
        MockWebServer server = new MockWebServer();
        String json = raw();
        server.enqueue(new MockResponse().setBody(json));
        server.start();
        JsonCache cache = new JsonCache(folder.newFolder());
        WeatherRepository repo = new WeatherRepository(new OkHttpClient.Builder().retryOnConnectionFailure(false)
            .callTimeout(2, TimeUnit.SECONDS).build(), cache,
            server.url("/api/weather/city/").toString());
        try {
            WeatherRepository.Result online = repo.fetch("101010100");
            assertFalse(online.cached); assertEquals(15, online.report.days.size());
            assertEquals(json, cache.read("101010100"));
            okhttp3.mockwebserver.RecordedRequest request = server.takeRequest();
            assertEquals("GET", request.getMethod());
            assertEquals("/api/weather/city/101010100", request.getPath());
        } finally { server.shutdown(); }
        assertTrue(repo.fetch("101010100").cached);
        try { repo.fetch("101020100"); fail("不能把北京缓存用于上海"); }
        catch (IOException expected) { assertNotNull(expected.getMessage()); }
    }
    @Test public void badResponseCannotOverwriteGoodCache() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{\"status\":403}"));
        server.start(); JsonCache cache = new JsonCache(folder.newFolder()); cache.write("101010100", raw());
        try {
            WeatherRepository repo = new WeatherRepository(new OkHttpClient(), cache,
                server.url("/").toString());
            assertTrue(repo.fetch("101010100").cached);
            assertEquals(raw(), cache.read("101010100"));
        } finally { server.shutdown(); }
    }
}
