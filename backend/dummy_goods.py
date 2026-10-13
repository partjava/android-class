"""Remote demonstration products from DummyJSON, without invented local fallback."""
import threading
import copy
import time
from decimal import Decimal, ROUND_HALF_UP
import requests


class GoodsUnavailable(RuntimeError):
    pass


class DummyGoodsCatalog:
    CATEGORIES = {
        'beauty': '美妆', 'fragrances': '美妆', 'skin-care': '美妆',
        'furniture': '家居', 'home-decoration': '家居', 'kitchen-accessories': '家居',
        'groceries': '食品', 'laptops': '数码', 'smartphones': '数码', 'tablets': '数码',
        'mobile-accessories': '数码', 'mens-watches': '数码', 'womens-watches': '数码',
        'mens-shirts': '服饰', 'mens-shoes': '服饰', 'womens-dresses': '服饰',
        'womens-shoes': '服饰', 'womens-bags': '服饰', 'womens-jewellery': '服饰',
        'sunglasses': '服饰', 'tops': '服饰', 'sports-accessories': '服饰',
    }

    def __init__(self):
        self._items = []
        self._expires = 0
        self._lock = threading.Lock()

    def list(self):
        with self._lock:
            if self._items and time.monotonic() < self._expires:
                return copy.deepcopy(self._items)
            try:
                response = requests.get('https://dummyjson.com/products?limit=0', timeout=(2, 4))
                response.raise_for_status()
                items = []
                for product in response.json()['products']:
                    price = Decimal(str(product['price'])) * Decimal('7.2')
                    if not price.is_finite() or price < 0:
                        raise ValueError('invalid price')
                    price = price.quantize(Decimal('.01'), rounding=ROUND_HALF_UP)
                    items.append({
                        'product_id': 'dummy-' + str(product['id']), 'title': product['title'],
                        'price': float(price), 'price_cents': int(price * 100), 'sales': 0,
                        'category': self.CATEGORIES.get(product.get('category'), '其他'),
                        'image_res': 'ic_shop', 'image_url': product.get('thumbnail', ''),
                        'description': product.get('description', ''), 'source': 'DummyJSON',
                        'details': copy.deepcopy(product),
                    })
                if not items:
                    raise ValueError('empty upstream catalog')
            except Exception as error:
                raise GoodsUnavailable('DummyJSON 商品加载失败，请稍后重试') from error
            self._items = items
            self._expires = time.monotonic() + 300
            return copy.deepcopy(items)
