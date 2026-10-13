"""Persistent, explicitly entered products. Never seeds or fetches sample data."""
import json
import sqlite3
import uuid
from contextlib import closing
from decimal import Decimal, InvalidOperation
from pathlib import Path


class ProductCatalog:
    def __init__(self, path):
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with closing(sqlite3.connect(self.path)) as db, db:
            db.execute('CREATE TABLE IF NOT EXISTS products (id TEXT PRIMARY KEY, json TEXT NOT NULL)')

    @staticmethod
    def _record(value, product_id):
        item = dict(value)
        try:
            price = Decimal(str(item['price']))
            if not price.is_finite() or price < 0 or price * 100 != (price * 100).to_integral_value():
                raise ValueError('价格必须是非负金额，最多两位小数')
        except (InvalidOperation, KeyError) as error:
            raise ValueError('价格无效') from error
        item['title'] = str(item.get('title', '')).strip()
        if not item['title']:
            raise ValueError('商品名称不能为空')
        item.update(product_id=product_id, price=float(price), price_cents=int(price * 100))
        item.setdefault('sales', 0)
        item.setdefault('category', '其他')
        item.setdefault('image_res', 'ic_shop')
        item.setdefault('image_url', '')
        return item

    def list(self):
        with closing(sqlite3.connect(self.path)) as db:
            return [json.loads(row[0]) for row in db.execute('SELECT json FROM products ORDER BY rowid DESC')]

    def get(self, product_id):
        with closing(sqlite3.connect(self.path)) as db:
            row = db.execute('SELECT json FROM products WHERE id=?', (product_id,)).fetchone()
            return json.loads(row[0]) if row else None

    def create(self, value):
        product_id = value.get('product_id') or uuid.uuid4().hex
        item = self._record(value, product_id)
        try:
            with closing(sqlite3.connect(self.path)) as db, db:
                db.execute('INSERT INTO products VALUES (?,?)', (product_id, json.dumps(item, ensure_ascii=False)))
        except sqlite3.IntegrityError as error:
            raise ValueError('商品ID已存在') from error
        return item

    def update(self, product_id, value):
        item = self._record(value, product_id)
        with closing(sqlite3.connect(self.path)) as db, db:
            result = db.execute('UPDATE products SET json=? WHERE id=?', (json.dumps(item, ensure_ascii=False), product_id))
            return item if result.rowcount else None

    def delete(self, product_id):
        with closing(sqlite3.connect(self.path)) as db, db:
            return bool(db.execute('DELETE FROM products WHERE id=?', (product_id,)).rowcount)
