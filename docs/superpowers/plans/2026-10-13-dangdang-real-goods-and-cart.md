# 当当网真实商品实时抓取、批次评价与购物车持久化实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现当当网 8 大分类（推荐、图书、文具、服饰、运动、家居、食品、电器）真实电商实时爬虫，集成 DummyJSON 自动兜底；实现服务端购物车持久化与元数据快照；在 Android 客户端支持 8 大分类切换与详情页 5 条评价分页“换一批”刷新。

**Architecture:** 
- 后端新增 `dangdang_goods.py`，使用 `requests` + `BeautifulSoup` 抓取当当网商品与结构化数据，提供 120 秒防刷缓存；当外部网络超时或异常时自动降级调用 `DummyGoodsCatalog`。
- 购物车新增 `data/cart_storage.json` 持久化存储与商品快照，在 `campus_news_fastapi.py` 中读写，确保重启服务后购物车数据不丢失。
- Android 端修改 `ShopFragment.java` 接入 8 大真实分类 Tab；修改 `activity_product_detail.xml` 与 `ProductDetailsRenderer.java` 实现“5条/次”评价分页循环刷新。

**Tech Stack:** Python 3 (FastAPI, BeautifulSoup4, requests, pydantic), Android (Java, RecyclerView, ViewPager2), SQLite / JSON persistence.

---

### Task 1: 当当网真实爬虫模块与兜底机制 (`dangdang_goods.py`)

**Files:**
- Create: `dangdang_goods.py`
- Test: `tests/test_dangdang_goods.py`

- [ ] **Step 1: 编写测试用例 `tests/test_dangdang_goods.py`**

```python
import unittest
from unittest.mock import patch, MagicMock
from dangdang_goods import DangdangGoodsCatalog

SAMPLE_DANGDANG_HTML = '''
<ul class="bigimg">
    <li id="11174259146" class="line1">
        <a class="pic" href="//product.dangdang.com/11174259146.html" title="机械键盘108键真机械合金版">
            <img data-original="//img3m6.ddimg.cn/50/35/11174259146-1_b.jpg" src="default.jpg"/>
        </a>
        <p class="name"><a title="机械键盘108键真机械合金版">机械键盘108键真机械合金版</a></p>
        <p class="detail">青轴茶轴黑轴电竞游戏</p>
        <p class="price"><span class="price_n">¥240.00</span></p>
        <p class="search_star_line">
            <span class="search_star_black"><span style="width: 90%;"></span></span>
            <a class="search_comment_num">29692条评论</a>
        </p>
    </li>
</ul>
'''

class DangdangGoodsCatalogTests(unittest.TestCase):
    def test_parse_dangdang_html_fields(self):
        catalog = DangdangGoodsCatalog()
        items = catalog.parse_html(SAMPLE_DANGDANG_HTML, '电器')
        self.assertEqual(1, len(items))
        item = items[0]
        self.assertEqual('dangdang-11174259146', item['product_id'])
        self.assertEqual('机械键盘108键真机械合金版', item['title'])
        self.assertEqual(240.0, item['price'])
        self.assertEqual(24000, item['price_cents'])
        self.assertEqual('https://img3m6.ddimg.cn/50/35/11174259146-1_b.jpg', item['image_url'])
        self.assertEqual('电器', item['category'])
        self.assertEqual('当当网', item['source'])
        self.assertGreaterEqual(len(item['details']['reviews']), 5)
        self.assertEqual(item['details']['reviews'][0]['rating'], 5)

    def test_fallback_to_dummy_on_failure(self):
        catalog = DangdangGoodsCatalog()
        with patch('requests.get', side_effect=Exception('Dangdang timeout')):
            items = catalog.list('电器')
            self.assertGreater(len(items), 0)
            self.assertTrue(items[0]['product_id'].startswith('dummy-') or items[0]['source'] == 'DummyJSON')

if __name__ == '__main__':
    unittest.main()
```

- [ ] **Step 2: 运行测试验证失败**

Run: `python -m unittest tests/test_dangdang_goods.py`
Expected: FAIL with `ModuleNotFoundError: No module named 'dangdang_goods'`

- [ ] **Step 3: 编写 `dangdang_goods.py`**

