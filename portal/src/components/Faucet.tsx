'use client'

import React from 'react';
import { useSovereignToken } from '../hooks/useSovereignToken';

export default function Faucet() {
    const { hasClaimed, claim } = useSovereignToken();

    return (
        <div className="bg-slate-900 p-8 rounded-2xl border border-slate-800 shadow-2xl max-w-lg mx-auto mt-12">
            <h2 className="text-3xl font-bold text-white mb-4">Mining Faucet</h2>
            <p className="text-slate-400 mb-8">
                Ready to mine on the Quantum network? Claim your one-time allocation of 10 SQT
                 to pay for your first quantum-secure transactions and setup your L2 node.
            </p>

            {hasClaimed ? (
                <div className="bg-emerald-500/10 border border-emerald-500 p-4 rounded-xl text-center">
                    <p className="text-emerald-500 font-bold">
                        ✅ Faucet Already Claimed
                    </p>
                    <p className="text-slate-500 text-sm mt-1">
                        Your account has already received its bootstrap allocation.
                    </p>
                </div>
            ) : (
                <button
                    onClick={() => claim()}
                    className="w-full py-4 bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-lg rounded-xl transition-all shadow-lg shadow-emerald-500/20"
                >
                    Claim 10 SQT
                </button>
            )}

            <div className="mt-8 pt-8 border-t border-slate-800">
                <h4 className="text-slate-500 text-xs font-bold uppercase tracking-widest mb-2">Funding Source</h4>
                <p className="text-slate-400 text-sm">
                    This faucet is funded by the **Sovereign Mining Reserve**. It does not impact the
                    AMM tradeable float or the price floor.
                </p>
            </div>
        </div>
    );
}
