# -*- coding: utf-8 -*-
"""
武汉晴川学院 校园应用 FastAPI 现代后端服务
提供标准 RESTful API (BaseURL: /api/v1) 与 Swagger 交互文档 (/docs)
支持双协议 (GET / POST)，驱动频道、新闻、商品、购物车全动态更新。
保持独立运行在 8000 端口，与 Flask 服务 (5000) 互不干扰。
"""

import os
import re
import time
import datetime
import socket
import urllib.parse
import requests
from typing import List, Optional, Dict, Any

from fastapi import FastAPI, Query, Path, Body, Request, HTTPException
from fastapi.responses import JSONResponse, Response, HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, field_validator
from decimal import Decimal

class GoodsWrite(BaseModel):
    title: str = Field(..., min_length=1, max_length=200)
    price: Decimal = Field(..., ge=0, max_digits=12, decimal_places=2, description="人民币价格，最多两位小数")
    category: str = Field("其他", min_length=1, max_length=32)
    sales: int = Field(0, ge=0, strict=True, description="实际录入的销量，不自动生成")
    image_url: str = Field("", pattern=r"^(https?://[^\s]+)?$")
    description: str = Field("", max_length=4000)

    @field_validator("title", "category")
    @classmethod
    def non_blank(cls, value):
        value = value.strip()
        if not value:
            raise ValueError("不能为空")
        return value

class GoodsCreate(GoodsWrite):
    product_id: Optional[str] = Field(None, pattern=r"^[A-Za-z0-9_-]{1,80}$", description="可不填，由服务器生成")

# 导入现有爬虫核心能力（零破坏复用已调通的官网解析器与网卡适配器）
try:
    from . import campus_news_server as cns
except ImportError:
    cns = None

