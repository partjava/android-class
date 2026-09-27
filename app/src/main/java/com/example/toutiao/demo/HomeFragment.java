package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends PageFragment {
    private static final List<News> userCreatedPosts = new ArrayList<>();
    public static void addUserPost(News news) {
        userCreatedPosts.add(0, news);
    }

    private int selectedTab = R.id.tab_recommend;
    private List<News> activeNews;
    private String savedQuery = "";
    @Override public View onCreateView(android.view.LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.fragment_home, parent, false);
    }

    private EditText etSearch;
    private RecyclerView rvNews;
    private ListView lvCraft;

    private List<News> newsList;
    private NewsMultiAdapter newsAdapter;

    private List<News> recommendList;//推荐
    private List<News> campusList;//校园 (对接 Flask 本地服务)
    private List<News> videoSmallList;//小视频
    private List<News> beijingList;//北京
    private List<News> entertainList;//娱乐

    private List<Craftsman> craftsmanList;
    private CraftsmanAdapter craftAdapter;

    private TextView tabRecommend, tabCampus, tabVideoSmall, tabBeijing, tabHot, tabEntertain;
    private TextView tvHomeTitle;
    private View llCampusBanner;
    private TextView tvCampusBannerText, btnCampusSync;
    private View llCampusFooter;
    private ProgressBar pbCampusLoading;
    private TextView tvCampusFooterText;

    private TextView[] allTabs;
    private int campusCurrentPage = 1;
    private int campusTotalPages = 153;
    private boolean isCampusLoadingMore = false;
    private boolean hasMoreCampusNews = true;
    private static final int CAMPUS_PAGE_SIZE = 15; // 与晴川学院官网完全一致：每页15条真实要闻

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindView();
        initNewsData();
        initCraftData();
        bindEvent();
        int restoreTab = savedInstanceState == null ? selectedTab : savedInstanceState.getInt("tab", selectedTab);
        String query = savedInstanceState == null ? savedQuery : savedInstanceState.getString("query", "");
        View targetTab = findViewById(restoreTab);
        if (targetTab != null) {
            targetTab.performClick();
        } else {
            findViewById(R.id.tab_recommend).performClick();
        }
        etSearch.setText(query);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (recommendList != null) {
            refreshRecommend();
        }
    }

    /** 重建推荐列表：发布页回来（用户动态可能新增）、接口头条到达共用这一个入口 */
    private void refreshRecommend() {
        recommendList = buildRecommend();
        if (activeNews == null || selectedTab == R.id.tab_recommend) {
            activeNews = recommendList;
            filterNews(etSearch != null ? etSearch.getText().toString() : "");
        }
    }

    private void bindView() {
        etSearch = findViewById(R.id.et_search);
        rvNews = findViewById(R.id.rv_news);
        lvCraft = findViewById(R.id.lv_craft);

        tvHomeTitle = findViewById(R.id.tv_home_title);
        llCampusBanner = findViewById(R.id.ll_campus_banner);
        tvCampusBannerText = findViewById(R.id.tv_campus_banner_text);
        btnCampusSync = findViewById(R.id.btn_campus_sync);

        llCampusFooter = findViewById(R.id.ll_campus_footer);
        pbCampusLoading = findViewById(R.id.pb_campus_loading);
        tvCampusFooterText = findViewById(R.id.tv_campus_footer_text);

        tabRecommend = findViewById(R.id.tab_recommend);
        tabCampus = findViewById(R.id.tab_campus);
        tabVideoSmall = findViewById(R.id.tab_video_small);
        tabBeijing = findViewById(R.id.tab_beijing);
        tabHot = findViewById(R.id.tab_hot);
        tabEntertain = findViewById(R.id.tab_entertain);

        allTabs = new TextView[]{tabRecommend, tabCampus, tabVideoSmall,
                tabBeijing, tabHot, tabEntertain};
    }

    private void initNewsData(){
        recommendList = buildRecommend();
        campusList = new ArrayList<>(NewsContentStore.campus());
        campusCurrentPage = 1;
        campusTotalPages = 153;
        hasMoreCampusNews = true;
        videoSmallList = buildVideoSmall();
        beijingList = buildBeijing();
        entertainList = buildEntertain();

        newsList = new ArrayList<>(recommendList);
        newsAdapter = new NewsMultiAdapter(newsList);

        rvNews.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNews.setAdapter(newsAdapter);

        //联网拉实时头条（天行数据 TianAPI）。key 未配置时方法直接返回，保持纯本地；
        //成功到达后拼进推荐频道顶部，失败静默回落本地数据，离线演示不受影响
        NewsApiStore.fetchHeadlines(10, new NewsApiStore.Callback() {
            @Override
            public void onSuccess(List<News> items) {
                NewsApiStore.cache(items);
                if (!isAdded()) {
                    return;
                }
                refreshRecommend();
                Toast.makeText(requireContext(),
                        "已加载 " + items.size() + " 条实时头条", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String reason) {
                //静默：保持本地内容，不打断浏览
            }
        });

        newsAdapter.setOnItemClickListener(news -> {
            if (news.getType() == News.TYPE_VIDEO) {
                Intent intent = new Intent(requireContext(), VideoDetailActivity.class);
                intent.putExtra(VideoDetailActivity.EXTRA_TITLE, news.getTitle());

                intent.putExtra(VideoDetailActivity.EXTRA_SOURCE,
                        news.getSource() + "  " + news.getTime());
                intent.putExtra(VideoDetailActivity.EXTRA_DESC,
                        news.getContent() == null ? "" : news.getContent());
                intent.putExtra(VideoDetailActivity.EXTRA_COVER, news.getImg1());

                startActivity(intent);
                return;
            }
            Intent intent = new Intent(requireContext(), NewsDetailActivity.class);
            intent.putExtra("title", news.getTitle());
            intent.putExtra("info", news.getSource() + "  " + news.getTime());
            intent.putExtra("img", news.getImg1());
            intent.putExtra("img2", news.getImg2());
            intent.putExtra("img3", news.getImg3());
            intent.putExtra("type", news.getType());
            String body = news.getContent();
            if (body == null || body.trim().isEmpty()) {
                body = NewsContentStore.contentOf(news.getTitle());
            }
            intent.putExtra("content", body == null ? "" : body);
            intent.putExtra("img_url", news.getImageUrl() == null ? "" : news.getImageUrl());
            intent.putExtra("img_url_2", news.getImageUrl2() == null ? "" : news.getImageUrl2());
            intent.putExtra("img_url_3", news.getImageUrl3() == null ? "" : news.getImageUrl3());
            intent.putExtra("blocks_json", news.getBlocksJson() == null ? "" : news.getBlocksJson());
            startActivity(intent);
        });
    }

    //列表页只组装摘要字段（标题/来源/时间/图片）；站内文章正文统一放在
    //NewsContentStore 里，点进详情页才按标题查询，主页不再一次性加载全部全文
    private List<News> buildRecommend(){
        List<News> list = new ArrayList<>(userCreatedPosts);
        //接口拉回的实时头条排在用户动态之后、本地文章之前；key 未配置时为空列表
        list.addAll(NewsApiStore.cachedHeadlines());
        list.addAll(NewsContentStore.recommend());
        return list;
    }

    private List<News> buildCampus(){
        return NewsContentStore.campus();
    }

    private List<News> buildVideoSmall(){
        return NewsContentStore.videoSmall();
    }

    private List<News> buildBeijing(){
        return NewsContentStore.beijing();
    }

    private List<News> buildEntertain(){
        return NewsContentStore.entertain();
    }
    private void initCraftData(){
        craftsmanList = new ArrayList<>();
        craftsmanList.add(new Craftsman("高凤林",
                "突破极限精度，将“龙的轨迹”划入太空；破解20载难题，让中国繁星映亮苍穹。焊花...",
                "高凤林，航天发动机焊接大师。为长征火箭焊接发动机，30多年，130多枚火箭发动机，零失误。他突破极限精度，将“龙的轨迹”划入太空；破解20载难题，让中国繁星映亮苍穹。",
                R.drawable.image1));
        craftsmanList.add(new Craftsman("李万君",
                "一把焊枪，一双妙手，他以柔情呵护复兴号的筋骨；千度烈焰，万次攻关，他用坚固...",
                "李万君，中车长春轨道客车股份有限公司高级技师，复兴号动车组转向架焊接专家。一把焊枪，一双妙手，他以柔情呵护复兴号的筋骨；千度烈焰，万次攻关，他用坚固的焊缝，为中国高铁护航。",
                R.drawable.image2));
        craftsmanList.add(new Craftsman("夏立",
                "技艺吹影镂尘，擦亮中华“翔龙”之目；组装妙至毫巅，铺就嫦娥奔月星途。当“天马”...",
                "中国电子科技集团公司第五十四研究所高级技师夏立 作为通信天线装配责任人，夏立先后承担了“天马”射电望远镜、远望号、索马里护航军舰、“9·3”阅兵参阅方阵上通信设施等的卫星天线预研与装配、校准任务，装配的齿轮间隙仅有0.004毫米，相当于一根头发丝的1/20粗细。在生产、组装工艺方面，夏立攻克了一个又一个难关，创造了一个又一个奇迹。 所获荣誉：全国技术能手、河北省金牌工人、河北省五一劳动奖章、2016年河北省军工大工匠。",
                R.drawable.image3));
        craftsmanList.add(new Craftsman("王进",
                "平步百米铁塔，横穿超、特高压。在“刀锋”上起舞，守护着岁月通明、灯火万家，...",
                "王进，国家电网特高压带电作业专家。平步百米铁塔，横穿超、特高压。在“刀锋”上起舞，守护着岁月通明、灯火万家。",
                R.drawable.image4));
        craftsmanList.add(new Craftsman("朱恒银",
                "从地表向地心，他让探宝“银针”不断挺进。一腔热血，融进千米厚土；一缕微光，射...",
                "朱恒银，地质钻探高级工程师。从地表向地心，他让探宝“银针”不断挺进。一腔热血，融进千米厚土；一缕微光，射向地层深处。",
                R.drawable.image5));
        craftsmanList.add(new Craftsman("乔素凯",
                "4米长杆，26年，56000步的零失误令人惊叹。是责任，是经验，更是他心里的“安...",
                "乔素凯，核燃料操作大师。4米长杆，26年，56000步的零失误令人惊叹。是责任，是经验，更是他心里的“安全”二字。",
                R.drawable.image6));
        craftsmanList.add(new Craftsman("陈行行",
                "青涩年华化为多彩绽放，精益求精铸就青春信仰。大国重器的加工平台上，他用极致...",
                "陈行行，精密数控加工高级技师。青涩年华化为多彩绽放，精益求精铸就青春信仰。大国重器的加工平台上，他用极致追求，书写青年工匠的担当。",
                R.drawable.image7));
        craftsmanList.add(new Craftsman("王树军",
                "他是维修工，也是设计师，更像是永不屈服的斗士！临危请命，只为国之重器不能受...",
                "王树军，高端装备维修工匠。他是维修工，也是设计师，更像是永不屈服的斗士！临危请命，只为国之重器不能受制于人。",
                R.drawable.image8));

        craftAdapter = new CraftsmanAdapter(craftsmanList);
        lvCraft.setAdapter(craftAdapter);

        lvCraft.setOnItemClickListener((parent, view, position, id) -> {
            Craftsman data = craftsmanList.get(position);
            showCraftDialog(data);
        });
    }

    private void resetTabColor(){
        for (TextView tab : allTabs) {
            tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.tab_inactive));
        }
    }

    private void selectTab(TextView activeTab){
        selectedTab = activeTab.getId();
        resetTabColor();
        activeTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_red));
        boolean isCampus = selectedTab == R.id.tab_campus;
        if (llCampusBanner != null) {
            llCampusBanner.setVisibility(isCampus ? View.VISIBLE : View.GONE);
        }
        if (llCampusFooter != null) {
            llCampusFooter.setVisibility(isCampus ? View.VISIBLE : View.GONE);
            if (isCampus) {
                updateCampusFooterUI("📖 滑动触底自动无限加载 · 已加载 " + campusList.size() + " 条", false);
            }
        }
    }

    private void bindNewsTab(TextView tab, List<News> data){
        tab.setOnClickListener(v -> switchNewsTab(data, tab));
    }

    private void switchNewsTab(List<News> data, TextView activeTab){
        selectTab(activeTab);
        activeNews = data;
        filterNews(etSearch.getText().toString());
        rvNews.scrollToPosition(0);//回到列表顶部

        rvNews.setVisibility(View.VISIBLE);
        lvCraft.setVisibility(View.GONE);
    }

    private void bindEvent() {
        activeNews = recommendList;
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { filterNews(s.toString()); }
            public void afterTextChanged(android.text.Editable s) { }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                String keyword = etSearch.getText().toString().trim();
                if (keyword.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入搜索内容", Toast.LENGTH_SHORT).show();
                } else {
                    filterNews(keyword);

                    InputMethodManager imm =
                            (InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                    }
                }
                return true;
            }
            return false;
        });

        bindNewsTab(tabRecommend, recommendList);

        //点击顶部大标题【仿今日头条】：在校园频道时重新从第1页刷新
        if (tvHomeTitle != null) {
            tvHomeTitle.setOnClickListener(v -> {
                if (selectedTab == R.id.tab_campus) {
                    refreshCampusFirstPage(true);
                } else {
                    tabCampus.performClick();
                }
            });
        }

        //点击【校园】频道，切换列表并自动检查加载
        tabCampus.setOnClickListener(v -> {
            switchNewsTab(campusList, tabCampus);
            if (campusList.isEmpty()) {
                refreshCampusFirstPage(false);
            }
        });

        //点击顶部同步条或按钮：重新从第1页爬取刷新
        if (tvCampusBannerText != null) {
            tvCampusBannerText.setOnClickListener(v -> refreshCampusFirstPage(true));
        }
        if (btnCampusSync != null) {
            btnCampusSync.setOnClickListener(v -> refreshCampusFirstPage(true));
        }

        // 触底手势监听：下拉后面没有了自动刷新，动态接上去，当前页+1
        rvNews.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (selectedTab != R.id.tab_campus || dy <= 0) {
                    return;
                }
                if (isCampusLoadingMore || !hasMoreCampusNews) {
                    return;
                }
                // 检测是否滑动触底（无法继续向下滑动，后面没有了）
                if (!recyclerView.canScrollVertically(1)) {
                    int nextPage = campusCurrentPage + 1;
                    loadMoreCampusNews(nextPage);
                }
            }
        });

        bindNewsTab(tabVideoSmall, videoSmallList);
        bindNewsTab(tabBeijing, beijingList);
        bindNewsTab(tabEntertain, entertainList);

        tabHot.setOnClickListener(v -> {
            selectTab(tabHot);
            filterNews(etSearch.getText().toString());
        });
    }

    /**
     * 触底手势自动无限刷新核心逻辑：
     * 1. 记录当前 pagenumber，下一步请求的 pagenumber = campusCurrentPage + 1
     * 2. 调用 /xiaoyuan/page/<pagenumber>
     * 3. 只要异步获取到内容，就把当前页计算 +1，并动态接到列表后面 (campusList.addAll)
     */
    private void loadMoreCampusNews(final int nextPage) {
        if (isCampusLoadingMore || !hasMoreCampusNews) {
            return;
        }
        isCampusLoadingMore = true;
        updateCampusFooterUI("⏳ 正在自动请求第 " + nextPage + " 页 (/xiaoyuan/page/" + nextPage + ")...", true);

        CampusNewsStore.fetchCampusNews(nextPage, CAMPUS_PAGE_SIZE, new CampusNewsStore.PageCallback() {
            @Override
            public void onSuccess(List<News> items, String queryTime, String school, int currentPage, int pageCount, int newsCount, int perPage, boolean hasMore) {
                if (!isAdded()) {
                    return;
                }
                // 只要异步获取到内容，就把当前页计算 +1
                campusCurrentPage = currentPage;
                campusTotalPages = Math.max(pageCount, 1);
                hasMoreCampusNews = hasMore && (currentPage < pageCount);

                if (items != null && !items.isEmpty()) {
                    // 动态加载到列表里面，自动接上去，不用切换页！
                    campusList.addAll(items);
                    if (selectedTab == R.id.tab_campus) {
                        activeNews = campusList;
                        filterNews(etSearch != null ? etSearch.getText().toString() : "");
                    }
                }

                isCampusLoadingMore = false;
                if (!hasMoreCampusNews) {
                    updateCampusFooterUI("🎉 已加载全部 " + campusList.size() + " 条校园要闻", false);
                } else {
                    updateCampusFooterUI("📖 已加载至第 " + campusCurrentPage + " 页 (累计 " + campusList.size() + " 条) · 下滑自动加载更多", false);
                }
            }

            @Override
            public void onFailure(String reason) {
                if (!isAdded()) {
                    return;
                }
                isCampusLoadingMore = false;
                updateCampusFooterUI("❌ 第 " + nextPage + " 页加载失败: " + reason + "，下滑可重试", false);
            }
        });
    }

    /**
     * 重新从第 1 页拉取刷新
     */
    private void refreshCampusFirstPage(final boolean userInitiated) {
        if (!isAdded() || isCampusLoadingMore) {
            return;
        }
        isCampusLoadingMore = true;
        campusCurrentPage = 1;
        hasMoreCampusNews = true;
        if (btnCampusSync != null) {
            btnCampusSync.setEnabled(false);
            btnCampusSync.setText("爬取中...");
        }
        updateCampusFooterUI("⏳ 正在请求第 1 页 (/xiaoyuan/page/1)...", true);
        if (userInitiated) {
            Toast.makeText(requireContext(), "正在从第 1 页重新爬取刷新...", Toast.LENGTH_SHORT).show();
        }

        CampusNewsStore.fetchCampusNews(1, CAMPUS_PAGE_SIZE, new CampusNewsStore.PageCallback() {
            @Override
            public void onSuccess(List<News> items, String queryTime, String school, int currentPage, int pageCount, int newsCount, int perPage, boolean hasMore) {
                if (!isAdded()) {
                    return;
                }
                campusCurrentPage = currentPage;
                campusTotalPages = Math.max(pageCount, 1);
                hasMoreCampusNews = hasMore && (currentPage < pageCount);

                campusList.clear();
                if (items != null && !items.isEmpty()) {
                    campusList.addAll(items);
                }
                if (selectedTab == R.id.tab_campus) {
                    activeNews = campusList;
                    filterNews(etSearch != null ? etSearch.getText().toString() : "");
                    rvNews.scrollToPosition(0);
                }

                if (btnCampusSync != null) {
                    btnCampusSync.setEnabled(true);
                    btnCampusSync.setText("🔄 刷新第1页");
                }
                if (tvCampusBannerText != null) {
                    tvCampusBannerText.setText("🏫 " + school + " · 触底自动无限加载 · " + queryTime);
                }

                isCampusLoadingMore = false;
                updateCampusFooterUI("📖 已加载第 1 页 (共 " + campusList.size() + " 条) · 下滑触底自动加载更多", false);
                if (userInitiated) {
                    Toast.makeText(requireContext(), "第 1 页刷新成功！已加载 " + campusList.size() + " 条", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String reason) {
                if (!isAdded()) {
                    return;
                }
                isCampusLoadingMore = false;
                if (btnCampusSync != null) {
                    btnCampusSync.setEnabled(true);
                    btnCampusSync.setText("🔄 刷新第1页");
                }
                updateCampusFooterUI("❌ 刷新失败: " + reason, false);
                if (userInitiated) {
                    Toast.makeText(requireContext(), "提示：" + reason, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateCampusFooterUI(String text, boolean isLoading) {
        if (pbCampusLoading != null) {
            pbCampusLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (tvCampusFooterText != null) {
            tvCampusFooterText.setText(text);
        }
    }

    private void filterNews(String query) {
        savedQuery = query;
        String key = query.trim().toLowerCase(java.util.Locale.ROOT);
        TextView empty = findViewById(R.id.tv_search_empty);
        boolean hot = selectedTab == R.id.tab_hot;
        rvNews.setVisibility(hot ? View.GONE : View.VISIBLE);
        lvCraft.setVisibility(hot ? View.VISIBLE : View.GONE);
        if (hot) {
            List<Craftsman> result = new ArrayList<>();
            for (Craftsman item : craftsmanList) if ((item.getName()+item.getDetailContent()).contains(key)) result.add(item);
            craftAdapter = new CraftsmanAdapter(result);
            lvCraft.setAdapter(craftAdapter);
            lvCraft.setOnItemClickListener((p,v,pos,id) -> showCraftDialog(result.get(pos)));
            empty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            newsList.clear();
            if (activeNews != null) for (News item : activeNews)
                if ((item.getTitle()+item.getSource()).toLowerCase(java.util.Locale.ROOT).contains(key)) newsList.add(item);
            newsAdapter.notifyDataSetChanged();
            empty.setVisibility(newsList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override public void onSaveInstanceState(Bundle out) {
        out.putInt("tab",selectedTab); out.putString("query",savedQuery);
        super.onSaveInstanceState(out);
    }

    private void showCraftDialog(Craftsman craftsman){
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_craftsman_detail,null);

        TextView tvName = dialogView.findViewById(R.id.tv_detail_name);
        TextView tvTopDesc = dialogView.findViewById(R.id.tv_detail_top_desc);
        ImageView ivImg = dialogView.findViewById(R.id.iv_detail_img);
        TextView tvLongText = dialogView.findViewById(R.id.tv_detail_long);
        TextView tvBtn = dialogView.findViewById(R.id.tv_btn_ok);

        tvName.setText(craftsman.getName());
        tvTopDesc.setText(craftsman.getShortDesc());
        ivImg.setImageResource(craftsman.getImgRes());
        tvLongText.setText(craftsman.getDetailContent());

        AlertDialog dialog = builder.setView(dialogView).create();
        tvBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public static class Craftsman {
        private String name;
        private String shortDesc;
        private String detailContent;
        private int imgRes;

        public Craftsman(String name, String shortDesc, String detailContent, int imgRes) {
            this.name = name;
            this.shortDesc = shortDesc;
            this.detailContent = detailContent;
            this.imgRes = imgRes;
        }

        public String getName() {
            return name;
        }
        public String getShortDesc() {
            return shortDesc;
        }
        public String getDetailContent() {
            return detailContent;
        }
        public int getImgRes() {
            return imgRes;
        }
    }

    class CraftsmanAdapter extends ArrayAdapter<Craftsman> {
        public CraftsmanAdapter(List<Craftsman> list) {
            super(requireContext(),0,list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Craftsman item = getItem(position);
            ViewHolderCraft holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_craftsman, parent, false);
                holder = new ViewHolderCraft();
                holder.iv = convertView.findViewById(R.id.iv_craft_img);
                holder.tvName = convertView.findViewById(R.id.tv_craft_name);
                holder.tvShort = convertView.findViewById(R.id.tv_craft_short);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolderCraft) convertView.getTag();
            }
            holder.iv.setImageResource(item.getImgRes());
            holder.tvName.setText(item.getName());
            holder.tvShort.setText(item.getShortDesc());
            return convertView;
        }

        class ViewHolderCraft {
            ImageView iv;
            TextView tvName;
            TextView tvShort;
        }
    }
}
