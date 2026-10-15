import unittest
from unittest.mock import patch, Mock
from backend.yanxuan_goods import YanxuanGoodsCatalog


class PaginationTests(unittest.TestCase):
    def test_sku_options_keep_real_combinations(self):
        raw = {'id': 1, 'name': '商品', 'retailPrice': 10, 'skuList': [
            {'itemSkuSpecValueList': [{'skuSpec': {'name': '颜色'}, 'skuSpecValue': {'value': '白色'}},
                                     {'skuSpec': {'name': '尺寸'}, 'skuSpecValue': {'value': '大号'}}]},
            {'retailPrice': 12, 'itemSkuSpecValueList': [{'skuSpec': {'name': '颜色'}, 'skuSpecValue': {'value': '黑色'}},
                                     {'skuSpec': {'name': '尺寸'}, 'skuSpecValue': {'value': '小号'}}]}]}
        details = YanxuanGoodsCatalog().parse_api_response({'result': [raw]}, '推荐')[0]['details']
        self.assertEqual(['颜色：白色 · 尺寸：大号', '颜色：黑色 · 尺寸：小号'], details['skuOptions'])
        self.assertEqual(12, details['skuOptionPrices']['颜色：黑色 · 尺寸：小号'])
    def test_filtered_duplicate_does_not_end_pagination(self):
        response = Mock()
        item = {'id': 1, 'name': '商品', 'retailPrice': 1}
        response.json.return_value = {'code': 200, 'data': {'result': [item, item]}}
        with patch('backend.yanxuan_goods.requests.get', return_value=response):
            page = YanxuanGoodsCatalog().list_page('推荐', limit=2)
            self.assertEqual(1, len(page['items']))
            self.assertTrue(page['has_more'])

    def test_pages_are_independent_and_do_not_wrap(self):
        catalog = YanxuanGoodsCatalog()
        def response(url, **kwargs):
            params = kwargs['params']
            page = params['page']
            result = Mock()
            result.json.return_value = {'code': 200, 'data': {'result': [
                {'id': page, 'name': '商品', 'retailPrice': 1}
            ] if page < 3 else []}}
            return result
        with patch('backend.yanxuan_goods.requests.get', side_effect=response) as get:
            first = catalog.list_page('推荐', page=1, limit=1)
            second = catalog.list_page('推荐', page=2, limit=1)
            self.assertNotEqual(first['items'][0]['product_id'], second['items'][0]['product_id'])
            self.assertFalse(catalog.list_page('推荐', page=3, limit=1)['has_more'])
            self.assertEqual(first, catalog.list_page('推荐', page=1, limit=1))
            catalog.list_page('推荐', page=1, limit=1, refresh=True)
            self.assertEqual(4, get.call_count)
