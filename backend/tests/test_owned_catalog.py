import tempfile
import unittest
from pathlib import Path
from backend.owned_catalog import ProductCatalog


class CatalogTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.path = Path(self.directory.name) / 'products.db'
        self.catalog = ProductCatalog(self.path)

    def tearDown(self):
        self.directory.cleanup()

    def test_empty_without_seeding(self):
        self.assertEqual([], self.catalog.list())

    def test_crud_survives_reopen(self):
        item = self.catalog.create({'product_id': 'my-book', 'title': '自有图书', 'price': '12.34', 'sales': 0, 'category': '其他'})
        self.assertEqual(12.34, item['price'])
        reopened = ProductCatalog(self.path)
        self.assertEqual('自有图书', reopened.get('my-book')['title'])
        item = reopened.update('my-book', {'title': '新标题', 'price': '9.99', 'sales': 2, 'category': '其他'})
        self.assertEqual(9.99, item['price'])
        with self.assertRaises(ValueError):
            reopened.create({'product_id': 'my-book', 'title': '重复', 'price': 1})
        self.assertTrue(reopened.delete('my-book'))
        self.assertFalse(reopened.delete('missing'))
        self.assertEqual([], ProductCatalog(self.path).list())

    def test_invalid_money_never_writes(self):
        for price in ['-1', 'NaN', 'Infinity', '1.001']:
            with self.assertRaises(ValueError):
                self.catalog.create({'title': '无效', 'price': price})
        self.assertEqual([], self.catalog.list())


if __name__ == '__main__':
    unittest.main()
