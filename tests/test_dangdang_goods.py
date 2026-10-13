import unittest
from unittest.mock import patch
from dangdang_goods import DangdangGoodsCatalog

SAMPLE_DANGDANG_HTML = '''
<ul class="bigimg">
    <li id="11174259146" class="line1">
        <a class="pic" href="//product.dangdang.com/11174259146.html" title="机械键盘108键真机械合金版">
            <img data-original="//img3m6.ddimg.cn/50/35/11174259146-1_b.jpg" src="default.jpg"/>
        </a>
        <p class="name"><a title="机械键盘108键真机械合金版">机械键盘108键真机械合金版</a></p>
        <p class="detail">青轴茶轴黑轴电竞游戏</p>
        <p class="price"><span class="price_n">¥240.00</span></p>
        <p class="search_star_line">
            <span class="search_star_black"><span style="width: 90%;"></span></span>
            <a class="search_comment_num">29692条评论</a>
        </p>
    </li>
</ul>
'''


class DangdangGoodsCatalogTests(unittest.TestCase):
    def test_parse_dangdang_html_fields(self):
        catalog = DangdangGoodsCatalog()
        items = catalog.parse_html(SAMPLE_DANGDANG_HTML, '电器')
        self.assertEqual(1, len(items))
        item = items[0]
        self.assertEqual('dangdang-11174259146', item['product_id'])
        self.assertEqual('机械键盘108键真机械合金版', item['title'])
        self.assertEqual(240.0, item['price'])
        self.assertEqual(24000, item['price_cents'])
        self.assertEqual('https://img3m6.ddimg.cn/50/35/11174259146-1_b.jpg', item['image_url'])
        self.assertEqual('电器', item['category'])
        self.assertEqual('当当网', item['source'])
        reviews = item['details']['reviews']
        self.assertGreaterEqual(len(reviews), 5)
        for r in reviews:
            self.assertIn('reviewerName', r)
            self.assertIn('rating', r)
            self.assertIn('comment', r)
            self.assertIn('date', r)
        self.assertEqual(reviews[0]['rating'], 5)

    def test_fallback_to_dummy_on_failure(self):
        catalog = DangdangGoodsCatalog()
        with patch('requests.get', side_effect=Exception('Dangdang timeout')):
            items = catalog.list('电器')
            self.assertGreater(len(items), 0)
            self.assertTrue(items[0]['product_id'].startswith('dummy-') or items[0]['source'] == 'DummyJSON')

    def test_fallback_delegates_to_dummy_goods_catalog(self):
        catalog = DangdangGoodsCatalog()
        mock_dummy_items = [{
            'product_id': 'dummy-999',
            'title': '测试兜底商品',
            'price': 88.0,
            'price_cents': 8800,
            'sales': 12,
            'category': '数码',
            'image_res': 'ic_shop',
            'image_url': 'https://example.com/item.jpg',
            'description': 'Dummy item',
            'source': 'DummyJSON',
            'details': {'reviews': [{'reviewerName': '测试', 'rating': 5, 'comment': '好', 'date': '2026-10-01'}]}
        }]
        with patch.object(catalog._fallback, 'list', return_value=mock_dummy_items) as mock_fallback_list:
            with patch('requests.get', side_effect=Exception('Dangdang timeout')):
                items = catalog.list('电器')
                self.assertTrue(mock_fallback_list.called)
                self.assertEqual(1, len(items))
                self.assertEqual('dummy-999', items[0]['product_id'])
                self.assertEqual('DummyJSON', items[0]['source'])


if __name__ == '__main__':
    unittest.main()
