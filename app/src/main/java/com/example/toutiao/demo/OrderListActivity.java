package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderListActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_TAB = "extra_initial_tab";

    private ImageView btnBack;
    private TextView tabAll, tabShipping, tabReceiving, tabCompleted, tabRefunded;
    private RecyclerView rvOrders;
    private LinearLayout llEmpty;
    private Button btnGoShop;

    private final List<TextView> tabViews = new ArrayList<>();
    private final String[] tabNames = {"全部", "待发货", "待收货", "已完成", "已退款"};
    private int currentTab = 0;

    private ShopStore store;
    private final List<ShopStore.OrderItem> orderList = new ArrayList<>();
    private OrderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        store = new ShopStore(this);
        initViews();
        setupTabs();
        setupEvents();

        String initialTab = getIntent().getStringExtra(EXTRA_INITIAL_TAB);
        if (initialTab != null) {
            for (int i = 0; i < tabNames.length; i++) {
                if (tabNames[i].equals(initialTab)) {
                    selectTab(i);
                    break;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tabAll = findViewById(R.id.tab_all);
        tabShipping = findViewById(R.id.tab_shipping);
        tabReceiving = findViewById(R.id.tab_receiving);
        tabCompleted = findViewById(R.id.tab_completed);
        tabRefunded = findViewById(R.id.tab_refunded);
        rvOrders = findViewById(R.id.rv_orders);
        llEmpty = findViewById(R.id.ll_empty);
        btnGoShop = findViewById(R.id.btn_go_shop);

        tabViews.add(tabAll);
        tabViews.add(tabShipping);
        tabViews.add(tabReceiving);
        tabViews.add(tabCompleted);
        tabViews.add(tabRefunded);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter();
        rvOrders.setAdapter(adapter);
    }

    private void setupTabs() {
        for (int i = 0; i < tabViews.size(); i++) {
            final int index = i;
            tabViews.get(i).setOnClickListener(v -> selectTab(index));
        }
        selectTab(0);
    }

    private void selectTab(int index) {
        currentTab = index;
        for (int i = 0; i < tabViews.size(); i++) {
            boolean sel = (i == index);
            tabViews.get(i).setTextColor(ContextCompat.getColor(this, sel ? R.color.brand_red : R.color.text_secondary));
            tabViews.get(i).getPaint().setFakeBoldText(sel);
        }
        loadOrders();
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());
        btnGoShop.setOnClickListener(v -> finish());
    }

    private void loadOrders() {
        orderList.clear();
        List<ShopStore.OrderItem> all = store.getOrders();
        String filter = tabNames[currentTab];

        for (ShopStore.OrderItem it : all) {
            if ("全部".equals(filter) || filter.equals(it.status)) {
                orderList.add(it);
            }
        }

        adapter.notifyDataSetChanged();
        boolean empty = orderList.isEmpty();
        llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            final TextView tvOrderId;
            final TextView tvOrderStatus;
            final LinearLayout llCardProducts;
            final TextView tvOrderTime;
            final TextView tvOrderSummary;
            final TextView btnActionSecondary;
            final TextView btnActionPrimary;

            VH(@NonNull View itemView) {
                super(itemView);
                tvOrderId = itemView.findViewById(R.id.tv_order_id);
                tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
                llCardProducts = itemView.findViewById(R.id.ll_card_products);
                tvOrderTime = itemView.findViewById(R.id.tv_order_time);
                tvOrderSummary = itemView.findViewById(R.id.tv_order_summary);
                btnActionSecondary = itemView.findViewById(R.id.btn_action_secondary);
                btnActionPrimary = itemView.findViewById(R.id.btn_action_primary);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ShopStore.OrderItem order = orderList.get(position);
            holder.tvOrderId.setText("订单编号: " + order.orderId);
            holder.tvOrderStatus.setText(order.status);
            holder.tvOrderTime.setText(order.createTime);

            int totalCount = 0;
            for (ShopStore.CartItem ci : order.items) totalCount += ci.quantity;
            holder.tvOrderSummary.setText("共 " + totalCount + " 件  实付 ¥" + String.format(Locale.CHINA, "%.2f", order.getActual()));

            // 状态文字颜色
            if ("待发货".equals(order.status)) {
                holder.tvOrderStatus.setTextColor(Color.parseColor("#FF9800"));
            } else if ("待收货".equals(order.status)) {
                holder.tvOrderStatus.setTextColor(Color.parseColor("#2196F3"));
            } else if ("已完成".equals(order.status)) {
                holder.tvOrderStatus.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                holder.tvOrderStatus.setTextColor(Color.parseColor("#9E9E9E"));
            }

            // 动态展示该订单的商品预览（最多显示 3 件）
            holder.llCardProducts.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(OrderListActivity.this);
            int showCount = Math.min(order.items.size(), 3);
            for (int i = 0; i < showCount; i++) {
                ShopStore.CartItem ci = order.items.get(i);
                View pView = inflater.inflate(R.layout.item_cart_product, holder.llCardProducts, false);
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

                holder.llCardProducts.addView(pView);
            }

            // 操作按钮动态配置
            configureActions(holder, order);

            holder.itemView.setOnClickListener(v -> openDetail(order.orderId));
        }

        private void configureActions(VH holder, ShopStore.OrderItem order) {
            holder.btnActionPrimary.setOnClickListener(v -> openDetail(order.orderId));
            holder.btnActionPrimary.setText("查看详情");

            if ("待发货".equals(order.status)) {
                holder.btnActionSecondary.setVisibility(View.VISIBLE);
                holder.btnActionSecondary.setText("申请退款");
                holder.btnActionSecondary.setOnClickListener(v -> {
                    new AlertDialog.Builder(OrderListActivity.this)
                            .setTitle("申请退款")
                            .setMessage("确定要取消此订单并退款吗？款项将原路退回。")
                            .setPositiveButton("申请退款", (d, w) -> {
                                store.updateOrderStatus(order.orderId, "已退款");
                                Toast.makeText(OrderListActivity.this, "退款已受理，订单已更新", Toast.LENGTH_SHORT).show();
                                loadOrders();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                });
                holder.btnActionPrimary.setText("提醒发货");
                holder.btnActionPrimary.setOnClickListener(v -> {
                    Toast.makeText(OrderListActivity.this, "已向商家发出催发货通知！", Toast.LENGTH_SHORT).show();
                });
            } else if ("待收货".equals(order.status)) {
                holder.btnActionSecondary.setVisibility(View.VISIBLE);
                holder.btnActionSecondary.setText("查看物流");
                holder.btnActionSecondary.setOnClickListener(v -> openDetail(order.orderId));

                holder.btnActionPrimary.setText("确认收货");
                holder.btnActionPrimary.setOnClickListener(v -> {
                    new AlertDialog.Builder(OrderListActivity.this)
                            .setTitle("确认收货")
                            .setMessage("请在确认收到商品且外观完好后再进行操作。")
                            .setPositiveButton("确认收货", (d, w) -> {
                                store.updateOrderStatus(order.orderId, "已完成");
                                Toast.makeText(OrderListActivity.this, "交易已完成！感谢您的购买", Toast.LENGTH_SHORT).show();
                                loadOrders();
                            })
                            .setNegativeButton("暂不确认", null)
                            .show();
                });
            } else if ("已完成".equals(order.status)) {
                holder.btnActionSecondary.setVisibility(View.VISIBLE);
                holder.btnActionSecondary.setText("删除订单");
                holder.btnActionSecondary.setOnClickListener(v -> deleteOrderPrompt(order.orderId));

                holder.btnActionPrimary.setText("再次购买");
                holder.btnActionPrimary.setOnClickListener(v -> {
                    for (ShopStore.CartItem ci : order.items) {
                        store.addToCart(ci.title, ci.priceCents, ci.imgRes, ci.category, ci.quantity);
                    }
                    Toast.makeText(OrderListActivity.this, "商品已重新加入购物车", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(OrderListActivity.this, CartActivity.class));
                });
            } else { // 已退款
                holder.btnActionSecondary.setVisibility(View.GONE);
                holder.btnActionPrimary.setText("删除订单");
                holder.btnActionPrimary.setOnClickListener(v -> deleteOrderPrompt(order.orderId));
            }
        }

        private void deleteOrderPrompt(String orderId) {
            new AlertDialog.Builder(OrderListActivity.this)
                    .setTitle("删除订单")
                    .setMessage("确定要删除这条订单记录吗？删除后不可恢复。")
                    .setPositiveButton("删除", (d, w) -> {
                        store.deleteOrder(orderId);
                        Toast.makeText(OrderListActivity.this, "订单已删除", Toast.LENGTH_SHORT).show();
                        loadOrders();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }

        private void openDetail(String orderId) {
            Intent intent = new Intent(OrderListActivity.this, OrderDetailActivity.class);
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, orderId);
            startActivity(intent);
        }

        @Override
        public int getItemCount() {
            return orderList.size();
        }
    }
}
