import json
import unittest
from unittest.mock import Mock, patch
from backend.yanxuan_goods import YanxuanGoodsCatalog


class DetailTests(unittest.TestCase):
    def test_detail_without_list_cache_and_refresh(self):
        raw = {'id': 4064594, 'name': '真实商品', 'retailPrice': 39,
               'primaryPicUrl': 'https://example.com/a.jpg',
               'itemDetail': {'picUrl1': 'https://example.com/b.jpg',
                              'detailHtml': '<p>介绍</p><img src="https://example.com/c.jpg">'}}
        html = 'var JSON_DATA_FROMFTL = {"item":' + json.dumps(raw) + ',"commentCount":12,"commentGoodRates":\'99%\'};'
        page = Mock(text=html)
        comments = Mock()
        comments.json.return_value = {'code': 200, 'data': {'commentList': [
            {'frontUserName': '用户', 'content': '好用', 'star': 5, 'createTime': 1700000000000}],
            'pagination': {'totalPage': 1}}}
        catalog = YanxuanGoodsCatalog()
        with patch('backend.yanxuan_goods.requests.get', side_effect=[page, comments, page, comments]) as get:
            item = catalog.detail('yanxuan-4064594')
            self.assertEqual('yanxuan-4064594', item['product_id'])
            self.assertIn('https://example.com/b.jpg', item['details']['images'])
            self.assertEqual('好用', item['details']['reviews'][0]['comment'])
            self.assertEqual('2023-11-15', item['details']['reviews'][0]['date'])
            self.assertEqual(12, item['details']['commentCount'])
            catalog.detail('yanxuan-4064594')
            self.assertEqual(2, get.call_count)
            catalog.detail('yanxuan-4064594', refresh=True)
            self.assertEqual(4, get.call_count)

    def test_invalid_id_never_requests_upstream(self):
        with patch('backend.yanxuan_goods.requests.get') as get:
            self.assertIsNone(YanxuanGoodsCatalog().detail('yanxuan-../bad'))
            get.assert_not_called()

    def test_comment_failure_preserves_real_product(self):
        raw = {'id': 10, 'name': '商品', 'retailPrice': 1}
        page = Mock(text='var JSON_DATA_FROMFTL = {"item":' + json.dumps(raw) + '};')
        with patch('backend.yanxuan_goods.requests.get', side_effect=[page, OSError('offline')]):
            details = YanxuanGoodsCatalog().detail('yanxuan-10')['details']
            self.assertEqual('unavailable', details['reviewsStatus'])
            self.assertNotIn('reviews', details)

    def test_detail_api_maps_unavailable_to_retryable_status(self):
        from backend import campus_news_fastapi as backend
        from fastapi.testclient import TestClient
        from backend.dummy_goods import GoodsUnavailable
        with patch.object(backend, 'goods_source', 'yanxuan'), patch.object(backend.yanxuan_catalog, 'detail', side_effect=GoodsUnavailable('请重试')):
            self.assertEqual(503, TestClient(backend.app).get('/api/v1/goods/yanxuan-10').status_code)

    def test_list_is_one_image_summary_without_fetching_details(self):
        from backend import campus_news_fastapi as backend
        from fastapi.testclient import TestClient
        item = {'product_id': 'yanxuan-10', 'title': '商品', 'price': 1,
                'image_url': 'https://example.com/a.jpg', 'details': {'reviews': [{'comment': '评价'}]}}
        with patch.object(backend, 'goods_source', 'yanxuan'), patch.object(backend.yanxuan_catalog, 'list_page', return_value={'items': [item], 'has_more': False}), patch.object(backend.yanxuan_catalog, 'detail') as detail:
            row = TestClient(backend.app).get('/api/v1/goods').json()['data']['items'][0]
            self.assertTrue(row['details']['summaryOnly'])
            self.assertEqual(1, len(row['details']['images']))
            self.assertNotIn('reviews', row['details'])
            self.assertIn('reviews', item['details'])
            detail.assert_not_called()
