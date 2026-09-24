package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

public final class LegalDialogs {
    private LegalDialogs() {}

    public static void showAgreement(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("用户协议")
                .setMessage("《今日头条用户服务协议》\n\n1. 账号使用规范：用户应当对在注册账号项下的一切行为承担全部责任。\n2. 知识产权：软件内包含的文字、图表、软件代码均属于原开发者或著作权人。\n3. 本软件作为高校《移动应用开发》课程作业演示项目，仅供教学演示与离线评估使用。")
                .setPositiveButton("同意并继续", null)
                .show();
    }

    public static void showPrivacy(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("隐私政策")
                .setMessage("《今日头条隐私政策声明》\n\n1. 数据存储位置：所有个人资料、点赞收藏、私信与设置均仅存放于设备内部私有存储目录。\n2. 无云端上传：本教学演示项目不连接任何外部数据收集服务器。\n3. 权限管理：仅申请必要的本地读写与媒体解码权限。")
                .setPositiveButton("我已知晓", null)
                .show();
    }

    public static void showHelp(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("遇到问题 / 帮助中心")
                .setMessage("常见问题解答：\n\nQ: 无法登录怎么办？\nA: 勾选协议后点击“进入本地体验”或使用预设账号 13800000001 密码 123456 直接登录。\n\nQ: 离线视频无法播放？\nA: 本项目内嵌 raw 视频资源，无需网络即可秒级播放。")
                .setPositiveButton("我知道了", null)
                .show();
    }
}
