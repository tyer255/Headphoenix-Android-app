const fetch = require('node-fetch');
fetch('https://api.cobalt.tools/api/json', {
  method: 'POST',
  headers: {
    'Accept': 'application/json',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ url: 'https://youtube.com/watch?v=LUgpPmj6nR8', isAudioOnly: true })
}).then(r => r.json()).then(console.log);
