"""The six user-selected Yanxuan searches, with no fabricated product data."""
import copy
import json
import re
from datetime import datetime, timezone, timedelta
from html.parser import HTMLParser
import threading
import time
import urllib.parse
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal, ROUND_HALF_UP
import requests
from .dummy_goods import GoodsUnavailable

CATEGORY_SEARCH_KEYWORDS = {
    '推荐': ['生活'], '数码': ['数码'], '家具': ['家具'],
    '穿搭': ['服饰'], '水果': ['水果'], '个护': ['护肤品'],
}
ALIASES = {'家居': '家具', '美食': '水果', '全部': '推荐'}
_FETCHERS = ThreadPoolExecutor(max_workers=6)


class _DescriptionParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.images, self.text = [], []

    def handle_starttag(self, tag, attrs):
        if tag == 'img':
            data = dict(attrs)
            url = data.get('data-original') or data.get('src') or ''
            if url.startswith('//'):
                url = 'https:' + url
            if url.startswith(('https://', 'http://')) and url not in self.images:
                self.images.append(url)

    def handle_data(self, text):
        if text.strip():
            self.text.append(text.strip())

def _request_search(url, **kwargs):
    try:
        return requests.get(url, **kwargs)
    except requests.ConnectionError:
        # One retry for a transient connection reset; HTTP rejection is not retried.
        return requests.get(url, **kwargs)


