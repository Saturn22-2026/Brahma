from typing import Dict, Any
from .payments import UniversalTransactionResult, TransactionStatus

class WebhookIngestor:
    """
    Translates provider-specific webhooks into UniversalTransactionResults.
    """

    @staticmethod
    def normalize_stripe(payload: Dict[str, Any]) -> UniversalTransactionResult:
        """Stripe Webhook -> Universal Result"""
        event_type = payload.get("type")
        data = payload.get("data", {}).get("object", {})

        status = TransactionStatus.PENDING
        if event_type == "payment_intent.succeeded":
            status = TransactionStatus.SUCCESS
        elif event_type == "payment_intent.payment_failed":
            status = TransactionStatus.FAILED

        return UniversalTransactionResult(
            provider="STRIPE",
            provider_transaction_id=data.get("id", ""),
            status=status,
            error_code=data.get("last_payment_error", {}).get("code") if status == TransactionStatus.FAILED else None,
            error_message=data.get("last_payment_error", {}).get("message") if status == TransactionStatus.FAILED else None
        )

    @staticmethod
    def normalize_iso20022(xml_payload: str) -> UniversalTransactionResult:
        """ISO 20022 camt.054 XML -> Universal Result"""
        # In a real app, use lxml to parse the complex XML
        print(f"[Webhook Ingestor] Parsing ISO 20022 camt.054 Notification...")

        # Simulated parsing logic
        if "<Sts>ACCP</Sts>" in xml_payload:
            status = TransactionStatus.SUCCESS
        else:
            status = TransactionStatus.FAILED

        return UniversalTransactionResult(
            provider="ISO20022_SWIFT",
            provider_transaction_id="SWIFT-RECV-123", # Extracted from XML
            status=status
        )
