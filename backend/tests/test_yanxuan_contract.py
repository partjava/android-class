import unittest
from unittest.mock import patch
from backend.yanxuan_goods import YanxuanGoodsCatalog
from backend.dummy_goods import GoodsUnavailable


class YanxuanContractTests(unittest.TestCase):
    def test_transient_connection_reset_is_retried_once(self):
        import requests
        from unittest.mock import Mock
        response = Mock()
        response.json.return_value = {'data': {'directly': {'searcherResult': {'result': []}}}}
        with patch('backend.yanxuan_goods.requests.get', side_effect=[requests.ConnectionError('reset'), response]) as get:
            self.assertEqual([], YanxuanGoodsCatalog().list('家具'))
            self.assertEqual(2, get.call_count)

    def test_queued_failed_requests_share_the_failure(self):
        import time
        from concurrent.futures import ThreadPoolExecutor
        catalog = YanxuanGoodsCatalog()
        def fail(*args, **kwargs):
            time.sleep(.05)
            raise OSError('offline')
        def load():
            with self.assertRaises(GoodsUnavailable):
                catalog.list('推荐')
        with patch('backend.yanxuan_goods.requests.get', side_effect=fail) as get:
            with ThreadPoolExecutor(max_workers=2) as pool:
                list(pool.map(lambda _: load(), range(2)))
            self.assertEqual(1, get.call_count)

    def test_each_tab_uses_the_users_exact_keyword(self):
        mapping = {'推荐': '生活', '数码': '数码', '家具': '家具', '穿搭': '服饰', '水果': '水果', '个护': '护肤品'}
        for category, keyword in mapping.items():
            with patch('backend.yanxuan_goods.requests.get') as get:
                get.return_value.json.return_value = {'data': {'directly': {'searcherResult': {'result': []}}}}
                self.assertEqual([], YanxuanGoodsCatalog().list(category))
                self.assertEqual(keyword, get.call_args.kwargs['params']['keyword'])
                self.assertEqual(1, get.call_count)

    def test_missing_fields_are_not_invented(self):
        response = {'data': {'directly': {'searcherResult': {'result': [{'id': 1, 'name': '官网商品', 'retailPrice': 19.9, 'simpleDesc': '官网描述'}]}}}}
        item = YanxuanGoodsCatalog().parse_api_response(response, '水果')[0]
        self.assertEqual(0, item['sales'])
        self.assertNotIn('rating', item['details'])
        self.assertNotIn('stock', item['details'])
        self.assertEqual([], item['details'].get('reviews', []))
        self.assertEqual('CNY', item['details']['currency'])
        self.assertEqual('网易严选', item['details']['source'])
        self.assertEqual('官网描述', item['details']['description'])

    def test_failure_does_not_fall_back_to_other_sources(self):
        with patch('backend.yanxuan_goods.requests.get', side_effect=OSError('offline')) as get:
            with self.assertRaises(GoodsUnavailable):
                YanxuanGoodsCatalog().list('推荐')
            self.assertEqual(1, get.call_count)

    def test_recommendation_does_not_filter_out_other_categories(self):
        from backend import campus_news_fastapi as backend
        from fastapi.testclient import TestClient
        with patch.object(backend, 'goods_source', 'yanxuan'), patch.object(backend.yanxuan_catalog, 'list_page', return_value={'items': [{'category': '数码', 'product_id': 'test', 'title': '推荐商品', 'price': 1}], 'has_more': False}):
            self.assertEqual(1, TestClient(backend.app).get('/api/v1/goods?category=推荐').json()['data']['total'])
