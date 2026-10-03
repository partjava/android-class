package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import android.graphics.Bitmap;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends PageFragment {
    private static final List<News> userCreatedPosts = new ArrayList<>();
    public static void addUserPost(News news) {
        userCreatedPosts.add(0, news);
    }

    private String selectedChannelId = "recommend";
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

    private List<Craftsman> craftsmanList;
    private CraftsmanAdapter craftAdapter;

    private HorizontalScrollView hsvTabBar;
    private LinearLayout llTabContainer;
    private View btnChannelManage;
    private List<Channel> myChannels;

    private TextView tvHomeTitle;
    private View llCampusBanner;
    private TextView tvCampusBannerText, btnCampusSync;
    private View llCampusFooter;
    private ProgressBar pbCampusLoading;
    private TextView tvCampusFooterText;

    private WebView wvSchoolPage;
    private ProgressBar pbSchoolLoading;

    // 官网5大实时爬虫新闻栏目状态模型
    public static class CrawlerState {
        public final String id;
        public final String catParam;
        public final String name;
        public final String officialSection;
        public final List<News> list = new ArrayList<>();
        public int currentPage = 1;
        public int totalPages = 153;
        public boolean hasMore = true;
        public boolean isLoading = false;

        public CrawlerState(String id, String catParam, String name, String officialSection) {
            this.id = id;
            this.catParam = catParam;
            this.name = name;
            this.officialSection = officialSection;
        }
    }

    private final Map<String, CrawlerState> crawlerStates = new HashMap<>();

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

        String restoreChannelId = savedInstanceState == null ? selectedChannelId : savedInstanceState.getString("channel_id", selectedChannelId);
        String query = savedInstanceState == null ? savedQuery : savedInstanceState.getString("query", "");

        buildChannelTabs();
        selectChannelById(restoreChannelId);

        if (etSearch != null) {
            etSearch.setText(query);
        }
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
        if (activeNews == null || "recommend".equals(selectedChannelId)) {
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

        wvSchoolPage = findViewById(R.id.wv_school_page);
        pbSchoolLoading = findViewById(R.id.pb_school_loading);
        initSchoolWebView();

        hsvTabBar = findViewById(R.id.hsv_tab_bar);
        llTabContainer = findViewById(R.id.ll_tab_container);
        btnChannelManage = findViewById(R.id.btn_channel_manage);

        View btnGuide = findViewById(R.id.btn_home_campus_guide);
        if (btnGuide != null) {
            btnGuide.setOnClickListener(v -> CampusGuideActivity.start(requireContext()));
        }

        android.widget.ViewFlipper vfBroadcast = findViewById(R.id.vf_campus_broadcast);
        if (vfBroadcast != null) {
            vfBroadcast.setOnClickListener(v -> showCampusNoticeDialog(vfBroadcast.getDisplayedChild()));
        }

        View llBroadcastBar = findViewById(R.id.ll_broadcast_bar);
        if (llBroadcastBar != null) {
            setupBroadcastSwipeToDismiss(llBroadcastBar, vfBroadcast);
        }
    }

    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    private void initSchoolWebView() {
        if (wvSchoolPage == null) return;
        WebSettings ws = wvSchoolPage.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        wvSchoolPage.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    return handleSchoolUrl(view, request.getUrl().toString());
                }
                return false;
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleSchoolUrl(view, url);
            }

            private boolean handleSchoolUrl(WebView view, String url) {
                if (url != null) {
                    if (url.startsWith("http://10.0.2.2:5000/school_page/") || url.startsWith("/school_page/")) {
                        String full = url.startsWith("/") ? ("http://10.0.2.2:5000" + url) : url;
                        view.loadUrl(full);
                        return true;
                    }
                    if (url.startsWith("https://www.qcuwh.cn/") || url.startsWith("http://www.qcuwh.cn/")) {
                        view.loadUrl("http://10.0.2.2:5000/school_page/" + selectedChannelId + "?url=" + android.net.Uri.encode(url));
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (pbSchoolLoading != null) pbSchoolLoading.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (pbSchoolLoading != null) pbSchoolLoading.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (pbSchoolLoading != null) pbSchoolLoading.setVisibility(View.GONE);
            }
        });
    }

    private boolean isSchoolChannel(String channelId) {
        if (channelId == null) return false;
        return "home".equals(channelId) || "survey".equals(channelId) || "org".equals(channelId) || "talent".equals(channelId)
                || "faculty".equals(channelId) || "research".equals(channelId) || "admissions".equals(channelId)
                || "party".equals(channelId) || "student".equals(channelId) || "culture".equals(channelId)
                || "service".equals(channelId) || "library".equals(channelId) || "hr".equals(channelId)
                || "openinfo".equals(channelId) || "mailbox".equals(channelId) || "contact".equals(channelId);
    }

    private boolean isCrawlerChannel(String channelId) {
        return channelId != null && crawlerStates.containsKey(channelId);
    }

    private void initCrawlerStates() {
        crawlerStates.put("campus", new CrawlerState("campus", "xxyw", "校园要闻", "聚焦晴川 · 学校要闻"));
        crawlerStates.put("tzgg", new CrawlerState("tzgg", "tzgg", "通知公告", "聚焦晴川 · 通知公告"));
        crawlerStates.put("jyjx", new CrawlerState("jyjx", "jyjx", "教育教学", "聚焦晴川 · 教育教学"));
        crawlerStates.put("mtgz", new CrawlerState("mtgz", "mtgz", "媒体关注", "聚焦晴川 · 媒体关注"));
        crawlerStates.put("xxry", new CrawlerState("xxry", "xxry", "学校荣誉", "特色晴川 · 学校荣誉"));
    }

    private void initNewsData(){
        initCrawlerStates();
        recommendList = buildRecommend();
        CrawlerState campusState = crawlerStates.get("campus");
        if (campusState != null) {
            campusState.list.addAll(NewsContentStore.campus());
            campusList = campusState.list;
        } else {
            campusList = new ArrayList<>(NewsContentStore.campus());
        }
        campusCurrentPage = 1;
        campusTotalPages = 153;
        hasMoreCampusNews = true;

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
            intent.putExtra("link", news.getLinkUrl() == null ? "" : news.getLinkUrl());
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

    private void buildChannelTabs() {
        if (llTabContainer == null) return;
        llTabContainer.removeAllViews();
        myChannels = ChannelStore.getMyChannels(requireContext());
        if (myChannels == null || myChannels.isEmpty()) {
            myChannels = ChannelStore.getDefaultMyChannels();
        }

        int paddingH = dpToPx(14);
        for (int i = 0; i < myChannels.size(); i++) {
            Channel channel = myChannels.get(i);
            TextView tabView = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            tabView.setLayoutParams(lp);
            tabView.setGravity(android.view.Gravity.CENTER);
            tabView.setPadding(paddingH, 0, paddingH, 0);
            tabView.setText(channel.getName());
            tabView.setTextSize(16);
            tabView.setTag(channel.getId());

            boolean isSelected = channel.getId().equals(selectedChannelId);
            tabView.setTextColor(ContextCompat.getColor(requireContext(),
                    isSelected ? R.color.brand_red : R.color.tab_inactive));
            tabView.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

            tabView.setOnClickListener(v -> selectChannel(channel));
            llTabContainer.addView(tabView);
        }
    }

    private void selectChannelById(String channelId) {
        if (myChannels == null || myChannels.isEmpty()) {
            myChannels = ChannelStore.getMyChannels(requireContext());
        }
        Channel target = null;
        for (Channel c : myChannels) {
            if (c.getId().equals(channelId)) {
                target = c;
                break;
            }
        }
        if (target == null && !myChannels.isEmpty()) {
            target = myChannels.get(0);
        }
        if (target != null) {
            selectChannel(target);
        }
    }

    private void selectChannel(Channel channel) {
        if (channel == null) return;
        selectedChannelId = channel.getId();

        // 更新顶部Tab高亮与字体样式，并将激活Tab平滑滑动至可见区域
        if (llTabContainer != null) {
            for (int i = 0; i < llTabContainer.getChildCount(); i++) {
                View child = llTabContainer.getChildAt(i);
                if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    boolean isMatch = channel.getId().equals(tv.getTag());
                    tv.setTextColor(ContextCompat.getColor(requireContext(),
                            isMatch ? R.color.brand_red : R.color.tab_inactive));
                    tv.setTypeface(null, isMatch ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

                    if (isMatch && hsvTabBar != null) {
                        int scrollX = child.getLeft() - dpToPx(30);
                        hsvTabBar.smoothScrollTo(Math.max(0, scrollX), 0);
                    }
                }
            }
        }

        // 晴川学院官网频道：直接在 App 内呈现学校官网原汁原味真实手机移动版网页
        if (isSchoolChannel(channel.getId())) {
            if (llCampusBanner != null) llCampusBanner.setVisibility(View.GONE);
            if (llCampusFooter != null) llCampusFooter.setVisibility(View.GONE);
            rvNews.setVisibility(View.GONE);
            lvCraft.setVisibility(View.GONE);
            if (wvSchoolPage != null) {
                wvSchoolPage.setVisibility(View.VISIBLE);
                String schoolPageUrl = "http://10.0.2.2:5000/school_page/" + channel.getId();
                wvSchoolPage.loadUrl(schoolPageUrl);
            }
            TextView empty = findViewById(R.id.tv_search_empty);
            if (empty != null) empty.setVisibility(View.GONE);
            return;
        }

        // 非官网原生网页频道时，隐藏学校网页 WebView
        if (wvSchoolPage != null) {
            wvSchoolPage.setVisibility(View.GONE);
        }

        // 5大实时爬虫新闻栏目（校园要闻、通知公告、教育教学、媒体关注、学校荣誉）：展示自动无限加载横幅与底栏状态
        boolean isCrawler = isCrawlerChannel(channel.getId());
        if (llCampusBanner != null) {
            llCampusBanner.setVisibility(isCrawler ? View.VISIBLE : View.GONE);
        }
        if (llCampusFooter != null) {
            llCampusFooter.setVisibility(isCrawler ? View.VISIBLE : View.GONE);
        }

        // 热点栏目展示工匠列表，其他栏目展示图文新闻资讯
        boolean isHot = "hot".equals(channel.getId());
        if (isHot) {
            rvNews.setVisibility(View.GONE);
            lvCraft.setVisibility(View.VISIBLE);
            filterNews(etSearch != null ? etSearch.getText().toString() : "");
        } else {
            rvNews.setVisibility(View.VISIBLE);
            lvCraft.setVisibility(View.GONE);

            if ("recommend".equals(channel.getId())) {
                activeNews = recommendList;
            } else if (isCrawler) {
                CrawlerState state = crawlerStates.get(channel.getId());
                if (state != null) {
                    if (tvCampusBannerText != null) {
                        tvCampusBannerText.setText("🏫 武汉晴川学院 · " + state.officialSection + " (实时同步)");
                    }
                    activeNews = state.list;
                    if (state.list.isEmpty()) {
                        refreshCrawlerFirstPage(channel.getId(), false);
                    } else {
                        updateCampusFooterUI("📖 滑动触底自动无限加载 · 已加载 " + state.list.size() + " 条", false);
                    }
                }
            } else {
                activeNews = NewsContentStore.getNewsByChannel(channel.getId());
            }

            filterNews(etSearch != null ? etSearch.getText().toString() : "");
            rvNews.scrollToPosition(0);
        }
    }

    private int dpToPx(int dp) {
        if (!isAdded()) return dp * 2;
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
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

        // 汉堡菜单：打开频道管理/编辑弹窗
        if (btnChannelManage != null) {
            btnChannelManage.setOnClickListener(v -> {
                ChannelManagerDialog.show(requireContext(), selectedChannelId, new ChannelManagerDialog.OnChannelChangeListener() {
                    @Override
                    public void onChannelSelected(Channel channel) {
                        selectChannel(channel);
                    }

                    @Override
                    public void onChannelsUpdated(List<Channel> updatedChannels) {
                        myChannels = updatedChannels;
                        buildChannelTabs();
                        selectChannelById(selectedChannelId);
                    }
                });
            });
        }

        // 点击顶部大标题【仿今日头条】：在爬虫频道时重新从第1页刷新
        if (tvHomeTitle != null) {
            tvHomeTitle.setOnClickListener(v -> {
                if (isCrawlerChannel(selectedChannelId)) {
                    refreshCrawlerFirstPage(selectedChannelId, true);
                } else {
                    selectChannelById("campus");
                }
            });
        }

        // 点击顶部同步条或按钮：重新从第1页爬取刷新
        if (tvCampusBannerText != null) {
            tvCampusBannerText.setOnClickListener(v -> {
                if (isCrawlerChannel(selectedChannelId)) {
                    refreshCrawlerFirstPage(selectedChannelId, true);
                }
            });
        }
        if (btnCampusSync != null) {
            btnCampusSync.setOnClickListener(v -> {
                if (isCrawlerChannel(selectedChannelId)) {
                    refreshCrawlerFirstPage(selectedChannelId, true);
                }
            });
        }

        // 触底手势监听：下拉后面没有了自动刷新，动态接上去，当前页+1
        rvNews.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!isCrawlerChannel(selectedChannelId) || dy <= 0) {
                    return;
                }
                CrawlerState state = crawlerStates.get(selectedChannelId);
                if (state == null || state.isLoading || !state.hasMore) {
                    return;
                }
                // 检测是否滑动触底（无法继续向下滑动，后面没有了）
                if (!recyclerView.canScrollVertically(1)) {
                    int nextPage = state.currentPage + 1;
                    loadMoreCrawlerNews(selectedChannelId, nextPage);
                }
            }
        });
    }

    /**
     * 触底手势自动无限刷新核心逻辑：
     * 1. 记录当前 pagenumber，下一步请求的 pagenumber = state.currentPage + 1
     * 2. 调用 /xiaoyuan/<cat>/page/<pagenumber>
     * 3. 只要异步获取到内容，就把当前页计算 +1，并动态接到列表后面 (state.list.addAll)
     */
    private void loadMoreCrawlerNews(final String channelId, final int nextPage) {
        final CrawlerState state = crawlerStates.get(channelId);
        if (state == null || state.isLoading || !state.hasMore) {
            return;
        }
        state.isLoading = true;
        isCampusLoadingMore = true;
        updateCampusFooterUI("⏳ 正在自动请求【" + state.name + "】第 " + nextPage + " 页...", true);

        CampusNewsStore.fetchCategoryNews(state.catParam, nextPage, CAMPUS_PAGE_SIZE, new CampusNewsStore.PageCallback() {
            @Override
            public void onSuccess(List<News> items, String queryTime, String school, int currentPage, int pageCount, int newsCount, int perPage, boolean hasMore) {
                if (!isAdded()) {
                    return;
                }
                state.currentPage = currentPage;
                state.totalPages = Math.max(pageCount, 1);
                state.hasMore = hasMore && (currentPage < pageCount);

                if (items != null && !items.isEmpty()) {
                    state.list.addAll(items);
                    if (channelId.equals(selectedChannelId)) {
                        activeNews = state.list;
                        filterNews(etSearch != null ? etSearch.getText().toString() : "");
                    }
                }

                state.isLoading = false;
                isCampusLoadingMore = false;
                if (!state.hasMore) {
                    updateCampusFooterUI("—— 已经到底啦，去看看其他内容吧 ——", false);
                } else {
                    updateCampusFooterUI("📖 已加载至第 " + state.currentPage + " 页 (累计 " + state.list.size() + " 条) · 下滑自动加载更多", false);
                }
            }

            @Override
            public void onFailure(String reason) {
                if (!isAdded()) {
                    return;
                }
                state.isLoading = false;
                isCampusLoadingMore = false;
                updateCampusFooterUI("❌ 第 " + nextPage + " 页加载失败: " + reason + "，下滑可重试", false);
            }
        });
    }

    /**
     * 重新从第 1 页拉取刷新
     */
    private void refreshCrawlerFirstPage(final String channelId, final boolean userInitiated) {
        final CrawlerState state = crawlerStates.get(channelId);
        if (state == null || !isAdded() || state.isLoading) {
            return;
        }
        state.isLoading = true;
        isCampusLoadingMore = true;
        state.currentPage = 1;
        state.hasMore = true;
        if (btnCampusSync != null) {
            btnCampusSync.setEnabled(false);
            btnCampusSync.setText("爬取中...");
        }
        updateCampusFooterUI("⏳ 正在请求【" + state.name + "】第 1 页...", true);
        if (userInitiated) {
            Toast.makeText(requireContext(), "正在从第 1 页重新爬取刷新【" + state.name + "】...", Toast.LENGTH_SHORT).show();
        }

        CampusNewsStore.fetchCategoryNews(state.catParam, 1, CAMPUS_PAGE_SIZE, new CampusNewsStore.PageCallback() {
            @Override
            public void onSuccess(List<News> items, String queryTime, String school, int currentPage, int pageCount, int newsCount, int perPage, boolean hasMore) {
                if (!isAdded()) {
                    return;
                }
                state.currentPage = currentPage;
                state.totalPages = Math.max(pageCount, 1);
                state.hasMore = hasMore && (currentPage < pageCount);

                state.list.clear();
                if (items != null && !items.isEmpty()) {
                    state.list.addAll(items);
                }
                if (channelId.equals(selectedChannelId)) {
                    activeNews = state.list;
                    filterNews(etSearch != null ? etSearch.getText().toString() : "");
                    rvNews.scrollToPosition(0);
                }

                if (btnCampusSync != null) {
                    btnCampusSync.setEnabled(true);
                    btnCampusSync.setText("🔄 刷新第1页");
                }
                if (tvCampusBannerText != null) {
                    tvCampusBannerText.setText("🏫 " + school + " · " + state.officialSection + " · " + queryTime);
                }

                state.isLoading = false;
                isCampusLoadingMore = false;
                updateCampusFooterUI("📖 已加载第 1 页 (共 " + state.list.size() + " 条) · 下滑触底自动加载更多", false);
                if (userInitiated) {
                    Toast.makeText(requireContext(), "【" + state.name + "】第 1 页刷新成功！已加载 " + state.list.size() + " 条", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String reason) {
                if (!isAdded()) {
                    return;
                }
                state.isLoading = false;
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

    private void refreshCampusFirstPage(final boolean userInitiated) {
        refreshCrawlerFirstPage("campus", userInitiated);
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

        if (isSchoolChannel(selectedChannelId)) {
            rvNews.setVisibility(View.GONE);
            lvCraft.setVisibility(View.GONE);
            if (wvSchoolPage != null) wvSchoolPage.setVisibility(View.VISIBLE);
            if (empty != null) empty.setVisibility(View.GONE);
            return;
        }

        boolean hot = "hot".equals(selectedChannelId);
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
            newsAdapter.setHighlightQuery(key);
            empty.setVisibility(newsList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        if (wvSchoolPage != null) {
            wvSchoolPage.stopLoading();
        }
        super.onDestroyView();
    }

    @Override public void onSaveInstanceState(Bundle out) {
        out.putString("channel_id", selectedChannelId);
        out.putString("query", savedQuery);
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

    private void showCampusNoticeDialog(int index) {
        String[] titles = {
                "【教务处】2026年春季学期选课与学分重修认定的通知",
                "【学工在线】第十二届晴川校园樱花文化艺术节即将启幕",
                "【图书馆】春季新书上架，24小时考研研读室开放预约",
                "【后勤保卫】关于春季校园电动车规范停放与安全用电提示"
        };
        String[] departments = {
                "教务处 · 教学运行科",
                "党委学工部 · 校团委",
                "晴川图书馆 · 读者服务部",
                "后勤保卫处 · 校园治安综合治理科"
        };
        String[] dates = {"2026-03-28", "2026-03-25", "2026-03-20", "2026-03-18"};
        String[] contents = {
                "全体本科生同学：\n\n　　2026年春季学期通识必修课、学科基础课及专业核心课选课工作已正式启动。请各位同学登录教务管理系统核对个人培养方案，在规定选课窗口期内完成退选与确认。\n\n　　重修及学分置换申请截止时间为4月15日17:00，逾期不予补报。\n\n教务处办公电话：027-87934455",
                "全校师生员工：\n\n　　春和景明，万株樱花盛开。学校定于本周五在龙泉山下樱花大道及晴川大剧场隆重举办‘第十二届晴川校园樱花文化艺术节’。\n\n　　活动包含草坪音乐节、晴川文创游园会、书画摄影展及汉服游园巡展，欢迎全校师生、广大校友及家长朋友们共赴春日盛会！",
                "各位读者：\n\n　　晴川图书馆新采购的5000余册前沿学科图书、考研指定教材及数字资源数据库现已正式上架。同时，二楼南侧考研研读专区已开启智慧预约系统，请同学们通过‘晴川智慧校园’刷卡选座，自觉保持安静整洁。",
                "全校师生：\n\n　　为切实维护校园交通秩序和消防安全，严禁在教学楼门厅、宿舍走廊及疏散通道停放电动车或飞线充电。请将车辆整齐停放于各宿舍楼下指定智能充电车棚内。感谢大家的配合与支持！"
        };

        int idx = (index >= 0 && index < titles.length) ? index : 0;
        new AlertDialog.Builder(requireContext())
                .setTitle(titles[idx])
                .setMessage("发文单位：" + departments[idx] + "\n发布日期：" + dates[idx] + "\n\n" + contents[idx])
                .setPositiveButton("我知道了", null)
                .show();
    }

    private void setupBroadcastSwipeToDismiss(View bar, View vfBroadcast) {
        View ivClose = bar.findViewById(R.id.iv_close_broadcast);
        if (ivClose != null) {
            ivClose.setOnClickListener(v -> dismissBroadcastBar(bar, true));
        }

        bar.setClickable(true);
        int touchSlop = ViewConfiguration.get(bar.getContext()).getScaledTouchSlop();

        View.OnTouchListener swipeListener = new View.OnTouchListener() {
            private float downX, downY;
            private boolean isSwiping = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        isSwiping = false;
                        return false;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (!isSwiping) {
                            if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                                isSwiping = true;
                                ViewParent parent = bar.getParent();
                                if (parent != null) {
                                    parent.requestDisallowInterceptTouchEvent(true);
                                }
                            }
                        }
                        if (isSwiping) {
                            bar.setTranslationX(dx);
                            float width = bar.getWidth();
                            float alpha = 1.0f - Math.min(1.0f, Math.abs(dx) / (width > 0 ? width : 1.0f)) * 0.7f;
                            bar.setAlpha(Math.max(0.2f, alpha));
                            return true;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isSwiping) {
                            float totalDx = event.getRawX() - downX;
                            float threshold = bar.getWidth() * 0.25f;
                            if (Math.abs(totalDx) > threshold) {
                                dismissBroadcastBar(bar, totalDx > 0);
                            } else {
                                bar.animate()
                                        .translationX(0f)
                                        .alpha(1.0f)
                                        .setDuration(180)
                                        .start();
                            }
                            isSwiping = false;
                            return true;
                        }
                        break;
                }
                return false;
            }
        };

        bar.setOnTouchListener(swipeListener);
        if (vfBroadcast != null) {
            vfBroadcast.setOnTouchListener(swipeListener);
        }
    }

    private void dismissBroadcastBar(View bar, boolean toRight) {
        float width = bar.getWidth() > 0 ? bar.getWidth() : 800f;
        float targetX = toRight ? (width + 300f) : -(width + 300f);
        bar.animate()
                .translationX(targetX)
                .alpha(0f)
                .setDuration(220)
                .withEndAction(() -> {
                    bar.setVisibility(View.GONE);
                    bar.setTranslationX(0f);
                    bar.setAlpha(1.0f);
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "已移除校园快讯", Toast.LENGTH_SHORT).show();
                    }
                })
                .start();
    }
}
