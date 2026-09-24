package com.example.toutiao.demo;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTitle;
    private TextView btnManage;
    private RecyclerView rvCart;
    private LinearLayout llEmpty;
    private Button btnGoShop;
    private LinearLayout llBottomBar;
    private CheckBox cbSelectAll;
    private LinearLayout llTotalInfo;
    private TextView tvTotalAmount;
    private Button btnCheckout;

    private ShopStore store;
    private final List<ShopStore.CartItem> cartList = new ArrayList<>();
    private CartAdapter adapter;
    private boolean isManageMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        store = new ShopStore(this);
        initViews();
        setupEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        btnManage = findViewById(R.id.btn_manage);
        rvCart = findViewById(R.id.rv_cart);
        llEmpty = findViewById(R.id.ll_empty);
        btnGoShop = findViewById(R.id.btn_go_shop);
        llBottomBar = findViewById(R.id.ll_bottom_bar);
        cbSelectAll = findViewById(R.id.cb_select_all);
        llTotalInfo = findViewById(R.id.ll_total_info);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        btnCheckout = findViewById(R.id.btn_checkout);

        rvCart.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter();
        rvCart.setAdapter(adapter);
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());

        btnGoShop.setOnClickListener(v -> finish());

        btnManage.setOnClickListener(v -> {
            isManageMode = !isManageMode;
            btnManage.setText(isManageMode ? "完成" : "管理");
            llTotalInfo.setVisibility(isManageMode ? View.INVISIBLE : View.VISIBLE);
            updateBottomBar();
        });

        cbSelectAll.setOnClickListener(v -> {
            boolean checked = cbSelectAll.isChecked();
            store.selectAll(checked);
            for (ShopStore.CartItem it : cartList) {
                it.selected = checked;
            }
            adapter.notifyDataSetChanged();
            updateBottomBar();
        });

        btnCheckout.setOnClickListener(v -> {
            List<ShopStore.CartItem> selected = getSelectedItems();
            if (selected.isEmpty()) {
                Toast.makeText(this, isManageMode ? "请勾选要删除的商品" : "请先勾选要结算的商品", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isManageMode) {
                new AlertDialog.Builder(this)
                        .setTitle("移除商品")
                        .setMessage("确定要从购物车移除选中的 " + selected.size() + " 件商品吗？")
                        .setPositiveButton("移除", (d, w) -> {
                            store.removeSelectedFromCart();
                            loadCartData();
                            Toast.makeText(this, "已移除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                // 打开确认订单页面
                Intent intent = new Intent(this, OrderConfirmActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadCartData() {
        cartList.clear();
        cartList.addAll(store.getCartItems());
        adapter.notifyDataSetChanged();

        boolean isEmpty = cartList.isEmpty();
        llEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvCart.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        llBottomBar.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        btnManage.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        tvTitle.setText(isEmpty ? "购物车" : "购物车(" + cartList.size() + ")");
        updateBottomBar();
    }

    private List<ShopStore.CartItem> getSelectedItems() {
        List<ShopStore.CartItem> res = new ArrayList<>();
        for (ShopStore.CartItem it : cartList) {
            if (it.selected) res.add(it);
        }
        return res;
    }

    private void updateBottomBar() {
        if (cartList.isEmpty()) return;

        int selectedCount = 0;
        long totalCents = 0;
        boolean allSelected = true;

        for (ShopStore.CartItem it : cartList) {
            if (it.selected) {
                selectedCount += it.quantity;
                totalCents += (it.priceCents * it.quantity);
            } else {
                allSelected = false;
            }
        }

        cbSelectAll.setChecked(allSelected && !cartList.isEmpty());
        tvTotalAmount.setText("¥" + String.format(Locale.CHINA, "%.2f", totalCents / 100.0));

        if (isManageMode) {
            btnCheckout.setText("删除(" + selectedCount + ")");
        } else {
            btnCheckout.setText("结算(" + selectedCount + ")");
        }
    }

    private class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            final CheckBox cbSelect;
            final ImageView ivThumb;
            final TextView tvTitle;
            final TextView tvCategory;
            final TextView tvPrice;
            final TextView btnMinus;
            final TextView tvQuantity;
            final TextView btnPlus;

            VH(@NonNull View itemView) {
                super(itemView);
                cbSelect = itemView.findViewById(R.id.cb_select);
                ivThumb = itemView.findViewById(R.id.iv_thumb);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvCategory = itemView.findViewById(R.id.tv_category);
                tvPrice = itemView.findViewById(R.id.tv_price);
                btnMinus = itemView.findViewById(R.id.btn_minus);
                tvQuantity = itemView.findViewById(R.id.tv_quantity);
                btnPlus = itemView.findViewById(R.id.btn_plus);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart_product, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ShopStore.CartItem item = cartList.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvCategory.setText(item.category);
            holder.tvPrice.setText("¥" + String.format(Locale.CHINA, "%.2f", item.getPrice()));
            holder.tvQuantity.setText(String.valueOf(item.quantity));
            holder.cbSelect.setChecked(item.selected);

            if (item.imgRes != 0) {
                holder.ivThumb.setImageResource(item.imgRes);
            } else {
                holder.ivThumb.setImageResource(R.drawable.shop_1);
            }

            holder.cbSelect.setOnClickListener(v -> {
                item.selected = holder.cbSelect.isChecked();
                store.setItemSelected(item.title, item.selected);
                updateBottomBar();
            });

            holder.btnMinus.setOnClickListener(v -> {
                if (item.quantity > 1) {
                    item.quantity--;
                    store.updateQuantity(item.title, item.quantity);
                    holder.tvQuantity.setText(String.valueOf(item.quantity));
                    updateBottomBar();
                } else {
                    new AlertDialog.Builder(CartActivity.this)
                            .setTitle("移除商品")
                            .setMessage("确定要从购物车移除【" + item.title + "】吗？")
                            .setPositiveButton("移除", (d, w) -> {
                                store.removeFromCart(item.title);
                                loadCartData();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            });

            holder.btnPlus.setOnClickListener(v -> {
                if (item.quantity < 99) {
                    item.quantity++;
                    store.updateQuantity(item.title, item.quantity);
                    holder.tvQuantity.setText(String.valueOf(item.quantity));
                    updateBottomBar();
                } else {
                    Toast.makeText(CartActivity.this, "单件商品限购99件", Toast.LENGTH_SHORT).show();
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(CartActivity.this)
                        .setTitle("商品操作")
                        .setItems(new String[]{"移出购物车"}, (d, w) -> {
                            store.removeFromCart(item.title);
                            loadCartData();
                        })
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return cartList.size();
        }
    }
}