app = FastAPI(
    title="晴川校园综合服务 API (FastAPI)",
    description="武汉晴川学院校园资讯、频道管理、商城与购物车 RESTful API，提供 GET/POST 双协议支持与交互式调试。",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# 允许跨域
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==============================================================================
# Pydantic 数据模型定义
# ==============================================================================

class ApiResponse(BaseModel):
    code: int = Field(200, description="状态码：200为成功")
    msg: str = Field("success", description="状态提示")
    data: Optional[Any] = Field(None, description="业务数据")

class ChannelItem(BaseModel):
    id: str = Field(..., description="频道唯一ID")
    name: str = Field(..., description="频道名称")
    fixed: bool = Field(False, description="是否固定频道")

class ChannelUpdateRequest(BaseModel):
    selected_ids: Optional[List[str]] = Field(None, description="选中的频道ID列表")
    channels: Optional[List[ChannelItem]] = Field(None, description="频道完整列表")

class NewsSearchRequest(BaseModel):
    keyword: Optional[str] = Field(None, description="搜索关键词")
    channel: Optional[str] = Field("xxyw", description="频道标识")
    page: int = Field(1, description="当前页码")
    size: int = Field(15, description="每页数量")

class GoodsFilterRequest(BaseModel):
    category: Optional[str] = Field("全部", description="商品分类")
    min_price: Optional[float] = Field(None, description="最低价格")
    max_price: Optional[float] = Field(None, description="最高价格")
    sort: Optional[str] = Field("default", description="排序方式：sales_desc, price_asc, price_desc")

class CartAddRequest(BaseModel):
    product_id: str = Field(..., description="已录入的商品ID")
    count: int = Field(1, description="加购数量")

class CartUpdateRequest(BaseModel):
    product_id: str = Field(..., description="商品ID")
    count: int = Field(..., description="更新后的数量，<=0表示删除")

# ==============================================================================
# 内存状态数据初始化
# ==============================================================================

# 1. 频道数据
DEFAULT_SELECTED_CHANNELS = [
    {"id": "c1", "name": "关注", "fixed": True},
    {"id": "c2", "name": "推荐", "fixed": True},
    {"id": "c3", "name": "热点", "fixed": False},
    {"id": "c4", "name": "校园", "fixed": False},
    {"id": "c5", "name": "科技", "fixed": False},
    {"id": "c6", "name": "数码", "fixed": False},
]
DEFAULT_CANDIDATE_CHANNELS = [
    {"id": "c7", "name": "体育", "fixed": False},
    {"id": "c8", "name": "娱乐", "fixed": False},
    {"id": "c9", "name": "财经", "fixed": False},
    {"id": "c10", "name": "军事", "fixed": False},
]
current_channel_state = {
    "selected": list(DEFAULT_SELECTED_CHANNELS),
    "candidate": list(DEFAULT_CANDIDATE_CHANNELS)
}

# 自有商品持久化；空库不会导入示例商品。
from .owned_catalog import ProductCatalog
from .dummy_goods import DummyGoodsCatalog, GoodsUnavailable
from .dangdang_goods import DangdangGoodsCatalog
from .yanxuan_goods import YanxuanGoodsCatalog
from .cart_persistence import CartStorage

remote_catalog = DummyGoodsCatalog()
dangdang_catalog = DangdangGoodsCatalog()
yanxuan_catalog = YanxuanGoodsCatalog()
goods_source = os.environ.get("CAMPUS_GOODS_SOURCE", "yanxuan")
product_catalog = ProductCatalog(os.environ.get("CAMPUS_GOODS_DB", os.path.join(os.path.dirname(os.path.dirname(__file__)), "data", "shop_products.db")))
cart_storage = CartStorage(os.environ.get("CAMPUS_CART_PATH", os.path.join(os.path.dirname(os.path.dirname(__file__)), "data", "cart_storage.json")))

def get_active_goods_catalog(category="推荐", limit: Optional[int] = 30, refresh: bool = False):
    if goods_source == "owned":
        items = product_catalog.list()
        return items[:limit] if (limit and limit > 0) else items
    if goods_source == "dummyjson":
        try:
            items = remote_catalog.list()
            return items[:limit] if (limit and limit > 0) else items
        except GoodsUnavailable as error:
            raise HTTPException(status_code=502, detail=str(error)) from error
    if goods_source == "dangdang":
        try:
            items = dangdang_catalog.list(category)
            return items[:limit] if (limit and limit > 0) else items
        except Exception as error:
            raise HTTPException(status_code=502, detail=str(error)) from error
    try:
        return yanxuan_catalog.list(category, limit=limit, refresh=refresh)
    except Exception as error:
        raise HTTPException(status_code=502, detail=str(error)) from error

def find_active_product(product_id: str):
    if goods_source == "owned":
        for item in product_catalog.list():
            if item.get("product_id") == product_id:
                return item
    elif goods_source == "dummyjson":
        try:
            for item in remote_catalog.list():
                if item.get("product_id") == product_id:
                    return item
        except Exception:
            pass
    elif goods_source == "dangdang":
        for cat_items in dangdang_catalog._cache.values():
            for item in cat_items:
                if item.get("product_id") == product_id:
                    return item
        try:
            for item in dangdang_catalog.list("推荐"):
                if item.get("product_id") == product_id:
                    return item
        except Exception:
            pass
        try:
            for item in dangdang_catalog._fallback.list():
                if item.get("product_id") == product_id:
                    return item
        except Exception:
            pass
    else:
        item = yanxuan_catalog.find(product_id)
        if item is not None:
            return item

    with cart_storage._lock:
        if product_id in cart_storage._items:
            snap = cart_storage._items[product_id].get("snapshot", {})
            if snap:
                return {
                    "product_id": product_id,
                    "title": snap.get("title", "未知商品"),
                    "price": float(snap.get("price", 0.0)),
                    "category": snap.get("category", "其他"),
                    "image_url": snap.get("image_url", ""),
                    "image_res": snap.get("image_res", "ic_shop"),
                    "description": snap.get("description", ""),
                    "sales": snap.get("sales", 0),
                    "source": snap.get("source", "")
                }
    return None

# 4. 新闻缓存 (内存缓存新闻列表，支持按 ID 查询)
news_detail_cache: Dict[str, Dict[str, Any]] = {}

# ==============================================================================
# 接口实现：健康检测与引擎识别
# ==============================================================================

@app.get("/api/health", summary="健康检测与引擎识别", tags=["系统服务"])
async def health_check():
    """用于 Android 端测试连接与识别当前后端框架"""
    return {
        "code": 200,
        "service": "campus-news",
        "engine": "fastapi",
        "port": 8000,
        "version": "1.0.0",
        "timestamp": int(time.time())
    }

# ==============================================================================
# 接口实现：频道管理 (Channel)
# ==============================================================================

@app.get("/api/v1/channel", summary="获取频道列表 (GET)", tags=["频道模块"])
@app.get("/channel", include_in_schema=False)
async def get_channels():
    """获取所有选中的频道以及未选中的候选频道列表"""
    return {
        "code": 200,
        "msg": "success",
        "data": {
            "selected": current_channel_state["selected"],
            "candidate": current_channel_state["candidate"]
        }
    }

@app.post("/api/v1/channel", summary="更新频道订阅配置 (POST)", tags=["频道模块"])
@app.post("/channel", include_in_schema=False)
async def update_channels(payload: ChannelUpdateRequest):
    """通过 POST 方法提交并更新用户自定义的频道列表"""
    all_known = {c["id"]: c for c in DEFAULT_SELECTED_CHANNELS + DEFAULT_CANDIDATE_CHANNELS}
    
    if payload.channels:
        current_channel_state["selected"] = [c.dict() for c in payload.channels]
    elif payload.selected_ids:
        new_selected = []
        for cid in payload.selected_ids:
            if cid in all_known:
                new_selected.append(all_known[cid])
        current_channel_state["selected"] = new_selected
        
        # 重新整理 candidate
        sel_set = set(payload.selected_ids)
        current_channel_state["candidate"] = [c for c in (DEFAULT_SELECTED_CHANNELS + DEFAULT_CANDIDATE_CHANNELS) if c["id"] not in sel_set]

    return {
        "code": 200,
        "msg": "频道订阅配置已更新",
        "data": {
            "selected_count": len(current_channel_state["selected"]),
            "candidate_count": len(current_channel_state["candidate"])
        }
    }

# ==============================================================================
# 接口实现：新闻资讯 (News)
# ==============================================================================

@app.get("/api/v1/news", summary="获取新闻列表 (GET)", tags=["新闻模块"])
@app.get("/api/news", include_in_schema=False)
@app.get("/api/campus/news", include_in_schema=False)
@app.get("/xiaoyuan/{cat_name}/page/{page_num}", include_in_schema=False)
async def get_news_list(
    request: Request,
    channel: str = Query("xxyw", description="栏目代号 (xxyw, tzgg, jyjx, mtgz, xxry)"),
    page: int = Query(1, ge=1, description="当前页码"),
    size: int = Query(15, ge=1, le=50, description="每页篇数"),
    cat_name: Optional[str] = None,
    page_num: Optional[int] = None
):
    """
    分页获取新闻列表。
    支持 GET 参数，优先直连学校官网实时抓取并解析高清配图。
    """
    target_cat = cat_name or channel or "xxyw"
    target_page = page_num or page or 1
    host = request.headers.get("host", "localhost:8000")

    items = []
    total_pages = 1
    cat_info = {}

    if cns:
        try:
            items, total_pages, cat_info = cns.crawl_qcu_page(target_cat, target_page, host)
        except Exception as e:
            print(f"[FastAPI 抓取警告] 实时爬虫异常: {e}")

    # 给每条新闻赋予稳定的 ID，并写入缓存供详情接口使用
    for idx, it in enumerate(items):
        raw_link = it.get("link", "")
        # 从链接提取新闻数字编号，如 /info/1042/12345.htm -> 12345
        m = re.search(r'/(\d+)\.htm', raw_link)
        news_id = m.group(1) if m else f"{target_cat}_{target_page}_{idx+1}"
        it["id"] = news_id
        news_detail_cache[news_id] = it

    return {
        "code": 200,
        "msg": "success",
        "school": "武汉晴川学院",
        "currentpage": target_page,
        "pagecount": total_pages,
        "newscount": total_pages * 15,
        "perpage": len(items),
        "newslist": items,
        "data": {
            "page": target_page,
            "page_count": total_pages,
            "total": len(items),
            "size": len(items),
            "has_more": target_page < total_pages,
            "category": target_cat,
            "category_name": cat_info.get("name", "") if isinstance(cat_info, dict) else "",
            "items": items
        }
    }

@app.get("/api/v1/news/{news_id}", summary="获取单篇新闻详情 (GET)", tags=["新闻模块"])
async def get_news_detail(news_id: str = Path(..., description="新闻ID")):
    """根据新闻ID获取完整正文与图文块"""
    if news_id in news_detail_cache:
        item = news_detail_cache[news_id]
        return {
            "code": 200,
            "msg": "success",
            "data": item
        }
    
    # 模拟或未找到兜底
    return {
        "code": 404,
        "msg": f"未找到编号为 {news_id} 的新闻详情，请先从列表拉取最新数据",
        "data": None
    }

@app.post("/api/v1/news/search", summary="条件检索新闻 (POST)", tags=["新闻模块"])
async def search_news(request: Request, payload: NewsSearchRequest):
    """通过 POST 方法提交检索条件（关键词、栏目、页码）"""
    host = request.headers.get("host", "localhost:8000")
    channel = payload.channel or "xxyw"
    page = payload.page or 1
    kw = (payload.keyword or "").strip()

    items = []
    if cns:
        try:
            items, total_pages, cat_info = cns.crawl_qcu_page(channel, page, host)
        except Exception as e:
            print(f"[FastAPI 搜索抓取警告] {e}")

    if kw:
        items = [it for it in items if kw in it.get("title", "") or kw in it.get("content", "")]

    return {
        "code": 200,
        "msg": "success",
        "data": {
            "keyword": kw,
            "channel": channel,
            "page": page,
            "size": len(items),
            "items": items
        }
    }

# ==============================================================================
# 接口实现：商城商品 (Goods)
# ==============================================================================

@app.get("/api/v1/goods", summary="获取商品列表 (GET)", tags=["商城模块"])
def get_goods_list(
    category: Optional[str] = Query("全部", description="分类：全部/推荐/数码/家具/穿搭/水果/个护"),
    keyword: Optional[str] = Query(None, description="搜索关键词"),
    limit: Optional[int] = Query(30, ge=1, le=100, description="单次返回商品数量，默认30条"),
    page: int = Query(1, ge=1, description="页码；加载更多请求下一页"),
    refresh: Optional[bool] = Query(False, description="是否强制重新获取并清除分类分页缓存")
):
    """默认获取网易严选官网商品；按页加载，每页默认30件。"""
    target_category = category if (category and category != "全部") else "推荐"
    if goods_source == "yanxuan":
        target_category = {"家居": "家具", "美食": "水果"}.get(target_category, target_category)
    if goods_source == "yanxuan":
        try:
            page_data = yanxuan_catalog.list_page(target_category, page=page, limit=limit, refresh=refresh)
        except GoodsUnavailable as error:
            raise HTTPException(status_code=502, detail=str(error)) from error
        result, has_more = page_data['items'], page_data['has_more']
    else:
        available = list(get_active_goods_catalog(target_category, limit=None, refresh=refresh))
        start = (page - 1) * limit
        result = available[start:start + limit]
        has_more = start + limit < len(available)
    if category and category not in ("全部", "推荐"):
        result = [g for g in result if g.get("category") == target_category]
    if keyword and keyword.strip():
        kw = keyword.strip()
        result = [g for g in result if kw in g.get("title", "")]

    if goods_source == "yanxuan":
        result = [dict(g, details={
            'source': g.get('source', '网易严选'), 'currency': g.get('currency', 'CNY'),
            'description': g.get('description', ''),
            'images': [g['image_url']] if g.get('image_url') else [],
            'summaryOnly': True,
        }) for g in result]

    return {
        "code": 200,
        "msg": "success",
        "data": {
            "total": len(result),
            "page": page,
            "has_more": has_more,
            "category": category,
            "items": result
        }
    }

@app.post("/api/v1/goods", summary="录入自有商品", tags=["商城管理"])
def create_goods(payload: GoodsCreate):
    try:
        item = product_catalog.create(payload.model_dump())
    except ValueError as error:
        raise HTTPException(status_code=409, detail=str(error)) from error
    return {"code": 200, "msg": "success", "data": item}

@app.put("/api/v1/goods/{product_id}", summary="更新自有商品", tags=["商城管理"])
def update_goods(product_id: str, payload: GoodsWrite):
    item = product_catalog.update(product_id, payload.model_dump())
    if item is None:
        raise HTTPException(status_code=404, detail="商品未找到")
    return {"code": 200, "msg": "success", "data": item}

@app.delete("/api/v1/goods/{product_id}", summary="删除自有商品", tags=["商城管理"])
def delete_goods(product_id: str):
    if not product_catalog.delete(product_id):
        raise HTTPException(status_code=404, detail="商品未找到")
    cart_storage.update_item(product_id, 0)
    return {"code": 200, "msg": "success", "data": {"product_id": product_id}}

@app.get("/api/v1/goods/{product_id}", summary="获取商品详情 (GET)", tags=["商城模块"])
def get_goods_detail(product_id: str = Path(..., description="商品ID"), refresh: bool = Query(False)):
    """获取指定商品的详细信息"""
    try:
        item = yanxuan_catalog.detail(product_id, refresh=refresh) if goods_source == "yanxuan" else find_active_product(product_id)
    except GoodsUnavailable as error:
        raise HTTPException(status_code=503, detail=str(error)) from error
    if item is not None:
        return {"code": 200, "msg": "success", "data": item}
    raise HTTPException(status_code=404, detail="商品未找到")

@app.post("/api/v1/goods/filter", summary="多条件筛选商品 (POST)", tags=["商城模块"])
def filter_goods(payload: GoodsFilterRequest):
    """通过 POST 方法按价格区间、分类与排序方式筛选商品"""
    cat = payload.category if (payload.category and payload.category != "全部") else "推荐"
    if goods_source == "yanxuan":
        cat = {"家居": "家具", "美食": "水果"}.get(cat, cat)
    result = list(get_active_goods_catalog(cat))
    if payload.category and payload.category not in ("全部", "推荐"):
        result = [g for g in result if g.get("category") == cat]
    if payload.min_price is not None:
        result = [g for g in result if g.get("price", 0) >= payload.min_price]
    if payload.max_price is not None:
        result = [g for g in result if g.get("price", 0) <= payload.max_price]

    if payload.sort == "sales_desc":
        result.sort(key=lambda x: x.get("sales", 0), reverse=True)
    elif payload.sort == "price_asc":
        result.sort(key=lambda x: x.get("price", 0))
    elif payload.sort == "price_desc":
        result.sort(key=lambda x: x.get("price", 0), reverse=True)

    return {
        "code": 200,
        "msg": "success",
        "data": {
            "count": len(result),
            "items": result
        }
    }

# ==============================================================================
# 接口实现：购物车 (Cart)
# ==============================================================================

def _calculate_cart():
    return cart_storage.get_cart()

@app.get("/api/v1/cart", summary="获取购物车清单 (GET)", tags=["购物车模块"])
async def get_cart():
    """获取当前用户的购物车商品明细与总计金额"""
    return {
        "code": 200,
        "msg": "success",
        "data": cart_storage.get_cart()
    }

@app.post("/api/v1/cart/add", summary="添加商品到购物车 (POST)", tags=["购物车模块"])
@app.post("/api/v1/cart", include_in_schema=False)
async def add_to_cart(payload: CartAddRequest):
    """向购物车新增商品或增加购买数量"""
    pid = payload.product_id
    product = find_active_product(pid)
    if product is None:
        raise HTTPException(status_code=404, detail="商品未找到")
    snapshot = {
        "title": product.get("title", ""),
        "price": float(product.get("price", 0.0)),
        "category": product.get("category", "其他"),
        "image_url": product.get("image_url", ""),
        "image_res": product.get("image_res", "ic_shop")
    }
    cart_data = cart_storage.add_item(pid, payload.count, snapshot)
    current_count = next((it["count"] for it in cart_data["items"] if it["product_id"] == pid), 0)
    return {
        "code": 200,
        "msg": "添加购物车成功",
        "data": {
            "product_id": pid,
            "count": current_count,
            "cart": cart_data
        }
    }

@app.post("/api/v1/cart/update", summary="更新购物车数量或删除 (POST)", tags=["购物车模块"])
async def update_cart(payload: CartUpdateRequest):
    """更新购物车中某项商品的数量（数量<=0将直接移除）"""
    pid = payload.product_id
    if payload.count > 0 and find_active_product(pid) is None:
        raise HTTPException(status_code=404, detail="商品未找到")
    cart_data = cart_storage.update_item(pid, payload.count)
    if payload.count <= 0:
        msg = "商品已从购物车移除"
        current_count = 0
    else:
        msg = "购物车数量已更新"
        current_count = next((it["count"] for it in cart_data["items"] if it["product_id"] == pid), 0)

    return {
        "code": 200,
        "msg": msg,
        "data": {
            "product_id": pid,
            "count": current_count,
            "cart": cart_data
        }
    }

@app.post("/api/v1/cart/clear", summary="清空购物车 (POST)", tags=["购物车模块"])
async def clear_cart():
    """清空购物车中所有已选商品"""
    cart_data = cart_storage.clear()
    return {
        "code": 200,
        "msg": "购物车已清空",
        "data": cart_data
    }

# ==============================================================================
# 视频模块：视频 / 专辑 / 天气（演示数据，与 App 内 VideoCatalog mock 一致）
# ==============================================================================

class VideoOut(BaseModel):
    id: str = Field(..., description="视频稳定ID")
    title: str = Field(..., description="标题")
    source: str = Field("", description="来源（副标题用）")
    duration: str = Field("00:08", description="时长")
    play_count: str = Field("本地样片", description="播放量文案")
    desc: str = Field("", description="简介")
    cover: str = Field("image2", description="封面 drawable 名")
    channel: str = Field("recommend", description="所属频道 recommend/small/movie/music")
    album_id: str = Field("", description="所属专辑ID，空为独立视频")
    like_count: int = Field(0, description="点赞数")
    comment_count: int = Field(0, description="评论数")

class AlbumOut(BaseModel):
    id: str = Field(..., description="专辑稳定ID")
    title: str = Field(..., description="专辑标题")
    subtitle: str = Field("", description="副标题")
    update_info: str = Field("", description="更新信息")
    cover: str = Field("image2", description="封面 drawable 名")
    video_ids: List[str] = Field(default_factory=list, description="按集顺序的视频ID列表")

class WeatherDay(BaseModel):
    date: str = Field(..., description="日期 MM-DD")
    week: str = Field(..., description="星期")
    high: int = Field(..., description="最高气温℃")
    low: int = Field(..., description="最低气温℃")
    cond: str = Field("晴", description="天气现象")

# 演示数据：20 条视频 = App VideoCatalog.seedMock() 的同源数据；
# cover 用 drawable 名，App 端经 getIdentifier 映射本机资源。
MOCK_VIDEOS = [
    {"id": "v01", "title": "纪录片：大国重器是怎样炼成的", "source": "央视新闻", "duration": "12:36", "play_count": "128万次播放", "desc": "镜头走进重型装备制造车间，记录一台大型设备从零件加工到整机装配的全过程。工人师傅们在微米级的精度要求下，一遍遍测量、打磨。", "cover": "image2", "channel": "recommend", "album_id": "album-daguo", "like_count": 8600, "comment_count": 326},
    {"id": "v02", "title": "延时摄影记录城市从黎明到黄昏", "source": "影像志", "duration": "04:12", "play_count": "56万次播放", "desc": "摄影师在城市最高处架设机位，连续拍摄十二个小时。整座城市的一天被压缩进短短几分钟。", "cover": "image7", "channel": "recommend", "album_id": "album-city", "like_count": 3200, "comment_count": 168},
    {"id": "v03", "title": "航拍中国：从空中俯瞰大好河山", "source": "航拍视角", "duration": "08:45", "play_count": "92万次播放", "desc": "镜头掠过奔腾的江河、金黄的麦田和错落的村落，山川河流呈现出与地面完全不同的壮阔面貌。", "cover": "image5", "channel": "recommend", "album_id": "album-daguo", "like_count": 5400, "comment_count": 271},
    {"id": "v04", "title": "实拍：消防员火场逆行救人全过程", "source": "现场实录", "duration": "06:03", "play_count": "213万次播放", "desc": "浓烟从窗口不断涌出。消防员沿着楼道逐层搜救，在五楼卧室内找到一名被困老人，背起老人沿原路撤离，整个过程不到六分钟。", "cover": "img4", "channel": "recommend", "album_id": "album-daguo", "like_count": 9600, "comment_count": 412},
    {"id": "v05", "title": "街头采访：你理想中的生活是什么样的", "source": "城市观察", "duration": "09:28", "play_count": "34万次播放", "desc": "记者在街头随机采访了二十位路人。答案各不相同，但大多朴素而具体。", "cover": "image8", "channel": "recommend", "album_id": "album-city", "like_count": 2100, "comment_count": 486},
    {"id": "v06", "title": "萌宠日常：猫咪第一次见到雪的反应", "source": "萌宠日记", "duration": "00:15", "play_count": "320万次播放", "desc": "第一次见到雪的猫咪先是愣在原地，随后小心翼翼地伸出一只爪子试探，最后还是忍不住扑了进去。", "cover": "image5", "channel": "small", "album_id": "", "like_count": 9800, "comment_count": 356},
    {"id": "v07", "title": "一分钟学会三道家常菜，新手零失败", "source": "厨房小窍门", "duration": "00:58", "play_count": "88万次播放", "desc": "西红柿炒蛋、蒜蓉青菜、可乐鸡翅，三步一步不落，照着做就行。", "cover": "img4", "channel": "small", "album_id": "", "like_count": 4200, "comment_count": 512},
    {"id": "v08", "title": "手艺人用木头做出会走的小机器人", "source": "手工达人", "duration": "00:42", "play_count": "156万次播放", "desc": "全程不用一颗钉子、一滴胶水，所有零件靠榫卯咬合。上紧发条后，机器人能稳稳走上好几米。", "cover": "image7", "channel": "small", "album_id": "", "like_count": 7300, "comment_count": 289},
    {"id": "v09", "title": "街头钢琴：路人即兴弹奏惊艳全场", "source": "街头艺术", "duration": "00:33", "play_count": "271万次播放", "desc": "琴键摆在广场中央，谁都可以坐下来弹一段。这位路人坐下后，围观的人群渐渐安静了。", "cover": "image8", "channel": "small", "album_id": "album-city", "like_count": 8900, "comment_count": 401},
    {"id": "v10", "title": "慢镜头：雨天窗外的十分钟", "source": "慢镜头", "duration": "00:21", "play_count": "19万次播放", "desc": "雨滴砸在玻璃上，慢慢汇成一道道水痕。没有旁白，只有雨声。", "cover": "image2", "channel": "small", "album_id": "", "like_count": 1200, "comment_count": 87},
    {"id": "v11", "title": "话剧《茶馆》复排上演 幕后纪录", "source": "戏剧观察", "duration": "15:20", "play_count": "45万次播放", "desc": "本轮复排保留了经典版本的结构，同时在舞台呈现上做了新的尝试。镜头跟拍排练厅里的两个月。", "cover": "image6", "channel": "movie", "album_id": "", "like_count": 2600, "comment_count": 143},
    {"id": "v12", "title": "电影幕后：一场雨戏是怎么拍出来的", "source": "影视工业", "duration": "11:08", "play_count": "67万次播放", "desc": "洒水车、灯光组、摄影机轨道，一场看起来只有两分钟的雨戏，背后是四十多人的配合。", "cover": "image2", "channel": "movie", "album_id": "", "like_count": 3400, "comment_count": 197},
    {"id": "v13", "title": "老片修复：让经典重新清晰起来", "source": "电影资料馆", "duration": "18:33", "play_count": "28万次播放", "desc": "修复师逐帧处理划痕和噪点，一部两小时的老电影往往需要几个月才能完成。", "cover": "image8", "channel": "movie", "album_id": "", "like_count": 1800, "comment_count": 96},
    {"id": "v14", "title": "配音演员现场：一个人配出整部剧", "source": "声音工坊", "duration": "07:56", "play_count": "103万次播放", "desc": "同一个人的声音在不同角色之间来回切换，录音棚外的观众听得目瞪口呆。", "cover": "img4", "channel": "movie", "album_id": "", "like_count": 6700, "comment_count": 318},
    {"id": "v15", "title": "经典配乐赏析：旋律如何讲故事", "source": "电影音乐", "duration": "13:14", "play_count": "39万次播放", "desc": "从主题动机到配器选择，拆解几段耳熟能详的旋律是怎么把情绪推上去的。", "cover": "image5", "channel": "movie", "album_id": "", "like_count": 2300, "comment_count": 152},
    {"id": "v16", "title": "国风音乐盛典：传统乐器演绎流行金曲", "source": "音乐现场", "duration": "22:41", "play_count": "74万次播放", "desc": "古筝、琵琶、笛箫与现代编曲结合，重新演绎了多首流行歌曲。熟悉的旋律换一种方式演奏，别有一番味道。", "cover": "image7", "channel": "music", "album_id": "", "like_count": 4800, "comment_count": 236},
    {"id": "v17", "title": "民谣现场：一把吉他唱完整个夏天", "source": "livehouse", "duration": "05:37", "play_count": "51万次播放", "desc": "小小的场地里挤满了人，唱到副歌时全场跟着一起哼。", "cover": "image5", "channel": "music", "album_id": "", "like_count": 2900, "comment_count": 174},
    {"id": "v18", "title": "交响乐团排练直击：指挥到底在说什么", "source": "古典频道", "duration": "09:02", "play_count": "22万次播放", "desc": "排练时指挥不断叫停，一遍遍调整某个声部的力度和进入时机。", "cover": "image6", "channel": "music", "album_id": "", "like_count": 1500, "comment_count": 118},
    {"id": "v19", "title": "草原上的长调：一个人的合唱", "source": "民歌采集", "duration": "06:48", "play_count": "18万次播放", "desc": "老人在空旷的草原上唱起长调，声音传得很远。录音师说，这种唱法已经很少有人会了。", "cover": "image2", "channel": "music", "album_id": "", "like_count": 940, "comment_count": 63},
    {"id": "v20", "title": "深夜电台：城市入睡之后的声音", "source": "声音志", "duration": "28:15", "play_count": "63万次播放", "desc": "凌晨两点的便利店、收摊的夜市、最后一班公交，把这些声音拼在一起，就是一座城市的深夜。", "cover": "image8", "channel": "music", "album_id": "", "like_count": 3600, "comment_count": 208},
]

MOCK_ALBUMS = [
    {"id": "album-daguo", "title": "大国重器系列", "subtitle": "硬核制造现场实录", "update_info": "每周五更新", "cover": "image2", "video_ids": ["v01", "v03", "v04"]},
    {"id": "album-city", "title": "城市影像志", "subtitle": "用镜头记录城市的一天", "update_info": "已完结", "cover": "image7", "video_ids": ["v02", "v05", "v09"]},
]


def _video_out(row: dict) -> dict:
    return {key: row.get(key, "") for key in
            ("id", "title", "source", "duration", "play_count", "desc", "cover", "channel", "album_id", "like_count", "comment_count")}


@app.get("/api/v1/videos", summary="获取视频列表 (GET)", tags=["视频模块"])
async def get_videos(
    channel: Optional[str] = Query(None, description="频道筛选 recommend/small/movie/music，缺省返回全部")
):
    """返回视频列表（演示数据），App 拉取失败时回落本地 mock"""
    rows = [v for v in MOCK_VIDEOS if channel in (None, "", v["channel"])]
    return ApiResponse(code=200, msg="success", data={"videos": [_video_out(v) for v in rows]})


@app.get("/api/v1/albums", summary="获取专辑列表 (GET)", tags=["视频模块"])
async def get_albums():
    """返回全部视频专辑（专题），含按集顺序的视频ID"""
    return ApiResponse(code=200, msg="success", data={"albums": MOCK_ALBUMS})


@app.get("/api/v1/albums/{album_id}", summary="获取专辑详情（含全集视频）", tags=["视频模块"])
async def get_album_detail(album_id: str = Path(..., description="专辑ID，如 album-daguo")):
    album = next((a for a in MOCK_ALBUMS if a["id"] == album_id), None)
    if album is None:
        return ApiResponse(code=404, msg="专辑不存在", data=None)
    episodes = [_video_out(v) for v in MOCK_VIDEOS if v["id"] in album["video_ids"]]
    episodes.sort(key=lambda v: album["video_ids"].index(v["id"]))
    return ApiResponse(code=200, msg="success", data={"album": album, "videos": episodes})


def _weather_mock(city: str) -> dict:
    """7 日演示天气：从今天起算，晴/多云/阴/小雨轮换"""
    conditions = ["晴", "多云", "阴", "小雨"]
    weeks = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]
    today = datetime.date.today()
    days = []
    for i in range(7):
        day = today + datetime.timedelta(days=i)
        days.append({
            "date": day.strftime("%m-%d"),
            "week": "今天" if i == 0 else weeks[day.weekday()],
            "high": 22 + (i * 3) % 4,
            "low": 9 + (i * 2) % 8,
            "cond": conditions[i % len(conditions)],
        })
    return {"city": city, "updated": today.strftime("%m-%d %H:%M"), "days": days}


