import React, { useState, useEffect } from 'react';
import { Activity, Shield, Cpu, Zap, Search } from 'lucide-react';

function App() {
  const [stats, setStats] = useState({ height: 42069, mined: 12500000, tps: 2.5 });
  const [blocks, setBlocks] = useState([
    { height: 42069, hash: "0x8a2b...f1c3", txs: 12, time: "2s ago" },
    { height: 42068, hash: "0x7c1d...e2b4", txs: 8, time: "14s ago" },
    { height: 42067, hash: "0x9e3f...a4d1", txs: 15, time: "27s ago" },
  ]);

  return (
    <div className="min-h-screen p-8">
      {/* Header */}
      <header className="flex justify-between items-center mb-12">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 bg-blue-600 rounded-lg flex items-center justify-center">
            <Shield size={32} />
          </div>
          <div>
            <h1 className="text-3xl font-bold">BTQ Explorer</h1>
            <p className="text-blue-400 text-sm">Post-Quantum Settlement Layer</p>
          </div>
        </div>
        <div className="flex gap-8">
          <StatCard label="Height" value={stats.height} icon={<Activity className="text-green-400" />} />
          <StatCard label="Circulating" value={stats.mined.toLocaleString() + " BTQ"} icon={<Cpu className="text-purple-400" />} />
          <StatCard label="Network Speed" value={stats.tps + " TPS"} icon={<Zap className="text-yellow-400" />} />
        </div>
      </header>

      {/* Search */}
      <div className="relative mb-12">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500" />
        <input
          type="text"
          placeholder="Search by Address, Hash, or Block Height..."
          className="w-full bg-gray-800 border border-gray-700 rounded-xl py-4 pl-12 pr-4 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Recent Blocks */}
        <section className="bg-gray-800 rounded-2xl p-6 border border-gray-700">
          <h2 className="text-xl font-bold mb-6 flex items-center gap-2">
            <Activity className="text-blue-500" /> Recent Blocks
          </h2>
          <div className="space-y-4">
            {blocks.map(block => (
              <div key={block.height} className="flex justify-between items-center p-4 bg-gray-900 rounded-xl hover:bg-gray-750 transition-colors cursor-pointer border border-transparent hover:border-gray-600">
                <div className="flex gap-4 items-center">
                  <div className="w-10 h-10 bg-gray-800 rounded-lg flex items-center justify-center font-bold text-blue-400">
                    {block.height}
                  </div>
                  <div>
                    <div className="text-sm text-gray-400 font-mono">{block.hash}</div>
                    <div className="text-xs text-gray-500">{block.txs} transactions</div>
                  </div>
                </div>
                <div className="text-sm text-gray-400">{block.time}</div>
              </div>
            ))}
          </div>
        </section>

        {/* AI Agent Activity (Pillar 4) */}
        <section className="bg-gray-800 rounded-2xl p-6 border border-gray-700">
          <h2 className="text-xl font-bold mb-6 flex items-center gap-2">
            <Cpu className="text-purple-500" /> AI Agent Sentinel
          </h2>
          <div className="space-y-4">
            <div className="p-4 bg-purple-900/20 border border-purple-500/30 rounded-xl">
              <div className="flex items-center gap-2 text-purple-400 mb-1">
                <Shield size={16} /> <span>$HOMIE AI Treasury</span>
              </div>
              <p className="text-sm">Buying 50,000 BTQ to stabilize price curve.</p>
              <div className="text-xs text-purple-600 mt-2">Transaction: 0x9a2...3b1</div>
            </div>
            <div className="p-4 bg-blue-900/20 border border-blue-500/30 rounded-xl">
              <div className="flex items-center gap-2 text-blue-400 mb-1">
                <Activity size={16} /> <span>Whale Extinguisher Event</span>
              </div>
              <p className="text-sm">VC Wallet (0x123...) attempted dump. 35% tax applied.</p>
              <div className="text-xs text-blue-600 mt-2">Taxed: 1.2M BTQ → Sovereign Reserve</div>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

function StatCard({ label, value, icon }) {
  return (
    <div className="bg-gray-800 p-4 rounded-xl border border-gray-700 flex items-center gap-4 min-w-[200px]">
      <div className="w-10 h-10 bg-gray-900 rounded-lg flex items-center justify-center">
        {icon}
      </div>
      <div>
        <div className="text-xs text-gray-500 uppercase font-bold">{label}</div>
        <div className="text-lg font-bold">{value}</div>
      </div>
    </div>
  );
}

export default App;
