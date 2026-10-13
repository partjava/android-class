"""Real-time Dangdang e-commerce crawler with DummyJSON fallback."""
import copy
import re
import time
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

STATIC_FALLBACK_ITEMS = [
    {
        'product_id': 'dummy-1',
        'title': '精选备用商品',
        'price': 199.0,
        'price_cents': 19900,
        'sales': 88,
        'category': '电器',
        'image_res': 'ic_shop',
        'image_url': '',
        'description': '网络异常时的备用商品',
        'source': 'DummyJSON',
        'details': {
            'brand': '品牌精选',
            'rating': 4.8,
            'stock': 50,
            'reviews': copy.deepcopy(CHINESE_REVIEWS)
        }
    }
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
            mapped = [
                it for it in dummy_items
                if it.get('category') == category or category == '推荐' or (category == '电器' and it.get('category') == '数码')
            ]
            result = mapped if mapped else dummy_items
            if result:
                return copy.deepcopy(result)
        except Exception:
            pass

        # Static fallback guarantee
        fallback_copy = copy.deepcopy(STATIC_FALLBACK_ITEMS)
        for item in fallback_copy:
            item['category'] = category
        return fallback_copy
