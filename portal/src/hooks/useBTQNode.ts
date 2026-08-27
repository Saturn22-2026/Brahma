import { useState, useEffect } from 'react';

export interface BTQBlock {
    index: number;
    timestamp: number;
    transactions: any[];
    previous_hash: string;
    nonce: number;
    miner: string;
}

export interface NetworkStats {
    chain_height: number;
    total_mined: number;
    difficulty: number;
    p2p_status: string;
}

export function useBTQNode() {
    const [stats, setStats] = useState<NetworkStats | null>(null);
    const [latestBlock, setLatestBlock] = useState<BTQBlock | null>(null);

    const fetchStats = async () => {
        try {
            const response = await fetch('http://localhost:8545', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    jsonrpc: '2.0',
                    method: 'btq_getNetworkStats',
                    params: [],
                    id: 1
                })
            });
            const data = await response.json();
            if (data.result) setStats(data.result);
        } catch (e) {
            console.error("Failed to fetch BTQ node stats", e);
        }
    };

    const fetchLatestBlock = async () => {
        try {
            const response = await fetch('http://localhost:8545', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    jsonrpc: '2.0',
                    method: 'btq_getLatestBlock',
                    params: [],
                    id: 2
                })
            });
            const data = await response.json();
            if (data.result) setLatestBlock(JSON.parse(data.result));
        } catch (e) {
            console.error("Failed to fetch latest BTQ block", e);
        }
    };

    useEffect(() => {
        fetchStats();
        fetchLatestBlock();
        const interval = setInterval(() => {
            fetchStats();
            fetchLatestBlock();
        }, 5000);
        return () => clearInterval(interval);
    }, []);

    return { stats, latestBlock };
}
