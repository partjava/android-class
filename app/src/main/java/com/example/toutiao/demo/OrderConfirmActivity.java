package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderConfirmActivity extends AppCompatActivity {

    public static final String EXTRA_DIRECT_TITLE = "extra_direct_title";
    public static final String EXTRA_DIRECT_PRICE = "extra_direct_price";
    public static final String EXTRA_DIRECT_IMG = "extra_direct_img";
    public static final String EXTRA_DIRECT_CAT = "extra_direct_cat";

    private ImageView btnBack;
    private TextView tvReceiverName;
    private TextView tvReceiverPhone;
    private TextView tvReceiverAddress;
    private TextView btnEditAddress;
    private LinearLayout llOrderProducts;
    private EditText etBuyerNote;
    private TextView tvItemsSubtotal;
    private TextView tvDiscountAmount;
    private TextView tvPayAmount;
    private Button btnSubmitOrder;

    private ShopStore store;
    private final List<ShopStore.CartItem> buyItems = new ArrayList<>();
    private long discountCents = 1500; // 默认满减与淘金币优惠 15.00 元

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_confirm);

        store = new ShopStore(this);
        initViews();
        loadItems();
        renderOrder();
        setupEvents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvReceiverName = findViewById(R.id.tv_receiver_name);
        tvReceiverPhone = findViewById(R.id.tv_receiver_phone);
        tvReceiverAddress = findViewById(R.id.tv_receiver_address);
        btnEditAddress = findViewById(R.id.btn_edit_address);
        llOrderProducts = findViewById(R.id.ll_order_products);
        etBuyerNote = findViewById(R.id.et_buyer_note);
        tvItemsSubtotal = findViewById(R.id.tv_items_subtotal);
        tvDiscountAmount = findViewById(R.id.tv_discount_amount);
        tvPayAmount = findViewById(R.id.tv_pay_amount);
        btnSubmitOrder = findViewById(R.id.btn_submit_order);
    }

    private void loadItems() {
        String directTitle = getIntent().getStringExtra(EXTRA_DIRECT_TITLE);
        if (directTitle != null && !directTitle.isEmpty()) {
            double price = getIntent().getDoubleExtra(EXTRA_DIRECT_PRICE, 99.0);
            int img = getIntent().getIntExtra(EXTRA_DIRECT_IMG, R.drawable.shop_1);
            String cat = getIntent().getStringExtra(EXTRA_DIRECT_CAT);
            buyItems.add(new ShopStore.CartItem(directTitle, Math.round(price * 100), 1, img, cat != null ? cat : "精选", true));
        } else {
            for (ShopStore.CartItem ci : store.getCartItems()) {
                if (ci.selected) {
                    buyItems.add(ci);
                }
            }
        }
    }

    private void renderOrder() {
        if (buyItems.isEmpty()) {
            Toast.makeText(this, "暂无待结算商品", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        llOrderProducts.removeAllViews();
        long subtotal = 0;
        LayoutInflater inflater = LayoutInflater.from(this);

        for (ShopStore.CartItem it : buyItems) {
            subtotal += (it.priceCents * it.quantity);
            View row = inflater.inflate(R.layout.item_cart_product, llOrderProducts, false);
            // 隐藏 CheckBox 与 +/- 步进器，只做只读展示
            row.findViewById(R.id.cb_select).setVisibility(View.GONE);
            row.findViewById(R.id.btn_minus).setVisibility(View.GONE);
            row.findViewById(R.id.btn_plus).setVisibility(View.GONE);

            ImageView iv = row.findViewById(R.id.iv_thumb);
            TextView tvTitle = row.findViewById(R.id.tv_title);
            TextView tvCat = row.findViewById(R.id.tv_category);
            TextView tvPrice = row.findViewById(R.id.tv_price);
            TextView tvQty = row.findViewById(R.id.tv_quantity);

            if (it.imgRes != 0) iv.setImageResource(it.imgRes);
            tvTitle.setText(it.title);
            tvCat.setText(it.category);
            tvPrice.setText("¥" + String.format(Locale.CHINA, "%.2f", it.getPrice()));
            tvQty.setText("× " + it.quantity);

            llOrderProducts.addView(row);
        }

        // 计算优惠与最终实付款
        long finalDiscount = Math.min(discountCents, subtotal > 2000 ? discountCents : 0);
        long actualPay = Math.max(0, subtotal - finalDiscount);

        tvItemsSubtotal.setText("¥" + String.format(Locale.CHINA, "%.2f", subtotal / 100.0));
        tvDiscountAmount.setText("-¥" + String.format(Locale.CHINA, "%.2f", finalDiscount / 100.0));
        tvPayAmount.setText("¥" + String.format(Locale.CHINA, "%.2f", actualPay / 100.0));
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());

        View llAddressCard = findViewById(R.id.ll_address_card);
        if (llAddressCard != null) {
            llAddressCard.setOnClickListener(v -> showEditAddressDialog());
        }
        btnEditAddress.setOnClickListener(v -> showEditAddressDialog());

        btnSubmitOrder.setOnClickListener(v -> {
            String name = tvReceiverName.getText().toString();
            String phone = tvReceiverPhone.getText().toString();
            String addr = tvReceiverAddress.getText().toString();
            String note = etBuyerNote.getText().toString().trim();

            ShopStore.OrderItem order = store.createOrder(buyItems, name, phone, addr, discountCents);
            Toast.makeText(this, "模拟支付成功！订单已生成", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.orderId);
            startActivity(intent);
            finish();
        });
    }

    private void showEditAddressDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText etName = new EditText(this);
        etName.setHint("收货人姓名");
        etName.setText(tvReceiverName.getText());
        layout.addView(etName);

        EditText etPhone = new EditText(this);
        etPhone.setHint("手机号码");
        etPhone.setText(tvReceiverPhone.getText());
        layout.addView(etPhone);

        EditText etAddr = new EditText(this);
        etAddr.setHint("详细地址");
        etAddr.setText(tvReceiverAddress.getText());
        layout.addView(etAddr);

        new AlertDialog.Builder(this)
                .setTitle("修改收货地址")
                .setView(layout)
                .setPositiveButton("保存", (d, w) -> {
                    String n = etName.getText().toString().trim();
                    String p = etPhone.getText().toString().trim();
                    String a = etAddr.getText().toString().trim();
                    if (!n.isEmpty()) tvReceiverName.setText(n);
                    if (!p.isEmpty()) tvReceiverPhone.setText(p);
                    if (!a.isEmpty()) tvReceiverAddress.setText(a);
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
