// Evaluate JS in the reader WebView via DevTools protocol
const WebSocket = require('ws');

const url = process.argv[2];
const expr = process.argv[3];

const ws = new WebSocket(url, { perMessageDeflate: false });
ws.on('open', () => {
    ws.send(JSON.stringify({ id: 1, method: 'Runtime.evaluate', params: { expression: expr, returnByValue: true } }));
});
ws.on('message', (data) => {
    const obj = JSON.parse(data.toString());
    if (obj.id === 1) {
        console.log(JSON.stringify(obj.result, null, 2));
        process.exit(0);
    }
});
ws.on('error', (e) => { console.error('WS error', e.message); process.exit(1); });
setTimeout(() => { console.error('timeout'); process.exit(1); }, 5000);
