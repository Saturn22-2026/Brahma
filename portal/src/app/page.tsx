'use client'

import React, { useState } from 'react';
import Header from '../components/Header';
import SovereignStats from '../components/SovereignStats';
import TradeInterface from '../components/TradeInterface';
import Explorer from '../components/Explorer';
import Faucet from '../components/Faucet';
import Governance from '../components/Governance';

type Tab = 'overview' | 'trade' | 'governance' | 'explorer' | 'faucet';

export default function Dashboard() {
    const [activeTab, setActiveTab] = useState<Tab>('overview');

    return (
        <div className="min-h-screen bg-black text-white selection:bg-emerald-500/30">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <Header />

                {/* Navigation Tabs */}
                <div className="flex border-b border-slate-800 mb-12 overflow-x-auto">
                    {(['overview', 'trade', 'governance', 'explorer', 'faucet'] as Tab[]).map((tab) => (
                        <button
                            key={tab}
                            onClick={() => setActiveTab(tab)}
                            className={`px-8 py-4 text-sm font-black uppercase tracking-widest transition-all border-b-2 ${
                                activeTab === tab
                                ? 'border-emerald-500 text-white'
                                : 'border-transparent text-slate-500 hover:text-slate-300'
                            }`}
                        >
                            {tab}
                        </button>
                    ))}
                </div>

                {/* Main Content Area */}
                <main className="pb-24">
                    {activeTab === 'overview' && (
                        <div className="animate-in fade-in slide-in-from-bottom-4 duration-700">
                            <SovereignStats />
                            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mt-8">
                                <div className="lg:col-span-2">
                                    <Explorer />
                                </div>
                                <div>
                                    <TradeInterface />
                                </div>
                            </div>
                        </div>
                    )}

                    {activeTab === 'trade' && (
                        <div className="animate-in zoom-in-95 duration-500">
                            <TradeInterface />
                        </div>
                    )}

                    {activeTab === 'governance' && (
                        <div className="animate-in slide-in-from-right-4 duration-500">
                            <Governance />
                        </div>
                    )}

                    {activeTab === 'explorer' && (
                        <div className="animate-in slide-in-from-left-4 duration-500">
                            <Explorer />
                        </div>
                    )}

                    {activeTab === 'faucet' && (
                        <div className="animate-in fade-in duration-500">
                            <Faucet />
                        </div>
                    )}
                </main>
            </div>

            {/* Global Footer Info */}
            <footer className="fixed bottom-0 w-full bg-black/80 backdrop-blur-md border-t border-slate-900 py-4 z-50">
                <div className="max-w-7xl mx-auto px-4 flex justify-between items-center text-[10px] uppercase font-bold tracking-tighter text-slate-600">
                    <div className="flex items-center gap-2">
                        <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></div>
                        Mainnet v1.1 Nodes: Active
                    </div>
                    <div>Post-Quantum Security: ML-DSA Enabled</div>
                    <div>© 2026 Bitcoin-Quantum Sovereign Nation</div>
                </div>
            </footer>
        </div>
    );
}
