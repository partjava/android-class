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
    private List<News> kangyiList;//健康/抗疫
    private List<News> videoSmallList;//小视频
    private List<News> beijingList;//北京
    private List<News> videoList;//视频
    private List<News> entertainList;//娱乐

    private List<Craftsman> craftsmanList;
    private CraftsmanAdapter craftAdapter;

    private TextView tabRecommend, tabKangyi, tabVideoSmall, tabBeijing, tabVideo, tabHot, tabEntertain;

    private TextView[] allTabs;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindView();
        initNewsData();
        initCraftData();
        bindEvent();
        int restoreTab = savedInstanceState == null ? selectedTab : savedInstanceState.getInt("tab", selectedTab);
        String query = savedInstanceState == null ? savedQuery : savedInstanceState.getString("query", "");
        findViewById(restoreTab).performClick();
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

        tabRecommend = findViewById(R.id.tab_recommend);
        tabKangyi = findViewById(R.id.tab_kangyi);
        tabVideoSmall = findViewById(R.id.tab_video_small);
        tabBeijing = findViewById(R.id.tab_beijing);
        tabVideo = findViewById(R.id.tab_video);
        tabHot = findViewById(R.id.tab_hot);
        tabEntertain = findViewById(R.id.tab_entertain);

        allTabs = new TextView[]{tabRecommend, tabKangyi, tabVideoSmall,
                tabBeijing, tabVideo, tabHot, tabEntertain};
    }

    private void initNewsData(){
        recommendList = buildRecommend();
        kangyiList = buildKangyi();
        videoSmallList = buildVideoSmall();
        beijingList = buildBeijing();
        videoList = buildVideo();
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
            intent.putExtra("type", news.getType());
            intent.putExtra("content", news.getContent() == null ? "" : news.getContent());
            //接口头条的远程封面与原文链接；本地文章这两个字段是 null
            intent.putExtra("img_url", news.getImageUrl() == null ? "" : news.getImageUrl());
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

    private List<News> buildKangyi(){
        return NewsContentStore.kangyi();
    }

    private List<News> buildVideoSmall(){
        return NewsContentStore.videoSmall();
    }

    private List<News> buildBeijing(){
        return NewsContentStore.beijing();
    }

    private List<News> buildVideo(){
        return NewsContentStore.video();
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
        bindNewsTab(tabKangyi, kangyiList);
        bindNewsTab(tabVideoSmall, videoSmallList);
        bindNewsTab(tabBeijing, beijingList);
        bindNewsTab(tabVideo, videoList);
        bindNewsTab(tabEntertain, entertainList);

        tabHot.setOnClickListener(v -> {
            selectTab(tabHot);
            filterNews(etSearch.getText().toString());
        });
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
