import sqlite3
import json
from abc import ABC, abstractmethod
from typing import Optional
from .payments import UniversalTransactionResult, TransactionStatus

class BaseTransactionStore(ABC):
    @abstractmethod
    def save_intent(self, intent_id: str, idempotency_key: str, data: dict):
        pass

    @abstractmethod
    def get_result(self, idempotency_key: str) -> Optional[dict]:
        pass

    @abstractmethod
    def save_result(self, idempotency_key: str, result: UniversalTransactionResult):
        pass

class SQLiteTransactionStore(BaseTransactionStore):
    def __init__(self, db_path: str = "transactions.db"):
        self.db_path = db_path
        self._init_db()

    def _init_db(self):
        with sqlite3.connect(self.db_path) as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    idempotency_key TEXT PRIMARY KEY,
                    intent_id TEXT,
                    intent_data TEXT,
                    status TEXT,
                    result_data TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)

    def save_intent(self, intent_id: str, idempotency_key: str, data: dict):
        with sqlite3.connect(self.db_path) as conn:
            conn.execute(
                "INSERT OR IGNORE INTO transactions (idempotency_key, intent_id, intent_data, status) VALUES (?, ?, ?, ?)",
                (idempotency_key, intent_id, json.dumps(data), "PENDING")
            )

    def get_result(self, idempotency_key: str) -> Optional[dict]:
        with sqlite3.connect(self.db_path) as conn:
            row = conn.execute(
                "SELECT result_data FROM transactions WHERE idempotency_key = ? AND status != 'PENDING'",
                (idempotency_key,)
            ).fetchone()
            return json.loads(row[0]) if row and row[0] else None

    def save_result(self, idempotency_key: str, result: UniversalTransactionResult):
        result_dict = {
            "provider": result.provider,
            "provider_transaction_id": result.provider_transaction_id,
            "status": result.status.value,
            "error_code": result.error_code,
            "error_message": result.error_message
        }
        with sqlite3.connect(self.db_path) as conn:
            conn.execute(
                "UPDATE transactions SET status = ?, result_data = ? WHERE idempotency_key = ?",
                (result.status.value, json.dumps(result_dict), idempotency_key)
            )