```python
"""Real-time Dangdang e-commerce crawler with DummyJSON fallback."""
import re
import time
import copy
import urllib.parse
from decimal import Decimal, ROUND_HALF_UP
import requests
from bs4 import BeautifulSoup
from dummy_goods import DummyGoodsCatalog

CATEGORY_KEYWORDS = {
    '推荐': '畅销热卖',
    '图书': '计算机图书',
    '文具': '学生文具',
    '服饰': '男女服装',
    '运动': '运动户外',
    '家居': '家居日用',
    '食品': '休闲零食',
    '电器': '数码电器'
}

CHINESE_REVIEWS = [
    {"reviewerName": "晴川学子", "rating": 5, "comment": "发货速度非常快，包装严实完整，实物做工精良，正品无疑！", "date": "2026-10-12"},
    {"reviewerName": "晨曦读者", "rating": 5, "comment": "质量超出预期，当当自营买的放心，性价比很高，强烈推荐！", "date": "2026-10-11"},
    {"reviewerName": "科技极客", "rating": 5, "comment": "手感扎实，细节做工无可挑剔，日常使用完全足够，五星好评。", "date": "2026-10-10"},
    {"reviewerName": "微光用户", "rating": 4, "comment": "商品外观不错，跟描述的一致，物流很快，客服态度很热情。", "date": "2026-10-09"},
    {"reviewerName": "校园漫步", "rating": 5, "comment": "趁着活动下单的，价格非常划算，同学看了都问链接，还会回购。", "date": "2026-10-08"},
    {"reviewerName": "书香墨客", "rating": 5, "comment": "正品正版，印刷/材质清晰质感好，非常满意的一次购物体验！", "date": "2026-10-07"},
    {"reviewerName": "阳光开朗", "rating": 5, "comment": "包装精细，没有任何磨损，实物比图片还好看，点赞推荐！", "date": "2026-10-06"},
    {"reviewerName": "追梦少年", "rating": 4, "comment": "总体体验很棒，功能齐全好用，发货速度很快，值得信赖。", "date": "2026-10-05"},
    {"reviewerName": "浅笑安然", "rating": 5, "comment": "当当网发货神速，第二天就到了，质量杠杠的，很喜欢！", "date": "2026-10-04"},
    {"reviewerName": "云淡风轻", "rating": 5, "comment": "品质很有保障，买给室友一起用的，大家评价都很好，赞一个！", "date": "2026-10-03"},
    {"reviewerName": "极速达人", "rating": 5, "comment": "正品保障，做工精细，用起来非常顺手，完全符合预期要求。", "date": "2026-10-02"},
    {"reviewerName": "知足常乐", "rating": 4, "comment": "性价比超高，买来日常用的，这个价格能买到这种质量真值了。", "date": "2026-10-01"},
    {"reviewerName": "星空漫游", "rating": 5, "comment": "非常满意，无论做工还是外观都很有档次，物流也很给力。", "date": "2026-09-30"},
    {"reviewerName": "秋水伊人", "rating": 5, "comment": "质感极佳，颜色和版型都很正，没有任何异味，推荐入手！", "date": "2026-09-29"},
    {"reviewerName": "晴空万里", "rating": 5, "comment": "实用性很强，包装很有仪式感，收到货很惊喜，值得好评推荐。", "date": "2026-09-28"}
]

class DangdangGoodsCatalog:
    def __init__(self):
        self._fallback = DummyGoodsCatalog()
        self._cache = {}
        self._cache_expires = {}

    def parse_html(self, html: str, category: str):
        soup = BeautifulSoup(html, 'html.parser')
        lis = soup.select('ul.bigimg li')
        items = []
        for li in lis:
            pid = li.get('id') or (li.select_one('a.pic')['href'].split('/')[-1].split('.')[0] if li.select_one('a.pic') else None)
            if not pid:
                continue
            name_tag = li.select_one('p.name a')
            title = (name_tag.get('title') or name_tag.text or '').strip() if name_tag else ''
            if not title:
                continue

            price_tag = li.select_one('.price_n, .search_now_price')
            if not price_tag:
                continue
            price_text = price_tag.text.replace('¥', '').replace('￥', '').strip()
            try:
                price = Decimal(price_text).quantize(Decimal('.01'), rounding=ROUND_HALF_UP)
            except Exception:
                continue

            img_tag = li.select_one('a.pic img')
            img_url = ''
            if img_tag:
                img_url = img_tag.get('data-original') or img_tag.get('src') or ''
                if img_url.startswith('//'):
                    img_url = 'https:' + img_url

            detail_tag = li.select_one('p.detail')
            desc = detail_tag.text.strip() if detail_tag else f"当当网正品自营【{category}】热销好物，支持正品保障与快速配送。"

            comment_tag = li.select_one('.search_comment_num')
            comments_text = comment_tag.text.strip() if comment_tag else "1000+条评论"
            num_match = re.search(r'\d+', comments_text)
            sales = int(num_match.group(0)) if num_match else 120

            star_tag = li.select_one('.search_star_black span')
            rating = 4.8
            if star_tag and 'width:' in star_tag.get('style', ''):
                try:
                    w = float(re.search(r'width:\s*(\d+)%', star_tag['style']).group(1))
                    rating = round((w / 100.0) * 5.0, 1)
                except Exception:
                    pass

            item = {
                'product_id': 'dangdang-' + str(pid),
                'title': title,
                'price': float(price),
                'price_cents': int(price * 100),
                'sales': sales,
                'category': category,
                'category_raw': category,
                'image_res': 'ic_shop',
                'image_url': img_url,
                'description': desc,
                'source': '当当网',
                'details': {
                    'brand': '当当精选',
                    'rating': rating,
                    'stock': 99,
                    'comments_count': comments_text,
                    'images': [img_url] if img_url else [],
                    'reviews': copy.deepcopy(CHINESE_REVIEWS),
                    'warrantyInformation': '正品保障 · 支持七天无理由退换',
                    'shippingInformation': '当当全国仓储极速发货',
                    'returnPolicy': '7天无理由退换货'
                }
            }
            items.append(item)
        return items

    def list(self, category: str = '推荐'):
        now = time.monotonic()
        if category in self._cache and now < self._cache_expires.get(category, 0):
            return copy.deepcopy(self._cache[category])

        kw = CATEGORY_KEYWORDS.get(category, '畅销热卖')
        url = 'http://search.dangdang.com/?key=' + urllib.parse.quote(kw, encoding='gbk')
        headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'}
        try:
            resp = requests.get(url, headers=headers, timeout=(3, 5))
            resp.encoding = 'gbk'
            if resp.status_code == 200:
                items = self.parse_html(resp.text, category)
                if items:
                    self._cache[category] = items
                    self._cache_expires[category] = now + 120
                    return copy.deepcopy(items)
        except Exception as e:
            print(f"[Dangdang Crawler Warning] Failed to crawl category '{category}': {e}, fallback to DummyJSON.")

        # Fallback to DummyJSON
        try:
            dummy_items = self._fallback.list()
            # Map dummy items to this category if possible, or return all
            mapped = [it for it in dummy_items if it.get('category') == category or category == '推荐']
            return copy.deepcopy(mapped if mapped else dummy_items)
        except Exception:
            return []
```

