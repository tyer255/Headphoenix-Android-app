import urllib.request
import re
import json

try:
    url = "https://www.youtube.com/watch?v=LUgpPmj6nR8"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as res:
        html = res.read().decode("utf-8")
        
        match = re.search(r'ytInitialPlayerResponse\s*=\s*({.+?});var', html)
        if match:
            data = json.loads(match.group(1))
            formats = data.get('streamingData', {}).get('adaptiveFormats', [])
            for f in formats:
                if 'audio' in f.get('mimeType', ''):
                    print("Found audio format!")
                    if 'signatureCipher' in f:
                        print("NEEDS CIPHER DECRYPTION!")
                    else:
                        print("URL:", f.get('url'))
                    break
        else:
            print("Could not find ytInitialPlayerResponse")
except Exception as e:
    print(e)
