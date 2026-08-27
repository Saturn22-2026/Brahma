'use client'

import React from 'react';
import { useBTQNode } from '../hooks/useBTQNode';

export default function Explorer() {
    const { stats, latestBlock } = useBTQNode();

    return (
        <div className="bg-slate-900 p-8 rounded-2xl border border-slate-800 shadow-2xl max-w-6xl mx-auto mt-12">
            <div className="flex justify-between items-center mb-8">
                <h2 className="text-3xl font-bold text-white">Block Explorer</h2>
                <div className="flex gap-4">
                    <div className="bg-slate-950 px-4 py-2 rounded-lg border border-slate-800">
                        <p className="text-slate-500 text-xs uppercase font-bold">Network Height</p>
                        <p className="text-xl font-mono text-emerald-500">{stats?.chain_height || 0}</p>
                    </div>
                    <div className="bg-slate-950 px-4 py-2 rounded-lg border border-slate-800">
                        <p className="text-slate-500 text-xs uppercase font-bold">P2P Nodes</p>
                        <p className="text-xl font-mono text-blue-400">{stats?.p2p_status === 'CONNECTED' ? 'Active' : 'Offline'}</p>
                    </div>
                </div>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full text-left">
                    <thead>
                        <tr className="text-slate-500 text-xs uppercase tracking-widest border-b border-slate-800">
                            <th className="pb-4 pl-4">Height</th>
                            <th className="pb-4">Timestamp</th>
                            <th className="pb-4">Miner</th>
                            <th className="pb-4">Txs</th>
                            <th className="pb-4 text-right pr-4">Reward</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/50">
                        {latestBlock && (
                            <tr className="text-white hover:bg-slate-800/30 transition-colors">
                                <td className="py-6 pl-4 font-mono text-emerald-500">#{latestBlock.index}</td>
                                <td className="py-6 text-sm text-slate-400">
                                    {new Date(latestBlock.timestamp * 1000).toLocaleString()}
                                </td>
                                <td className="py-6 text-sm font-mono truncate max-w-[200px]">
                                    {latestBlock.miner}
                                </td>
                                <td className="py-6">
                                    <span className="bg-slate-800 px-3 py-1 rounded-full text-xs">
                                        {latestBlock.transactions.length}
                                    </span>
                                </td>
                                <td className="py-6 text-right pr-4 font-bold">
                                    {(50 * Math.exp(-0.0001 * latestBlock.index)).toFixed(4)} BTQ
                                </td>
                            </tr>
                        )}
                        <tr className="opacity-30 italic text-slate-500">
                            <td className="py-4 pl-4" colSpan={5}>Streaming live network data...</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div className="mt-8 p-4 bg-emerald-500/5 border border-emerald-500/20 rounded-xl flex items-center gap-3">
                <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></div>
                <p className="text-emerald-500 text-xs font-bold uppercase tracking-widest">
                    Real-time Synchronization Active
                </p>
            </div>
        </div>
    );
}
