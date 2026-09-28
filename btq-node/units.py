"""Brahma Coin ledger units. 1 coin = 100_000_000 base units. Consensus uses int only."""
from __future__ import annotations

from decimal import Decimal, ROUND_DOWN

UNITS_PER_BRAH = 100_000_000


def to_units(value) -> int:
    if value is None:
        return 0
    if isinstance(value, bool):
        raise ValueError("invalid amount")
    # JSON ints are whole Brahma Coin. Never treat an int as already-scaled units.
    return int((Decimal(str(value)) * UNITS_PER_BRAH).to_integral_value(rounding=ROUND_DOWN))


def from_units(units: int) -> str:
    d = Decimal(int(units)) / Decimal(UNITS_PER_BRAH)
    return format(d, "f")


def from_units_float(units: int) -> float:
    return float(Decimal(int(units)) / Decimal(UNITS_PER_BRAH))
