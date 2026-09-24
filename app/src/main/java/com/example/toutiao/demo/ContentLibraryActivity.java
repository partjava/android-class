package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 创作与内容中心：管理「我的作品」、「我的收藏」与「浏览历史」。
 */
public class ContentLibraryActivity extends AppCompatActivity {
    private String currentKind = "posts";

    private ImageView ivBack;
    private TextView tvTitle;
    private TextView tvCompose;

    private TextView tabPosts, tabSaved, tabHistory;
    private ListView lvContent;
    private LinearLayout llEmpty;
    private TextView tvEmptyHint;

    private ContentAdapter adapter;
    private final java.util.List<JSONObject> dataList = new java.util.ArrayList<>();

    public static void open(Context context, String kind) {
        context.startActivity(new Intent(context, ContentLibraryActivity.class).putExtra("kind", kind));
    }

    public static void compose(Context context) {
        PublishDialog.show(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_library);

        String extraKind = getIntent().getStringExtra("kind");
        if (extraKind != null && !extraKind.isEmpty()) {
            currentKind = extraKind;
        }

        initViews();
        bindEvents();
        switchKind(currentKind);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_library_back);
        tvTitle = findViewById(R.id.tv_library_title);
        tvCompose = findViewById(R.id.tv_library_compose);

        tabPosts = findViewById(R.id.tab_lib_posts);
        tabSaved = findViewById(R.id.tab_lib_saved);
        tabHistory = findViewById(R.id.tab_lib_history);

        lvContent = findViewById(R.id.lv_library_content);
        llEmpty = findViewById(R.id.ll_library_empty);
        tvEmptyHint = findViewById(R.id.tv_empty_hint);

        adapter = new ContentAdapter();
        lvContent.setAdapter(adapter);
    }

    private void bindEvents() {
        ivBack.setOnClickListener(v -> finish());
        tvCompose.setOnClickListener(v -> PublishDialog.show(this));

        tabPosts.setOnClickListener(v -> switchKind("posts"));
        tabSaved.setOnClickListener(v -> switchKind("saved"));
        tabHistory.setOnClickListener(v -> switchKind("history"));

        lvContent.setOnItemClickListener((parent, view, position, id) -> {
            JSONObject item = dataList.get(position);
            Intent intent = new Intent(this, NewsDetailActivity.class);
            intent.putExtra("title", item.optString("title"));
            intent.putExtra("info", item.optString("info"));
            intent.putExtra("content", item.optString("content"));
            intent.putExtra("img", item.optInt("img", 0));
            intent.putExtra("type", item.optInt("type", News.TYPE_TEXT));
            startActivity(intent);
        });
    }

    private void switchKind(String kind) {
        this.currentKind = kind;

        int activeColor = ContextCompat.getColor(this, R.color.brand_red);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        tabPosts.setTextColor("posts".equals(kind) ? activeColor : inactiveColor);
        tabSaved.setTextColor("saved".equals(kind) ? activeColor : inactiveColor);
        tabHistory.setTextColor("history".equals(kind) ? activeColor : inactiveColor);

        if ("posts".equals(kind)) {
            tvTitle.setText("我的作品");
            tvEmptyHint.setText("您还没有发布过作品，快点击右上角发布微头条或文章吧！");
        } else if ("saved".equals(kind)) {
            tvTitle.setText("我的收藏");
            tvEmptyHint.setText("暂无收藏文章，去首页阅读并收藏精彩资讯吧！");
        } else {
            tvTitle.setText("浏览历史");
            tvEmptyHint.setText("暂无浏览历史记录，阅读新闻后会自动在此归档。");
        }

        loadData();
    }

    private void loadData() {
        dataList.clear();
        ContentStore store = new ContentStore(this);
        JSONArray rows = store.list(currentKind);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject obj = rows.optJSONObject(i);
            if (obj != null) {
                dataList.add(obj);
            }
        }
        adapter.notifyDataSetChanged();

        boolean isEmpty = dataList.isEmpty();
        llEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        lvContent.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void confirmDelete(JSONObject item) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除这条记录吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    new ContentStore(this).remove(currentKind, item.optString("title"));
                    loadData();
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private class ContentAdapter extends BaseAdapter {
        @Override public int getCount() { return dataList.size(); }
        @Override public Object getItem(int position) { return dataList.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(ContentLibraryActivity.this)
                        .inflate(R.layout.item_content_library, parent, false);
                holder = new ViewHolder();
                holder.tvTitle = convertView.findViewById(R.id.tv_item_title);
                holder.tvSnippet = convertView.findViewById(R.id.tv_item_content_snippet);
                holder.tvInfo = convertView.findViewById(R.id.tv_item_info);
                holder.tvDelete = convertView.findViewById(R.id.tv_item_delete);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            JSONObject item = dataList.get(position);
            holder.tvTitle.setText(item.optString("title"));

            String content = item.optString("content").replace("\n", " ").trim();
            holder.tvSnippet.setText(content);
            holder.tvInfo.setText(item.optString("info"));

            holder.tvDelete.setOnClickListener(v -> confirmDelete(item));

            return convertView;
        }

        class ViewHolder {
            TextView tvTitle, tvSnippet, tvInfo, tvDelete;
        }
    }
}
