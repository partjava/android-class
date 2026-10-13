"""Real-time NetEase Yanxuan e-commerce client with official category tree and fallback."""
import re
import copy
import time
import random
import urllib.parse
from decimal import Decimal, ROUND_HALF_UP
import requests
import urllib3
from dummy_goods import DummyGoodsCatalog

urllib3.disable_warnings()

# 官方分类 ID（直接对接网易严选官方类目树）
OFFICIAL_CATEGORY_IDS = {
    '数码': 1043000,    # 官方数码家电 (吹风机/剃须刀/按摩仪/电动牙刷/数码配件等 300+ 件)
    '家居': 1005000,    # 官方床品家纺与居家 (乳胶枕/四件套/羽绒被/家居日用等 130+ 件)
    '穿搭': 1010000,    # 官方男装/服饰 (羽绒服/衬衫/卫衣/裤装/配饰等 150+ 件)
    '美食': 109342004,  # 官方休闲零食 (坚果/肉脯/纯黑咖啡/冲饮等 110+ 件)
    '个护': 109342012   # 官方美妆个护 (洁面/水乳/洗发水/身体乳等 260+ 件)
}

CATEGORY_KEYWORDS = {
    '推荐': '热销',
    '数码': '数码',
    '家居': '居家',
    '穿搭': '服饰',
    '美食': '食品',
    '个护': '个护'
}

CHINESE_REVIEWS = [
    {"reviewerName": "晴川学子", "rating": 5, "comment": "网易严选品质一直在线，做工用料非常扎实，正品无疑！", "date": "2026-10-12"},
    {"reviewerName": "晨曦买家", "rating": 5, "comment": "包装精美，发货速度很快，实物比图片质感还要好，性价比极高。", "date": "2026-10-11"},
    {"reviewerName": "科技先锋", "rating": 5, "comment": "细节设计非常人性化，日常使用很顺手，五星好评！", "date": "2026-10-10"},
    {"reviewerName": "微光微风", "rating": 4, "comment": "跟描述的一模一样，没有任何异味，顺丰快递第二天就送到了。", "date": "2026-10-09"},
    {"reviewerName": "校园生活", "rating": 5, "comment": "活动价买的特别划算，室友看了也想买，严选品质信得过。", "date": "2026-10-08"},
    {"reviewerName": "素锦流年", "rating": 5, "comment": "自营正品很让人安心，极简设计风格很好看，非常满意！", "date": "2026-10-07"},
    {"reviewerName": "阳光男孩", "rating": 5, "comment": "品质超出预期，颜色很正，做工无挑剔，值得回购推荐。", "date": "2026-10-06"},
    {"reviewerName": "清风徐来", "rating": 4, "comment": "用了一周感觉很耐用，手感舒适，整体体验非常棒。", "date": "2026-10-05"},
    {"reviewerName": "极简主义", "rating": 5, "comment": "严选的设计就是舒服，简约大方，物流和售后都很有保障。", "date": "2026-10-04"},
    {"reviewerName": "浅笑安然", "rating": 5, "comment": "发货神速，质量杠杠的，已经推荐给身边的朋友了！", "date": "2026-10-03"},
    {"reviewerName": "云淡风轻", "rating": 5, "comment": "做工精细，材质优良，这个价格能买到这种品质物超所值。", "date": "2026-10-02"},
    {"reviewerName": "静水深流", "rating": 4, "comment": "日常必备好物，实用性很强，很符合我的预期要求。", "date": "2026-10-01"},
    {"reviewerName": "星辰大海", "rating": 5, "comment": "超级喜欢这个配色和质感，大牌代工品质名不虚传！", "date": "2026-09-30"},
    {"reviewerName": "知足常乐", "rating": 5, "comment": "正品自营保障，细节拉满，给网易严选的品控点赞！", "date": "2026-09-29"},
    {"reviewerName": "晴空万里", "rating": 5, "comment": "包装很讲究，收到货非常惊喜，正品买得放心，好评！", "date": "2026-09-28"}
]

STATIC_FALLBACK_ITEMS = [
    {
        'product_id': 'yanxuan-fallback-1',
        'title': '网易严选 便携随行大容量保温杯',
        'price': 79.0,
        'price_cents': 7900,
        'sales': 1200,
        'category': '家居',
        'image_res': 'ic_shop',
        'image_url': 'https://yanxuan-item.nosdn.127.net/29117f0a594d46e55b41144df4ef8b4a.jpg',
        'description': '316L不锈钢内胆，24小时长效保温保冷。',
        'source': '网易严选',
        'details': {
            'brand': '网易严选',
            'rating': 4.9,
            'stock': 99,
            'reviews': copy.deepcopy(CHINESE_REVIEWS)
        }
    }
]


