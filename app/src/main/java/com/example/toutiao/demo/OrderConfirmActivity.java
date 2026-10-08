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

    public static final String EXTRA_DIRECT_ID = "extra_direct_id";
    public static final String EXTRA_DIRECT_SKU = "extra_direct_sku";
    public static final String EXTRA_DIRECT_TITLE = "extra_direct_title";
    public static final String EXTRA_DIRECT_PRICE_CENTS = "extra_direct_price_cents";
    public static final String EXTRA_DIRECT_PRICE = "extra_direct_price";
    public static final String EXTRA_DIRECT_IMG = "extra_direct_img";
    public static final String EXTRA_DIRECT_CAT = "extra_direct_cat";
    //直接购买时的数量，商品详情页的 SKU 面板带过来；默认 1 兼容旧调用方
    public static final String EXTRA_DIRECT_QTY = "extra_direct_qty";

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

    private LinearLayout llCouponSelector;
    private TextView tvCouponLabel;
    private TextView tvCouponDiscount;
    private TextView tvCoinLabel;
    private TextView tvCoinDiscount;

    private ShopStore store;
    private ProfileStore profileStore;
    private final List<ShopStore.CartItem> buyItems = new ArrayList<>();
    private List<ShopStore.CouponItem> availableCoupons = new ArrayList<>();
    private ShopStore.CouponItem selectedCoupon = null;

    private long subtotalCents = 0;
    private long couponDiscountCents = 0;
    private long coinDiscountCents = 0;
    private int coinsToDeduct = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_confirm);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.brand_red));
            androidx.core.view.WindowInsetsControllerCompat controller =
                    new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.setAppearanceLightStatusBars(false);
        }

        store = new ShopStore(this);
        profileStore = new ProfileStore(this);
        initViews();
        loadDefaultAddress();
        loadItems();
        initCouponsAndCoins();
        renderOrder();
        setupEvents();
    }

    private void loadDefaultAddress() {
        ShopStore.AddressItem defaultAddr = store.getDefaultAddress();
        if (defaultAddr != null) {
            tvReceiverName.setText(defaultAddr.name);
            tvReceiverPhone.setText(defaultAddr.phone);
            tvReceiverAddress.setText("[" + defaultAddr.tag + "] " + defaultAddr.fullAddress);
        }
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

        llCouponSelector = findViewById(R.id.ll_coupon_selector);
        tvCouponLabel = findViewById(R.id.tv_coupon_label);
        tvCouponDiscount = findViewById(R.id.tv_coupon_discount);
        tvCoinLabel = findViewById(R.id.tv_coin_label);
        tvCoinDiscount = findViewById(R.id.tv_coin_discount);
    }

    private void loadItems() {
        String directTitle = getIntent().getStringExtra(EXTRA_DIRECT_TITLE);
        if (directTitle != null && !directTitle.isEmpty()) {
            double price = getIntent().getDoubleExtra(EXTRA_DIRECT_PRICE, 99.0);
            long priceCents = getIntent().getLongExtra(EXTRA_DIRECT_PRICE_CENTS, Math.round(price * 100));
            int img = getIntent().getIntExtra(EXTRA_DIRECT_IMG, R.drawable.shop_1);
            String cat = getIntent().getStringExtra(EXTRA_DIRECT_CAT);
            int qty = getIntent().getIntExtra(EXTRA_DIRECT_QTY, 1);
            String productId = getIntent().getStringExtra(EXTRA_DIRECT_ID);
            String sku = getIntent().getStringExtra(EXTRA_DIRECT_SKU);
            if (productId == null) productId = ShopItem.legacyProductId(img, directTitle, cat);
            buyItems.add(new ShopStore.CartItem(productId, sku, directTitle, priceCents, qty, img, cat != null ? cat : "精选", true));
        } else {
            for (ShopStore.CartItem ci : store.getCartItems()) {
                if (ci.selected) {
                    buyItems.add(ci);
                }
            }
        }
    }

    private void initCouponsAndCoins() {
        subtotalCents = 0;
        for (ShopStore.CartItem it : buyItems) {
            subtotalCents = Math.addExact(subtotalCents, it.subtotalCents());
        }
        availableCoupons = store.getAvailableCoupons(subtotalCents);
        if (!availableCoupons.isEmpty()) {
            // 默认自动优选抵扣金额最高的大额神券
            selectedCoupon = availableCoupons.get(0);
            for (ShopStore.CouponItem c : availableCoupons) {
                if (c.discountAmount > selectedCoupon.discountAmount) {
                    selectedCoupon = c;
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
        LayoutInflater inflater = LayoutInflater.from(this);

        for (ShopStore.CartItem it : buyItems) {
            View row = inflater.inflate(R.layout.item_cart_product, llOrderProducts, false);
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
            tvCat.setText(it.category + " · " + it.sku);
            tvPrice.setText("¥" + String.format(Locale.CHINA, "%.2f", it.getPrice()));
            tvQty.setText("× " + it.quantity);

            llOrderProducts.addView(row);
        }

        recalculateDiscounts();
    }

    private void recalculateDiscounts() {
        // 1. 优惠券计算
        if (selectedCoupon != null) {
            couponDiscountCents = Math.max(0, Math.min(subtotalCents, selectedCoupon.discountAmount * 100L));
            if (tvCouponLabel != null) tvCouponLabel.setText("店铺优惠券 · " + selectedCoupon.title);
            if (tvCouponDiscount != null) tvCouponDiscount.setText("-¥" + String.format(Locale.CHINA, "%.2f", couponDiscountCents / 100.0) + " >");
        } else {
            couponDiscountCents = 0;
            if (tvCouponLabel != null) tvCouponLabel.setText("店铺优惠券");
            if (tvCouponDiscount != null) {
                tvCouponDiscount.setText(availableCoupons.isEmpty() ? "暂无可用券 >" : availableCoupons.size() + " 张可用 >");
            }
        }

        // 2. 淘金币抵扣计算 (100金币 = 1元，最高抵扣 500 金币即 5 元)
        int userCoins = profileStore.getCoins();
        long remaining = Math.max(0, subtotalCents - couponDiscountCents);
        int maxUsableCoins = Math.min(userCoins, 500);
        // 不超过剩余金额
        int cappedCoins = (int) Math.min(maxUsableCoins, (remaining / 100) * 100);
        coinsToDeduct = Math.max(0, (cappedCoins / 100) * 100);
        coinDiscountCents = (coinsToDeduct / 100) * 100L;

        if (tvCoinLabel != null) {
            tvCoinLabel.setText("淘金币抵扣 (可用 " + userCoins + " 金币)");
        }
        if (tvCoinDiscount != null) {
            if (coinDiscountCents > 0) {
                tvCoinDiscount.setText("-¥" + String.format(Locale.CHINA, "%.2f", coinDiscountCents / 100.0) + " (耗" + coinsToDeduct + "币)");
            } else {
                tvCoinDiscount.setText("-¥0.00");
            }
        }

        // 3. 最终实付
        long totalDiscount = couponDiscountCents + coinDiscountCents;
        long actualPay = Math.max(0, subtotalCents - totalDiscount);

        tvItemsSubtotal.setText("¥" + String.format(Locale.CHINA, "%.2f", subtotalCents / 100.0));
        tvDiscountAmount.setText("-¥" + String.format(Locale.CHINA, "%.2f", totalDiscount / 100.0));
        tvPayAmount.setText("¥" + String.format(Locale.CHINA, "%.2f", actualPay / 100.0));
    }

    private void showCouponSelectorDialog() {
        if (availableCoupons.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("优惠券选择")
                    .setMessage("当前订单金额未达到您所拥有优惠券的使用门槛，或暂无可用优惠券。\n\n提示：可在商城【领券中心】免费领取无门槛与大额神券！")
                    .setPositiveButton("我知道了", null)
                    .setNeutralButton("去领券中心", (d, w) -> {
                        WebActivity.open(this, "领券中心", "file:///android_asset/web/coupons.html");
                    })
                    .show();
            return;
        }

        String[] items = new String[availableCoupons.size() + 1];
        items[0] = "不使用优惠券";
        int checkedItem = 0;
        for (int i = 0; i < availableCoupons.size(); i++) {
            ShopStore.CouponItem c = availableCoupons.get(i);
            items[i + 1] = "立减 ¥" + c.discountAmount + " (" + c.title + " · 满" + c.minSpend + "可用)";
            if (selectedCoupon != null && selectedCoupon.id.equals(c.id)) {
                checkedItem = i + 1;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("选择店铺优惠券")
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                    if (which == 0) {
                        selectedCoupon = null;
                    } else {
                        selectedCoupon = availableCoupons.get(which - 1);
                    }
                    recalculateDiscounts();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());

        View llAddressCard = findViewById(R.id.ll_address_card);
        if (llAddressCard != null) {
            llAddressCard.setOnClickListener(v -> showEditAddressDialog());
        }
        btnEditAddress.setOnClickListener(v -> showEditAddressDialog());

        if (llCouponSelector != null) {
            llCouponSelector.setOnClickListener(v -> showCouponSelectorDialog());
        }

        btnSubmitOrder.setText("模拟支付并创建订单");
        btnSubmitOrder.setOnClickListener(v -> {
            btnSubmitOrder.setEnabled(false);
            recalculateDiscounts();
            String name = tvReceiverName.getText().toString();
            String phone = tvReceiverPhone.getText().toString();
            String addr = tvReceiverAddress.getText().toString();
            String note = etBuyerNote.getText().toString().trim();

            long totalDiscount = couponDiscountCents + coinDiscountCents;

            ShopStore.OrderItem order;
            try {
                order = store.createOrder(buyItems, name, phone, addr, totalDiscount,
                        getIntent().getStringExtra(EXTRA_DIRECT_TITLE) == null);
            } catch (RuntimeException failure) {
                btnSubmitOrder.setEnabled(true);
                Toast.makeText(this, "订单保存失败，请重试", Toast.LENGTH_LONG).show();
                return;
            }
            if (selectedCoupon != null && couponDiscountCents > 0) store.markCouponUsed(selectedCoupon.id);
            if (coinsToDeduct > 0) profileStore.deductCoins(coinsToDeduct);
            String successMsg = "模拟支付成功！实付 ¥" + String.format(Locale.CHINA, "%.2f", order.actualCents / 100.0);
            if (selectedCoupon != null) {
                successMsg += " (券省¥" + selectedCoupon.discountAmount + ")";
            }
            if (coinsToDeduct > 0) {
                successMsg += " (币抵¥" + (coinDiscountCents / 100.0) + ")";
            }
            Toast.makeText(this, successMsg, Toast.LENGTH_LONG).show();

            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.orderId);
            startActivity(intent);
            finish();
        });
    }

    private void showEditAddressDialog() {
        List<ShopStore.AddressItem> addresses = store.getAddresses();
        String[] items = new String[addresses.size() + 1];
        int selectedIndex = 0;
        String currentAddr = tvReceiverAddress.getText().toString();

        for (int i = 0; i < addresses.size(); i++) {
            ShopStore.AddressItem a = addresses.get(i);
            String label = "[" + a.tag + "] " + a.name + " (" + a.phone + ")\n" + a.fullAddress + (a.isDefault ? " 【默认】" : "");
            items[i] = label;
            if (currentAddr.contains(a.fullAddress)) {
                selectedIndex = i;
            }
        }
        items[addresses.size()] = "➕ 新增晴川收货地址…";

        new AlertDialog.Builder(this)
                .setTitle("选择晴川收货地址")
                .setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == addresses.size()) {
                        showAddNewAddressDialog();
                    } else {
                        ShopStore.AddressItem selected = addresses.get(which);
                        tvReceiverName.setText(selected.name);
                        tvReceiverPhone.setText(selected.phone);
                        tvReceiverAddress.setText("[" + selected.tag + "] " + selected.fullAddress);
                        store.setDefaultAddress(selected.id);
                        Toast.makeText(this, "已切换为：" + selected.name + " (" + selected.tag + ")", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAddNewAddressDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText etName = new EditText(this);
        etName.setHint("收货人姓名 (如：张同学)");
        layout.addView(etName);

        EditText etPhone = new EditText(this);
        etPhone.setHint("手机号码 (如：13800138000)");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        EditText etTag = new EditText(this);
        etTag.setHint("地址标签 (如：宿舍 / 教学楼 / 快递点)");
        etTag.setText("宿舍");
        layout.addView(etTag);

        EditText etAddr = new EditText(this);
        etAddr.setHint("晴川校内详细地址 (如：6号楼502室)");
        layout.addView(etAddr);

        new AlertDialog.Builder(this)
                .setTitle("新增晴川收货地址")
                .setView(layout)
                .setPositiveButton("保存并使用", (d, w) -> {
                    String n = etName.getText().toString().trim();
                    String p = etPhone.getText().toString().trim();
                    String t = etTag.getText().toString().trim();
                    String a = etAddr.getText().toString().trim();
                    if (n.isEmpty()) n = "晴川学子";
                    if (p.isEmpty()) p = "13800138000";
                    if (t.isEmpty()) t = "宿舍";
                    if (a.isEmpty()) a = "武汉晴川学院 学生公寓";
                    String fullAddr = a.contains("武汉晴川学院") ? a : "武汉市东湖高新区 武汉晴川学院 " + a;

                    store.addAddress(n, p, t, fullAddr, true);
                    tvReceiverName.setText(n);
                    tvReceiverPhone.setText(p);
                    tvReceiverAddress.setText("[" + t + "] " + fullAddr);
                    Toast.makeText(this, "新增地址成功并已设为默认！", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
