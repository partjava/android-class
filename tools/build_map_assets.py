# -*- coding: utf-8 -*-
"""
晴川三维全景互动导览图资源构建与校准脚本
本脚本使用项目内部的相对目录 ./map_assets，完全独立于任何外部或本机绝对路径。
"""

import os
import cv2
import numpy as np

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAP_ASSETS_DIR = os.path.join(PROJECT_ROOT, "map_assets")
EACH_DIR = os.path.join(MAP_ASSETS_DIR, "每个方位")
RES_DRAWABLE_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "res", "drawable")


def read_image_safe(file_path):
    if not os.path.exists(file_path):
        return None
    data = np.fromfile(file_path, dtype=np.uint8)
    return cv2.imdecode(data, cv2.IMREAD_COLOR)


def write_image_safe(file_path, img):
    ext = os.path.splitext(file_path)[1]
    ret, buf = cv2.imencode(ext, img)
    if ret:
        buf.tofile(file_path)


def process_user_crops():
    print(f"Loading assets from relative path: {MAP_ASSETS_DIR}")
    if not os.path.exists(EACH_DIR):
        print(f"Directory not found: {EACH_DIR}")
        return

    mapping = {
        "gym": "体育馆.png",
        "library": "图书馆.png",
        "lab_1": "实验楼1（5楼）.png",
        "lab_2": "实验楼二（5楼）.png",
        "theater_market": "小剧场（2楼）、超市（1楼）.png",
        "lake": "情缘湖.png",
        "stadium": "操场.png",
        "teach_1": "教学楼一（5楼）.png",
        "teach_2": "教学楼二（5楼）.png",
        "teach_3": "教学楼三（5楼）.png",
        "teach_4": "教学楼四（5楼）.png",
        "south_gate": "晴川大门.png",
        "admin_1": "综合楼一（5楼）.png",
        "admin_2": "综合楼二（5楼）.png",
        "art_museum": "美术馆.png",
        "computer_center": "计算机实验室.png",
        "canteen_main": "食堂（3楼）.png",
        "dorm_cluster": "所有宿舍区域.png",
    }

    target_w, target_h = 640, 360

    for key, fname in mapping.items():
        src_path = os.path.join(EACH_DIR, fname)
        img = read_image_safe(src_path)
        if img is None:
            print(f"Warning: {fname} not found!")
            continue

        h, w = img.shape[:2]
        scale = min(target_w / float(w), target_h / float(h))
        nw, nh = int(w * scale), int(h * scale)
        resized = cv2.resize(img, (nw, nh), interpolation=cv2.INTER_LANCZOS4)

        blur_bg = cv2.resize(img, (target_w, target_h))
        blur_bg = cv2.GaussianBlur(blur_bg, (51, 51), 0)
        canvas = (blur_bg * 0.35).astype(np.uint8)

        ox = (target_w - nw) // 2
        oy = (target_h - nh) // 2
        canvas[oy : oy + nh, ox : ox + nw] = resized

        out_path = os.path.join(RES_DRAWABLE_DIR, f"user_crop_{key}.jpg")
        write_image_safe(out_path, canvas)
        print(f"Generated drawable: user_crop_{key}.jpg")


if __name__ == "__main__":
    process_user_crops()