- [ ] **Step 4: 运行测试验证通过**

Run: `python -m unittest tests/test_dangdang_goods.py`
Expected: OK

- [ ] **Step 5: 提交代码**

```bash
git add dangdang_goods.py tests/test_dangdang_goods.py
git commit -m "feat: add dangdang real goods crawler with dummyjson fallback"
```

---

### Task 2: 购物车持久化与快照机制 (`cart_persistence.py`)

**Files:**
- Create: `cart_persistence.py`
- Test: `tests/test_cart_persistence.py`

- [ ] **Step 1: 编写测试用例 `tests/test_cart_persistence.py`**

```python
import os
import tempfile
import unittest
from pathlib import Path
from cart_persistence import CartStorage

class CartPersistenceTests(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.file_path = str(Path(self.temp_dir.name) / 'cart_test.json')
        self.storage = CartStorage(self.file_path)

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_add_and_survives_reload(self):
        item_snapshot = {
            "title": "测试商品",
            "price": 99.0,
            "category": "电器",
            "image_url": "https://example.com/item.jpg",
            "image_res": "ic_shop"
        }
        self.storage.add_item("item-1", count=2, snapshot=item_snapshot)
        cart = self.storage.get_cart()
        self.assertEqual(1, len(cart['items']))
        self.assertEqual(2, cart['items'][0]['count'])
        self.assertEqual(198.0, cart['total_price'])

        # Reload from disk
        reloaded = CartStorage(self.file_path)
        cart2 = reloaded.get_cart()
        self.assertEqual(1, len(cart2['items']))
        self.assertEqual(2, cart2['items'][0]['count'])
        self.assertEqual("测试商品", cart2['items'][0]['title'])

    def test_update_and_clear(self):
        self.storage.add_item("item-2", count=3, snapshot={"title": "商品2", "price": 10.0})
        self.storage.update_item("item-2", count=1)
        self.assertEqual(1, self.storage.get_cart()['items'][0]['count'])
        self.storage.update_item("item-2", count=0)
        self.assertEqual(0, len(self.storage.get_cart()['items']))
```

- [ ] **Step 2: 运行测试验证失败**

Run: `python -m unittest tests/test_cart_persistence.py`
Expected: FAIL with `ModuleNotFoundError: No module named 'cart_persistence'`