class YanxuanGoodsCatalog:
    def __init__(self):
        self._cache = {}
        self._cache_expires = {}
        self._batch_offset = {}
        self._failures = {}
        self._locks = {}
        self._guard = threading.Lock()
        self._detail_cache = {}

    def detail(self, product_id, refresh=False):
        if not re.fullmatch(r'yanxuan-[0-9]{1,15}', product_id):
            return None
        with self._guard:
            lock = self._locks.setdefault(product_id, threading.Lock())
        with lock:
            cached = self._detail_cache.get(product_id)
            if cached and not refresh and time.monotonic() < cached[0]:
                return copy.deepcopy(cached[1])
            item_id = product_id.removeprefix('yanxuan-')
            url = 'https://you.163.com/item/detail?id=' + item_id
            headers = {'User-Agent': 'Mozilla/5.0', 'Referer': url}
            try:
                response = _request_search(url, headers=headers, timeout=(2, 5))
                response.raise_for_status()
                html = response.text
                start = re.search(r'var\s+JSON_DATA_FROMFTL\s*=\s*\{\s*"item"\s*:', html)
                if not start:
                    raise ValueError('Missing item data')
                raw, _ = json.JSONDecoder().raw_decode(html, start.end())
                if str(raw.get('id')) != item_id:
                    raise ValueError('Mismatched item ID')
                previous = self.find(product_id)
                items = self.parse_api_response({'result': [raw]}, previous['category'] if previous else '推荐')
                if not items:
                    raise ValueError('Invalid item data')
                item = items[0]
                details = item['details']
                detail = raw.get('itemDetail') or {}
                for key in ['picUrl1', 'picUrl2', 'picUrl3', 'picUrl4', 'picUrl5']:
                    image = detail.get(key) or ''
                    if image.startswith('//'):
                        image = 'https:' + image
                    if image.startswith(('https://', 'http://')) and image not in details['images']:
                        details['images'].append(image)
                parser = _DescriptionParser()
                parser.feed(detail.get('detailHtml') or '')
                details['descriptionImages'] = parser.images
                if parser.text:
                    details['description'] = '\n'.join(parser.text)
                details['attributes'] = [{a.get('attrName', '参数'): a.get('attrValue', '')}
                                         for a in raw.get('attrList') or []]
                count = re.search(r'"commentCount"\s*:\s*(\d+)', html)
                rate = re.search(r'''"commentGoodRates"\s*:\s*['"]([\d.]+%)['"]''', html)
                if count:
                    details['commentCount'] = int(count[1])
                if rate:
                    details['commentGoodRates'] = rate[1]
            except Exception as error:
                raise GoodsUnavailable('网易严选商品详情暂时无法加载，请重试') from error
            try:
                response = _request_search('https://you.163.com/xhr/comment/listByItemByTag.json',
                    params={'itemId': item_id, 'tag': '全部', 'size': 20, 'page': 1,
                            'orderBy': 0, 'oldItemTag': '全部', 'oldItemOrderBy': 0, 'tagChanged': 0},
                    headers=headers, timeout=(2, 4))
                response.raise_for_status()
                payload = response.json()
                if str(payload.get('code')) != '200':
                    raise ValueError('Comments unavailable')
                reviews = []
                for review in (payload.get('data') or {}).get('commentList') or []:
                    timestamp = review.get('createTime')
                    date = datetime.fromtimestamp(timestamp / 1000, timezone(timedelta(hours=8))).strftime('%Y-%m-%d') if isinstance(timestamp, (int, float)) else ''
                    reviews.append({'reviewerName': review.get('frontUserName') or '匿名用户',
                                    'comment': review.get('content') or '', 'rating': review.get('star'),
                                    'date': date})
                details['reviews'] = reviews
                details['reviewsStatus'] = 'loaded'
            except Exception:
                details['reviewsStatus'] = 'unavailable'
            self._detail_cache[product_id] = (time.monotonic() + 300, item)
            return copy.deepcopy(item)

    def parse_api_response(self, data, category):
        d = data.get('data') if isinstance(data.get('data'), dict) else data
        if 'categoryItemList' in d:
            raw_items = [item for group in d.get('categoryItemList', []) for item in group.get('itemList', [])]
        elif 'directly' in d:
            raw_items = ((d.get('directly') or {}).get('searcherResult') or {}).get('result') or []
        else:
            raw_items = d.get('result') or []
        result, seen = [], set()
        category = ALIASES.get(category, category)
        for raw in raw_items:
            if not isinstance(raw, dict):
                continue
            item_id, name = raw.get('id'), str(raw.get('name') or '').strip()
            if not item_id or item_id in seen or not name:
                continue
            try:
                amount = raw.get('retailPrice')
                if amount is None:
                    amount = raw.get('unitPrice')
                price = Decimal(str(amount))
                if not price.is_finite() or price < 0:
                    continue
                price = price.quantize(Decimal('.01'), rounding=ROUND_HALF_UP)
            except Exception:
                continue
            seen.add(item_id)
            def image(url):
                url = str(url or '')
                if url.startswith('//'):
                    url = 'https:' + url
                return url if url.startswith(('https://', 'http://')) else ''
            pic = image(raw.get('primaryPicUrl') or raw.get('listPicUrl'))
            images = []
            for url in [pic, image(raw.get('listPicUrl'))]:
                if url and url not in images:
                    images.append(url)
            details = {
                'source': '网易严选', 'currency': 'CNY', 'price': float(price),
                'description': raw.get('simpleDesc') or '', 'images': images,
                'sku': str(raw.get('primarySkuId') or ''),
                'source_url': 'https://you.163.com/item/detail?id=' + str(item_id),
            }
            for key in ['brand', 'rating', 'stock', 'reviews', 'warrantyInformation', 'shippingInformation', 'returnPolicy']:
                if raw.get(key) is not None:
                    details[key] = copy.deepcopy(raw[key])
            if raw.get('counterPrice') is not None:
                details['counterPrice'] = raw['counterPrice']
            specs = []
            for sku in raw.get('skuList') or []:
                for spec in sku.get('itemSkuSpecValueList') or []:
                    label = (spec.get('skuSpec') or {}).get('name')
                    value = (spec.get('skuSpecValue') or {}).get('value')
                    if label and value and f'{label}：{value}' not in specs:
                        specs.append(f'{label}：{value}')
            if specs:
                details['specifications'] = specs
            sold = raw.get('sellVolume')
            sales = sold if isinstance(sold, int) and not isinstance(sold, bool) and sold >= 0 else 0
            result.append({
                'product_id': 'yanxuan-' + str(item_id), 'title': name,
                'price': float(price), 'price_cents': int(price * 100), 'sales': sales,
                'category': category, 'image_res': 'ic_shop', 'image_url': pic,
                'description': details['description'], 'source': '网易严选',
                'currency': 'CNY', 'details': details,
            })
        return result

    def _fetch_category_raw(self, category):
        keyword = CATEGORY_SEARCH_KEYWORDS[category][0]
        try:
            future = _FETCHERS.submit(_request_search, 'https://you.163.com/xhr/search/search.json',
                params={'keyword': keyword, 'size': 100, 'page': 1, 'sortType': 0, 'descSorted': 'true', 'categoryId': 0, 'matchType': 0},
                headers={'User-Agent': 'Mozilla/5.0', 'Referer': 'https://you.163.com/search?keyword=' + urllib.parse.quote(keyword)},
                timeout=(2, 4))
            response = future.result(timeout=12)
            response.raise_for_status()
            payload = response.json()
            if payload.get('code') is not None and str(payload['code']) != '200':
                raise ValueError('upstream search failed')
            return self.parse_api_response(payload, category)
        except Exception as error:
            raise GoodsUnavailable(f'网易严选“{keyword}”搜索暂时无法连接，请重试') from error

    def list(self, category='推荐', limit=30, refresh=False):
        category = ALIASES.get(category, category)
        if category not in CATEGORY_SEARCH_KEYWORDS:
            raise ValueError('不支持的商品分类')
        with self._guard:
            lock = self._locks.setdefault(category, threading.Lock())
        with lock:
            failure = self._failures.get(category)
            if failure and time.monotonic() - failure[0] < 1:
                raise GoodsUnavailable(failure[1])
            if category not in self._cache or time.monotonic() >= self._cache_expires.get(category, 0):
                try:
                    items = self._fetch_category_raw(category)
                except GoodsUnavailable as error:
                    self._failures[category] = (time.monotonic(), str(error))
                    raise
                self._failures.pop(category, None)
                with self._guard:
                    self._cache[category] = items
                self._cache_expires[category] = time.monotonic() + 300
            items = self._cache[category]
            if not items or limit is None or limit <= 0 or len(items) <= limit:
                return copy.deepcopy(items)
            offset = self._batch_offset.get(category, 0)
            if refresh:
                offset = (offset + limit) % len(items)
                self._batch_offset[category] = offset
            batch = items[offset:offset + limit]
            if len(batch) < limit:
                batch += items[:limit - len(batch)]
            return copy.deepcopy(batch)

    def find(self, product_id):
        with self._guard:
            for items in self._cache.values():
                for item in items:
                    if item['product_id'] == product_id:
                        return copy.deepcopy(item)
        return None
