import { useState } from 'react';
import Head from 'next/head';

export default function Home() {
  const [wallet, setWallet] = useState(null);
  const [faucetStatus, setFaucetStatus] = useState('');
  const [airdropStatus, setAirdropStatus] = useState('');
  const [airdropAddress, setAirdropAddress] = useState('');
  const [loading, setLoading] = useState(false);

  const generateWallet = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/wallet');
      const data = await res.json();
      setWallet(data);
    } catch (e) {
      console.error(e);
    }
    setLoading(false);
  };

  const claimFaucet = async () => {
    if (!wallet) return alert('Generate a wallet first!');
    setFaucetStatus('Processing...');
    try {
      const res = await fetch('/api/faucet', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ address: wallet.address })
      });
      const data = await res.json();
      setFaucetStatus(data.message || data.error);
    } catch (e) {
      setFaucetStatus('Failed to connect to node.');
    }
  };

  const executeAirdrop = async () => {
    if (!airdropAddress) return alert('Enter an address!');
    setAirdropStatus('Processing...');
    try {
      const res = await fetch('/api/airdrop', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ address: airdropAddress })
      });
      const data = await res.json();
      setAirdropStatus(data.message || data.error);
    } catch (e) {
      setAirdropStatus('Failed to execute airdrop.');
    }
  };

  const shareUrl = encodeURIComponent('https://bitcoin-quantum.org');
  const shareText = encodeURIComponent('Check out this Quantum Crypto Hub I am running!');

  return (
    <div className="min-h-screen bg-black text-white font-sans p-8">
      <Head>
        <title>Quantum Crypto Hub | BTQ</title>
      </Head>

      <div className="max-w-4xl mx-auto">
        <header className="text-center mb-12">
          <h1 className="text-5xl font-extrabold text-green-400 mb-4">Quantum Crypto Hub</h1>
          <p className="text-xl text-gray-400">A shared device for wallet generation, faucets, and airdrops.</p>
        </header>

        {/* Social Sharing */}
        <div className="flex justify-center gap-4 mb-12">
          <a
            href={`https://twitter.com/intent/tweet?url=${shareUrl}&text=${shareText}`}
            target="_blank"
            rel="noopener noreferrer"
            className="px-6 py-2 bg-blue-500 hover:bg-blue-600 rounded-lg font-bold transition"
          >
            Share on Twitter
          </a>
          <a
            href={`https://t.me/share/url?url=${shareUrl}&text=${shareText}`}
            target="_blank"
            rel="noopener noreferrer"
            className="px-6 py-2 bg-sky-600 hover:bg-sky-700 rounded-lg font-bold transition"
          >
            Share on Telegram
          </a>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {/* Wallet Section */}
          <section className="p-6 border border-gray-800 rounded-xl bg-gray-900 shadow-lg">
            <h2 className="text-2xl font-bold mb-4 flex items-center gap-2">
              <span className="w-8 h-8 rounded-full bg-green-500/20 text-green-500 flex items-center justify-center text-sm">1</span>
              Generate Wallet
            </h2>
            <p className="text-gray-400 mb-6">Generate a NIST Dilithium3 secured identity.</p>
            <button
              onClick={generateWallet}
              disabled={loading}
              className="w-full py-3 bg-green-500 hover:bg-green-600 disabled:bg-gray-800 text-black font-bold rounded-lg transition"
            >
              {loading ? 'Expanding Entropy...' : 'Generate New Wallet'}
            </button>
            {wallet && (
              <div className="mt-6 p-4 bg-black rounded-lg border border-gray-800 overflow-hidden">
                <div className="space-y-4">
                  <div>
                    <p className="text-xs text-gray-500 uppercase tracking-wider mb-1">Sovereign Address</p>
                    <p className="text-sm font-mono break-all text-green-400">{wallet.address}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 uppercase tracking-wider mb-1">Private Key</p>
                    <p className="text-sm font-mono break-all text-red-400">{wallet.privateKey}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 uppercase tracking-wider mb-1">Mnemonic Phrase</p>
                    <p className="text-sm font-mono break-all text-gray-300">{wallet.mnemonic}</p>
                  </div>
                </div>
                <p className="mt-4 text-xs text-red-500 font-bold">⚠️ Save these securely! They are not stored on this device.</p>
              </div>
            )}
          </section>

          {/* Faucet Section */}
          <section className="p-6 border border-gray-800 rounded-xl bg-gray-900 shadow-lg">
            <h2 className="text-2xl font-bold mb-4 flex items-center gap-2">
              <span className="w-8 h-8 rounded-full bg-green-500/20 text-green-500 flex items-center justify-center text-sm">2</span>
              Claim from Faucet
            </h2>
            <p className="text-gray-400 mb-6">Get your initial BTQ bootstrap drip.</p>
            <button
              onClick={claimFaucet}
              disabled={!wallet}
              className="w-full py-3 bg-green-500 hover:bg-green-600 disabled:bg-gray-800 text-black font-bold rounded-lg transition"
            >
              Claim 0.1 Test Tokens
            </button>
            {faucetStatus && (
              <div className="mt-4 p-3 bg-green-500/10 border border-green-500/20 rounded text-green-500 text-sm break-all">
                {faucetStatus}
              </div>
            )}
          </section>

          {/* Airdrop Section */}
          <section className="p-6 border border-gray-800 rounded-xl bg-gray-900 shadow-lg md:col-span-2">
            <h2 className="text-2xl font-bold mb-4 flex items-center gap-2">
              <span className="w-8 h-8 rounded-full bg-green-500/20 text-green-500 flex items-center justify-center text-sm">3</span>
              Airdrop Tool
            </h2>
            <p className="text-gray-400 mb-6">Send a batch distribution via the AI Council.</p>
            <div className="space-y-4">
              <input
                type="text"
                placeholder="Enter address to airdrop to"
                value={airdropAddress}
                onChange={(e) => setAirdropAddress(e.target.value)}
                className="w-full p-3 bg-black border border-gray-800 rounded-lg text-white placeholder-gray-600 focus:border-green-500 outline-none transition"
              />
              <button
                onClick={executeAirdrop}
                className="w-full py-3 bg-green-500 hover:bg-green-600 text-black font-bold rounded-lg transition"
              >
                Send Airdrop
              </button>
            </div>
            {airdropStatus && (
              <div className="mt-4 p-3 bg-blue-500/10 border border-blue-500/20 rounded text-blue-400 text-sm break-all">
                {airdropStatus}
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
