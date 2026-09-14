"""Operator env: BRAH_* first, then legacy BTQ_* so old scripts still start the node."""
from __future__ import annotations

import os


def env(suffix: str, default: str | None = None) -> str | None:
    """Read BRAH_<suffix>, then BTQ_<suffix>, then default."""
    for prefix in ("BRAH_", "BTQ_"):
        key = prefix + suffix
        if key in os.environ:
            return os.environ[key]
    return default
