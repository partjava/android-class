import importlib
import unittest
from pathlib import Path


class BackendLayoutTests(unittest.TestCase):
    def test_fastapi_import_and_existing_data_paths(self):
        server = importlib.import_module('backend.campus_news_fastapi')
        from fastapi.testclient import TestClient
        self.assertEqual(200, TestClient(server.app).get('/api/health').status_code)
        self.assertIsNotNone(server.cns)
        self.assertEqual(Path(server.__file__).resolve().parents[1] / 'data' / 'shop_products.db',
                         Path(server.product_catalog.path).resolve())

    def test_all_python_sources_are_under_backend(self):
        root = Path(__file__).resolve().parents[2]
        self.assertFalse(list(root.glob('*.py')))