class YanxuanGoodsCatalog:
    def __init__(self):
        self._fallback = DummyGoodsCatalog()
        self._cache = {}          # category -> all items list (100~300+ items)
        self._cache_expires = {}  # category -> expire timestamp
        self._batch_offset = {}   # category -> current batch offset for 换一批

    def parse_api_response(self, data: dict, category: str):
        raw_items = []
        d = data.get('data') if isinstance(data.get('data'), dict) else data
        if 'categoryItemList' in d:
            for grp in d.get('categoryItemList', []):
                raw_items.extend(grp.get('itemList', []))
        elif 'directly' in d:
            sr = d.get('directly') or {}
            raw_items = (sr.get('searcherResult') or {}).get('result') or []
        elif 'result' in d:
            raw_items = d.get('result') or []

        items = []
        seen_ids = set()
        for it in raw_items:
            item_id = it.get('id')
            if not item_id or item_id in seen_ids:
                continue
            seen_ids.add(item_id)
            name = (it.get('name') or '').strip()
            if not name:
                continue

            raw_price = it.get('retailPrice') or it.get('unitPrice') or 0.0
            try:
                price = Decimal(str(raw_price)).quantize(Decimal('.01'), rounding=ROUND_HALF_UP)
            except Exception:
                continue

            pic = it.get('primaryPicUrl') or it.get('listPicUrl') or ''
            if pic.startswith('//'):
                pic = 'https:' + pic

            desc = it.get('simpleDesc') or f"网易严选【{category}】热销爆款，官方正品，严选好物。"
            sales = it.get('sellVolume') or random.randint(800, 3500)

            item = {
                'product_id': 'yanxuan-' + str(item_id),
                'title': name,
                'price': float(price),
                'price_cents': int(price * 100),
                'sales': sales,
                'category': category,
                'category_raw': category,
                'image_res': 'ic_shop',
                'image_url': pic,
                'description': desc,
                'source': '网易严选',
                'details': {
                    'brand': '网易严选',
                    'rating': 4.9,
                    'stock': 99,
                    'comments_count': f"{sales}+条好评",
                    'images': [pic] if pic else [],
                    'reviews': copy.deepcopy(CHINESE_REVIEWS),
                    'warrantyInformation': '网易严选正品保障 · 30天无忧退换',
                    'shippingInformation': '顺丰极速配送 · 严选华中大仓直发',
                    'returnPolicy': '30天无理由退换货'
                }
            }
            items.append(item)
        return items

    def _fetch_category_raw(self, category: str):
        headers = {
            'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148'
        }
        # 1. 优先请求网易严选官方类目树 (精确匹配官网分类)
        cid = OFFICIAL_CATEGORY_IDS.get(category)
        if cid:
            try:
                url = f'https://m.you.163.com/item/list.json?categoryId={cid}'
                resp = requests.get(url, headers=headers, verify=False, timeout=(3, 6))
                if resp.status_code == 200:
                    items = self.parse_api_response(resp.json(), category)
                    if items:
                        return items
            except Exception as e:
                print(f"[Yanxuan Official Tree Warning] category {category} (cid {cid}) failed: {e}")

        # 2. 搜索接口兜底
        kw = CATEGORY_KEYWORDS.get(category, category)
        try:
            search_url = f'https://you.163.com/xhr/search/search.json?keyword={urllib.parse.quote(kw)}&size=100'
            resp = requests.get(search_url, headers=headers, verify=False, timeout=(3, 5))
            if resp.status_code == 200:
                items = self.parse_api_response(resp.json(), category)
                if items:
                    return items
        except Exception as e:
            print(f"[Yanxuan Search Warning] category {category} failed: {e}")

        return []

    def _get_or_load_all_items(self, category: str):
        now = time.monotonic()
        if category in self._cache and now < self._cache_expires.get(category, 0):
            return self._cache[category]

        items = []
        if category == '推荐':
            # 汇聚各官方大类的头部尖货
            for sub_cat in ['数码', '家居', '穿搭', '美食', '个护']:
                sub_items = self._get_or_load_all_items(sub_cat)
                if sub_items:
                    items.extend(sub_items[:25])
            if items:
                random.seed(42)
                random.shuffle(items)
        else:
            try:
                items = self._fetch_category_raw(category)
            except Exception as e:
                print(f"[Yanxuan API Warning] Failed to fetch category '{category}': {e}, fallback.")

        if not items:
            # Fallback to DummyJSON
            try:
                dummy_items = self._fallback.list()
                cat_map = {'数码': '数码', '家居': '家居', '穿搭': '服饰', '美食': '食品', '个护': '美妆'}
                target = cat_map.get(category)
                mapped = [it for it in dummy_items if it.get('category') == target or category == '推荐']
                items = mapped if mapped else dummy_items
            except Exception:
                fallback_copy = copy.deepcopy(STATIC_FALLBACK_ITEMS)
                for it in fallback_copy:
                    it['category'] = category
                items = fallback_copy

        self._cache[category] = items
        self._cache_expires[category] = now + 300  # 5分钟缓存
        return items

    def list(self, category: str = '推荐', limit: int = 30, refresh: bool = False):
        if refresh:
            now = time.monotonic()
            if now > self._cache_expires.get(category, 0) - 240:
                self._cache.pop(category, None)

        all_items = self._get_or_load_all_items(category)
        if not all_items:
            return []

        if limit is None or limit <= 0 or len(all_items) <= limit:
            return copy.deepcopy(all_items)

        current_offset = self._batch_offset.get(category, 0)
        if refresh:
            current_offset = (current_offset + limit) % len(all_items)
            self._batch_offset[category] = current_offset

        batch = all_items[current_offset:current_offset + limit]
        if len(batch) < limit:
            batch.extend(all_items[:limit - len(batch)])
        return copy.deepcopy(batch)
