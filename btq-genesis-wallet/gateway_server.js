const http = require('http');

const PORT = 3000;

const server = http.createServer((req, res) => {
    console.log(`[Sentinel Gateway] Request: ${req.method} ${req.url}`);

    if (req.url.startsWith('/activate')) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            status: 'success',
            genesis_rank: 499,
            bounty_dispatched: true
        }));
    } else {
        res.writeHead(404);
        res.end();
    }
});

server.listen(PORT, () => {
    console.log(`===================================================`);
    console.log(`   SENTINEL L1 GATEWAY SIMULATOR ACTIVE          `);
    console.log(`   Listening on port ${PORT}                     `);
    console.log(`===================================================`);
});
