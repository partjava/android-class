import unittest
from unittest.mock import patch
from backend.dummy_goods import DummyGoodsCatalog, GoodsUnavailable


class DummyGoodsTests(unittest.TestCase):
    def test_preserves_complete_details_and_images(self):
        catalog = DummyGoodsCatalog()
        product = {'id': 2, 'title': '全字段商品', 'price': 5, 'images': ['https://example.com/a.png', 'https://example.com/b.png'],
                   'brand': '测试品牌', 'rating': 4.2, 'stock': 20, 'sku': 'sku-two', 'weight': 3,
                   'dimensions': {'width': 10, 'height': 12, 'depth': 5}, 'reviews': [{'reviewerName': '读者', 'rating': 4, 'comment': '完整评价'}],
                   'shippingInformation': 'Ships in 1 week', 'returnPolicy': '30 days return policy'}
        with patch('backend.dummy_goods.requests.get') as get:
            get.return_value.json.return_value = {'products': [product]}
            result = catalog.list()[0]
            self.assertEqual(product, result['details'])
            result['details']['reviews'][0]['comment'] = 'changed'
            self.assertEqual('完整评价', catalog.list()[0]['details']['reviews'][0]['comment'])

    def test_loads_remote_products_and_reuses_cache(self):
        catalog = DummyGoodsCatalog()
        with patch('backend.dummy_goods.requests.get') as get:
            get.return_value.json.return_value = {'products': [{'id': 1, 'title': '展示商品', 'price': 9.99, 'category': 'beauty', 'thumbnail': 'https://example.com/product.jpg'}]}
            items = catalog.list()
            self.assertEqual('dummy-1', items[0]['product_id'])
            self.assertEqual('美妆', items[0]['category'])
            self.assertEqual('https://example.com/product.jpg', items[0]['image_url'])
            self.assertEqual(71.93, items[0]['price'])
            self.assertEqual(0, items[0]['sales'])
            self.assertEqual(items, catalog.list())
            self.assertEqual(1, get.call_count)

    def test_failure_does_not_generate_local_goods(self):
        with patch('backend.dummy_goods.requests.get', side_effect=OSError('offline')):
            with self.assertRaises(GoodsUnavailable):
                DummyGoodsCatalog().list()


if __name__ == '__main__':
    unittest.main()
