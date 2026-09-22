package com.example.toutiao.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

/**
 * 商城瀑布流适配器。
 *
 * 和 NewsMultiAdapter 的区别：布局只有一种。
 * 错落感不来自适配器，而来自 StaggeredGridLayoutManager（两列各自独立排）
 * 加上 item_shop.xml 里 adjustViewBounds 的图片（高度按原图比例自适应）。
 */
public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    private final List<ShopItem> itemList;

    public ShopAdapter(List<ShopItem> list) {
        this.itemList = list;
    }

    //点击回调，和 NewsMultiAdapter 同一套写法
    public interface OnItemClickListener {
        void onItemClick(ShopItem item);
    }

    private OnItemClickListener itemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    static class ShopViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvPrice, tvSales;

        ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_shop_cover);
            tvTitle = itemView.findViewById(R.id.tv_shop_title);
            tvPrice = itemView.findViewById(R.id.tv_shop_price);
            tvSales = itemView.findViewById(R.id.tv_shop_sales);
        }
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop, parent, false);
        final ShopViewHolder holder = new ShopViewHolder(view);
        view.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && itemClickListener != null) {
                itemClickListener.onItemClick(itemList.get(position));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        ShopItem item = itemList.get(position);
        holder.ivCover.setImageResource(item.getImgRes());
        holder.tvTitle.setText(item.getTitle());
        //价格统一格式化成两位小数，19.9 和 128 显示成同一风格
        holder.tvPrice.setText(String.format(Locale.CHINA, "¥%.2f", item.getPrice()));
        holder.tvSales.setText(formatSales(item.getSales()));
    }

    //已售件数：过万就换算成"x.x万"，和淘宝的展示习惯一致
    private String formatSales(int sales) {
        if (sales >= 10000) {
            return String.format(Locale.CHINA, "已售%.1f万件", sales / 10000.0);
        }
        return "已售" + sales + "件";
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
