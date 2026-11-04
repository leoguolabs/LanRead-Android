// Evaluate JS in the reader WebView via DevTools protocol to diagnose toolbar toggle
import net from 'node:net';

const pageId = process.argv[2] || '2697D853DF1F469B3E27647F9716EC2A';
const expr = process.argv[3] || "document.title";

const socket = net.connect(9222, '127.0.0.1');
let buffer = '';
let id = 0;

socket.on('data', (chunk) => {
    buffer += chunk.toString();
    // try parse messages
    let idx;
    while ((idx = buffer.indexOf('}')) >= 0) {
        // HTTP upgrade handshake happens first; after upgrade frames are JSON with 4-byte length header
        break;
    }
});

// WebSocket client implementation (minimal, no masking for client->server requires masking)
function wsSend(obj) {
    const payload = Buffer.from(JSON.stringify(obj));
    const mask = Buffer.from([1, 2, 3, 4]);
    let header;
    if (payload.length < 126) {
        header = Buffer.from([0x81, 0x80 | payload.length]);
    } else if (payload.length < 65536) {
        header = Buffer.alloc(4);
        header[0] = 0x81; header[1] = 0x80 | 126;
        header.writeUInt16BE(payload.length, 2);
    } else {
        header = Buffer.alloc(10);
        header[0] = 0x81; header[1] = 0x80 | 127;
        header.writeBigUInt64BE(BigInt(payload.length), 2);
    }
    const masked = Buffer.alloc(payload.length);
    for (let i = 0; i < payload.length; i++) masked[i] = payload[i] ^ mask[i % 4];
    socket.write(Buffer.concat([header, mask, masked]));
}

let messages = [];
socket.on('data', (chunk) => {
    messages.push(chunk);
});

const key = Buffer.from('0123456789abcdef0123456789abcdef').toString('base64');
socket.on('connect', () => {
    socket.write(`GET /devtools/page/${pageId} HTTP/1.1\r\nHost: 127.0.0.1:9222\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: ${key}\r\nSec-WebSocket-Version: 13\r\n\r\n`);
});

setTimeout(() => {
    wsSend({ id: 1, method: 'Runtime.evaluate', params: { expression: expr, returnByValue: true } });
}, 500);

setTimeout(() => {
    const all = Buffer.concat(messages);
    // find JSON payloads after handshake
    const text = all.toString('utf8');
    const jsonStart = text.indexOf('{');
    // Extract all complete JSON objects with our id
    const results = [];
    let depth = 0, startIdx = -1;
    for (let i = jsonStart; i < text.length; i++) {
        if (text[i] === '{') { if (depth === 0) startIdx = i; depth++; }
        else if (text[i] === '}') { depth--; if (depth === 0 && startIdx >= 0) { results.push(text.slice(startIdx, i + 1)); startIdx = -1; } }
    }
    for (const r of results) {
        try {
            const obj = JSON.parse(r);
            if (obj.id === 1) {
                console.log(JSON.stringify(obj.result, null, 2));
                process.exit(0);
            }
        } catch (e) { /* partial */ }
    }
    console.log('RAW TAIL:', text.slice(-800));
    process.exit(1);
}, 2500);
