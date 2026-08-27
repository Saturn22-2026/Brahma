'use client'

import React from 'react';

export default function Governance() {
    return (
        <div className="bg-slate-900 p-8 rounded-2xl border border-slate-800 shadow-2xl max-w-4xl mx-auto mt-12">
            <h2 className="text-3xl font-bold text-white mb-6">Sovereign DAO</h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-12">
                <div className="bg-slate-950 p-6 rounded-xl border border-slate-800">
                    <h3 className="text-emerald-400 font-bold mb-2">Empowerment Fund</h3>
                    <p className="text-slate-400 text-sm mb-4">Community-voted grants for developers and ecosystem growth.</p>
                    <p className="text-2xl font-mono text-white">2,310,000 SQT</p>
                </div>

                <div className="bg-slate-950 p-6 rounded-xl border border-slate-800">
                    <h3 className="text-blue-400 font-bold mb-2">Wealth Wallet</h3>
                    <p className="text-slate-400 text-sm mb-4">Strategic long-term holdings managed by DAO vote.</p>
                    <p className="text-2xl font-mono text-white">2,310,000 SQT</p>
                </div>
            </div>

            <h3 className="text-xl font-bold text-white mb-4">Active Proposals</h3>
            <div className="space-y-4">
                <div className="bg-slate-950 p-6 rounded-xl border border-slate-800 flex justify-between items-center">
                    <div>
                        <p className="text-white font-medium">#1: Allocate 50k SQT for Quantum Audit</p>
                        <p className="text-slate-500 text-xs mt-1">Status: Voting Active | Ends in 4 days</p>
                    </div>
                    <button className="bg-emerald-500/10 text-emerald-500 border border-emerald-500/50 px-6 py-2 rounded-lg font-bold hover:bg-emerald-500 hover:text-white transition-all">
                        VOTE
                    </button>
                </div>

                <div className="bg-slate-950 p-6 rounded-xl border border-slate-800 flex justify-between items-center opacity-50">
                    <div>
                        <p className="text-white font-medium">#0: Genesis Liquidity Seeding</p>
                        <p className="text-slate-500 text-xs mt-1">Status: Succeeded | Executed</p>
                    </div>
                    <span className="text-emerald-500 font-bold text-sm">PASSED</span>
                </div>
            </div>
        </div>
    );
}
