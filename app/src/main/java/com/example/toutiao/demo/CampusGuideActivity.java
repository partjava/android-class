package com.example.toutiao.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class CampusGuideActivity extends AppCompatActivity {

    public static void start(Context context) {
        Intent intent = new Intent(context, CampusGuideActivity.class);
        context.startActivity(intent);
    }

    public static class Landmark {
        public String name;
        public String tag;
        public String desc;
        public String hours;
        public String location;
        public String phone;
        public String tips;

        public Landmark(String name, String tag, String desc, String hours, String location, String phone, String tips) {
            this.name = name;
            this.tag = tag;
            this.desc = desc;
            this.hours = hours;
            this.location = location;
            this.phone = phone;
            this.tips = tips;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_guide);

        ImageView ivBack = findViewById(R.id.iv_guide_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        LinearLayout container = findViewById(R.id.ll_landmarks_container);
        renderLandmarks(container);
    }

    private void renderLandmarks(LinearLayout container) {
        List<Landmark> landmarks = getCampusLandmarks();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Landmark lm : landmarks) {
            View card = inflater.inflate(R.layout.item_campus_landmark, container, false);
            TextView tvName = card.findViewById(R.id.tv_landmark_name);
            TextView tvTag = card.findViewById(R.id.tv_landmark_tag);
            TextView tvDesc = card.findViewById(R.id.tv_landmark_desc);
            TextView tvHours = card.findViewById(R.id.tv_landmark_hours);
            TextView tvLocation = card.findViewById(R.id.tv_landmark_location);
            View btnDetail = card.findViewById(R.id.btn_landmark_detail);

            tvName.setText(lm.name);
            tvTag.setText(lm.tag);
            tvDesc.setText(lm.desc);
            tvHours.setText(lm.hours);
            tvLocation.setText(lm.location);

            View.OnClickListener clickListener = v -> showLandmarkDetail(lm);
            card.setOnClickListener(clickListener);
            btnDetail.setOnClickListener(clickListener);

            container.addView(card);
        }
    }

    private void showLandmarkDetail(Landmark lm) {
        StringBuilder sb = new StringBuilder();
        sb.append(lm.desc).append("\n\n");
        sb.append(lm.hours).append("\n");
        sb.append(lm.location).append("\n");
        if (lm.phone != null && !lm.phone.isEmpty()) {
            sb.append(lm.phone).append("\n");
        }
        sb.append("\n").append(lm.tips);

        new AlertDialog.Builder(this)
                .setTitle(lm.name + " · 详细导引")
                .setMessage(sb.toString())
                .setPositiveButton("我知道了", null)
                .show();
    }

    private List<Landmark> getCampusLandmarks() {
        List<Landmark> list = new ArrayList<>();

        list.add(new Landmark(
                "📚 晴川图书馆",
                "研学圣地",
                "藏书百万册，依山傍水，内设静音自习区、考研研读室、学术报告厅及电子检索中心。",
                "⏰ 开放时间：周一至周日 08:00 - 22:00（期末考试周延长至22:30）",
                "📍 校园方位：核心教学区东侧，临近龙泉山晨读小径",
                "📞 服务热线：027-87934400",
                "💡 导学贴士：二楼南侧为考研学子专属研修席位，三楼设有全景落地窗观湖阅读区，环境安静惬意。"
        ));

        list.add(new Landmark(
                "🏛️ 综合教学楼",
                "地标主楼",
                "晴川地标性古典红砖建筑，学术报告大厅、校级行政服务中心与主要学院行政办公中心所在地。",
                "⏰ 开放时间：每日 07:30 - 21:30",
                "📍 校园方位：正校门主轴线正前方，迎宾广场正北侧",
                "📞 服务热线：027-87934455",
                "💡 导学贴士：一楼大厅设有学生一站式办事大厅，可办理教务证明打印、学籍核验与校园卡补办。"
        ));

        list.add(new Landmark(
                "🎓 智慧教学楼群 (A/B/C/D座)",
                "教学实验",
                "标准化智慧多媒体教室群、计算机云计算实训基地、电子信息与电气工程高精尖实验室。",
                "⏰ 开放时间：每日 07:30 - 21:30",
                "📍 校园方位：综合楼后方中心教学连廊，四座连体通达",
                "📞 服务热线：027-87934466",
                "💡 导学贴士：全楼栋覆盖校园无线网络覆盖（QCU-WiFi），教室内配有空调与护眼LED智慧灯光系统。"
        ));

        list.add(new Landmark(
                "⚽ 田径运动场 & 室内体育馆",
                "阳光运动",
                "400米标准塑胶跑道、天然草坪足球场、室外灯光网球场、篮球场群及室内羽毛球乒乓球馆。",
                "⏰ 开放时间：每日 06:00 - 22:00（夜间灯光开放至21:30）",
                "📍 校园方位：校园西侧生活运动区，紧邻学生宿舍群",
                "📞 服务热线：027-87934488",
                "💡 导学贴士：早晨06:30与傍晚18:00为师生跑步锻炼高峰期，校园阳光长跑打卡点位即设在田径场主看台入口。"
        ));

        list.add(new Landmark(
                "🎭 晴川大剧场",
                "艺术殿堂",
                "高规格现代化千人剧场，承办学校大型开学典礼、毕业晚会、校园文化樱花艺术节及高雅艺术进校园巡演。",
                "⏰ 开放时间：演出活动及重大典礼期间开放",
                "📍 校园方位：文体艺术中心主楼",
                "📞 服务热线：027-87934422",
                "💡 导学贴士：座位容量1200余座，配有专业声学舞台与剧院级灯光音响，是晴川学子展现才华的璀璨舞台。"
        ));

        list.add(new Landmark(
                "🍱 美食生活街区 (第一/第二食堂)",
                "美食生活",
                "涵盖大众自选快餐、风味特色档口、铁板烧烤、生滚靓汤、奶茶咖啡休闲吧，风味正宗价格实惠。",
                "⏰ 营业时间：早餐 07:00-09:00，午餐 11:00-13:30，晚餐 16:30-21:00",
                "📍 校园方位：学生公寓生活区中枢",
                "📞 服务热线：027-87934477",
                "💡 导学贴士：支持校园卡、微信支付与支付宝，二食堂二楼特设特色夜宵档口与现磨咖啡吧。"
        ));

        list.add(new Landmark(
                "🌸 龙泉山与汤逊湖自然风光",
                "生态名胜",
                "晴川天然绿色生态屏障。环山面水，三月万株樱花竞相盛放，微风拂湖，白鹭翻飞，诗意盎然。",
                "⏰ 开放时间：全天开放",
                "📍 校园方位：学校东部与南部外围环湖步道",
                "📞 景观导览：晴川樱花季摄影打卡推荐点",
                "💡 导学贴士：每年三月中旬为晴川樱花盛花期，环湖绿道与龙泉山脚下的樱花小道为校园最佳摄影打卡点。"
        ));

        return list;
    }
}
