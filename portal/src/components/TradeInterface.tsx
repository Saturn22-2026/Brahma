'use client'

import React, { useState, useEffect } from 'react';
import { useSovereignToken } from '../hooks/useSovereignToken';

export default function TradeInterface() {
    const { price, float, buy, sell } = useSovereignToken();
    const [amount, setAmount] = useState('');
    const [isBuy, setIsBuy] = useState(true);
    const [whaleAlert, setWhaleAlert] = useState(false);
    const [taxEstimate, setTaxEstimate] = useState(0);

    const MAX_SELL_RATIO = 0.025; // 2.5%

    useEffect(() => {
        if (!isBuy && amount && float) {
            const ratio = parseFloat(amount) / parseFloat(float);
            if (ratio > MAX_SELL_RATIO) {
                setWhaleAlert(true);
                // Simple estimation logic matching the contract
                const excess = ratio - MAX_SELL_RATIO;
                if (excess < 0.01) setTaxEstimate(5);
                else if (excess < 0.02) setTaxEstimate(15);
                else if (excess < 0.05) setTaxEstimate(25);
                else setTaxEstimate(35);
            } else {
                setWhaleAlert(false);
            }
        } else {
            setWhaleAlert(false);
        }
    }, [amount, isBuy, float]);

    const handleTrade = () => {
        if (!amount) return;
        if (isBuy) {
            buy(amount);
        } else {
            sell(amount);
        }
    };

    return (
        <div className="bg-slate-900 p-8 rounded-2xl border border-slate-800 shadow-2xl max-w-lg mx-auto">
            <div className="flex gap-4 mb-6">
                <button
                    onClick={() => setIsBuy(true)}
                    className={`flex-1 py-2 rounded-lg font-bold transition-all ${isBuy ? 'bg-emerald-500 text-white' : 'bg-slate-800 text-slate-400'}`}
                >
                    BUY
                </button>
                <button
                    onClick={() => setIsBuy(false)}
                    className={`flex-1 py-2 rounded-lg font-bold transition-all ${!isBuy ? 'bg-rose-500 text-white' : 'bg-slate-800 text-slate-400'}`}
                >
                    SELL
                </button>
            </div>

            <div className="mb-6">
                <label className="block text-slate-400 text-sm mb-2">
                    {isBuy ? 'Amount in ETH' : 'Amount in SQT'}
                </label>
                <input
                    type="number"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="0.00"
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl p-4 text-white text-xl focus:outline-none focus:border-emerald-500 transition-colors"
                />
            </div>

            {whaleAlert && (
                <div className="bg-rose-500/10 border border-rose-500 p-4 rounded-xl mb-6 animate-pulse">
                    <p className="text-rose-500 font-bold text-sm">
                        ⚠️ WHALE ALERT: This sale exceeds 2.5% of the float.
                        A progressive tax of {taxEstimate}% will be applied to fund the Sovereign Reserve.
                    </p>
                </div>
            )}

            <div className="bg-slate-950 p-4 rounded-xl mb-8 flex justify-between items-center border border-slate-800">
                <span className="text-slate-500 text-sm">Estimated Received</span>
                <span className="text-white font-mono font-bold">
                    {amount ? (isBuy ? (parseFloat(amount) / parseFloat(price)).toFixed(2) : (parseFloat(amount) * parseFloat(price)).toFixed(4)) : '0.00'}
                    {isBuy ? ' SQT' : ' ETH'}
                </span>
            </div>

            <button
                onClick={handleTrade}
                className={`w-full py-4 rounded-xl font-bold text-lg transition-all ${isBuy ? 'bg-emerald-500 hover:bg-emerald-600 shadow-emerald-500/20' : 'bg-rose-500 hover:bg-rose-600 shadow-rose-500/20'} shadow-lg`}
            >
                Confirm {isBuy ? 'Purchase' : 'Sale'}
            </button>
        </div>
    );
}
