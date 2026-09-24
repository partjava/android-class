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
            recommendList = buildRecommend();
            if (activeNews == null || selectedTab == R.id.tab_recommend) {
                activeNews = recommendList;
                filterNews(etSearch != null ? etSearch.getText().toString() : "");
            }
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
            startActivity(intent);
        });
    }

    private List<News> buildRecommend(){
        List<News> list = new ArrayList<>(userCreatedPosts);
        list.add(new News(News.TYPE_SINGLE_IMG, "深中通道正式通车运营：世界级跨海集群工程创下十项世界之最", "新华社 3.8万评", "刚刚", R.drawable.news_smart_city, 0, 0)
                .withContent("　　历时七年艰苦建设，连接深圳与中山的核心跨海交通大动脉——深中通道正式开通试运营。作为当今世界上建设难度极高的跨海集群工程之一，深中通道集“桥、岛、隧、水下互通”于一体，全长约24公里。\n\n　　通车后，深圳至中山的车程从原本的两小时大幅缩短至30分钟左右，珠江口东西两岸正式迈入“半小时生活交通圈”。工程在超宽钢壳混凝土沉管隧道、超大跨度悬索桥抗风等技术领域攻克多项世界级难题，创造了十项国际工程建设新纪录。\n\n　　交通运输部相关负责人表示，深中通道的贯通将加速粤港澳大湾区人流、物流、资金流高效互联互通，为区域经济高质量一体化发展注入澎湃动能。"));

        list.add(new News(News.TYPE_THREE_IMG, "中国空间站生命科学实验获突破进展：水稻空间全生命周期培养成功", "央视新闻 2.6万评", "15分钟前", R.drawable.news_space_rocket, R.drawable.news_tech_chip, R.drawable.news_medical_health)
                .withContent("　　在中国空间站问天与梦天实验舱内，多项空间生命科学实验取得阶段性重大突破。科研团队在微重力环境下顺利完成了拟南芥和水稻从种子到种子的全生命周期空间培养实验，这也是国际上首次在轨获得水稻完整空间种子。\n\n　　中科院分子植物卓越中心专家介绍，空间微重力环境对植物开花结实、向光性及养分转运具有深远影响。本次实验系统解析了空间微重力对水稻株型调控的分子机制，为未来开发适应空间微重力生态系统的特异农作物新品种奠定了坚实科学基础。\n\n　　航天员乘组后续还将对带回地面的空间种子开展深入基因测序与表型分析，探索深空探测长期生命保障系统的可持续粮食供应方案。"));

        list.add(new News(News.TYPE_TEXT, "重磅政策：全国跨省异地就医直接结算率突破90%，惠及超1.2亿参保群众", "人民日报 1.9万评", "28分钟前", 0, 0, 0)
                .withContent("　　国家医保局最新数据显示，全国跨省异地就医直接结算服务体系已覆盖所有省份、统筹地区及主要基层医疗机构，全国跨省异地就医住院费用直接结算率已稳步突破90%，惠及群众超过1.2亿人次。\n\n　　如今，参保人员只需通过国家医保服务平台App或地方政务小程序完成手机端一键备案，在异地定点医院就医即可直接刷卡或扫医保码实时结算，彻底告别了过去“垫资跑腿、反复开具证明、跨省报销周期长”的痛点困境。\n\n　　下一步，医保部门将持续拓展门诊慢特病跨省直接结算病种范围，推进京津冀、长三角、成渝等重点城市群医保同城化无感结算，让改革红利更深入温润千家万户。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "300兆瓦级压缩空气储能示范项目成功并网，新型绿色储能筑牢电网调峰安全", "科技日报 8420评", "42分钟前", R.drawable.news_green_energy, 0, 0)
                .withContent("　　由我国自主研发制造的300兆瓦先进压缩空气储能国家示范工程顺利完成168小时满负荷试运行，正式并入国家电网。该项目利用废弃地下深层盐穴作为储气库，单机储能量突破1200兆瓦时，综合转换效率超72%，关键技术完全自主可控。\n\n　　在用电低谷时，系统利用富余的清洁风光电能将空气压缩储存至地下数百米的深层盐穴；在用电高峰时释放高压空气驱动膨胀发电机组发电，犹如给城市电网装上了一个巨大的“绿色超级充电宝”。\n\n　　国家能源局技术专家指出，大容量压缩空气储能凭借寿命长、成本低、占地少、本质安全等优势，将成为支撑大规模新能源消纳与新型电力系统稳定运行的核心支柱。"));

        list.add(new News(News.TYPE_THREE_IMG, "复兴号智能动车组驰骋高寒高原，中国高铁运营里程突破4.5万公里稳居世界第一", "新华社 4.2万评", "1小时前", R.drawable.news_train_speed, R.drawable.img1, R.drawable.img2)
                .withContent("　　随着多条新建高速铁路干线的开通，我国高速铁路营业里程已突破4.5万公里，稳居世界第一位。新型复兴号智能动车组在零下40摄氏度的高寒高原以及复杂海风盐雾环境下实现了常态化时速350公里平稳运营。\n\n　　新一代复兴号搭载了千兆以太网车载网络、5G智能车载Wi-Fi、智能温控与故障自诊断系统，全车部署了超过4000个高精度智能传感器，实现了关键转向架、牵引变流器、受电弓状态的毫秒级实时安全预警。\n\n　　中国铁道科学研究院首席工程师表示，中国高铁已形成从勘察设计、工程建设、装备制造到智能运营维护的全产业链完整技术体系，正持续向更安全、更绿色、更智能的高速铁路技术高地稳步迈进。"));

        list.add(new News(News.TYPE_VIDEO, "【超燃实拍】万米深潜“奋斗者”号探索马里亚纳海沟，记录深渊生命奇迹", "央视纪录 3.1万评", "1小时前", R.drawable.news_ocean_sea, 0, 0)
                .withContent("　　镜头跟随我国全海深载人潜水器“奋斗者”号下潜至马里亚纳海沟万米“挑战者深渊”。在水压超过110兆帕、完全黑暗冰冷的极端极端深海环境中，科研人员借助超高亮度LED矩阵与4K超高清深海摄像机，记录下了珍贵的生物影像。\n\n　　画面中，晶莹剔透的深海狮子鱼、巨大的端足类钩虾在海底热液与沉积物间自如游弋，展现了深海生命顽强的演化奇迹。机械臂精准完成了深渊岩石原位取样和宏生物样品的无损采集。\n\n　　“奋斗者”号的连续深渊科考突破，标志着我国深海载人深潜科考能力已达到全球领跑水平，为人类探索未知海洋世界打开了全景窗口。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "车规级高算力芯片打破海外垄断：首批全自主主控芯片进入主流车企前装量产", "经济日报 1.2万评", "2小时前", R.drawable.news_tech_chip, 0, 0)
                .withContent("　　在智能网联汽车核心部件领域，我国自主研发的高算力车规级主控芯片与智能座舱MCU正式完成百万公里严苛路测，并与国内多家一线汽车品牌签订前装量产订单，彻底打破了长期依赖进口的供应链瓶颈。\n\n　　该系列芯片采用先进7纳米制程工艺与车规级功能安全架构设计，算力达到行业顶尖水准，且在耐高低温冲击、抗电磁干扰及功耗控制指标上均优于国际同类产品，全面满足L3级以上自动驾驶全场景决策需求。\n\n　　汽车工业协会分析师表示，国产芯片从外围控制器向核心计算平台攻坚成功，标志着我国新能源智能汽车产业生态自立自强迈出历史性一步。"));

        list.add(new News(News.TYPE_THREE_IMG, "三星堆最新数字化考古成果震撼发布：AI三维拼接跨坑青铜神坛重现千年前古蜀文明", "光明日报 2.3万评", "2小时前", R.drawable.news_museum_art, R.drawable.img3, R.drawable.img4)
                .withContent("　　四川省文物考古研究院与多所高校科研团队联合公布三星堆遗址最新数字化复原成果。借助AI计算机视觉与高精度三维激光扫描技术，历经3000年跨越不同祭祀坑出土的数件残损青铜器构件，首次实现了数字虚拟拼接复原。\n\n　　拼接完成的“青铜神坛”构形宏大精妙，底部神兽托举，中间跪坐人像神态肃穆，顶部神殿结构雕镂细密，充分体现了古蜀先民天马行空的想象力与登峰造极的青铜铸造技艺。\n\n　　数字化技术不仅避免了对珍贵脆弱文物的物理二次损伤，更让观众通过沉浸式全息投影，身临其境感受中华文明源远流长的多元一体魅力。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "西北“沙戈荒”大型风光基地装机容量破亿千瓦，茫茫黄沙化身“绿色光伏蓝海”", "中国能源报 9150评", "3小时前", R.drawable.news_solar_energy, 0, 0)
                .withContent("　　在内蒙古库布其沙漠、甘肃腾格里沙漠深处，成片排列的蓝黑色光伏板在阳光照耀下犹如浩瀚的蓝色海洋。据国家发展改革委最新统计，全国大型风电光伏基地第一批重点项目已全面建成投产，装机容量突破1亿千瓦大关。\n\n　　基地创造性采用了“板上发电、板下种植、板间养殖”的生态立体治沙模式。光伏阵列有效降低了地表风速与土壤水分蒸发，使昔日寸草不生的沙丘长出了黄芪、甘草等沙生经济作物，实现“绿电增产”与“荒漠治理”双赢。\n\n　　通过“西电东送”特高压直流通道，这些来自茫茫戈壁的绿色电力源源不断输往东部沿海千万家庭，为我国“双碳”目标提供坚实绿色底色。"));

        list.add(new News(News.TYPE_TEXT, "教育部印发实施意见：全面推进数字校园与智慧课堂建设，以数字化赋能教育均衡", "中国青年报 1.5万评", "3小时前", 0, 0, 0)
                .withContent("　　教育部日前正式印发《全面深化中小学数字化教育应用实施方案》，明确提出到2026年底，全国中小学校园网络万兆互联与班级千兆直达率达100%，国家中小学智慧教育平台数字教学资源将覆盖全部学科和学段。\n\n　　方案重点强调利用虚拟仿真实验、AI智能批阅、“名校名师同步课堂”等现代教育技术，将东部沿海及省会城市的优质教育资源无缝直连偏远山区学校，有效缩小城乡校际教育资源差距。\n\n　　多地试点学校校长反馈，数字课堂不仅显著提升了课堂互动效率与学生学习兴趣，更让偏远乡村孩子也能平等享受名师辅导与科学实验探究的乐趣。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "三江源国家公园雪豹种群恢复至千只以上，“雪山之王”频现见证高原生态筑底向好", "新华网 1.8万评", "4小时前", R.drawable.news_nature_park, 0, 0)
                .withContent("　　最新生态综合科考与红外相机长期监测成果显示，位于青藏高原腹地的三江源国家公园雪豹种群数量已稳定恢复至1200只以上，藏羚羊数量由不足2万只恢复到7万余只，高原旗舰物种保护取得历史性成效。\n\n　　建立国家公园体制以来，当地探索实施“一户一岗”生态管护员制度，近两万名世居牧民转型为专业管护员，积极参与巡山护草、反盗猎以及生物多样性日常监测，形成了人与自然和谐共生的典范格局。\n\n　　世界自然保护联盟（IUCN）专家对此给予高度评价，认为三江源的生态治理实践为全球高原干旱区濒危大中型食肉动物保护提供了极具借鉴价值的“中国方案”。"));

        list.add(new News(News.TYPE_THREE_IMG, "全国青少年科技创新大赛落幕：中学生自研“农田病虫害智能巡检飞行器”斩获特等奖", "科技日报 7600评", "5小时前", R.drawable.news_education_youth, R.drawable.news_school_study, R.drawable.img5)
                .withContent("　　第38届全国青少年科技创新大赛在国家科技馆圆满闭幕。来自全国31个省市自治区的近千件优秀青少年科创发明作品同台展示，涵盖人工智能、低碳环保、仿生工程等前沿方向。\n\n　　获得本届特等奖的作品是由三名高二学生自主研发的“农田多光谱微型巡检飞行器”。他们利用开源单片机与边缘计算视觉模块，训练了轻量级卷积神经网络模型，能以98.2%的高准确率在低空飞行中实时识别小麦条锈病等初期病害，并自动下发精准变量喷药路径。\n\n　　评委会院士专家称赞道，当代青少年展现了出色的工程动手能力与敏锐的社会问题洞察力，为建设科技强国储备了朝气蓬勃的后备军力量。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "国家超级计算中心发布新一代量子模拟算法，复杂药物靶点分子计算提速万倍", "环球科学 8890评", "5小时前", R.drawable.news_tech_chip, 0, 0)
                .withContent("　　国家超级计算中心联合中科院计算所发布最新突破性量子经典混合架构模拟算法框架。该框架利用国产超算的强劲异构算力，成功实现了对数百个量子比特量子态纠缠演化的保真模拟。\n\n　　在抗肿瘤新型靶向药物先导化合物的分子对接与结合自由能计算实测中，该算法将原本需要耗费数月甚至半年的超大分子模拟任务，大幅压缩至数十分钟内完成，计算效率提升超过万倍。\n\n　　生物医药科研人员表示，超算与量子算法的深度融合将极大地缩减新药早期研发周期与资金投入，为我国前沿生物医药研发装上了强大的“超级数字引擎”。"));

        list.add(new News(News.TYPE_VIDEO, "【纪录片】大国工匠的毫厘之光：手工研磨超高精度空间反射镜背后的三十年坚守", "央视新闻客户端 2.9万评", "6小时前", R.drawable.news_smart_city, 0, 0)
                .withContent("　　纪录片镜头对准了长春光学精密机械与物理研究所的一级特级技师。在恒温恒湿的超净车间内，他正手持特制研磨头，对直径达两米的空间望远镜碳化硅非球面反射镜进行极限修形。\n\n　　为了使镜面面形精度达到纳米级（相当于头发丝直径的五万分之一），机器抛光后必须依靠具有三十年丰富经验的工匠进行纯手工微米感知修磨。每一次指尖触碰与力度掌控，都倾注了极致的专注与匠心。\n\n　　正是无数这样默默奉献的大国工匠，以严谨细致、追求完美的职业精神，为我国航天观测、深空探测打造出一只只明亮锐利的“千里眼”。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "新一代全磁悬浮人工心脏临床应用成效卓越：百余名终末期心衰患者重获新生", "健康中国 1.4万评", "7小时前", R.drawable.news_medical_health, 0, 0)
                .withContent("　　由国家心血管病中心阜外医院领衔开展的国产第三代全磁悬浮左心室辅助装置（人工心脏）长期随访结果在国际权威医学期刊发表。随访显示，入组患者术后五年生存率达到国际顶尖水准，血栓发生率低于1.5%。\n\n　　这款人工心脏核心泵体直径仅约26毫米，重量不足90克，叶轮在完全无机械接触的磁悬浮磁场中高速旋转，最大程度降低了对血液红细胞的机械剪切破坏。\n\n　　主刀专家指出，该装置不仅打破了国际技术封锁，还将治疗费用大幅降低60%以上，为广大终末期重症心脏衰竭患者带来了延续生命尊严的新曙光。"));

        list.add(new News(News.TYPE_THREE_IMG, "低空经济开辟万亿级新赛道：国产城市空中交通eVTOL完成首次跨江商业航线验证", "澎湃新闻 2.1万评", "8小时前", R.drawable.img6, R.drawable.img7, R.drawable.img8)
                .withContent("　　一架流线型纯电动垂直起降航空器（eVTOL）平稳升空，沿着预定低空空域航线顺利完成跨江飞行，并精准降落于对岸停机坪。这是我国首条城市低空立体交通空中巴士航线的全流程适航验证。\n\n　　该飞行器配备多冗余电机与分布式电力推进系统，续航里程超过250公里，巡航时速达200公里，噪声仅为传统燃油直升机的十分之一，且实现了零碳尾气排放。\n\n　　民航与产业经济专家指出，随着空域协同管理与低空雷达监视网的完善，低空文旅、空中通勤和紧急医疗转运等多元场景正加速商业化落地，低空经济将成为拉动高端制造的新引擎。"));

        list.add(new News(News.TYPE_TEXT, "里程碑！我国新能源汽车年产销量双双突破1000万辆大关，占全球市场份额超六成", "工信微报 3.4万评", "9小时前", 0, 0, 0)
                .withContent("　　工业和信息化部最新统计数据显示，我国新能源汽车年度生产和销售规模正式突破1000万辆历史大关，成为全球首个新能源汽车年产销量迈入千万级的国家，连续十年稳居全球首位。\n\n　　在核心技术方面，国产动力电池能量密度已提升至300瓦时/公斤以上，主流车型快充技术进入“充电10分钟、续航400公里”时代，固态电池装车试验稳步推进；自主品牌新能源车远销欧洲、中东、东南亚等全球160多个国家与地区。\n\n　　中国汽车工业协会负责同志表示，这标志着中国汽车工业历经数十年积淀，终于在新能源与智能化的“新赛道”上实现了弯道超车与跨越式领跑。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "AI大模型深度助力国家古籍修复：万卷明清濒危善本善篇实现“高保真数字重生”", "光明日报 6720评", "10小时前", R.drawable.news_museum_art, 0, 0)
                .withContent("　　国家图书馆与智能科技实验室联合启动古籍“数字焕新”工程。研发团队针对残缺墨迹、虫蛀孔洞、霉斑氧化等复杂病害，训练了专门的多模态古籍文意补全与图像修复大模型。\n\n　　在传统手工精裱装订保护原件的同时，系统能通过上下文语义推演与字体笔锋特征匹配，对缺损文字进行概率补全并以置信度标记，修复效率较纯人工推敲提升百倍以上。\n\n　　目前已有两千余部珍贵古籍善本以超高清数字化形式上线开放检索，海内外学者足不出户即可在云端翻阅千年文脉瑰宝。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "“智慧粮仓”守护大国粮仓：数字气调储粮与物联网传感器使储粮损耗率降至1%以下", "农民日报 8100评", "11小时前", R.drawable.img9, 0, 0)
                .withContent("　　走进中央储备粮智慧直属库，高大平房仓内整洁明亮。数百个数字温湿度与气体浓度传感器深深埋入数米深的粮堆，将数据每隔15分钟实时同步至粮库智慧管控大屏。\n\n　　粮库广泛应用氮气气调储粮、智能内环流通风以及控温储粮技术，在不使用任何化学药剂的情况下，通过充入高纯度氮气使害虫和霉菌窒息，真正做到了常年“低温低氧、绿色保鲜”。\n\n　　监测表明，现代化智慧仓储使粮食仓储周期损耗率严格控制在0.8%以内，远低于国际同行业标准，牢牢夯实了大国粮食安全的战略底座。"));

        list.add(new News(News.TYPE_THREE_IMG, "全民健身掀起绿色运动风潮：城市智慧绿道与可穿戴运动监测成为青年生活新风尚", "体育观察 1.1万评", "12小时前", R.drawable.news_sports_field, R.drawable.img10, R.drawable.img1)
                .withContent("　　随着初秋天高气爽，各大城市沿江沿湖规划建设的环城智慧健身绿道迎来大批运动爱好者。智能步道不仅配备了太阳能座椅、应急急救AED与雾化降温装置，还在沿途设置了无感运动打卡柱。\n\n　　市民无需携带手机，仅通过智能人脸识别或智能手环感应，即可自动记录配速、心率、步幅与卡路里消耗，并生成个人专属科学运动指导报告。\n\n　　运动医学专家提醒，科学规律的慢跑与抗阻训练能显著改善心肺功能与情绪状态，呼吁广大市民循序渐进，享受健康文明的绿色生活方式。"));

        list.add(new News(News.TYPE_VIDEO, "【实况航拍】横断山脉上的“天路奇迹”：雅西高速云端双螺旋隧道穿云破雾", "中国交通 1.7万评", "14小时前", R.drawable.news_train_speed, 0, 0)
                .withContent("　　航拍无人机穿透清晨翻滚的云海，俯瞰被誉为“天梯高速”的雅西高速公路。公路在险峻的大凉山深峡中蜿蜒爬升，整条线路每向前行进1公里，海拔就上升7.5米。\n\n　　最为叹为观止的是干海子和铁寨子两座特大双螺旋隧道。为了在极短的水平距离内克服数百米的险峻高差，工程师构想出让公路在山体内部“画圆圈盘旋上升”的巧妙方案，创下多项世界公路建设之最。\n\n　　通车十余年来，雅西高速彻底打通了攀西地区与外界的高速物流走廊，让大凉山深处的核桃、苹果等农特产一路欢畅直达全国餐桌。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "新一代全域数字孪生城市底座启用：城市应急事件全流程感知响应时间缩减60%", "新华网 9300评", "16小时前", R.drawable.news_smart_city, 0, 0)
                .withContent("　　智慧城市全域感知与数字孪生综合指挥调度大厅内，三维高精城市实景模型实时跃动着交通流、积水点、供电管网与消防报警信息。这是最新投运的城市综合大脑操作系统。\n\n　　系统通过接入全城数万个物联网感知探头与无人机巡检节点，一旦发生暴雨内涝或道路管线异常，AI算法能在3秒内自动研判影响半径，并在5秒内生成跨部门联动处置预案。\n\n　　市政管理局相关负责人介绍，数字孪生技术的深入应用，使城市运行从“被动事后处置”全面转型为“主动事前预防”，让城市更有韧性、更具温度。"));

        list.add(new News(News.TYPE_TEXT, "反垄断与算法透明度新规正式施行：严禁平台“大数据杀熟”，全面保障消费者知情选择权", "法治日报 1.3万评", "18小时前", 0, 0, 0)
                .withContent("　　国家市场监督管理总局联合多部门发布的《互联网平台算法推荐管理与反不正当竞争新规》自本月起正式施行，对平台经济算法滥用与不正当价格歧视立下硬性规矩。\n\n　　新规明确规定，互联网服务提供者不得利用用户画像、消费记录或设备信息对同等交易条件下的消费者实行差异化定价（即俗称的“大数据杀熟”）；同时必须为用户提供便捷易用的“一键关闭个性化推荐”开关。\n\n　　执法部门表示，将通过常态化算法抽检与专项督查机制，严厉查处隐蔽性算法合谋违规行为，坚定维护公平竞争的市场秩序与消费者合法权益。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "白鹤滩水电站累计输送绿色清洁水电突破1500亿度，相当于减排二氧化碳1.2亿吨", "央视财经 2.2万评", "20小时前", R.drawable.news_green_energy, 0, 0)
                .withContent("　　金沙江下游白鹤滩水电站水声轰鸣，16台单机容量百万千瓦的水轮发电机组全部处于安全高效满负荷运行状态。中国三峡集团发布数据显示，电站累计外送清洁电量已顺利突破1500亿千瓦时大关。\n\n　　作为当今世界在建规模最大、技术难度最高的水电工程，白鹤滩水电站与乌东德、溪洛渡、向家坝、三峡、葛洲坝等六座巨型梯级电站共同构成了世界最大的“走廊清洁能源走廊”。\n\n　　这些清洁水能源源不断输送至华东、华中及粤港澳大湾区，每年有效替代标准煤4500万吨以上，为我国优化区域能源结构和绿色低碳转型构筑起坚实屏障。"));

        list.add(new News(News.TYPE_THREE_IMG, "传统非遗走进现代校园：木偶戏、苏绣与皮影老艺人手把手指导青年学子共创新国潮", "中国文化报 7900评", "昨天 15:30", R.drawable.news_museum_art, R.drawable.img2, R.drawable.img3)
                .withContent("　　在“非遗文化浸润校园”主题展演活动现场，古朴的皮影戏台前坐满了聚精会神的中小学生。国家级非遗传承人耐心地向孩子们讲解皮影雕刻、油彩上色与挑线操控的传统技艺。\n\n　　学生们不仅认真临摹传统纹样，更发挥天马行空的创意，将中国航天员、国产大飞机等现代元素融入皮影造型和木偶剧情创作中，排演出一幕幕别开生面的“新国潮皮影戏”。\n\n　　教育专家指出，非遗进校园不仅仅是一次技艺体验，更是以美育人、激发年轻一代中华文化自信与民族自豪感的生动载体。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "“中国天眼”FAST发现脉冲星数量已突破1000颗，为探索宇宙奥秘确立世界级基准", "中国科学院 2.8万评", "昨天 12:00", R.drawable.news_space_rocket, 0, 0)
                .withContent("　　中国科学院国家天文台宣布，被誉为“中国天眼”的500米口径球面射电望远镜（FAST）发现的新脉冲星数量已正式突破1000颗，其中包括毫秒脉冲星、双星系统及球状星团脉冲星等丰富样本，发现数量超过同期欧美所有射电望远镜的总和。\n\n　　脉冲星被天文学界誉为宇宙中超高精度的“天然时钟”。大量高灵敏度脉冲星的发现，为人类检验广义相对论、探测低频引力波以及构建未来自主深空自主导航系统提供了核心基础物理参数。\n\n　　目前FAST正向全球科学界稳步开放观测机时，多项利用FAST数据产出的突破性科学论文相继荣登国际顶级学术刊物《自然》与《科学》。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "乡村数字普惠金融创新赋能：手机“一键秒贷”支持新型农业经营主体适度规模发展", "经济日报 6400评", "昨天 09:40", R.drawable.news_school_study, 0, 0)
                .withContent("　　在皖北优质小麦种植示范区，家庭农场主老李正在用手机银行客户端提交春耕化肥采购授信申请。依托大数据农业保险与卫星遥感长势核验模型，短短两分钟内，20万元无抵押普惠信用贷款便审核到账。\n\n　　过去农户办理生产贷款需要反复跑网点、找担保人，周期长达半个月。如今多家国有大行依托涉农数据共享互通，推出了“信用户、信用村、信用镇”纯线上纯信用信贷产品，随借随还、按天计息。\n\n　　人民银行金融研究所分析，普惠数字金融精准滴灌田间地头，极大缓解了新型职业农民扩大生产、购置农机的资金周转压力，激活了乡村产业振兴的内生动力。"));

        list.add(new News(News.TYPE_THREE_IMG, "敦煌莫高窟“数字藏经洞”全球访问破千万人次：毫米级全息交互重现千年丝路文明", "人民日报文创 3.6万评", "昨天 08:20", R.drawable.news_museum_art, R.drawable.news_music_concert, R.drawable.img4)
                .withContent("　　敦煌研究院联合国内领先数字化团队历时五年打造的“数字藏经洞”云端体验项目，累计全球独立访问用户数顺利突破1000万人次。来自全球数十个国家和地区的观众足不出户即可“走进”洞窟，细细品读千年前珍贵写本与壁画艺术。\n\n　　技术团队运用激光扫描、摄影测量与毫米级三维点云重建技术，高保真还原了莫高窟第17窟藏经洞在晚唐、五代直至宋代的历史沿革，并利用AI语音驱动历史人物生动讲述敦煌文物的流转与保护历程。\n\n　　专家指出，现代数字化技术的赋能，不仅让饱经风霜的古老文化遗产得以“永久保存”，更为中华优秀传统文化走向世界架设起绚丽的数字化丝绸之路。"));

        return list;
    }

    private List<News> buildKangyi(){
        List<News> list = new ArrayList<>();
        list.add(new News(News.TYPE_SINGLE_IMG, "健康中国行动深入推进：我国居民人均预期寿命提升至78.6岁", "国家卫健委 1.4万评", "刚刚", R.drawable.news_medical_health, 0, 0)
                .withContent("　　国务院新闻办举行新闻发布会，介绍健康中国行动实施以来的显著成效。数据显示，我国居民主要健康指标已居于中高收入国家前列，人均预期寿命由2015年的76.3岁提高到78.6岁，孕产妇和婴儿死亡率持续降至历史新低。\n\n　　全国已建成紧密型县域医共体两千余个，基层医疗卫生机构标准化达标率超过90%，大病不出县、常见病多发病在基层解决的就医格局正在加速形成。\n\n　　卫健委表示，未来将进一步强化以健康为中心的慢病综合防控体系，推进全民健康生活方式普及。"));

        list.add(new News(News.TYPE_THREE_IMG, "新型mRNA多价靶向疫苗临床研发取得重要突破，生物医药全链条自主创新提速", "科技日报 9200评", "1小时前", R.drawable.news_medical_health, R.drawable.news_tech_chip, R.drawable.img1)
                .withContent("　　我国生物医药国家重点实验室在新型多价mRNA疫苗传递载体研究中取得关键进展。研发团队突破了纳米脂质颗粒（LNP）核心专利壁垒，合成出具有完全自主知识产权的新型可离子化脂质分子。\n\n　　实验表明，该载体在体内展现出更优异的稳定性和特异性靶向递送效率，有效降低了传统注射副反应，在多株流行性病毒动物攻毒模型中表现出极强的交叉中和抗体保护力。\n\n　　药监部门已开辟创新药械绿色审评审批通道，助力国产原创新药加速走向国际舞台。"));

        list.add(new News(News.TYPE_TEXT, "全国爱国卫生运动委员会：普及科学膳食指南，深入开展全民控盐减糖健康专项行动", "人民日报 8100评", "2小时前", 0, 0, 0)
                .withContent("　　全国爱卫办联合多部门印发健康饮食行动指引，号召全社会倡导“三减三健”（减盐、减油、减糖，健康口腔、健康体重、健康骨骼）理念。\n\n　　指导意见推动大中型餐饮企业和外卖平台标注食物热量与盐糖成分，并在学校食堂、机关企事业单位食堂全面推行低盐低脂健康营养餐食。\n\n　　专家指出，慢性心脑血管疾病与日常不良饮食结构密切相关，减盐减油是投入产出比极高的健康防线。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "儿童青少年前沿近视防控体系覆盖全国万所学校：白天户外活动2小时成硬指标", "新华社 1.1万评", "3小时前", R.drawable.news_school_study, 0, 0)
                .withContent("　　教育部与国家疾控局联合部署新一轮儿童青少年视力保护行动。新规明确要求各中小学校严格落实每天一小时体育课与一小时大课间户外活动，确保学生每日阳光下户外时间不低于两小时。\n\n　　全国各校正普遍采用电子屈光监测档案，每学期对中小学生视力开展动态跟踪建档，发现假性近视早期迹象及时通知家长进行干预指导。\n\n　　眼科医学专家强调，充足的日间自然光照射能刺激视网膜多巴胺分泌，是科学界公认最有效的近视预防手段。"));

        list.add(new News(News.TYPE_THREE_IMG, "5G远程超声与微创手术机器人下沉基层边疆：边远牧民在家门口享受三甲专家诊疗", "央视新闻 1.6万评", "4小时前", R.drawable.news_medical_health, R.drawable.img2, R.drawable.img3)
                .withContent("　　在海拔4500米的青藏高原县级医院，一台5G远程超声机器人正在北京三甲医院专家的精准操控下平稳运行。通过超低时延的5G网络切片技术，千里之外的专家操纵手柄，机械臂在病患腹部微米级移动，屏幕上实时传回超高清超声切面影像。\n\n　　当天，这套系统成功帮助一名患有急腹症的藏族同胞确诊并制定了科学微创救治方案，避免了长达数百公里的颠簸转诊。\n\n　　国家卫健委正加速推广‘互联网+智慧医疗’模式，让顶尖医疗资源插上科技翅膀飞入寻常百姓家。"));

        list.add(new News(News.TYPE_TEXT, "科普辟谣：秋冬季节呼吸道感染高发，专家详解科学用药与居家科学护理误区", "健康时报 5800评", "5小时前", 0, 0, 0)
                .withContent("　　针对秋冬季节流感、支原体及合胞病毒等呼吸道病原体混合传播风险，国家呼吸医学中心专家对公众关心的用药疑惑进行了权威解答。\n\n　　专家特别强调，抗生素对病毒性感染无效，滥用阿奇霉素或头孢不仅无益于病情，还可能导致肠道菌群失调和细菌耐药性产生。居家护理应注重室内通风、多喝温水、保证充分睡眠，一旦出现持续高热不退或胸闷气促应及时就医。\n\n　　保持良好的洗手习惯和科学佩戴口罩仍是抵御呼吸道飞沫传播的有效武器。"));

        return list;
    }

    private List<News> buildVideoSmall(){
        List<News> list = new ArrayList<>();
        list.add(new News(News.TYPE_SINGLE_IMG, "高燃短片：复兴号高铁司机第一视角穿行秦岭大桥，穿云破雾极速飞驰", "铁道光影 2.8万评", "刚刚", R.drawable.news_train_speed, 0, 0)
                .withContent("　　列车驾驶室前方4K超宽摄像机记录下震撼画面：时速350公里的复兴号动车组掠过壮丽的秦岭连绵群山，瞬间穿越一座座巍峨的崇山深隧与跨江特大桥。\n\n　　动车司机目光如炬，严格执行每一次手指口呼与信号确认，展现出当代中国高铁人的专业与严谨。"));

        list.add(new News(News.TYPE_THREE_IMG, "国家级非遗传承人展示纯手工金丝楠木榫卯微缩天坛，毫厘不差不用一根铁钉", "匠心手作 1.7万评", "20分钟前", R.drawable.img5, R.drawable.img6, R.drawable.img7)
                .withContent("　　老手艺人耗时整整八个月，用纯天然金丝楠木按照1:100比例精准缩微复制了北京天坛祈年殿。整座模型包含上千个微型斗拱与梁柱，完全依靠传统榫卯咬合支撑。\n\n　　轻按旋转底座，每一层屋檐与藻井精巧绝伦，展现了东方传统木构建筑巅峰的力学与美学智慧。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "红外相机抓拍萌趣瞬间：东北虎豹国家公园两只野生幼崽林间嬉戏打闹", "大自然物语 3.4万评", "1小时前", R.drawable.news_nature_park, 0, 0)
                .withContent("　　架设在长白山深处的红外触发式相机记录下生动画面：两只出生不久的野生东北虎幼崽在厚厚的白雪中翻滚嬉戏，虎妈妈在一旁悠闲守护。\n\n　　东北虎豹国家公园设立以来，野生虎豹种群已由最初的数十只恢复扩大至百只以上，中俄跨境生态廊道全面畅通。"));

        list.add(new News(News.TYPE_TEXT, "短视频科普：为什么空间站里的航天员吃面包会有危险？原来碎屑暗藏隐患", "太空漫谈 9800评", "2小时前", 0, 0, 0)
                .withContent("　　在空间站微重力环境下，任何细小的食物残渣或面包屑都不会自然掉落，而是会悬浮在空中。一旦飘入航天员鼻腔或气管可能导致窒息，吸入精密仪器设备更可能引发短路或火灾。\n\n　　因此空间站特制餐食多为压缩块状、膏体或一口即可吞下的特制无屑食品。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "延时摄影记录中国空间站飞越北京夜空，金色轨迹与鸟巢水立方同框", "星空追光者 2.1万评", "3小时前", R.drawable.news_space_rocket, 0, 0)
                .withContent("　　在晴朗无云的初秋傍晚，摄影师在奥林匹克公园精心架设超长曝光机位。中国空间站犹如一颗明亮的星辰从西北方升起，划出一道壮美的金色弧光，与地面的鸟巢、水立方交相辉映。\n\n　　镜头记录下了现代科技与城市建筑交融的浪漫画卷。"));

        list.add(new News(News.TYPE_THREE_IMG, "微距摄影探索昆虫世界的‘铠甲勇士’：独角仙羽化破蛹重生的精彩瞬间", "自然镜头 1.3万评", "4小时前", R.drawable.img8, R.drawable.img9, R.drawable.img10)
                .withContent("　　摄影师通过高倍微距镜头，连续守候72小时记录下双叉犀金龟（独角仙）从半透明蛹皮中蜕变羽化的珍贵全程。\n\n　　新生的外骨骼在几小时内从洁白如玉逐渐氧化硬化为深沉光亮的漆黑硬甲，生命蜕变的奇妙令人叹为观止。"));

        return list;
    }

    private List<News> buildBeijing(){
        List<News> list = new ArrayList<>();
        list.add(new News(News.TYPE_SINGLE_IMG, "北京中轴线申遗成功：千年文脉在新时代焕发勃勃生机", "北京日报 3.2万评", "刚刚", R.drawable.news_museum_art, 0, 0)
                .withContent("　　在联合国教科文组织第46届世界遗产大会上，“北京中轴线——中国理想都城秩序的杰作”正式列入《世界遗产名录》，我国世界遗产总数达到59项。\n\n　　全长7.8公里的北京中轴线始建于13世纪，历经元、明、清及近现代，汇聚了钟鼓楼、景山、故宫、天安门、正阳门及天坛、先农坛等世界级建筑杰作，代表了中国传统都城规划营建的崇高理念。\n\n　　北京市文物局表示，将以申遗成功为契机，深化老城整体保护与历史街区有机更新，让千年文脉更好融入现代市民美好生活。"));

        list.add(new News(News.TYPE_THREE_IMG, "中关村科学城前沿科技展启幕：自主类人机器人与具身智能领跑未来产业", "科技日报 1.8万评", "1小时前", R.drawable.news_tech_chip, R.drawable.news_smart_city, R.drawable.img1)
                .withContent("　　2026年中关村前沿科技展在北京海淀中关村国家自主创新示范区展示中心隆重开幕。展会汇聚了来自全国数百家顶尖人工智能与高精尖智造企业。\n\n　　在具身智能展区，多款自主研发的双足仿生人形机器人展示了在复杂地形自适应行走、灵巧手高精度装配以及自然人机语音互动的硬核本领。\n\n　　展会同期还发布了多项支持颠覆性技术创新的孵化扶持政策，加速北京国际科技创新中心建设。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "亮马河国际风情水岸升级开放：游船穿行夜色光影，打造‘不夜北京’活力地标", "首都文旅 1.5万评", "2小时前", R.drawable.news_smart_city, 0, 0)
                .withContent("　　朝阳区亮马河国际风情水岸全面完成二期品质提升工程并正式向市民游客亮相。全长数公里的亲水绿道串联起燕莎商圈、蓝色港湾与朝阳公园。\n\n　　夜幕降临，两岸桥梁与树木在环保LED艺术光影下倒映波光粼粼的水面，游船穿梭其间，两岸咖啡厅与创意集市游人如织，成为展现国际消费中心城市魅力的金名片。"));

        list.add(new News(News.TYPE_TEXT, "北京城市副中心‘三大文化建筑’全面对外开放：图书馆、博物馆与大剧院交相辉映", "新华社 2.1万评", "3小时前", 0, 0, 0)
                .withContent("　　坐落于通州大运河畔的北京城市副中心三大文化建筑——北京艺术中心、北京城市图书馆、北京大运河博物馆全面进入常态化开放阶段。\n\n　　其中北京城市图书馆拥有世界最大的超高玻璃幕墙和壮丽的‘银杏森林’阅读中庭，藏书容量超八百万册；三大建筑均达到最高绿色三星级建筑标准，成为市民精神文化生活的全新地标。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "大兴国际机场综合立体交通枢纽日客流量破15万人次，京津冀‘一小时交通圈’通达顺畅", "北京青年报 1.1万评", "4小时前", R.drawable.news_train_speed, 0, 0)
                .withContent("　　作为‘新世界七大奇迹’之首的北京大兴国际机场迎来客流高峰。机场集高铁、城际铁路、城市轨道交通与高速公路于一体，旅客可在航站楼地下一层实现高铁与地铁的零距离同台换乘。\n\n　　京雄城际铁路、津兴城际铁路的高密度开行，使得天津、雄安新区前往大兴机场的通勤时间压缩至30分钟左右，有力助推京津冀协同发展。"));

        list.add(new News(News.TYPE_THREE_IMG, "故宫博物院特展迎客流高峰：历代书画青铜国宝荟萃，沉浸式数字导览引人入胜", "光明日报 2.7万评", "5小时前", R.drawable.news_museum_art, R.drawable.img3, R.drawable.img4)
                .withContent("　　故宫文华殿历代珍品书画年度大展正式拉开帷幕，展出宋元明清国宝级名画及先秦铭文重器近两百件套，多件文物为近十年来首次公开面向公众展出。\n\n　　展览引入了全景数字长卷触摸屏和AR增强现实导览眼镜，观众可放大至微米级细节品味古代大师的线条笔意与钤印神韵，感受中华传统文化的厚重与典雅。"));

        return list;
    }

    private List<News> buildVideo(){
        List<News> list = new ArrayList<>();
        list.add(new News(News.TYPE_VIDEO, "【超清实拍】雪域高原上的钢铁动脉：青藏铁路守护者战风雪保畅通", "现场实录 3.2万评", "刚刚", R.drawable.news_train_speed, 0, 0)
                .withContent("　　镜头深入海拔近5000米的风火山段。在狂风肆虐、含氧量不足平原一半的高寒缺氧环境下，铁路工务段职工顶着零下30摄氏度的严寒开展线路几何尺寸巡检与冻土路基监测。\n\n　　他们日复一日地在生命禁区徒步巡线，用青春与热血守护着连接雪域高原各族群众的‘幸福天路’，确保进出藏旅客列车万无一失安全开行。"));

        list.add(new News(News.TYPE_VIDEO, "【纪录片】大国重器：世界最大水轮发电机组在白鹤滩地下厂房是怎样吊装成功的", "纪录片频道 2.4万评", "1小时前", R.drawable.news_green_energy, 0, 0)
                .withContent("　　记录下千吨级水轮发电机转子吊装的震撼全过程。起重机桥机吊具悬挂着重达2100吨的巨型转子，在百米深地下主厂房空间平稳位移近百米，最终精准落入定子圆心。\n\n　　现场激光测距仪器显示，庞然大物的下落对接同心度偏差不足0.05毫米，再次展示了中国重型工业制造与吊装施工的世界级水准。"));

        list.add(new News(News.TYPE_VIDEO, "【4K航拍】江山如画：无人机穿越万里长城金山岭，看雄关漫道云卷云舒", "空中视角 1.9万评", "2小时前", R.drawable.news_nature_park, 0, 0)
                .withContent("　　高清航拍镜头掠过崇山峻岭间依山而建的长城敌楼。清晨薄雾散去，金色晨曦为古老的砖石城垣镀上一层璀璨的光辉。\n\n　　蜿蜒起伏的城墙与苍翠险峻的天然断崖完美融合，磅礴气势令人心潮澎湃，展现出中华民族坚韧不拔的伟大精神图腾。"));

        list.add(new News(News.TYPE_VIDEO, "【科普解密】空间站机械臂究竟有多强？它如何在太空‘抓小行星’与搬运实验舱", "航天瞭望 2.8万评", "3小时前", R.drawable.news_space_rocket, 0, 0)
                .withContent("　　空间站核心舱搭载的十米长大机械臂拥有7个自由度，不仅能够像人类手臂一样灵活攀爬转移，更具备抓举25吨重大型实验舱段的巨大臂力。\n\n　　视频利用3D动画与太空真实操作录像，全方位解析了机械臂在辅助航天员出舱行走、搬运大型载荷以及应急巡检中的关键核心作用。"));

        list.add(new News(News.TYPE_VIDEO, "【城市镜像】延时摄影记录深圳前海十年变迁：从荒芜滩涂到现代化国际新城", "城市观察 1.5万评", "4小时前", R.drawable.news_smart_city, 0, 0)
                .withContent("　　十年光阴被浓缩在短短两分钟的延时影像中。原本海水拍打的滩涂滩头，在一台台塔吊与工程车的忙碌轰鸣中，拔地而起一座座现代化高科技总部大厦与生态海湾公园。\n\n　　前海的沧桑巨变，正是中国改革开放勇立潮头、开拓创新的生动缩影与崭新名片。"));

        list.add(new News(News.TYPE_VIDEO, "【实录采访】青年工匠说：在微米级的世界里，我找到了人生的追求与价值", "青年说 1.1万评", "5小时前", R.drawable.news_education_youth, 0, 0)
                .withContent("　　采访了三位荣获全国青年岗位能手的95后数控技术骨干。他们在普通的车工、钳工岗位上潜心钻研多年，在航空发动机叶片精密加工与模具制造中练就了一手绝活。\n\n　　他们坦言：‘技术没有捷径，把一件事做到极致，平凡的岗位也能绽放出最绚烂的青春光彩。’"));

        return list;
    }

    private List<News> buildEntertain(){
        List<News> list = new ArrayList<>();
        list.add(new News(News.TYPE_SINGLE_IMG, "中国交响乐团新年音乐会巡演圆满收官：经典国乐与交响编制擦出璀璨火花", "音乐中国 2.3万评", "刚刚", R.drawable.news_music_concert, 0, 0)
                .withContent("　　由中国交响乐团携手多位杰出国乐演奏家呈现的巡回音乐会在国家大剧院音乐厅落下帷幕。管弦乐《千里江山》与二胡协奏曲《长城随想》响彻金色殿堂，博得全场观众经久不息的掌声。\n\n　　指挥家在返场致辞中表示，中国传统音乐具有独特的意境与韵味，通过现代大型交响乐的丰富配器与声部对位，能够向世界听众更加立体生动地讲述当代中国故事。"));

        list.add(new News(News.TYPE_THREE_IMG, "舞蹈诗剧《只此青绿》迎来第500场演职纪念：传统宋风雅韵席卷海内外舞台", "艺坛风采 3.5万评", "1小时前", R.drawable.news_museum_art, R.drawable.img8, R.drawable.img9)
                .withContent("　　以北宋传世名画《千里江山图》为创作背景的现象级舞蹈诗剧《只此青绿》在保利剧院迎来第500场演出。剧场内座无虚席，许多青年观众身着汉服盛装观演。\n\n　　舞者们以静穆优雅的‘青绿腰’与典雅端庄的步伐，将宋代水墨丹青的山河气韵化为舞台上流动的诗篇，成功实现了传统优秀传统文化与现代舞台艺术形式的创造性转化与创新性发展。"));

        list.add(new News(News.TYPE_TEXT, "全国优秀现实题材电视剧展播启动：聚焦乡村振兴与青年创业，讲好普通人奋斗故事", "影视周刊 1.2万评", "2小时前", 0, 0, 0)
                .withContent("　　国家广电总局正式启动新一轮现实主义题材电视剧展播季。入选作品坚持‘深入生活、扎根人民’的创作导向，抛弃悬浮狗血套路，将镜头对准工厂车间、乡村田野与科技园区。\n\n　　多部电视剧以温暖细腻的笔触刻画了第一书记、返乡创业青年与一线科研人员的奋斗抉择，引发了年轻观众的强烈情感共鸣。"));

        list.add(new News(News.TYPE_SINGLE_IMG, "北京人艺经典话剧《茶馆》再次登台：老艺术家薪火相传，场场一票难求", "戏剧经典 1.8万评", "3小时前", R.drawable.img10, 0, 0)
                .withContent("　　北京人民艺术剧院首都剧场内掌声雷动，老舍先生经典名作《茶馆》开启新一轮纪念演出。本次复排特别注重青年演员梯队的传帮带，老中青三代艺术家同台献艺。\n\n　　王掌柜、常四爷、松二爷等经典形象在舞台上栩栩如生，地道的京腔京韵与深刻的历史沧桑感，再次向世人展现了经典现实主义戏剧穿越时空的永恒艺术魅力。"));

        list.add(new News(News.TYPE_THREE_IMG, "国际青年纪录片节揭晓各大奖项：中国青年导演原生态环保纪录长片斩获金奖", "光影艺术 1.4万评", "4小时前", R.drawable.news_nature_park, R.drawable.img1, R.drawable.img2)
                .withContent("　　在刚刚闭幕的第20届国际青年纪录片电影节上，由我国青年创作团队历时三年深入横断山脉拍摄的自然生态纪录长片《高原森林的守望者》荣膺最佳长纪录片金奖。\n\n　　评委会盛赞影片不仅拥有惊艳震撼的高清摄影与原生态自然声场，更深刻探讨了人与自然生态相互依存的哲学思考，展现出新生代中国纪录片人开阔的世界眼光。"));

        list.add(new News(News.TYPE_TEXT, "全国青年文学奖揭晓：以数字化与未来城市为背景的青年作家小说集广受好评", "文艺观察 7600评", "5小时前", 0, 0, 0)
                .withContent("　　中国作家协会主办的新一届青年文学奖正式公布评选结果。多部聚焦人工智能时代人际关系重构、城市化进程中青年心理成长的新锐中短篇小说脱颖而出。\n\n　　评委会主任表示，当代青年作家擅长捕捉科技日新月异给日常生活带来的微妙震颤，展现了充沛的想象力与扎实扎根中国大地的生活感知力。"));

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
