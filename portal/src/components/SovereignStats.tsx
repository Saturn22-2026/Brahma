'use client'

import React from 'react';
import { useSovereignToken } from '../hooks/useSovereignToken';

export default function SovereignStats() {
    const { price, float, userBalance } = useSovereignToken();

    return (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
            <div className="bg-slate-900 p-6 rounded-xl border border-slate-800">
                <h3 className="text-slate-400 text-sm font-medium mb-1">Current Price</h3>
                <p className="text-2xl font-bold text-white">{parseFloat(price).toFixed(6)} ETH</p>
            </div>

            <div className="bg-slate-900 p-6 rounded-xl border border-slate-800">
                <h3 className="text-slate-400 text-sm font-medium mb-1">Tradeable Float</h3>
                <p className="text-2xl font-bold text-emerald-400">{parseFloat(float).toLocaleString()} SQT</p>
            </div>

            <div className="bg-slate-900 p-6 rounded-xl border border-slate-800">
                <h3 className="text-slate-400 text-sm font-medium mb-1">Your Balance</h3>
                <p className="text-2xl font-bold text-blue-400">{parseFloat(userBalance).toLocaleString()} SQT</p>
            </div>
        </div>
    );
}
