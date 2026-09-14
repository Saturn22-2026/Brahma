package com.btq.wallet

/**
 * @title Sovereign Wallet Constants
 * @dev Production-ready parameters for the Bitcoin-Quantum ecosystem.
 */
object Constants {
    // --- NETWORK & RPC ---
    // In Production/Mainnet, this points to the local node (Sovereign mode)
    const val RPC_URL = "http://localhost:8545" 
    
    // --- CORE CONTRACTS (HARDCODED PER  GENESIS) ---
    const val BTQ_TOKEN_ADDRESS = "0x5FbDB2315678afecb367f032d93F642f64180aa3"
    
    // --- L2 ASSET SYMBOLS ---
    val L2_SYMBOLS = mapOf(
        1 to "HOMIE",
        2 to "SLUM",
        3 to "CRAZY",
        4 to "BOUJIE",
        5 to "QMILE",
        6 to "SOF",
        7 to "5AVE",
        8 to "POOKIE"
    )
    
    // --- SECURITY PARAMETERS ---
    const val DMS_SWEEP_DAYS = 14
    const val MIN_DUST_THRESHOLD = 0.0001
}
