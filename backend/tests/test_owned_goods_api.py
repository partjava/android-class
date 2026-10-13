import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


class GoodsApiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.directory = tempfile.TemporaryDirectory()
        cls.previous = os.environ.get('CAMPUS_GOODS_DB')
        os.environ['CAMPUS_GOODS_DB'] = str(Path(cls.directory.name) / 'api.db')
        from backend import campus_news_fastapi
        cls.backend = campus_news_fastapi

    @classmethod
    def tearDownClass(cls):
        if cls.previous is None:
            os.environ.pop('CAMPUS_GOODS_DB', None)
        else:
            os.environ['CAMPUS_GOODS_DB'] = cls.previous
        cls.directory.cleanup()

    def setUp(self):
        from backend.owned_catalog import ProductCatalog
        from fastapi.testclient import TestClient
        import uuid
        self.backend.product_catalog = ProductCatalog(Path(self.directory.name) / (uuid.uuid4().hex + '.db'))
        self.previous_source = self.backend.goods_source
        self.backend.goods_source = 'owned'
        self.client = TestClient(self.backend.app)

    def tearDown(self):
        self.backend.goods_source = self.previous_source

    def test_empty_and_crud_without_network(self):
        with patch.object(self.backend.requests, 'get', side_effect=AssertionError('No sample requests')):
            self.assertEqual([], self.client.get('/api/v1/goods').json()['data']['items'])
            body = dict(product_id='owned', title='我的商品', price=12.34, category='家居', sales=0)
            result = self.client.post('/api/v1/goods', json=body)
            self.assertEqual(200, result.status_code, result.text)
            self.assertEqual(1234, result.json()['data']['price_cents'])
            self.assertEqual(409, self.client.post('/api/v1/goods', json=body).status_code)
            self.assertEqual('我的商品', self.client.get('/api/v1/goods/owned').json()['data']['title'])
            self.assertEqual(1, self.client.get('/api/v1/goods?category=家居').json()['data']['total'])
            self.assertEqual(0, self.client.post('/api/v1/goods/filter', json={'min_price': 99}).json()['data']['count'])
            body.pop('product_id'); body['price'] = 10
            self.assertEqual(10, self.client.put('/api/v1/goods/owned', json=body).json()['data']['price'])
            self.assertEqual(200, self.client.delete('/api/v1/goods/owned').status_code)
            self.assertEqual(404, self.client.get('/api/v1/goods/owned').status_code)
            self.assertEqual([], self.client.get('/api/v1/goods').json()['data']['items'])

    def test_validation_and_missing_goods(self):
        for price in [-1, 0.001]:
            self.assertEqual(422, self.client.post('/api/v1/goods', json={'title': '无效', 'price': price}).status_code)
        self.assertEqual(404, self.client.get('/api/v1/goods/catalog-1').status_code)
        self.assertEqual(404, self.client.post('/api/v1/cart/add', json={'product_id': 'catalog-1', 'count': 1}).status_code)


if __name__ == '__main__':
    unittest.main()
