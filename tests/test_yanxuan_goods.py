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

    def test_fallback_on_network_error(self):
        catalog = YanxuanGoodsCatalog()
        with patch('requests.get', side_effect=Exception('offline')):
            items = catalog.list('数码')
            self.assertGreater(len(items), 0)


if __name__ == '__main__':
    unittest.main()
