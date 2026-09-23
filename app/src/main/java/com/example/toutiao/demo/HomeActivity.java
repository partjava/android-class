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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private EditText etSearch;
    private RecyclerView rvNews;
    private ListView lvCraft;
    private BottomNavigationView bottomNav;

    //newsList 是"当前正在显示"的那一份，NewsMultiAdapter 持有它的引用。
    //切换标签时对这个对象做 clear + addAll，再 notifyDataSetChanged 就能刷新。
    private List<News> newsList;
    private NewsMultiAdapter newsAdapter;

    //每个标签页各自的数据
    private List<News> recommendList;//推荐
    private List<News> kangyiList;//抗疫
    private List<News> videoSmallList;//小视频
    private List<News> beijingList;//北京
    private List<News> videoList;//视频
    private List<News> entertainList;//娱乐

    private List<Craftsman> craftsmanList;
    private CraftsmanAdapter craftAdapter;

    //全部标签
    private TextView tabRecommend, tabKangyi, tabVideoSmall, tabBeijing, tabVideo, tabHot, tabEntertain;
    //7 个标签收进数组,resetTabColor() 遍历它统一重置,不用逐个写 7 行
    private TextView[] allTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        bindView();
        initNewsData();
        initCraftData();
        bindEvent();
    }

    private void bindView() {
        etSearch = findViewById(R.id.et_search);
        rvNews = findViewById(R.id.rv_news);
        lvCraft = findViewById(R.id.lv_craft);
        bottomNav = findViewById(R.id.bottom_nav);

        //绑定所有标签
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

    //初始化新闻数据：每个标签页各有一套内容
    private void initNewsData(){
        recommendList = buildRecommend();
        kangyiList = buildKangyi();
        videoSmallList = buildVideoSmall();
        beijingList = buildBeijing();
        videoList = buildVideo();
        entertainList = buildEntertain();

        //默认显示"推荐"
        newsList = new ArrayList<>(recommendList);
        newsAdapter = new NewsMultiAdapter(newsList);

        //RecyclerView 必须自己指定布局管理器，这点和 ListView 不一样
        rvNews.setLayoutManager(new LinearLayoutManager(this));
        rvNews.setAdapter(newsAdapter);

        //点击某条新闻进入详情页；视频类型的改走播放页
        newsAdapter.setOnItemClickListener(news -> {
            if (news.getType() == News.TYPE_VIDEO) {
                Intent intent = new Intent(HomeActivity.this, VideoDetailActivity.class);
                intent.putExtra(VideoDetailActivity.EXTRA_TITLE, news.getTitle());
                //来源列沿用新闻详情页的拼法："来源  时间"
                intent.putExtra(VideoDetailActivity.EXTRA_SOURCE,
                        news.getSource() + "  " + news.getTime());
                intent.putExtra(VideoDetailActivity.EXTRA_DESC,
                        news.getContent() == null ? "" : news.getContent());
                intent.putExtra(VideoDetailActivity.EXTRA_COVER, news.getImg1());
                //时长和播放量 News 模型里没有，不传——
                //播放页取不到会把这两个控件藏掉，不编假数据
                startActivity(intent);
                return;
            }
            Intent intent = new Intent(HomeActivity.this, NewsDetailActivity.class);
            intent.putExtra("title", news.getTitle());
            intent.putExtra("info", news.getSource() + "  " + news.getTime());
            intent.putExtra("img", news.getImg1());
            intent.putExtra("type", news.getType());
            intent.putExtra("content", news.getContent() == null ? "" : news.getContent());
            startActivity(intent);
        });
    }

    //===== 各标签页的数据 =====
    // News 的 4 种类型：TYPE_TEXT 无图 / TYPE_SINGLE_IMG 单图 / TYPE_THREE_IMG 三图 / TYPE_VIDEO 视频

    private List<News> buildRecommend(){
        List<News> list = new ArrayList<>();
        list.add(new News(News.TYPE_TEXT, "各地餐企齐行动，杜绝餐饮浪费", "央视新闻客户端 9884评", "6小时前", 0,0,0)
                .withContent("　　近日，多地餐饮企业联合发起\"光盘行动\"倡议，在门店推出小份菜、半份菜。\n\n　　顾客用餐结束后，服务员会主动询问是否需要打包。多家餐企还设置了\"节约监督员\"岗位，对剩余较多的餐桌进行提醒。\n\n　　业内人士表示，杜绝餐饮浪费不仅要靠商家引导，更需要消费者养成按需点餐的习惯。"));
        list.add(new News(News.TYPE_SINGLE_IMG, "花菜有人焯水，有人直接炒，都错了，看饭店大厨如何做", "味美食记 18评", "刚刚", R.drawable.img1,0,0)
                .withContent("　　很多人做花菜时习惯先焯水，其实这样会让花菜失去脆嫩的口感。\n\n　　饭店大厨的做法是：把花菜掰成小朵，用淡盐水浸泡十分钟，捞出沥干。锅中放油，先下花菜大火干煸两分钟，逼出多余水分，再放调料快速翻炒出锅。\n\n　　这样炒出来的花菜既入味又爽脆，比焯水好吃得多。"));
        list.add(new News(News.TYPE_THREE_IMG, "睡觉时，双脚突然蹬一下，有踩空感，像高楼坠落，是咋回事？", "灵富康健康 78评", "1小时前", R.drawable.img2,R.drawable.img3,R.drawable.img4)
                .withContent("　　这种感觉在医学上叫\"临睡肌抽跃症\"，是一种正常的生理现象。\n\n　　人在入睡过程中，大脑皮层逐渐抑制，但部分神经仍处于兴奋状态。当肌肉突然收到一个错误信号时，就会产生一次急速的收缩，让人感觉像踩空了一样。\n\n　　过度疲劳、压力大、缺钙或睡前剧烈运动都可能让它更频繁。如果只是偶尔出现，不必担心；若频繁发生影响睡眠，建议就医检查。"));
        list.add(new News(News.TYPE_SINGLE_IMG, "实拍外卖小哥砸开小吃店的卷帘门救火，灭火后淡定继续送外卖", "生活小记 678评", "2小时前", R.drawable.img5,0,0)
                .withContent("　　事发当天下午，一家小吃店突然冒出浓烟，路过的外卖骑手发现后立即停车。\n\n　　他先是呼喊店内是否有人，确认无人后，用随车的工具砸开卷帘门，和周边商户一起用灭火器将火扑灭。\n\n　　确认火势完全控制后，他拍了拍身上的灰，骑上车继续送单。他说：\"餐还在保温箱里，不能耽误顾客吃饭。\""));
        list.add(new News(News.TYPE_THREE_IMG, "还没成熟就被迫提前采摘，8毛一斤却没人要，果农无奈：不摘不行", "禾木报告 189评", "3小时前", R.drawable.img6,R.drawable.img7,R.drawable.img8)
                .withContent("　　眼下正是水果集中上市的季节，但不少产区的果农却高兴不起来。\n\n　　由于前期天气反常，果子成熟期推迟，而收购商又急着要货，果农只能提前采摘。没熟透的果子口感差，收购价被压到八毛一斤，仍旧少人问津。\n\n　　有果农说，不摘的话果子会烂在地里，摘了至少还能收回一点成本。"));
        list.add(new News(News.TYPE_SINGLE_IMG, "大会、大展、大赛一起来，北京电竞\"好嗨哟\"", "燕鸣 304评", "4小时前", R.drawable.img9,0,0)
                .withContent("　　近期，北京接连举办多场电竞相关活动，涵盖产业大会、主题展览和职业赛事。\n\n　　展会现场设置了电竞设备体验区、游戏文化展区和周边市集，吸引大量年轻人前来打卡。同期举行的职业联赛更是座无虚席。\n\n　　业内认为，电竞正从单纯的项目赛事，向文旅、消费等更多领域延伸。"));
        return list;
    }

    private List<News> buildKangyi(){
        List<News> list = new ArrayList<>();
        list.add(new News(News.TYPE_SINGLE_IMG, "全国疫苗接种持续推进，加强免疫接种工作有序开展", "央视新闻 2315评", "1小时前", R.drawable.img10,0,0)
                .withContent("　　各地按照知情、同意、自愿的原则，持续推进新冠疫苗接种工作。\n\n　　为方便老年人接种，多个社区设置了临时接种点，并提供上门接送服务。接种现场配备医务人员全程值守，确保接种安全。"));
        list.add(new News(News.TYPE_TEXT, "多地优化调整防控措施，科学精准做好疫情防控工作", "人民日报 1876评", "2小时前", 0,0,0)
                .withContent("　　各地结合实际情况，不断优化防控举措，提高防控的科学性和精准性。\n\n　　相关部门表示，将继续做好重点场所、重点人群的健康监测，同时保障群众正常的生产生活秩序。"));
        list.add(new News(News.TYPE_THREE_IMG, "方舱医院陆续休舱，最后一批患者治愈出院", "新华社 943评", "3小时前", R.drawable.img1,R.drawable.img2,R.drawable.img3)
                .withContent("　　随着疫情防控形势持续向好，多地方舱医院完成使命陆续休舱。\n\n　　最后一批治愈患者在医护人员的陪同下走出方舱，现场响起掌声。不少患者与照顾自己多日的医护人员合影道别。"));
        list.add(new News(News.TYPE_SINGLE_IMG, "社区志愿者坚守一线，为居民配送生活物资", "本地新闻 562评", "5小时前", R.drawable.img4,0,0)
                .withContent("　　在各小区门口，志愿者们忙着分拣、搬运居民订购的生活物资。\n\n　　他们中有的是党员，有的是普通居民，主动报名参与到社区服务中来。一位志愿者说：\"能帮上一点忙，心里就踏实。\""));
        return list;
    }

    private List<News> buildVideoSmall(){
        List<News> list = new ArrayList<>();
        list.add(new News(News.TYPE_SINGLE_IMG, "萌宠日常：猫咪第一次见到雪的反应太可爱了", "萌宠日记 3204评", "刚刚", R.drawable.img2,0,0)
                .withContent("　　第一次见到雪的猫咪先是愣在原地，随后小心翼翼地伸出一只爪子试探。\n\n　　碰到冰凉的雪面后，它猛地缩回爪子，围着雪堆转了好几圈，最后还是忍不住扑了进去。"));
        list.add(new News(News.TYPE_THREE_IMG, "手艺人用木头做出会走的小机器人，全程不用一颗钉子", "手工达人 1587评", "30分钟前", R.drawable.img5,R.drawable.img6,R.drawable.img7)
                .withContent("　　这位手艺人用传统榫卯结构，做出了一台靠重力驱动、能够自己行走的木质机器人。\n\n　　整个作品没有使用一颗钉子或一滴胶水，所有零件都靠榫卯咬合。上紧发条后，机器人能稳稳地走上好几米。"));
        list.add(new News(News.TYPE_SINGLE_IMG, "航拍中国：从空中俯瞰祖国大好河山", "航拍视角 2761评", "1小时前", R.drawable.img8,0,0)
                .withContent("　　镜头从雪山之巅缓缓升起，掠过奔腾的江河、金黄的麦田和错落的村落。\n\n　　从高空俯瞰，山川河流呈现出与地面完全不同的壮阔面貌。"));
        list.add(new News(News.TYPE_TEXT, "一分钟学会三道家常菜，新手也能零失败", "厨房小窍门 892评", "2小时前", 0,0,0)
                .withContent("　　第一道西红柿炒蛋：鸡蛋打散加少许温水，炒至半凝固盛出，再下西红柿炒出汁，回锅翻匀即可。\n\n　　第二道蒜蓉青菜：青菜焯水十秒捞出，热油爆香蒜末淋上去。\n\n　　第三道可乐鸡翅：鸡翅两面煎黄，倒入可乐没过鸡翅，小火收汁。"));
        return list;
    }

    private List<News> buildBeijing(){
        List<News> list = new ArrayList<>();
        list.add(new News(News.TYPE_SINGLE_IMG, "北京地铁新线开通，市民出行更加便捷", "北京日报 1123评", "1小时前", R.drawable.img3,0,0)
                .withContent("　　新开通的线路串联起多个居民区和产业园区，早晚高峰时段发车间隔缩短至三分钟。\n\n　　不少市民表示，新线开通后通勤时间明显减少，换乘也更加方便。"));
        list.add(new News(News.TYPE_THREE_IMG, "故宫秋色正浓，银杏叶金黄铺满宫墙", "文旅北京 2489评", "2小时前", R.drawable.img6,R.drawable.img7,R.drawable.img8)
                .withContent("　　深秋时节，故宫内的银杏进入最佳观赏期，金黄的叶片与红墙黄瓦相映成趣。\n\n　　不少游客特意选在这个时候前来，用镜头记录下这一年一度的景致。"));
        list.add(new News(News.TYPE_SINGLE_IMG, "北京环球影城迎来客流高峰，热门项目排队超一小时", "北京青年报 1765评", "3小时前", R.drawable.img9,0,0)
                .withContent("　　假期期间，园区迎来大批游客，多个热门骑乘项目排队时间超过一小时。\n\n　　园区方面增加了演出场次，并延长了部分区域的开放时间，以分散客流。"));
        list.add(new News(News.TYPE_TEXT, "中关村论坛开幕，多项前沿科技成果集中亮相", "科技日报 738评", "4小时前", 0,0,0)
                .withContent("　　本届论坛聚焦人工智能、量子信息、生命科学等前沿领域。\n\n　　展会现场，多款自主研发的芯片、机器人和医疗设备首次公开亮相，吸引众多观众驻足体验。"));
        return list;
    }

    //视频标签：全部用 TYPE_VIDEO，对应 item_news_video 这种带封面的布局
    private List<News> buildVideo(){
        List<News> list = new ArrayList<>();
        list.add(new News(News.TYPE_VIDEO, "实拍：消防员火场逆行救人全过程", "现场实录 4521评", "刚刚", R.drawable.img4,0,0)
                .withContent("　　画面中，浓烟从窗口不断涌出。消防员佩戴空气呼吸器，沿着楼道逐层搜救。\n\n　　在五楼的一间卧室内，他们找到了一名被困老人，迅速为其戴上备用面罩，背起老人沿原路撤离。整个过程不到六分钟。"));
        list.add(new News(News.TYPE_VIDEO, "纪录片：大国重器是怎样炼成的", "纪录片频道 2134评", "1小时前", R.drawable.img1,0,0)
                .withContent("　　镜头走进重型装备制造车间，记录下一台大型设备从零件加工到整机装配的全过程。\n\n　　工人师傅们在毫米甚至微米级的精度要求下，一遍遍测量、打磨。他们说，差一点都不行。"));
        list.add(new News(News.TYPE_VIDEO, "街头采访：你理想中的生活是什么样的", "城市观察 1456评", "2小时前", R.drawable.img7,0,0)
                .withContent("　　记者在街头随机采访了二十位路人。\n\n　　有人说希望家人健康平安，有人说想有一间属于自己的小屋，还有人说想去看一次大海。答案各不相同，但大多朴素而具体。"));
        list.add(new News(News.TYPE_VIDEO, "延时摄影记录城市从黎明到黄昏", "影像志 982评", "3小时前", R.drawable.img2,0,0)
                .withContent("　　摄影师在城市最高处架设机位，连续拍摄十二个小时。\n\n　　镜头里，晨光越过楼群，车流由稀疏变得密集，又在暮色中渐渐亮起灯火。整座城市的一天被压缩进短短几十秒。"));
        return list;
    }

    private List<News> buildEntertain(){
        List<News> list = new ArrayList<>();
        list.add(new News(News.TYPE_SINGLE_IMG, "大众电影百花奖揭晓，多部影片获奖", "娱乐前线 3672评", "刚刚", R.drawable.img8,0,0)
                .withContent("　　本届百花奖各奖项由观众投票产生，多部现实题材影片收获颇丰。\n\n　　获奖者在台上表示，感谢观众的支持，会继续努力拍出更好的作品。"));
        list.add(new News(News.TYPE_THREE_IMG, "国风音乐盛典落幕，传统乐器演绎流行金曲", "音乐现场 1893评", "1小时前", R.drawable.img9,R.drawable.img10,R.drawable.img1)
                .withContent("　　盛典上，古筝、琵琶、笛箫等传统乐器与现代编曲结合，重新演绎了多首流行歌曲。\n\n　　观众表示，熟悉的旋律换一种方式演奏，听起来别有一番味道。"));
        list.add(new News(News.TYPE_TEXT, "话剧《茶馆》复排上演，连演三十场座无虚席", "戏剧观察 724评", "2小时前", 0,0,0)
                .withContent("　　本轮复排保留了经典版本的结构，同时在舞台呈现上做了新的尝试。\n\n　　演出票开售当天即告售罄，不少观众表示是第一次走进剧场看话剧。"));
        list.add(new News(News.TYPE_SINGLE_IMG, "综艺新季开播，素人选手表现亮眼", "综艺速递 1345评", "3小时前", R.drawable.img2,0,0)
                .withContent("　　新一季节目取消了明星常驻设定，全部由素人选手参与。\n\n　　首期播出后，几位选手凭借扎实的基本功和真实的表现获得大量讨论。"));
        return list;
    }

    //初始化大国工匠数据
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
        //工匠列表点击事件
        lvCraft.setOnItemClickListener((parent, view, position, id) -> {
            Craftsman data = craftsmanList.get(position);
            showCraftDialog(data);
        });
    }

    //重置所有标签文字颜色。
    //颜色不再硬编码，统一引用 colors.xml 的 token，
    //改色板时首页 tab 会跟着变，不用再同步两处。
    private void resetTabColor(){
        for (TextView tab : allTabs) {
            tab.setTextColor(ContextCompat.getColor(this, R.color.tab_inactive));
        }
    }

    //高亮选中的标签，其余恢复默认色
    private void selectTab(TextView activeTab){
        resetTabColor();
        activeTab.setTextColor(ContextCompat.getColor(this, R.color.brand_red));
    }

    //给一个新闻标签绑定点击事件：选中它并切换到对应数据
    private void bindNewsTab(TextView tab, List<News> data){
        tab.setOnClickListener(v -> switchNewsTab(data, tab));
    }

    //切换新闻标签：换上该标签自己的数据并刷新列表
    private void switchNewsTab(List<News> data, TextView activeTab){
        selectTab(activeTab);

        newsList.clear();
        newsList.addAll(data);
        newsAdapter.notifyDataSetChanged();
        rvNews.scrollToPosition(0);//回到列表顶部

        rvNews.setVisibility(View.VISIBLE);
        lvCraft.setVisibility(View.GONE);
    }

    private void bindEvent() {
        //==== 搜索框：输入关键词后按键盘右下角的搜索键触发 ====
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                String keyword = etSearch.getText().toString().trim();
                if (keyword.isEmpty()) {
                    Toast.makeText(HomeActivity.this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HomeActivity.this, "搜索：" + keyword, Toast.LENGTH_SHORT).show();
                    //收起软键盘
                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                    }
                }
                return true;
            }
            return false;
        });

        //底部导航选中首页
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if(itemId == R.id.nav_home){
                    Toast.makeText(HomeActivity.this,"当前在首页",Toast.LENGTH_SHORT).show();
                }else if(itemId == R.id.nav_video){
                    //跳转到视频页，和其他几个 tab 用同一套栈管理策略
                    Intent intent = new Intent(HomeActivity.this, VideoActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }else if(itemId == R.id.nav_add){
                    Toast.makeText(HomeActivity.this,"点击发布",Toast.LENGTH_SHORT).show();
                }else if(itemId == R.id.nav_shop){
                    //跳转到商城页,复用栈里已有的 ShopActivity,
                    //和上面跳"我的"用的是同一套栈管理策略
                    Intent intent = new Intent(HomeActivity.this, ShopActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }else if(itemId == R.id.nav_mine){
                    //跳转到我的页面。
                    //CLEAR_TOP + SINGLE_TOP 会复用栈里已有的 MineActivity，
                    //不加的话反复切"首页/我的"会让返回栈无限堆叠。
                    //注意不要用 finish() 代替：那样"我的"就成了栈底，
                    //从"我的"按返回键会直接退出应用。
                    Intent intent = new Intent(HomeActivity.this, MineActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
                return true;
            }
        });

        //==== 分类标签：每个标签换上自己那一套新闻数据 ====
        bindNewsTab(tabRecommend, recommendList);
        bindNewsTab(tabKangyi, kangyiList);
        bindNewsTab(tabVideoSmall, videoSmallList);
        bindNewsTab(tabBeijing, beijingList);
        bindNewsTab(tabVideo, videoList);
        bindNewsTab(tabEntertain, entertainList);

        //热点标签显示的是大国工匠列表，和其他标签不是一套数据
        tabHot.setOnClickListener(v -> {
            selectTab(tabHot);
            rvNews.setVisibility(View.GONE);
            lvCraft.setVisibility(View.VISIBLE);
        });
    }

    //弹出工匠详情弹窗
    private void showCraftDialog(Craftsman craftsman){
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        View dialogView = LayoutInflater.from(HomeActivity.this).inflate(R.layout.dialog_craftsman_detail,null);

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

    //=====大国工匠实体类=====
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

    //工匠适配器
    class CraftsmanAdapter extends ArrayAdapter<Craftsman> {
        public CraftsmanAdapter(List<Craftsman> list) {
            super(HomeActivity.this,0,list);
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
