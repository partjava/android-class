import os
import tempfile
import unittest
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient

import campus_news_fastapi
from cart_persistence import CartStorage

SAMPLE_BOOK_HTML = '''
<ul class="bigimg">
    <li id="29123456" class="line1">
        <a class="pic" href="//product.dangdang.com/29123456.html" title="Python编程从入门到实践">
            <img data-original="//img3m6.ddimg.cn/50/35/29123456-1_b.jpg" src="default.jpg"/>
        </a>
        <p class="name"><a title="Python编程从入门到实践">Python编程从入门到实践</a></p>
        <p class="detail">畅销计算机编程经典教程</p>
        <p class="price"><span class="price_n">¥89.00</span></p>
        <p class="search_star_line">
            <span class="search_star_black"><span style="width: 95%;"></span></span>
            <a class="search_comment_num">50000条评论</a>
        </p>
    </li>
</ul>
'''


class FastApiDangdangIntegrationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp_dir = tempfile.TemporaryDirectory()
        cls.cart_path = os.path.join(cls.temp_dir.name, "cart_test.json")
        cls.previous_cart = campus_news_fastapi.cart_storage
        cls.previous_source = campus_news_fastapi.goods_source
        campus_news_fastapi.cart_storage = CartStorage(cls.cart_path)
        campus_news_fastapi.goods_source = "dangdang"
        cls.client = TestClient(campus_news_fastapi.app)

    @classmethod
    def tearDownClass(cls):
        campus_news_fastapi.cart_storage = cls.previous_cart
        campus_news_fastapi.goods_source = cls.previous_source
        cls.temp_dir.cleanup()

    def setUp(self):
        campus_news_fastapi.cart_storage.clear()
        campus_news_fastapi.dangdang_catalog._cache.clear()

    @patch('dangdang_goods.requests.get')
    def test_dangdang_goods_query_and_cart_lifecycle(self, mock_get):
        mock_resp = MagicMock()
        mock_resp.status_code = 200
        mock_resp.text = SAMPLE_BOOK_HTML
        mock_get.return_value = mock_resp

        # 1. Test GET /api/v1/goods?category=图书 returns status 200, code 200, data contains total and items
        res = self.client.get("/api/v1/goods?category=图书")
        self.assertEqual(200, res.status_code)
        body = res.json()
        self.assertEqual(200, body.get("code"))
        self.assertIn("data", body)
        data = body["data"]
        self.assertIn("total", data)
        self.assertIn("items", data)
        self.assertGreaterEqual(data["total"], 1)
        self.assertGreaterEqual(len(data["items"]), 1)

        product = data["items"][0]
        product_id = product["product_id"]
        self.assertEqual("dangdang-29123456", product_id)
        self.assertEqual("Python编程从入门到实践", product["title"])
        self.assertEqual(89.0, product["price"])
        self.assertEqual("图书", product["category"])

        # 2. Test POST /api/v1/cart/clear clears cart
        clear_res = self.client.post("/api/v1/cart/clear")
        self.assertEqual(200, clear_res.status_code)
        clear_body = clear_res.json()
        self.assertEqual(200, clear_body.get("code"))
        self.assertEqual([], clear_body["data"]["items"])
        self.assertEqual(0, clear_body["data"]["total_count"])
        self.assertEqual(0.0, clear_body["data"]["total_price"])

        # 3. Test POST /api/v1/cart/add with product_id, verify count and data in cart
        add_res = self.client.post("/api/v1/cart/add", json={"product_id": product_id, "count": 2})
        self.assertEqual(200, add_res.status_code)
        add_body = add_res.json()
        self.assertEqual(200, add_body.get("code"))
        self.assertEqual(product_id, add_body["data"]["product_id"])
        self.assertEqual(2, add_body["data"]["count"])
        cart_data = add_body["data"]["cart"]
        self.assertEqual(2, cart_data["total_count"])
        self.assertEqual(178.0, cart_data["total_price"])
        self.assertEqual(1, len(cart_data["items"]))

        # 4. Test GET /api/v1/cart returns the added item with its snapshot (title, price, count)
        cart_res = self.client.get("/api/v1/cart")
        self.assertEqual(200, cart_res.status_code)
        cart_body = cart_res.json()
        self.assertEqual(200, cart_body.get("code"))
        cart_items = cart_body["data"]["items"]
        self.assertEqual(1, len(cart_items))
        item = cart_items[0]
        self.assertEqual(product_id, item["product_id"])
        self.assertEqual("Python编程从入门到实践", item["title"])
        self.assertEqual(89.0, item["price"])
        self.assertEqual(2, item["count"])
        self.assertEqual(178.0, item["subtotal"])

        # 5. Test POST /api/v1/cart/update with count=0 removes the item
        update_res = self.client.post("/api/v1/cart/update", json={"product_id": product_id, "count": 0})
        self.assertEqual(200, update_res.status_code)
        update_body = update_res.json()
        self.assertEqual(200, update_body.get("code"))
        self.assertEqual(0, update_body["data"]["count"])
        self.assertEqual(0, update_body["data"]["cart"]["total_count"])
        self.assertEqual(0.0, update_body["data"]["cart"]["total_price"])
        self.assertEqual([], update_body["data"]["cart"]["items"])

        # Verify GET /api/v1/cart is empty
        final_res = self.client.get("/api/v1/cart")
        self.assertEqual(200, final_res.status_code)
        self.assertEqual([], final_res.json()["data"]["items"])
        self.assertEqual(0, final_res.json()["data"]["total_count"])

    @patch('dangdang_goods.requests.get')
    def test_cart_snapshot_persists_across_storage_reload(self, mock_get):
        mock_resp = MagicMock()
        mock_resp.status_code = 200
        mock_resp.text = SAMPLE_BOOK_HTML
        mock_get.return_value = mock_resp

        # Fetch goods to populate cache
        self.client.get("/api/v1/goods?category=图书")
        # Add to cart
        self.client.post("/api/v1/cart/add", json={"product_id": "dangdang-29123456", "count": 1})

        # Reload storage from disk
        reloaded = CartStorage(self.cart_path)
        cart = reloaded.get_cart()
        self.assertEqual(1, cart["total_count"])
        self.assertEqual("dangdang-29123456", cart["items"][0]["product_id"])
        self.assertEqual("Python编程从入门到实践", cart["items"][0]["title"])
        self.assertEqual(89.0, cart["items"][0]["price"])


if __name__ == '__main__':
    unittest.main()
