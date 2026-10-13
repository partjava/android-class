import os
import tempfile
import unittest
import threading
from pathlib import Path
from cart_persistence import CartStorage


class CartPersistenceTests(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.file_path = str(Path(self.temp_dir.name) / 'cart_test.json')
        self.storage = CartStorage(self.file_path)

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_add_and_survives_reload(self):
        item_snapshot = {
            "title": "测试商品",
            "price": 99.0,
            "category": "电器",
            "image_url": "https://example.com/item.jpg",
            "image_res": "ic_shop"
        }
        res = self.storage.add_item("item-1", count=2, snapshot=item_snapshot)
        self.assertEqual(2, res['total_count'])
        self.assertEqual(198.0, res['total_price'])

        cart = self.storage.get_cart()
        self.assertEqual(1, len(cart['items']))
        self.assertEqual("item-1", cart['items'][0]['product_id'])
        self.assertEqual(2, cart['items'][0]['count'])
        self.assertEqual(99.0, cart['items'][0]['price'])
        self.assertEqual(198.0, cart['items'][0]['subtotal'])
        self.assertEqual("测试商品", cart['items'][0]['title'])
        self.assertEqual("电器", cart['items'][0]['category'])
        self.assertEqual("https://example.com/item.jpg", cart['items'][0]['image_url'])
        self.assertEqual("ic_shop", cart['items'][0]['image_res'])
        self.assertEqual(2, cart['total_count'])
        self.assertEqual(198.0, cart['total_price'])

        # Reload from disk
        reloaded = CartStorage(self.file_path)
        cart2 = reloaded.get_cart()
        self.assertEqual(1, len(cart2['items']))
        self.assertEqual(2, cart2['items'][0]['count'])
        self.assertEqual("测试商品", cart2['items'][0]['title'])
        self.assertEqual(198.0, cart2['total_price'])

    def test_multiple_add_accumulates_count(self):
        item_snapshot = {"title": "书籍", "price": 25.0}
        self.storage.add_item("book-1", count=1, snapshot=item_snapshot)
        # Add again without snapshot; should retain existing snapshot and increment count
        res = self.storage.add_item("book-1", count=3)
        self.assertEqual(4, res['total_count'])
        self.assertEqual(100.0, res['total_price'])
        self.assertEqual("书籍", res['items'][0]['title'])

    def test_update_and_clear(self):
        self.storage.add_item("item-2", count=3, snapshot={"title": "商品2", "price": 10.0})
        self.storage.update_item("item-2", count=1)
        cart = self.storage.get_cart()
        self.assertEqual(1, cart['items'][0]['count'])
        self.assertEqual(10.0, cart['total_price'])

        # update count <= 0 should remove the item
        self.storage.update_item("item-2", count=0)
        self.assertEqual(0, len(self.storage.get_cart()['items']))
        self.assertEqual(0, self.storage.get_cart()['total_count'])
        self.assertEqual(0.0, self.storage.get_cart()['total_price'])

        # add items and test clear()
        self.storage.add_item("item-3", count=2, snapshot={"title": "商品3", "price": 15.5})
        self.storage.add_item("item-4", count=1, snapshot={"title": "商品4", "price": 20.0})
        self.assertEqual(3, self.storage.get_cart()['total_count'])
        self.assertEqual(51.0, self.storage.get_cart()['total_price'])

        res = self.storage.clear()
        self.assertEqual(0, len(res['items']))
        self.assertEqual(0, res['total_count'])
        self.assertEqual(0.0, res['total_price'])

        # Verify cleared state persists across reload
        reloaded = CartStorage(self.file_path)
        self.assertEqual(0, len(reloaded.get_cart()['items']))

    def test_corrupted_file_recovery(self):
        with open(self.file_path, 'w', encoding='utf-8') as f:
            f.write("invalid json content")
        corrupted_storage = CartStorage(self.file_path)
        self.assertEqual(0, len(corrupted_storage.get_cart()['items']))

    def test_concurrent_access(self):
        threads = []
        def worker(tid):
            for i in range(10):
                self.storage.add_item(f"p-{tid}-{i}", count=1, snapshot={"title": f"Item {i}", "price": 5.0})

        for t in range(5):
            th = threading.Thread(target=worker, args=(t,))
            threads.append(th)
            th.start()

        for th in threads:
            th.join()

        cart = self.storage.get_cart()
        self.assertEqual(50, cart['total_count'])
        self.assertEqual(250.0, cart['total_price'])


if __name__ == '__main__':
    unittest.main()
