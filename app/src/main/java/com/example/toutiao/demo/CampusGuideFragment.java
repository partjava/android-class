package com.example.toutiao.demo;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.List;

public class CampusGuideFragment extends PageFragment {

    private Campus3DMapView mapView;
    private RecyclerView rvQuickLandmarks;
    private QuickLandmarkAdapter quickAdapter;
    private List<CampusLandmark> allLandmarks = new ArrayList<>();
    private List<CampusLandmark> displayLandmarks = new ArrayList<>();

    private TextView btnSwitchMarked;
    private TextView chipAll, chipTeaching, chipLab, chipCulture, chipLiving;
    private TextView tvLandmarkCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_campus_guide, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        allLandmarks = CampusLandmark.getAllLandmarks();
        displayLandmarks = new ArrayList<>(allLandmarks);

        mapView = view.findViewById(R.id.campus_map_view);
        rvQuickLandmarks = view.findViewById(R.id.rv_quick_landmarks);
        btnSwitchMarked = view.findViewById(R.id.btn_switch_marked);
        tvLandmarkCount = view.findViewById(R.id.tv_landmark_count);

        View ivBack = view.findViewById(R.id.iv_guide_back);
        if (ivBack != null && getActivity() instanceof CampusGuideActivity) {
            ivBack.setVisibility(View.VISIBLE);
            ivBack.setOnClickListener(v -> requireActivity().finish());
        }

        chipAll = view.findViewById(R.id.chip_all);
        chipTeaching = view.findViewById(R.id.chip_teaching);
        chipLab = view.findViewById(R.id.chip_lab);
        chipCulture = view.findViewById(R.id.chip_culture);
        chipLiving = view.findViewById(R.id.chip_living);

        // 初始化交互地图
        mapView.setLandmarks(allLandmarks);
        mapView.setOnLandmarkClickListener(this::showLandmarkDetailBottomSheet);

        // 地图控制按钮
        view.findViewById(R.id.btn_map_zoom_in).setOnClickListener(v -> mapView.zoomIn());
        view.findViewById(R.id.btn_map_zoom_out).setOnClickListener(v -> mapView.zoomOut());
        view.findViewById(R.id.btn_map_reset).setOnClickListener(v -> {
            mapView.resetView();
            Toast.makeText(getContext(), "已复位至全景视角", Toast.LENGTH_SHORT).show();
        });

        // 切换对比原图
        btnSwitchMarked.setOnClickListener(v -> {
            mapView.toggleMarkedMap();
            boolean marked = mapView.isMarkedMode();
            btnSwitchMarked.setText(marked ? "🗺️ 纯净实景" : "🏷️ 层数对照");
            Toast.makeText(getContext(), marked ? "已切换至原图标注层数对照模式" : "已切换至高清纯净全景模式", Toast.LENGTH_SHORT).show();
        });

        // 分类标签
        setupChips();

