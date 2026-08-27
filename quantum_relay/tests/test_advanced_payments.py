import pytest
import os
import json
import uuid
from ..common.payment_router import ChameleonPaymentRouter
from ..common.payments import UniversalPaymentIntent, Currency, TransactionStatus
from ..common.persistence import SQLiteTransactionStore

def test_dynamic_plugin_loading():
    """Verify that adapters are loaded from the plugins directory."""
    plugins_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "plugins")
    router = ChameleonPaymentRouter(plugins_path=plugins_dir)

    assert "STRIPE" in router._adapters
    assert "FUTURE_CBDC_RAIL" in router._adapters
    assert "ISO20022_SWIFT" in router._adapters

def test_idempotency_and_persistence(tmp_path):
    """Verify that the same intent processed twice returns the cached result."""
    db_path = str(tmp_path / "test_tx.db")
    store = SQLiteTransactionStore(db_path=db_path)

    plugins_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "plugins")
    router = ChameleonPaymentRouter(plugins_path=plugins_dir, store=store)

    intent = UniversalPaymentIntent(
        intent_id="ord_test_1",
        amount=1000,
        currency=Currency.USD,
        description="Idempotency Test",
        customer_email="test@user.com",
        payment_token="tok_123",
        idempotency_key="unique_key_999"
    )

    # First process
    result1 = router.process(intent)
    assert result1.status == TransactionStatus.SUCCESS

    # Second process with same idempotency key
    result2 = router.process(intent)
    assert result2.status == TransactionStatus.SUCCESS
    assert result2.provider_transaction_id == result1.provider_transaction_id

    # Verify it didn't call the adapter logic again (mocked via output check in a real test,
    # but here we rely on the cached result being returned)

def test_iso20022_translation():
    """Verify ISO 20022 adapter generates interbank-style results."""
    plugins_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "plugins")
    router = ChameleonPaymentRouter(plugins_path=plugins_dir)

    intent = UniversalPaymentIntent(
        intent_id="ord_iso_1",
        amount=5000,
        currency=Currency.EUR,
        description="Global Transfer",
        customer_email="sender@bank.com",
        payment_token="IBAN12345",
        idempotency_key=uuid.uuid4().hex
    )

    result = router.process(intent, preferred_rail="ISO20022_SWIFT")
    assert result.status == TransactionStatus.SUCCESS
    assert result.provider == "ISO20022_SWIFT"
    assert result.provider_transaction_id.startswith("SWIFT-")

if __name__ == "__main__":
    # Simple manual run
    test_dynamic_plugin_loading()
    print("Dynamic Loading Test Passed!")
    test_iso20022_translation()
    print("ISO 20022 Translation Test Passed!")
