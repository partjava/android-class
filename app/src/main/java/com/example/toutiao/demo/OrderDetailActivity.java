package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    private ImageView btnBack;
    private LinearLayout llStatusHeader;
    private TextView tvDetailStatusTitle;
    private TextView tvDetailStatusDesc;
    private TextView tvExpressCompany;
    private TextView btnCopyExpress;
    private TextView tvLogisticsTimeline;
    private TextView tvReceiverName;
    private TextView tvReceiverPhone;
    private TextView tvReceiverAddress;
    private LinearLayout llDetailProducts;
    private TextView tvPriceTotal;
    private TextView tvPriceDiscount;
    private TextView tvPriceActual;
    private TextView tvMetaOrderId;
    private TextView tvMetaCreateTime;
    private TextView btnContactService;
    private TextView btnActionSecondary;
    private Button btnActionPrimary;

    private ShopStore store;
    private ShopStore.OrderItem currentOrder;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        store = new ShopStore(this);
        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);

        initViews();
        loadOrderDetail();
        setupEvents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        llStatusHeader = findViewById(R.id.ll_status_header);
        tvDetailStatusTitle = findViewById(R.id.tv_detail_status_title);
        tvDetailStatusDesc = findViewById(R.id.tv_detail_status_desc);
        tvExpressCompany = findViewById(R.id.tv_express_company);
        btnCopyExpress = findViewById(R.id.btn_copy_express);
        tvLogisticsTimeline = findViewById(R.id.tv_logistics_timeline);
        tvReceiverName = findViewById(R.id.tv_receiver_name);
        tvReceiverPhone = findViewById(R.id.tv_receiver_phone);
        tvReceiverAddress = findViewById(R.id.tv_receiver_address);
        llDetailProducts = findViewById(R.id.ll_detail_products);
        tvPriceTotal = findViewById(R.id.tv_price_total);
        tvPriceDiscount = findViewById(R.id.tv_price_discount);
        tvPriceActual = findViewById(R.id.tv_price_actual);
        tvMetaOrderId = findViewById(R.id.tv_meta_order_id);
        tvMetaCreateTime = findViewById(R.id.tv_meta_create_time);
        btnContactService = findViewById(R.id.btn_contact_service);
        btnActionSecondary = findViewById(R.id.btn_action_secondary);
        btnActionPrimary = findViewById(R.id.btn_action_primary);
    }

    private void loadOrderDetail() {
        if (orderId == null) {
            finish();
            return;
        }

        currentOrder = store.getOrder(orderId);
        if (currentOrder == null) {
            Toast.makeText(this, "未找到该订单", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Header Status
        updateHeaderStatus();

        // Logistics
        tvExpressCompany.setText(currentOrder.expressCompany + ": " + currentOrder.expressNumber);
        if ("已完成".equals(currentOrder.status)) {
            tvLogisticsTimeline.setText("· [已签收] 快件已妥投，签收人：本人签收\n· [派送中] 快递员正在为您派送中\n· [武汉市] 快件已到达光谷转运中心\n· [已揽收] 顺丰速运 已收取快件");
        } else if ("待收货".equals(currentOrder.status)) {
            tvLogisticsTimeline.setText("· [派送中] 快递员正在为您派送，请保持电话畅通\n· [武汉市] 快件已到达光谷转运中心\n· [干线运输] 快件正发往武汉转运中心\n· [已揽收] 顺丰速运 已收取快件");
        } else if ("已退款".equals(currentOrder.status)) {
            tvLogisticsTimeline.setText("· [退款成功] 款项已退回原支付账户\n· [申请已受理] 商家已同意退款申请\n· [申请退款] 买家发起了退款申请");
        } else {
            tvLogisticsTimeline.setText("· [仓库处理] 商品已打印电子面单，正在打包贴单\n· [备货完成] 智能仓库已完成拣货出库\n· [订单已确认] 买家已支付成功，等待发货");
        }

        // Address
        tvReceiverName.setText(currentOrder.receiverName);
        tvReceiverPhone.setText(currentOrder.receiverPhone);
        tvReceiverAddress.setText(currentOrder.receiverAddress);

        // Products
        llDetailProducts.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (ShopStore.CartItem ci : currentOrder.items) {
            View pView = inflater.inflate(R.layout.item_cart_product, llDetailProducts, false);
            pView.findViewById(R.id.cb_select).setVisibility(View.GONE);
            pView.findViewById(R.id.btn_minus).setVisibility(View.GONE);
            pView.findViewById(R.id.btn_plus).setVisibility(View.GONE);

            ImageView iv = pView.findViewById(R.id.iv_thumb);
            TextView tvTitle = pView.findViewById(R.id.tv_title);
            TextView tvCat = pView.findViewById(R.id.tv_category);
            TextView tvPrice = pView.findViewById(R.id.tv_price);
            TextView tvQty = pView.findViewById(R.id.tv_quantity);

            if (ci.imgRes != 0) iv.setImageResource(ci.imgRes);
            tvTitle.setText(ci.title);
            tvCat.setText(ci.category);
            tvPrice.setText("¥" + String.format(Locale.CHINA, "%.2f", ci.getPrice()));
            tvQty.setText("× " + ci.quantity);

            llDetailProducts.addView(pView);
        }

        // Prices
        tvPriceTotal.setText("¥" + String.format(Locale.CHINA, "%.2f", currentOrder.getTotal()));
        tvPriceDiscount.setText("-¥" + String.format(Locale.CHINA, "%.2f", currentOrder.getDiscount()));
        tvPriceActual.setText("¥" + String.format(Locale.CHINA, "%.2f", currentOrder.getActual()));

        // Meta
        tvMetaOrderId.setText("订单编号: " + currentOrder.orderId);
        tvMetaCreateTime.setText("创建时间: " + currentOrder.createTime);

        // Bottom Actions
        updateBottomActions();
    }

    private void updateHeaderStatus() {
        switch (currentOrder.status) {
            case "待发货":
                llStatusHeader.setBackgroundColor(Color.parseColor("#FF9800"));
                tvDetailStatusTitle.setText("等待商家发货");
                tvDetailStatusDesc.setText("您的订单正在加急打包中，预计24小时内发出");
                break;
            case "待收货":
                llStatusHeader.setBackgroundColor(Color.parseColor("#2196F3"));
                tvDetailStatusTitle.setText("商家已发货");
                tvDetailStatusDesc.setText("顺丰速运正在派送中，预计今日送达，请注意查收");
                break;
            case "已完成":
                llStatusHeader.setBackgroundColor(Color.parseColor("#4CAF50"));
                tvDetailStatusTitle.setText("交易已完成");
                tvDetailStatusDesc.setText("感谢您在头条商城选购商品，期待再次光临！");
                break;
            default: // 已退款
                llStatusHeader.setBackgroundColor(Color.parseColor("#78909C"));
                tvDetailStatusTitle.setText("退款成功");
                tvDetailStatusDesc.setText("款项已成功退回，退款金额 ¥" + String.format(Locale.CHINA, "%.2f", currentOrder.getActual()));
                break;
        }
    }

    private void updateBottomActions() {
        switch (currentOrder.status) {
            case "待发货":
                btnActionSecondary.setVisibility(View.VISIBLE);
                btnActionSecondary.setText("申请退款");
                btnActionSecondary.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("申请退款")
                            .setMessage("确定要申请退款吗？退款将原路返还。")
                            .setPositiveButton("申请退款", (d, w) -> {
                                store.updateOrderStatus(orderId, "已退款");
                                Toast.makeText(this, "退款已受理", Toast.LENGTH_SHORT).show();
                                loadOrderDetail();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                });

                btnActionPrimary.setText("提醒发货");
                btnActionPrimary.setOnClickListener(v -> {
                    Toast.makeText(this, "已向商家发出催发货通知！", Toast.LENGTH_SHORT).show();
                });
                break;

            case "待收货":
                btnActionSecondary.setVisibility(View.VISIBLE);
                btnActionSecondary.setText("延长收货");
                btnActionSecondary.setOnClickListener(v -> {
                    Toast.makeText(this, "已为您延长收货时间 3 天", Toast.LENGTH_SHORT).show();
                });

                btnActionPrimary.setText("确认收货");
                btnActionPrimary.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("确认收货")
                            .setMessage("请在确认收到商品且外观完好后再进行操作。")
                            .setPositiveButton("确认收货", (d, w) -> {
                                store.updateOrderStatus(orderId, "已完成");
                                Toast.makeText(this, "已确认收货，交易完成！", Toast.LENGTH_SHORT).show();
                                loadOrderDetail();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                });
                break;

            case "已完成":
                btnActionSecondary.setVisibility(View.VISIBLE);
                btnActionSecondary.setText("删除订单");
                btnActionSecondary.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("删除订单")
                            .setMessage("确定要删除这条订单吗？")
                            .setPositiveButton("删除", (d, w) -> {
                                store.deleteOrder(orderId);
                                Toast.makeText(this, "订单已删除", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                });

                btnActionPrimary.setText("再次购买");
                btnActionPrimary.setOnClickListener(v -> {
                    for (ShopStore.CartItem ci : currentOrder.items) {
                        store.addToCart(ci.title, ci.priceCents, ci.imgRes, ci.category, ci.quantity);
                    }
                    Toast.makeText(this, "商品已重新加入购物车", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, CartActivity.class));
                });
                break;

            default: // 已退款
                btnActionSecondary.setVisibility(View.GONE);
                btnActionPrimary.setText("删除订单");
                btnActionPrimary.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("删除订单")
                            .setMessage("确定要删除这条退款订单记录吗？")
                            .setPositiveButton("删除", (d, w) -> {
                                store.deleteOrder(orderId);
                                Toast.makeText(this, "订单已删除", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                });
                break;
        }
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());

        btnCopyExpress.setOnClickListener(v -> {
            if (currentOrder != null && currentOrder.expressNumber != null) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("express_number", currentOrder.expressNumber));
                    Toast.makeText(this, "运单号已复制到剪贴板", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnContactService.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("user_name", "商城官方客服");
            intent.putExtra("chat_type", "private");
            startActivity(intent);
        });
    }
}
