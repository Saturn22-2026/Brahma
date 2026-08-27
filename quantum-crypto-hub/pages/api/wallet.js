import { exec } from 'child_process';
import path from 'path';

export default function handler(req, res) {
  // Path to the native wallet-gen binary we built in Phase 65
  const binPath = path.resolve('../btq-node/target/release/wallet-gen');

  exec(binPath, (error, stdout, stderr) => {
    if (error) {
      // Mock generation if binary not found in dev env
      const mockAddr = `0xPQC_${Math.random().toString(16).slice(2, 10)}`;
      return res.status(200).json({
        address: mockAddr,
        status: 'Mocked (Production Binary Not Found)'
      });
    }

    // Parse stdout for the address
    const match = stdout.match(/Sovereign Address:\s+(0x[a-fA-F0-9]+)/);
    const address = match ? match[1] : 'Unknown';

    res.status(200).json({ address, algorithm: 'Dilithium3' });
  });
}