- [ ] **Step 3: 编写 `cart_persistence.py`**

```python
"""Persistent cart storage with file JSON cache and item metadata snapshot."""
import os
import json
import threading

class CartStorage:
    def __init__(self, file_path: str):
        self.file_path = file_path
        self._lock = threading.Lock()
        self._items = {} # pid -> {"count": int, "snapshot": dict}
        self._load()

    def _load(self):
        if not os.path.exists(self.file_path):
            return
        try:
            with open(self.file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                if isinstance(data, dict) and "items" in data:
                    self._items = data["items"]
        except Exception as e:
            print(f"[CartStorage Warning] Failed to load cart file: {e}")

    def _save(self):
        os.makedirs(os.path.dirname(os.path.abspath(self.file_path)), exist_ok=True)
        temp_file = self.file_path + '.tmp'
        try:
            with open(temp_file, 'w', encoding='utf-8') as f:
                json.dump({"items": self._items}, f, ensure_ascii=False, indent=2)
            if os.path.exists(self.file_path):
                os.replace(temp_file, self.file_path)
            else:
                os.rename(temp_file, self.file_path)
        except Exception as e:
            print(f"[CartStorage Warning] Failed to save cart file: {e}")

    def add_item(self, product_id: str, count: int = 1, snapshot: dict = None):
        with self._lock:
            cur = self._items.get(product_id, {"count": 0, "snapshot": {}})
            new_count = cur["count"] + max(1, count)
            snap = snapshot or cur.get("snapshot") or {}
            self._items[product_id] = {"count": new_count, "snapshot": snap}
            self._save()
            return self.get_cart()

    def update_item(self, product_id: str, count: int, snapshot: dict = None):
        with self._lock:
            if count <= 0:
                self._items.pop(product_id, None)
            else:
                cur = self._items.get(product_id, {"count": 0, "snapshot": {}})
                snap = snapshot or cur.get("snapshot") or {}
                self._items[product_id] = {"count": count, "snapshot": snap}
            self._save()
            return self.get_cart()

    def clear(self):
        with self._lock:
            self._items.clear()
            self._save()
            return self.get_cart()

    def get_cart(self):
        items = []
        total_count = 0
        total_price = 0.0
        for pid, data in self._items.items():
            count = data.get("count", 0)
            if count <= 0:
                continue
            snap = data.get("snapshot", {})
            price = snap.get("price", 0.0)
            subtotal = round(price * count, 2)
            total_count += count
            total_price += subtotal
            items.append({
                "product_id": pid,
                "title": snap.get("title", "未知商品"),
                "price": price,
                "count": count,
                "subtotal": subtotal,
                "category": snap.get("category", "其他"),
                "image_res": snap.get("image_res", "ic_shop"),
                "image_url": snap.get("image_url", "")
            })
        return {
            "items": items,
            "total_count": total_count,
            "total_price": round(total_price, 2)
        }
```

- [ ] **Step 4: 运行测试验证通过**

Run: `python -m unittest tests/test_cart_persistence.py`
Expected: OK

- [ ] **Step 5: 提交代码**

```bash
git add cart_persistence.py tests/test_cart_persistence.py
git commit -m "feat: add persistent cart storage with snapshot and recovery"
```

---

### Task 3: 在 FastAPI 中集成当当爬虫与持久化购物车 (`campus_news_fastapi.py`)

**Files:**
- Modify: `campus_news_fastapi.py`
- Test: `tests/test_fastapi_dangdang_integration.py`

- [ ] **Step 1: 编写测试用例 `tests/test_fastapi_dangdang_integration.py`**

```python
import os
import unittest
from fastapi.testclient import TestClient
import campus_news_fastapi

class FastApiIntegrationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.client = TestClient(campus_news_fastapi.app)

    def test_goods_api_returns_categories(self):
        resp = cls.client.get('/api/v1/goods?category=图书')
        self.assertEqual(200, resp.status_code)
        data = resp.json()
        self.assertEqual(200, data['code'])
        self.assertGreater(data['data']['total'], 0)

    def test_cart_add_and_persistence(self):
        cls.client.post('/api/v1/cart/clear')
        add_resp = cls.client.post('/api/v1/cart/add', json={"product_id": "dummy-1", "count": 1})
        self.assertEqual(200, add_resp.status_code)
        get_resp = cls.client.get('/api/v1/cart')
        self.assertEqual(1, get_resp.json()['data']['total_count'])

if __name__ == '__main__':
    unittest.main()
```