@app.get("/api/v1/weather", summary="7日天气曲线数据（演示数据）", tags=["天气模块"])
async def get_weather(city: str = Query("武汉", description="城市名，演示数据不区分城市")):
    """ECharts 天气曲线数据源；App 端拉取失败时使用内置 mock"""
    return ApiResponse(code=200, msg="success", data=_weather_mock(city))

# ==============================================================================
# 兼容接口：图片中继与官网页面
# ==============================================================================

@app.get("/api/image_proxy", summary="官网图片代理", tags=["系统服务"])
async def image_proxy(url: str = Query(..., description="原始图片绝对链接")):
    """转发学校官网新闻图片（避开防盗链及证书问题）"""
    if cns and cns.image_cache and url in cns.image_cache:
        data, content_type = cns.image_cache[url]
        return Response(content=data, media_type=content_type)

    if cns and cns.session:
        try:
            resp = cns.session.get(url, timeout=10, verify=False)
            if resp.status_code == 200:
                c_type = resp.headers.get("Content-Type", "image/jpeg")
                cns.image_cache[url] = (resp.content, c_type)
                return Response(content=resp.content, media_type=c_type)
        except Exception as e:
            return JSONResponse(status_code=500, content={"error": str(e)})

    return JSONResponse(status_code=404, content={"error": "Image not found"})

