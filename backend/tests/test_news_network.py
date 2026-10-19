import unittest
from backend import campus_news_server as server


class NewsNetworkTests(unittest.TestCase):
    def test_news_session_uses_system_route(self):
        for scheme in ('https://', 'http://'):
            adapter = server.session.get_adapter(scheme)
            self.assertFalse(adapter.poolmanager.connection_pool_kw.get('source_address'),
                             'Hostname address order can select a VMware adapter without internet')

    def test_upstream_failure_is_not_reported_as_empty_success(self):
        from unittest.mock import patch
        from fastapi.testclient import TestClient
        from backend.campus_news_fastapi import app
        with patch.object(server, 'crawl_qcu_page', side_effect=RuntimeError('offline')):
            response = TestClient(app).get('/xiaoyuan/xxyw/page/1')
        self.assertEqual(502, response.status_code)
