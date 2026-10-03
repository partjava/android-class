package com.example.toutiao.demo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CampusLandmark implements Serializable {
    public String id;
    public String name;             // 短名，如 "教1栋 · 5F"
    public String fullName;         // 全称，如 "教学楼1栋（智慧教学中心）"
    public String totalFloors;      // 楼层信息，如 "共5层"
    public String category;         // 分类：教学综合 / 实验科创 / 文体研学 / 生活宿区
    public float normX;             // 全景图归一化 X (0.0 ~ 1.0)
    public float normY;             // 全景图归一化 Y (0.0 ~ 1.0)
    public int resImage;            // 真实实景特写图
    public int resMarkedImage;      // 标注对比图
    public String tag;              // 标签微标
    public String desc;             // 概述
    public String floorsGuide;      // 楼层分布详情
    public String openHours;        // 开放时间
    public String servicePhone;     // 服务电话
    public String tips;             // 导览贴士

    public CampusLandmark(String id, String name, String fullName, String totalFloors,
                          String category, float normX, float normY,
                          int resImage, int resMarkedImage, String tag,
                          String desc, String floorsGuide, String openHours,
                          String servicePhone, String tips) {
        this.id = id;
        this.name = name;
        this.fullName = fullName;
        this.totalFloors = totalFloors;
        this.category = category;
        this.normX = normX;
        this.normY = normY;
        this.resImage = resImage;
        this.resMarkedImage = resMarkedImage;
        this.tag = tag;
        this.desc = desc;
        this.floorsGuide = floorsGuide;
        this.openHours = openHours;
        this.servicePhone = servicePhone;
        this.tips = tips;
    }

    public static List<CampusLandmark> getAllLandmarks() {
        List<CampusLandmark> list = new ArrayList<>();

        // 1. 图书馆
        list.add(new CampusLandmark(
                "library", "📚 图书馆 · 4F", "晴川图书馆（研学地标）", "共4层",
                "文体研学", 0.5271f, 0.2836f,
                R.drawable.user_crop_library, R.drawable.img_crop_library_marked,
                "藏书百万 · 依山傍水",
                "依傍惜缘湖，背靠龙泉山。集藏书、数字阅览、考研研修、学术报告于一体的现代化文献中心。",
                "1F：借还书大厅 · 密集书库 · 电子阅览中心\n2F：社会科学阅览区 · 考研专属研修席位（智慧预约）\n3F：自然科学与工程技术书库 · 临湖全景落地窗研修台\n4F：特藏文献阅览室 · 学术报告厅 · 研讨沙龙空间",
                "周一至周日 08:00 - 22:00（考试周延长至 22:30）",
                "027-87934400",
                "二楼南侧配有考研学子专属固定席位；三楼观湖平台视野极佳，是晴川最美读书圣地。"
        ));

        // 2. 惜缘湖 / 情缘湖
        list.add(new CampusLandmark(
                "lake", "🌊 惜缘湖", "惜缘湖（情缘湖畔水景）", "自然水系",
                "文体研学", 0.5336f, 0.4305f,
                R.drawable.user_crop_lake, R.drawable.img_crop_lake_marked,
                "碧波微澜 · 樱花环绕",
                "晴川核心生态水景名片。湖水碧如翡翠，周边修筑有亲水环湖塑胶步道与樱花长廊，白鹭常栖。",
                "环湖景观带：亲水栈道 · 樱花大道 · 湖心石桥 · 亲水观景台",
                "全天开放",
                "后勤保卫：027-87934488",
                "三月樱花季环湖漫步最惬意，黄昏时分夕阳映照湖面与图书馆倒影，是绝佳摄影机位。"
        ));

        // 3. 田径运动场
        list.add(new CampusLandmark(
                "stadium", "⚽ 田径场", "晴川田径运动场", "标准看台",
                "文体研学", 0.7424f, 0.4115f,
                R.drawable.user_crop_stadium, R.drawable.img_crop_stadium_marked,
                "阳光长跑 · 塑胶跑道",
                "400米标准国际塑胶跑道、天然草坪足球场、全天候灯光照明看台，是全校体育盛会与日常晨晚练中枢。",
                "主看台区：主席台 · 体能测试中心 · 器材室\n运动区：400米环形跑道 · 11人制标准足球场 · 跳远沙坑",
                "每日 06:00 - 22:00（夜间灯光开放至 21:30）",
                "027-87934488",
                "校园阳光长跑打卡点位设在主看台入口旁；早晚晨跑人群较多，请遵守逆时针跑步礼仪。"
        ));

        // 4. 室内体育馆
        list.add(new CampusLandmark(
                "gym", "🏀 体育馆 · 3F", "室内综合体育馆", "共3层",
                "文体研学", 0.7216f, 0.2655f,
                R.drawable.user_crop_gym, R.drawable.img_crop_gym_marked,
                "室内场馆 · 运动健身",
                "全天候现代化室内运动场馆，内设标准木地板篮球场、羽毛球场、乒乓球室与健身力量区。",
                "1F：乒乓球综合训练馆 · 健身力量房 · 运动康复室\n2F：标准木地板室内篮球主比赛场 · 羽毛球场地群\n3F：形体舞蹈房 · 健美操训练厅 · 裁判工作室",
                "周一至周日 08:30 - 21:30",
                "027-87934489",
                "入馆打球请穿着无痕运动鞋；校队集训期间部分羽毛球场地需提前预约。"
        ));

        // 5. 教1栋
        list.add(new CampusLandmark(
                "teach_1", "🎓 教1栋 · 5F", "教学楼1栋（基础教学大楼）", "共5层",
                "教学综合", 0.3930f, 0.8684f,
                R.drawable.user_crop_teach_1, R.drawable.img_crop_teach_1_marked,
                "智慧多媒体 · 标准教室",
                "学校核心本科教学大楼之一，配备高流明多媒体投影、双师智慧黑板与静音空调系统。",
                "1F：101-112 阶梯大教室（通识课、公共大课专用）\n2F：201-216 标准多媒体教室（外语、思政课教学区）\n3F：301-316 专业基础课教室群\n4F：401-416 数字化互动多媒体智慧研讨室\n5F：501-512 学院教师教研备课室与答辩会议室",
                "每日 07:30 - 21:30",
                "教务值班：027-87934455",
                "每层洗手间旁均设有冷热直饮水机；晚自习期间1~2楼大教室开放自由自习。"
        ));

        // 6. 教2栋
        list.add(new CampusLandmark(
                "teach_2", "🎓 教2栋 · 5F", "教学楼2栋（专业核心大楼）", "共5层",
                "教学综合", 0.2354f, 0.9152f,
                R.drawable.user_crop_teach_2, R.drawable.img_crop_teach_2_marked,
                "梯形大教室 · 答辩报告厅",
                "承载计算机、电子信息、商学院等核心专业课程教学，梯形阶梯多媒体大教室功能完备。",
                "1F：大型阶梯学术报告厅（200人座，配备同传系统）\n2F：201-214 专业核心课教室群\n3F：301-314 软硬件结合示范教学大厅\n4F：401-414 本科毕业设计专用研讨教室\n5F：学术沙龙与产学研合作项目孵化室",
                "每日 07:30 - 21:30",
                "教学服务：027-87934456",
                "全楼覆盖 QCU-WiFi，支持一键投屏与录播课程回放。"
        ));

        // 7. 教3栋
        list.add(new CampusLandmark(
                "teach_3", "🎓 教3栋 · 5F", "教学楼3栋（文理综合大楼）", "共5层",
                "教学综合", 0.4154f, 0.6728f,
                R.drawable.user_crop_teach_3, R.drawable.img_crop_teach_3_marked,
                "文理融合 · 智慧研讨",
                "承载外国语、文学艺术、数学建模等综合学科教学，中庭绿意环绕，通透明亮。",
                "1F：多语种语音同声传译实训室 · 多媒体大教室\n2F：高等数学与建模教学研讨中心\n3F：301-312 智慧交互讨论型教室\n4F：401-412 本科文科专业课程大厅\n5F：学科竞赛备赛工作室与社团学术中心",
                "每日 07:30 - 21:30",
                "027-87934457",
                "教3栋与教1栋之间有空中风雨连廊相连，下雨天可免淋雨直接穿行。"
        ));

        // 8. 教4栋
        list.add(new CampusLandmark(
                "teach_4", "🎓 教4栋 · 5F", "教学楼4栋（创新研学大楼）", "共5层",
                "教学综合", 0.2654f, 0.7317f,
                R.drawable.user_crop_teach_4, R.drawable.img_crop_teach_4_marked,
                "创新研讨 · 小班互动",
                "配备圆桌讨论式多功能智慧教室，重点服务创新创业、学科交叉研讨与小班精品课程。",
                "1F：大学生创新创业成果展示厅 · 小班研讨室群\n2F：精品示范录播教室 · 远程微格教学研讨区\n3F：跨学科协同教学创新基地\n4F：学术报告与研究生预科教学大厅\n5F：学科带头人工作室与教发中心研讨室",
                "每日 07:30 - 21:30",
                "027-87934458",
                "教4栋西侧林荫道直通小剧场与食堂，下课就餐路线极近。"
        ));

        // 9. 综1栋
        list.add(new CampusLandmark(
                "admin_1", "🏛️ 综1栋 · 5F", "综合楼1栋（党政服务与一站式大厅）", "共5层",
                "教学综合", 0.7698f, 0.7373f,
                R.drawable.user_crop_admin_1, R.drawable.img_crop_admin_1_marked,
                "行政中枢 · 一站式服务",
                "学校核心行政办公与师生服务中枢，一楼设有一站式学生办事大厅，综合业务一窗通办。",
                "1F：学生一站式综合服务大厅（教务/学工/财务/户籍/卡务）\n2F：党政办公室 · 党委组织部 · 宣传部\n3F：教务处 · 科研处 · 质量评估监控中心\n4F：人事处（党委教师工作部）· 财务处办公室\n5F：第一会议室 · 校长办公会议厅",
                "周一至周五 08:30 - 12:00, 14:00 - 17:30",
                "一站式大厅：027-87934455",
                "办理成绩单打印、在读证明及学生证补办请直接前往一楼自助打印终端机，凭校园卡24小时可打。"
        ));

        // 10. 综2栋
        list.add(new CampusLandmark(
                "admin_2", "🏛️ 综2栋 · 5F", "综合楼2栋（学院行政与学工中心）", "共5层",
                "教学综合", 0.7008f, 0.5602f,
                R.drawable.user_crop_admin_2, R.drawable.img_crop_admin_2_marked,
                "学院中枢 · 团委学工",
                "各二级学院主要党政行政办公室、校团委及招生就业处所在地，临近实验实训集群。",
                "1F：招生就业处 · 毕业生就业指导中心 · 招聘宣讲厅\n2F：校团委 · 学生会 · 青年志愿者协会办公室\n3F：计算机学院 · 电子信息学院联合党政办公室\n4F：商学院 · 外国语学院联合党政办公室\n5F：设计工程学院 · 传媒艺术学院办公室",
                "周一至周五 08:30 - 17:30",
                "就业指导：027-87934411",
                "每年春招秋招期间，一楼大厅与前坪会举办多场大型校园双选会。"
        ));

        // 11. 实1栋
        list.add(new CampusLandmark(
                "lab_1", "🔬 实1栋 · 5F", "实验实训楼1栋（智能工程实验中心）", "共5层",
                "实验科创", 0.8625f, 0.5561f,
                R.drawable.user_crop_lab_1, R.drawable.img_crop_lab_1_marked,
                "工程实训 · 智能装备",
                "集电气工程、智能电网、机器人自动化于一体的高规格实训中心，配有高压仿真平台。",
                "1F：电力系统继电保护实训基地 · 高压电气绝缘实验室\n2F：工业机器人与PLC智能控制自动化实训大厅\n3F：嵌入式系统与单片机创新设计实验室\n4F：物联网与传感器网络技术实训中心\n5F：电子设计竞赛备赛基地与创新工坊",
                "每日 08:00 - 21:00",
                "实验安全科：027-87934460",
                "进入实验室请严格佩戴实验工牌，遵从安全操作规程，严禁携带易燃物及食物饮料。"
        ));

        // 12. 实2栋
        list.add(new CampusLandmark(
                "lab_2", "🔬 实2栋 · 5F", "实验实训楼2栋（信息技术与交叉中心）", "共5层",
                "实验科创", 0.9021f, 0.7331f,
                R.drawable.user_crop_lab_2, R.drawable.img_crop_lab_2_marked,
                "前沿科技 · 交叉融合",
                "涵盖光电工程、通信技术、智能虚拟仿真及跨境电商实训，支撑多学科前沿实验教学。",
                "1F：光电信息与微波通信高精尖实验室\n2F：虚拟现实（VR/AR）仿真交互实训空间\n3F：数字媒体与动画渲染工作站集群\n4F：现代金融与跨境电商数字化模拟交易大厅\n5F：企业级产教融合联合工程实验室",
                "每日 08:00 - 21:00",
                "027-87934461",
                "机房全天候配备冷气恒温系统与千兆光纤内网环境。"
        ));

        // 13. 美术馆
        list.add(new CampusLandmark(
                "art_museum", "🎨 美术馆 · 3F", "晴川美术馆（艺术设计展厅）", "共3层",
                "实验科创", 0.7844f, 0.5357f,
                R.drawable.user_crop_art_museum, R.drawable.img_crop_art_museum_marked,
                "艺术展厅 · 美学熏陶",
                "典雅现代的校园艺术殿堂，常年展出师生优秀美术作品、毕业设计创意展、非遗书画巡展。",
                "1F：大型主题美术临展厅 · 雕塑艺术空间\n2F：视觉传达与环境艺术毕业设计固定展厅\n3F：学术沙龙鉴赏厅 · 艺术大师工作室",
                "周二至周日 09:00 - 17:00（周一闭馆）",
                "027-87934462",
                "毕业季期间举办盛大的晴川毕业设计开放周，面向全社会免费观展。"
        ));

        // 14. 计算机中心 & 大数据中心
        list.add(new CampusLandmark(
                "computer_center", "💻 计算中心 · 4F", "计算机中心 & 大数据中心", "共4层",
                "实验科创", 0.8242f, 0.7391f,
                R.drawable.user_crop_computer_center, R.drawable.img_crop_computer_center_marked,
                "云上机房 · 大数据算力",
                "全校公共计算机机房群、云桌面教学中心、超算数据中枢与国家级计算机等级考点。",
                "1F：全校校园网核心机房 · 统一身份认证与数据中心服务器机房\n2F：第1~4 云桌面机房（全国计算机等级考试考场）\n3F：大数据与人工智能算法实训机房群\n4F：软件工程全生命周期开发实战工场",
                "每日 08:00 - 21:30",
                "网络信息中心：027-87934433",
                "计算机等级考试（NCRE）及教资机考指定考场；上机自习需凭个人学号刷卡登录。"
        ));

        // 15. 学生大食堂
        list.add(new CampusLandmark(
                "canteen_main", "🍱 主食堂 · 3F", "学生第一餐饮中心（主食堂）", "共3层",
                "生活宿区", 0.3404f, 0.3457f,
                R.drawable.user_crop_canteen_main, R.drawable.img_crop_canteen_main_marked,
                "美食荟萃 · 风味特色",
                "晴川最大餐饮生活聚集地，三层宽敞明亮就餐大厅，汇集全国各地风味美食、大众快餐与烘焙轻食。",
                "1F：大众经济自选快餐 · 铁板烧 · 生滚海鲜砂锅粥 · 水饺小笼包\n2F：风味特色档口（黄焖鸡/麻辣烫/重庆小面/过桥米线/清真餐厅）\n3F：教工餐厅 · 聚会包厢 · 阳光露台咖啡烘焙吧",
                "早餐 06:40-09:00，午餐 11:00-13:30，晚餐 16:30-20:30",
                "饮食服务科：027-87934477",
                "支持校园一卡通刷卡及微信、支付宝扫码支付；二楼特设清真餐饮专属档口。"
        ));

        // 16. 小剧场 & 校园超市
        list.add(new CampusLandmark(
                "theater_market", "🎭 剧场&超市 · 2F", "晴川小剧场 & 综合惠民超市", "共2层",
                "生活宿区", 0.2997f, 0.5009f,
                R.drawable.user_crop_theater_market, R.drawable.img_crop_theater_market_marked,
                "人文剧场 · 便民商超",
                "一楼为大型校园平价自选超市与快递服务驿站，二楼为配备专业灯光音响的人文学术小剧场。",
                "1F：天猫校园平价超市 · 水果鲜切吧 · 快递综合自提中心（菜鸟驿站）\n2F：晴川小剧场（容量500人，适合话剧、十佳歌手、学术研讨巡讲）",
                "超市：07:30 - 22:30；剧场依活动安排开放",
                "便民热线：027-87934478",
                "一楼超市提供生活日用品、文具打印、冷饮零食全套采购，价格实惠贴心。"
        ));

        // 17. 学生公寓区
        list.add(new CampusLandmark(
                "dorm_cluster", "🏠 学生公寓 · 6F", "学生公寓园区（寝1~10栋集群）", "共10栋 · 各6层",
                "生活宿区", 0.5080f, 0.0850f,
                R.drawable.user_crop_dorm_cluster, R.drawable.user_crop_dorm_cluster,
                "宜居家园 · 6层标准公寓",
                "10栋标准化园林式学生公寓群。每栋均为6层建筑，配备人脸识别门禁、智能充电桩与自习活动室。",
                "寝1栋~寝4栋：男生公寓园区（近大食堂与运动场）\n寝5栋~寝8栋：核心生活公寓园区（近小食堂与洗浴中心）\n寝9栋~寝10栋：林荫学生公寓（依山面湖，视野幽静）\n公共配套：每层配备共享洗衣房、烘干机与智能冷热开水机",
                "公寓门禁开放：06:00 - 23:00（周末及节假日 23:30 闭门）",
                "宿舍物业服务中心：027-87934499",
                "楼下设有智能共享单车停放区与带雨棚充电车棚，严禁私拉电线或携带违章大功率电器。"
        ));

        // 18. 学校南门 / 正校门
        list.add(new CampusLandmark(
                "south_gate", "🚪 正大门", "武汉晴川学院南大门（正门广场）", "校门牌楼",
                "教学综合", 0.5984f, 0.9166f,
                R.drawable.user_crop_south_gate, R.drawable.img_crop_south_gate_marked,
                "晴川地标 · 迎宾牌坊",
                "晴川标志性古典正门大门，汉白玉校名牌匾，前方为开阔的礼仪喷泉迎宾广场与绿色林荫大道。",
                "入口设施：智能车辆自动抬杆识别系统 · 访客行人无感人脸门禁通道 · 保卫值班亭",
                "24小时常态开放通行",
                "校卫保卫处：027-87934110",
                "校外车辆入校须提前在微信公众号或保卫处登记报备；新生报到迎宾主打卡点。"
        ));

        return list;
    }
}
