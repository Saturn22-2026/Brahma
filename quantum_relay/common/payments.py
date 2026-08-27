from dataclasses import dataclass
from typing import Optional
from enum import Enum

class Currency(Enum):
    USD = "USD"
    EUR = "EUR"
    BTC = "BTC"

class TransactionStatus(Enum):
    PENDING = "PENDING"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    REFUNDED = "REFUNDED"

@dataclass
class UniversalPaymentIntent:
    """The standard internal representation of a payment."""
    intent_id: str
    amount: int # Always use smallest denomination (e.g., cents)
    currency: Currency
    description: str
    customer_email: str
    payment_token: str # Tokenized card/crypto wallet info (Never raw PAN)
    status: TransactionStatus = TransactionStatus.PENDING

@dataclass
class UniversalTransactionResult:
    """The standard internal representation of a payment result."""
    provider: str
    provider_transaction_id: str
    status: TransactionStatus
    error_code: Optional[str] = None
    error_message: Optional[str] = None
