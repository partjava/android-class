package com.example.toutiao.demo;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageDemoActivity extends AppCompatActivity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private ProfileStore store;
    private ProfileBackup backup;
    private TextView result;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_storage_demo);
        store = new ProfileStore(this); backup = new ProfileBackup(getFilesDir());
        result = findViewById(R.id.storage_result);
        findViewById(R.id.storage_back).setOnClickListener(v -> finish());
        findViewById(R.id.storage_prefs_read).setOnClickListener(v -> run("SharedPreferences读取", () -> display(store.snapshot())));
        findViewById(R.id.storage_prefs_write).setOnClickListener(v -> {
            android.widget.EditText input = new android.widget.EditText(this); input.setText(store.get("nickname"));
            input.setSingleLine(true); input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle("写入昵称到SharedPreferences").setView(input)
                    .setPositiveButton("保存", null).setNegativeButton("取消", null).create();
            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                try { store.set("nickname", input.getText().toString()); dialog.dismiss(); result.setText("SharedPreferences写入成功\n" + display(store.snapshot())); }
                catch (IllegalArgumentException e) { input.setError(e.getMessage()); }
            })); dialog.show();
        });
        findViewById(R.id.storage_text_write).setOnClickListener(v -> run("UTF-8文本写入", () -> { Map<String,String> data=store.snapshot(); backup.writeText(data); return "文件："+ProfileBackup.TEXT_FILE+"\n"+display(data); }));
        findViewById(R.id.storage_text_read).setOnClickListener(v -> run("UTF-8文本读取", () -> display(backup.readText())));
        findViewById(R.id.storage_xml_write).setOnClickListener(v -> run("XML写入", () -> { Map<String,String> data=store.snapshot(); backup.writeXml(data); return "文件："+ProfileBackup.XML_FILE+"\n"+display(data); }));
        findViewById(R.id.storage_xml_read).setOnClickListener(v -> run("XML读取", () -> display(backup.readXml())));
        findViewById(R.id.storage_text_restore).setOnClickListener(v -> confirmRestore(false));
        findViewById(R.id.storage_xml_restore).setOnClickListener(v -> confirmRestore(true));
        findViewById(R.id.storage_sqlite_read).setOnClickListener(v -> run("SQLite读取", () -> new DemoDatabase(this).summary()));
        result.setText("主资料：SharedPreferences / profile\n备份目录："+getFilesDir()+"\n读取仅展示备份，明确恢复才更新个人资料。\n"+display(store.snapshot()));
    }
    private void confirmRestore(boolean xml) {
        new AlertDialog.Builder(this).setTitle("恢复个人资料")
                .setMessage("将用"+(xml?"XML":"UTF-8文本")+"备份覆盖个人资料。先完整校验备份，成功后一次性写入；钱包与其他设置保留。")
                .setPositiveButton("恢复", (d,w) -> run("恢复", () -> { Map<String,String> data=xml?backup.readXml():backup.readText(); store.restore(data); return display(store.snapshot()); }))
                .setNegativeButton("取消",null).show();
    }
    private interface Work { String run() throws Exception; }
    private void run(String title, Work work) {
        result.setText(title+"处理中…");
        io.execute(() -> {
            String message;
            try { message=title+"成功\n"+work.run(); } catch(Exception e) { message=title+"失败："+e.getMessage(); }
            final String text=message;
            runOnUiThread(() -> { if(!isFinishing()&&!isDestroyed()) result.setText(text); });
        });
    }
    private static String display(Map<String,String> data) {
        StringBuilder out=new StringBuilder(); for(Map.Entry<String,String> field:data.entrySet()) out.append(field.getKey()).append("：").append(field.getValue()).append('\n'); return out.toString();
    }
    @Override protected void onDestroy() { io.shutdown(); super.onDestroy(); }
}
