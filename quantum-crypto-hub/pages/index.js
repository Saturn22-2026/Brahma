import React, { useState } from 'react';
import Head from 'next/head';

export default function Home() {
  const [wallet, setWallet] = useState(null);
  const [faucetStatus, setFaucetStatus] = useState('');
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

  const requestFaucet = async () => {
    if (!wallet) return;
    setFaucetStatus('Requesting drip...');
    try {
      const res = await fetch('/api/faucet', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ address: wallet.address }),
      });
      const data = await res.json();
      setFaucetStatus(data.message || data.error);
    } catch (e) {
      setFaucetStatus('Failed to connect to node.');
    }
  };

  return (
    <div className="container">
      <Head>
        <title>Quantum Crypto Hub | BTQ</title>
        <link rel="icon" href="/favicon.ico" />
      </Head>

      <main>
        <h1 className="title">Bitcoin-Quantum Sovereign Hub</h1>
        <p className="description">Your Gateway to Post-Quantum Sovereignty</p>

        <div className="grid">
          <section className="card">
            <h2>PQC Wallet</h2>
            <p>Generate a NIST Dilithium3 secured identity.</p>
            <button onClick={generateWallet} disabled={loading}>
              {loading ? 'Expanding Entropy...' : 'Generate New Keys'}
            </button>
            {wallet && (
              <div className="wallet-info">
                <p><strong>Address:</strong> {wallet.address}</p>
                <p className="warning">Backup your keys. Security level: Dilithium3</p>
              </div>
            )}
          </section>

          <section className="card">
            <h2>Sovereign Faucet</h2>
            <p>Get your initial BTQ bootstrap drip.</p>
            <button onClick={requestFaucet} disabled={!wallet}>Request 100 BTQ</button>
            {faucetStatus && <p className="status">{faucetStatus}</p>}
          </section>

          <section className="card">
            <h2>Airdrop Status</h2>
            <p>Monitor the AI Council linear drip.</p>
            <div className="airdrop-feed">
              <p>Next Cycle: 14:00 UTC</p>
              <p>Burn Rate: 2.5% Whale Tax Active</p>
            </div>
          </section>

          <section className="card">
            <h2>Ecosystem Socials</h2>
            <div className="links">
              <a href="https://x.com/BitcoinQuantum" target="_blank">X (Twitter)</a>
              <a href="#">Governance Forum</a>
              <a href="#">Documentation</a>
            </div>
          </section>
        </div>
      </main>

      <style jsx>{`
        .container { min-height: 100vh; padding: 0 0.5rem; display: flex; flex-direction: column; justify-content: center; align-items: center; background: #0a0a0a; color: #fff; font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif; }
        main { padding: 5rem 0; flex: 1; display: flex; flex-direction: column; justify-content: center; align-items: center; }
        .title { margin: 0; line-height: 1.15; font-size: 4rem; text-align: center; color: #00ff88; }
        .description { line-height: 1.5; font-size: 1.5rem; text-align: center; color: #888; }
        .grid { display: flex; align-items: center; justify-content: center; flex-wrap: wrap; max-width: 800px; margin-top: 3rem; }
        .card { margin: 1rem; flex-basis: 45%; padding: 1.5rem; text-align: left; color: inherit; text-decoration: none; border: 1px solid #333; border-radius: 10px; transition: color 0.15s ease, border-color 0.15s ease; background: #111; }
        .card:hover { border-color: #00ff88; }
        .card h2 { margin: 0 0 1rem 0; font-size: 1.5rem; }
        .card p { margin: 0; font-size: 1rem; line-height: 1.5; color: #aaa; }
        button { margin-top: 1rem; padding: 0.5rem 1rem; background: #00ff88; color: #000; border: none; border-radius: 5px; cursor: pointer; font-weight: bold; }
        button:disabled { background: #333; cursor: not-allowed; }
        .wallet-info { margin-top: 1rem; padding: 0.5rem; background: #000; border-radius: 5px; font-family: monospace; font-size: 0.8rem; }
        .warning { color: #ff3333; margin-top: 0.5rem !important; font-size: 0.7rem !important; }
        .status { margin-top: 1rem; color: #00ff88; font-size: 0.9rem; }
        .links { display: flex; flex-direction: column; gap: 0.5rem; margin-top: 1rem; }
        .links a { color: #00ff88; text-decoration: none; }
      `}</style>

      <style jsx global>{`
        html, body { padding: 0; margin: 0; background: #0a0a0a; }
      `}</style>
    </div>
  );
}
