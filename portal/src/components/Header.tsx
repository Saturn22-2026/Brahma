'use client'

import React from 'react';
import Image from 'next/image';

export default function Header() {
    return (
        <header className="flex items-center justify-between py-6 mb-12 border-b border-slate-800">
            <div className="flex items-center gap-4">
                <div className="relative w-12 h-12 rounded-full overflow-hidden border-2 border-emerald-500 shadow-lg shadow-emerald-500/20">
                    <Image
                        src="/assets/logo.png"
                        alt="Bitcoin-Quantum Logo"
                        fill
                        className="object-cover"
                    />
                </div>
                <div>
                    <h1 className="text-2xl font-black text-white tracking-tighter uppercase italic">
                        Bitcoin<span className="text-emerald-500">-Quantum</span>
                    </h1>
                    <p className="text-xs text-slate-500 font-bold tracking-[0.2em] uppercase">
                        Sovereign Economic Layer
                    </p>
                </div>
            </div>

            <div className="flex items-center gap-6">
                <nav className="hidden md:flex items-center gap-6 text-sm font-bold text-slate-400">
                    <a href="#" className="hover:text-white transition-colors">Market</a>
                    <a href="#" className="hover:text-white transition-colors">Governance</a>
                    <a href="#" className="hover:text-white transition-colors">Airdrop</a>
                </nav>
                <button className="px-6 py-2 bg-white text-black font-black text-sm rounded-full hover:bg-emerald-500 hover:text-white transition-all">
                    Connect Wallet
                </button>
            </div>
        </header>
    );
}
