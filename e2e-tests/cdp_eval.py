# CDP helper: evaluate JS in the LanRead reader WebView via Chrome DevTools Protocol
import json, sys, websocket

def cdp_eval(expr):
    pages = json.loads(__import__('urllib.request', fromlist=['urlopen']).urlopen('http://127.0.0.1:9223/json').read().decode())
    page = next(p for p in pages if p['type'] == 'page')
    ws = websocket.create_connection(page['webSocketDebuggerUrl'], timeout=10)
    ws.send(json.dumps({'id': 1, 'method': 'Runtime.evaluate', 'params': {'expression': expr, 'returnByValue': True}}))
    while True:
        msg = json.loads(ws.recv())
        if msg.get('id') == 1:
            ws.close()
            result = msg.get('result', {}).get('result', {})
            if 'value' in result:
                return result['value']
            return json.dumps(msg.get('result', {}))[:800]

if __name__ == '__main__':
    expr = sys.argv[1] if len(sys.argv) > 1 else 'document.title'
    out = cdp_eval(expr)
    print(out)
