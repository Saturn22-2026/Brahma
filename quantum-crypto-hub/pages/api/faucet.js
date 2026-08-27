export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).end();

  const { address } = req.body;
  if (!address) return res.status(400).json({ error: 'Address required' });

  try {
    const rpcRes = await fetch(process.env.RPC_URL || 'http://localhost:8545', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        jsonrpc: '2.0',
        method: 'btq_requestFaucet',
        params: [address],
        id: 1,
      }),
    });
    const data = await rpcRes.json();

    if (data.error) {
      return res.status(400).json({ error: data.error.message });
    }

    res.status(200).json({ message: '100 BTQ Drip Initialized. Check explorer.' });
  } catch (e) {
    res.status(500).json({ error: 'Sovereign Node Offline.' });
  }
}