@app.get("/school_page/{page_id}", summary="官网自适应页面中继", tags=["系统服务"])
async def school_page_endpoint(request: Request, page_id: str, url: Optional[str] = None):
    """转发官网移动端精美页面"""
    if cns and hasattr(cns, "app"):
        target_path = f"/school_page/{page_id}"
        if url:
            target_path += f"?url={urllib.parse.quote(url, safe='')}"
        try:
            with cns.app.test_client() as client:
                resp = client.get(target_path)
                return HTMLResponse(content=resp.get_data(as_text=True), status_code=resp.status_code)
        except Exception as e:
            print(f"[FastAPI school_page 异常] {e}")
    return HTMLResponse(f"<h3>晴川官网展示页: {page_id}</h3>")

# ==============================================================================
# 服务启动入口
# ==============================================================================

if __name__ == "__main__":
    import uvicorn
    print("=" * 68)
    print("  [*] 武汉晴川学院 FastAPI 综合服务启动中 (端口: 8000)...")
    print("  ● 交互式 Swagger API 文档: http://localhost:8000/docs")
    print("  ● 统一 API 根路径: http://localhost:8000/api/v1")
    print("  ● Android 模拟器访问地址: http://10.0.2.2:8000/api/v1")
    print("  ● 现有 Flask 5000 服务完全保留，两者可并存运行！")
    print("=" * 68)
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=False)