        // 底部快捷地标列表
        rvQuickLandmarks.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        quickAdapter = new QuickLandmarkAdapter(displayLandmarks, lm -> {
            mapView.selectLandmark(lm.id);
            mapView.animateToLandmark(lm);
            showLandmarkDetailBottomSheet(lm);
        });
        rvQuickLandmarks.setAdapter(quickAdapter);
    }

    private void setupChips() {
        View.OnClickListener chipListener = v -> {
            resetChips();
            String cat = "全部";
            if (v.getId() == R.id.chip_all) {
                cat = "全部";
                highlightChip(chipAll);
            } else if (v.getId() == R.id.chip_teaching) {
                cat = "教学综合";
                highlightChip(chipTeaching);
            } else if (v.getId() == R.id.chip_lab) {
                cat = "实验科创";
                highlightChip(chipLab);
            } else if (v.getId() == R.id.chip_culture) {
                cat = "文体研学";
                highlightChip(chipCulture);
            } else if (v.getId() == R.id.chip_living) {
                cat = "生活宿区";
                highlightChip(chipLiving);
            }

            mapView.filterCategory(cat);

            displayLandmarks.clear();
            if ("全部".equals(cat)) {
                displayLandmarks.addAll(allLandmarks);
            } else {
                for (CampusLandmark lm : allLandmarks) {
                    if (lm.category.contains(cat)) {
                        displayLandmarks.add(lm);
                    }
                }
            }
            quickAdapter.notifyDataSetChanged();
            if (tvLandmarkCount != null) {
                tvLandmarkCount.setText(displayLandmarks.size() + "处地标已标定");
            }
        };

        chipAll.setOnClickListener(chipListener);
        chipTeaching.setOnClickListener(chipListener);
        chipLab.setOnClickListener(chipListener);
        chipCulture.setOnClickListener(chipListener);
        chipLiving.setOnClickListener(chipListener);
    }

    private void resetChips() {
        TextView[] chips = {chipAll, chipTeaching, chipLab, chipCulture, chipLiving};
        for (TextView c : chips) {
            c.setBackgroundResource(R.drawable.bg_chip);
            c.setTextColor(Color.parseColor("#CBD5E1"));
        }
    }

    private void highlightChip(TextView c) {
        c.setBackgroundResource(R.drawable.bg_badge);
        c.setTextColor(Color.WHITE);
    }

    private void showLandmarkDetailBottomSheet(CampusLandmark lm) {
        if (getContext() == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(getContext()).inflate(R.layout.dialog_campus_landmark_detail, null);

        ImageView ivPhoto = sheet.findViewById(R.id.iv_landmark_photo);
        TextView btnTogglePhoto = sheet.findViewById(R.id.btn_toggle_marked_photo);
        TextView tvFloorBadge = sheet.findViewById(R.id.tv_landmark_floor_badge);
        TextView tvTitle = sheet.findViewById(R.id.tv_detail_title);
        TextView tvTag = sheet.findViewById(R.id.tv_detail_tag);
        TextView tvDesc = sheet.findViewById(R.id.tv_detail_desc);
        TextView tvFloors = sheet.findViewById(R.id.tv_detail_floors);
        TextView tvHours = sheet.findViewById(R.id.tv_detail_hours);
        TextView tvPhone = sheet.findViewById(R.id.tv_detail_phone);
        TextView tvTips = sheet.findViewById(R.id.tv_detail_tips);
        View btnOk = sheet.findViewById(R.id.btn_detail_ok);

        ivPhoto.setImageResource(lm.resImage);
        tvFloorBadge.setText("🏢 " + lm.totalFloors);
        tvTitle.setText(lm.fullName);
        tvTag.setText(lm.tag);
        tvDesc.setText(lm.desc);
        tvFloors.setText(lm.floorsGuide);
        tvHours.setText("⏰ 开放时间：" + lm.openHours);
        tvPhone.setText("📞 服务电话：" + lm.servicePhone);
        tvTips.setText("💡 导学贴士：" + lm.tips);

        final boolean[] showMarked = {false};
        btnTogglePhoto.setOnClickListener(v -> {
            showMarked[0] = !showMarked[0];
            ivPhoto.setImageResource(showMarked[0] ? lm.resMarkedImage : lm.resImage);
            btnTogglePhoto.setText(showMarked[0] ? "📷 查看纯净实景" : "🏷️ 查看层数标记图");
        });

        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(sheet);
        dialog.show();
    }

    // 快捷地标 RecyclerView Adapter
    private static class QuickLandmarkAdapter extends RecyclerView.Adapter<QuickLandmarkAdapter.ViewHolder> {
        private final List<CampusLandmark> list;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(CampusLandmark lm);
        }

        QuickLandmarkAdapter(List<CampusLandmark> list, OnItemClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quick_landmark, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CampusLandmark lm = list.get(position);
            holder.tvName.setText(lm.name);
            holder.tvFloor.setText("🏢 " + lm.totalFloors);
            holder.tvTag.setText(lm.tag);
            holder.ivThumb.setImageResource(lm.resImage);
            holder.itemView.setOnClickListener(v -> listener.onItemClick(lm));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumb;
            TextView tvName, tvFloor, tvTag;

            ViewHolder(View itemView) {
                super(itemView);
                ivThumb = itemView.findViewById(R.id.iv_quick_thumb);
                tvName = itemView.findViewById(R.id.tv_quick_name);
                tvFloor = itemView.findViewById(R.id.tv_quick_floor);
                tvTag = itemView.findViewById(R.id.tv_quick_tag);
            }
        }
    }
}