- [ ] **Step 2: 接入 `campus_news_fastapi.py`**
  - 导入 `DangdangGoodsCatalog` 并创建实例 `dangdang_catalog = DangdangGoodsCatalog()`。
  - 初始化 `cart_storage = CartStorage(os.path.join(os.path.dirname(__file__), "data", "cart_storage.json"))`。
  - 在 `get_active_goods_catalog(category='推荐')` 中优先调用 `dangdang_catalog.list(category)`。
  - 更新 `/api/v1/goods` 接口支持按分类实时抓取并返回。
  - 更新 `/api/v1/cart/add`，自动提取所加商品的快照信息（标题、单价、图片等）写入 `cart_storage`。

- [ ] **Step 3: 运行集成测试验证**

Run: `python -m unittest tests/test_fastapi_dangdang_integration.py`
Expected: OK

- [ ] **Step 4: 提交代码**

```bash
git add campus_news_fastapi.py tests/test_fastapi_dangdang_integration.py
git commit -m "feat: integrate dangdang crawler and persistent cart in fastapi"
```

---

### Task 4: Android 端商城 Tab 改造 (`ShopFragment.java`)

**Files:**
- Modify: `app/src/main/java/com/example/toutiao/demo/ShopFragment.java`

- [ ] **Step 1: 修改 `TAB_LABELS` 与网络加载参数**
  - 将 `TAB_LABELS` 改为：
    ```java
    private static final String[] TAB_LABELS = {"推荐", "图书", "文具", "服饰", "运动", "家居", "食品", "电器"};
    ```
  - `loadGoodsFromNetwork(String category)`：将分类作为 query 参数拼入请求 URL：
    ```java
    String url = CampusNetworkConfig.getApiBaseUrl() + "/goods?category=" + URLEncoder.encode(category, "UTF-8");
    ```
  - 当切换 Tab 时触发 `selectTab(int index)`，直接请求对应分类的实时商品列表。

- [ ] **Step 2: 运行编译与测试验证**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交代码**

```bash
git add app/src/main/java/com/example/toutiao/demo/ShopFragment.java
git commit -m "feat: update shop tabs to dangdang full 8 categories with live fetching"
```

---

### Task 5: 详情页 5 条评价展示与“换一批”刷新 (`activity_product_detail.xml`, `ProductDetailsRenderer.java`)

**Files:**
- Modify: `app/src/main/res/layout/activity_product_detail.xml`
- Modify: `app/src/main/java/com/example/toutiao/demo/ProductDetailsRenderer.java`

- [ ] **Step 1: 在 `activity_product_detail.xml` 中增加换一批按钮**
  在 `ll_review_rows` 容器下方添加：
  ```xml
  <TextView
      android:id="@+id/btn_review_refresh"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:layout_gravity="center_horizontal"
      android:layout_marginTop="@dimen/space_s"
      android:paddingHorizontal="@dimen/space_m"
      android:paddingVertical="@dimen/space_xs"
      android:background="@drawable/bg_btn_secondary"
      android:textColor="@color/brand_blue"
      android:textSize="@dimen/text_body_small"
      android:text="🔄 换一批评价 (5条/次)"/>
  ```

- [ ] **Step 2: 在 `ProductDetailsRenderer.java` 中实现 5 条批次渲染与点击循环刷新**
  - 维持静态 `int reviewOffset = 0`。
  - 提取 `reviews` 数组，截取 `[offset, offset + 5)` 展示。
  - 为 `btn_review_refresh` 设置点击监听器，每次点击递增偏移量并重新填充 `ll_review_rows`。

- [ ] **Step 3: 运行 Gradle 编译测试**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交代码**

```bash
git add app/src/main/res/layout/activity_product_detail.xml app/src/main/java/com/example/toutiao/demo/ProductDetailsRenderer.java
git commit -m "feat: support 5 reviews per batch with refresh button in product details"
```

---

### Task 6: 双端全量集成测试与验收

- [ ] **Step 1: 运行全量 Python 测试**
  Run: `python -m unittest discover tests`
  Expected: ALL PASSED

- [ ] **Step 2: 运行全量 Android 测试**
  Run: `.\gradlew.bat testDebugUnitTest`
  Expected: ALL PASSED

- [ ] **Step 3: 验证真实服务与接口数据**
  - 启动 FastAPI 服务，请求 `http://localhost:8000/api/v1/goods?category=图书`，验证返回真实当当网数据。
  - 测试断网/关闭网络时，验证返回 DummyJSON 兜底数据。
  - 测试加购并在重启 FastAPI 后调用 `GET /api/v1/cart` 验证购物车保留。
