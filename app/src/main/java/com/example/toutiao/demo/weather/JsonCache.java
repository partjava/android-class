package com.example.toutiao.demo.weather;

import com.example.toutiao.demo.R;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** One raw UTF-8 response per city. Never stores UI/model serialization. */
public final class JsonCache {
    private final File directory;
    public JsonCache(File directory) { this.directory = directory; }
    private File file(String city) throws IOException {
        if (city == null || !city.matches("\\d{9}")) throw new IOException("城市编码无效");
        return new File(directory, city + ".json");
    }
    public synchronized void write(String city, String raw) throws IOException {
        File target = file(city);
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("无法创建天气缓存目录");
        File temporary = File.createTempFile(city, ".tmp", directory);
        try {
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(raw.getBytes(StandardCharsets.UTF_8)); output.getFD().sync();
            }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } finally { if (temporary.exists()) temporary.delete(); }
    }
    public synchronized String read(String city) throws IOException {
        File path = file(city);
        return path.isFile() ? new String(Files.readAllBytes(path.toPath()), StandardCharsets.UTF_8) : null;
    }
    public synchronized long savedAt(String city) throws IOException { return file(city).lastModified(); }
}
