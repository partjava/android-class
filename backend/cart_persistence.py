"""Persistent cart storage with file JSON cache and item metadata snapshot."""
import os
import json
import threading


class CartStorage:
    def __init__(self, file_path: str):
        self.file_path = file_path
        self._lock = threading.Lock()
        self._items = {}  # pid -> {"count": int, "snapshot": dict}
        self._load()

    def _load(self):
        if not os.path.exists(self.file_path):
            return
        try:
            with open(self.file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                if isinstance(data, dict) and "items" in data:
                    self._items = data["items"]
        except Exception as e:
            print(f"[CartStorage Warning] Failed to load cart file: {e}")

    def _save_unlocked(self):
        dir_name = os.path.dirname(os.path.abspath(self.file_path))
        if dir_name:
            os.makedirs(dir_name, exist_ok=True)
        temp_file = self.file_path + '.tmp'
        try:
            with open(temp_file, 'w', encoding='utf-8') as f:
                json.dump({"items": self._items}, f, ensure_ascii=False, indent=2)
            os.replace(temp_file, self.file_path)
        except Exception as e:
            print(f"[CartStorage Warning] Failed to save cart file: {e}")

    def _save(self):
        with self._lock:
            self._save_unlocked()

    def _get_cart_unlocked(self):
        items = []
        total_count = 0
        total_price = 0.0
        for pid, data in self._items.items():
            count = data.get("count", 0)
            if count <= 0:
                continue
            snap = data.get("snapshot", {})
            try:
                price = float(snap.get("price", 0.0))
            except (ValueError, TypeError):
                price = 0.0
            subtotal = round(price * count, 2)
            total_count += count
            total_price += subtotal
            items.append({
                "product_id": pid,
                "title": snap.get("title", "未知商品"),
                "price": price,
                "count": count,
                "subtotal": subtotal,
                "category": snap.get("category", "其他"),
                "image_res": snap.get("image_res", "ic_shop"),
                "image_url": snap.get("image_url", "")
            })
        return {
            "items": items,
            "total_count": total_count,
            "total_price": round(total_price, 2)
        }

    def add_item(self, product_id: str, count: int = 1, snapshot: dict = None):
        with self._lock:
            cur = self._items.get(product_id, {"count": 0, "snapshot": {}})
            new_count = cur["count"] + max(1, count)
            snap = snapshot if snapshot is not None else cur.get("snapshot", {})
            self._items[product_id] = {"count": new_count, "snapshot": snap}
            self._save_unlocked()
            return self._get_cart_unlocked()

    def update_item(self, product_id: str, count: int, snapshot: dict = None):
        with self._lock:
            if count <= 0:
                self._items.pop(product_id, None)
            else:
                cur = self._items.get(product_id, {"count": 0, "snapshot": {}})
                snap = snapshot if snapshot is not None else cur.get("snapshot", {})
                self._items[product_id] = {"count": count, "snapshot": snap}
            self._save_unlocked()
            return self._get_cart_unlocked()

    def clear(self):
        with self._lock:
            self._items.clear()
            self._save_unlocked()
            return self._get_cart_unlocked()

    def get_cart(self):
        with self._lock:
            return self._get_cart_unlocked()
