# Python 后端

FastAPI 和 Flask 服务、商品数据源、持久化模块及 Python 测试集中在此目录。

在项目根目录运行（Python 3.10+）：

```powershell
python -m pip install -r backend/requirements.txt
python -m backend.campus_news_fastapi
```

FastAPI 使用 8000 端口，交互式接口文档为 http://localhost:8000/docs 。也可以双击根目录的 `启动校园新闻服务_FastAPI.bat`。

Flask 新闻服务使用 5000 端口，入口为 `python -m backend.campus_news_server`，根目录原有双击启动入口仍可使用。

Python 测试：

```powershell
python -m pip install -r backend/requirements-dev.txt
python -m unittest discover -s backend/tests
```

目录职责：

| 文件 | 职责 |
| --- | --- |
| campus_news_fastapi.py | 频道、新闻、商品、购物车 API |
| campus_news_server.py | 原有 Flask 服务和晴川官网新闻解析 |
| yanxuan_goods.py | 默认网易严选列表、详情、多图及真实评价 |
| dummy_goods.py / dangdang_goods.py | 可选历史数据源 |
| owned_catalog.py | 可选自有商品 SQLite 管理 |
| cart_persistence.py | 购物车 JSON 持久化 |
| tests/ | 后端回归测试 |

运行数据仍保存在根目录 `data/shop_products.db` 和 `data/cart_storage.json`，目录调整不会新建另一套数据。`CAMPUS_GOODS_DB`、`CAMPUS_CART_PATH` 可覆盖路径。

完整接口说明和当前展示行为见 [FastAPI 设计文档](../fastapi设计文档.txt)。
