import unittest
from unittest.mock import patch
from yanxuan_goods import YanxuanGoodsCatalog

SAMPLE_YANXUAN_RESPONSE = {
    "code": "200",
    "data": {
        "directly": {
            "searcherResult": {
                "result": [
                    {
                        "id": 10086,
                        "name": "【网易严选】PokiBuds真无线快充蓝牙耳机",
                        "retailPrice": 179.0,
                        "sellVolume": 2500,
                        "listPicUrl": "https://yanxuan-item.nosdn.127.net/test.jpg",
                        "simpleDesc": "超长续航，双麦降噪"
                    }
                ]
            }
        }
    }
}

SAMPLE_OFFICIAL_TREE_RESPONSE = {
    "code": "200",
    "data": {
        "currentCategory": {"id": 1043000, "name": "数码家电"},
        "categoryItemList": [
            {
                "category": {"name": "个人护理电器"},
                "itemList": [
                    {
                        "id": 4266534,
                        "name": "OIDIRE 低辐射低噪高速吹风机",
                        "retailPrice": 169.0,
                        "sellVolume": 1500,
                        "primaryPicUrl": "https://yanxuan-item.nosdn.127.net/blower.jpg",
                        "simpleDesc": "恒温负离子"
                    },
                    {
                        "id": 4264381,
                        "name": "倍轻松头皮养护头皮按摩梳",
                        "retailPrice": 799.0,
                        "sellVolume": 900,
                        "primaryPicUrl": "https://yanxuan-item.nosdn.127.net/scalp.jpg",
                        "simpleDesc": "红外光波抓揉"
                    }
                ]
            }
        ]
    }
}


class YanxuanGoodsCatalogTests(unittest.TestCase):
    def test_parse_response_fields(self):
        catalog = YanxuanGoodsCatalog()
        items = catalog.parse_api_response(SAMPLE_YANXUAN_RESPONSE, '数码')
        self.assertEqual(1, len(items))
        item = items[0]
        self.assertEqual('yanxuan-10086', item['product_id'])
        self.assertEqual('【网易严选】PokiBuds真无线快充蓝牙耳机', item['title'])
        self.assertEqual(179.0, item['price'])
        self.assertEqual(17900, item['price_cents'])
        self.assertEqual('https://yanxuan-item.nosdn.127.net/test.jpg', item['image_url'])
        self.assertEqual('数码', item['category'])
        self.assertEqual('网易严选', item['source'])
        self.assertGreaterEqual(len(item['details']['reviews']), 5)
        self.assertEqual(item['details']['reviews'][0]['rating'], 5)

    def test_parse_official_category_tree(self):
        catalog = YanxuanGoodsCatalog()
        items = catalog.parse_api_response(SAMPLE_OFFICIAL_TREE_RESPONSE, '数码')
        self.assertEqual(2, len(items))
        self.assertEqual('yanxuan-4266534', items[0]['product_id'])
        self.assertEqual('OIDIRE 低辐射低噪高速吹风机', items[0]['title'])
        self.assertEqual(169.0, items[0]['price'])
        self.assertEqual('yanxuan-4264381', items[1]['product_id'])

    def test_batch_rotation_30_items(self):
        catalog = YanxuanGoodsCatalog()
        # Seed 70 mock items
        fake_items = [{'product_id': f'fake-{i}', 'title': f'Item {i}', 'price': 10.0, 'category': '数码'} for i in range(70)]
        catalog._cache['数码'] = fake_items
        catalog._cache_expires['数码'] = 9999999999

        batch1 = catalog.list('数码', limit=30, refresh=False)
        self.assertEqual(30, len(batch1))
        self.assertEqual('fake-0', batch1[0]['product_id'])

        batch2 = catalog.list('数码', limit=30, refresh=True)
        self.assertEqual(30, len(batch2))
        self.assertEqual('fake-30', batch2[0]['product_id'])

        batch3 = catalog.list('数码', limit=30, refresh=True)
        self.assertEqual(30, len(batch3))
        self.assertEqual('fake-60', batch3[0]['product_id'])

    def test_fallback_on_network_error(self):
        catalog = YanxuanGoodsCatalog()
        with patch('requests.get', side_effect=Exception('offline')):
            items = catalog.list('数码')
            self.assertGreater(len(items), 0)


if __name__ == '__main__':
    unittest.main()
