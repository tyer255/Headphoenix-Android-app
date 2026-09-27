const express = require('express');
const cors = require('cors');
const spotifyUrlInfo = require('spotify-url-info');

const app = express();
const PORT = 3000;
const REMOTE_API_BASE = 'https://spotify2.ai.studio/api';

// Initialize spotify-url-info with Node's native fetch
const spotify = spotifyUrlInfo(globalThis.fetch);

app.use(cors());
app.use(express.json());

// Request logging for debugging
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

// Helper for standardized API responses
function sendSuccess(res, data, status = 200) {
  return res.status(status).json({
    success: true,
    ...data
  });
}

function sendError(res, code, message, status = 400) {
  return res.status(status).json({
    success: false,
    error: {
      code,
      message,
    },
  });
}

// ================= SPOTIFY PLAYLIST EXTRACTOR =================
/**
 * POST /api/playlist/extract
 * Request: { url: string }
 * Extracts metadata and track list from Spotify playlist share URL
 */
app.post('/api/playlist/extract', async (req, res) => {
  const { url } = req.body || {};

  // 1. Validate URL presence and type
  if (!url || typeof url !== 'string' || !url.trim()) {
    return sendError(res, 'EMPTY_URL', 'Spotify playlist URL cannot be empty. Please enter a valid playlist link.', 400);
  }

  const cleanUrl = url.trim();

  // 2. Validate URL syntax
  let parsedUrl;
  try {
    parsedUrl = new URL(cleanUrl);
  } catch (e) {
    return sendError(res, 'INVALID_URL', 'The link provided is not a valid URL format.', 400);
  }

  const hostname = parsedUrl.hostname.toLowerCase();
  const isSpotifyDomain = hostname === 'open.spotify.com' ||
    hostname.endsWith('.spotify.com') ||
    hostname === 'spotify.link' ||
    hostname.endsWith('.spotify.link');

  if (!isSpotifyDomain) {
    return sendError(res, 'INVALID_DOMAIN', 'The URL must be a Spotify link (e.g., https://open.spotify.com/playlist/...).', 400);
  }

  // 3. Validate entity type (must be playlist)
  const isPlaylistPath = parsedUrl.pathname.includes('/playlist/');
  const isShortLink = hostname.includes('spotify.link'); // short links redirect, let library resolve

  if (!isPlaylistPath && !isShortLink) {
    let entityType = 'unsupported Spotify link';
    if (parsedUrl.pathname.includes('/track/')) entityType = 'track';
    else if (parsedUrl.pathname.includes('/album/')) entityType = 'album';
    else if (parsedUrl.pathname.includes('/artist/')) entityType = 'artist';
    else if (parsedUrl.pathname.includes('/show/') || parsedUrl.pathname.includes('/episode/')) entityType = 'podcast';

    return sendError(res, 'NOT_A_PLAYLIST', `The provided link is for a Spotify ${entityType}. This extractor only supports Spotify playlists.`, 400);
  }

  // 4. Fetch playlist details using spotify-url-info
  try {
    console.log(`[EXTRACT] Fetching playlist metadata for: ${cleanUrl}`);
    const data = await spotify.getData(cleanUrl);

    if (!data) {
      return sendError(res, 'PLAYLIST_NOT_FOUND', 'Could not find or load the Spotify playlist. Make sure it is public and active.', 404);
    }

    if (data.type && data.type !== 'playlist') {
      return sendError(res, 'NOT_A_PLAYLIST', `The link resolved to a Spotify ${data.type} instead of a playlist.`, 400);
    }

    // Extract cover image (prefer highest resolution)
    const images = data.coverArt?.sources || data.images || data.visualIdentity?.image || [];
    let coverImage = '';
    if (Array.isArray(images) && images.length > 0) {
      const bestImage = images.find(img => img.url) || images[0];
      coverImage = bestImage ? (bestImage.url || '') : '';
    }

    // Extract raw tracks
    const rawTrackList = Array.isArray(data.trackList) ? data.trackList : [];

    // Normalize and deduplicate by Spotify track URI while strictly preserving playlist order
    const seenUris = new Set();
    const tracks = [];

    for (const t of rawTrackList) {
      const uri = t.uri || (t.id ? `spotify:track:${t.id}` : `spotify:track:${tracks.length}`);
      if (!seenUris.has(uri)) {
        seenUris.add(uri);

        // Normalize artist string
        let artistName = 'Unknown Artist';
        if (typeof t.subtitle === 'string' && t.subtitle.trim()) {
          artistName = t.subtitle.trim();
        } else if (Array.isArray(t.artists) && t.artists.length > 0) {
          artistName = t.artists.map(a => a.name || a).filter(Boolean).join(', ');
        } else if (typeof t.artist === 'string' && t.artist.trim()) {
          artistName = t.artist.trim();
        }

        const durationMs = typeof t.duration === 'number' ? Math.round(t.duration) : 0;
        const trackTitle = t.title || t.name || 'Untitled Track';
        const previewAudio = t.audioPreview?.url || t.previewUrl || null;

        tracks.push({
          name: trackTitle,
          artist: artistName,
          duration: durationMs,
          spotifyUri: uri,
          previewUrl: previewAudio,
          imageUrl: coverImage || null
        });
      }
    }

    const totalExtracted = tracks.length;

    // The spotify-url-info embed API provides up to 100 tracks for any playlist
    const isTruncated = totalExtracted >= 100;
    const note = isTruncated
      ? 'This extractor currently supports the first 100 tracks of this playlist.'
      : undefined;

    console.log(`[EXTRACT] Successfully extracted ${totalExtracted} tracks from "${data.name || data.title}" (truncated: ${isTruncated})`);

    return res.status(200).json({
      success: true,
      playlist: {
        name: data.title || data.name || 'Spotify Playlist',
        image: coverImage || null,
        spotifyUrl: cleanUrl,
        description: data.description || data.subtitle || null
      },
      totalExtracted: totalExtracted,
      tracks: tracks,
      truncated: isTruncated,
      note: note || null
    });

  } catch (err) {
    console.error('[EXTRACT ERROR]', err);
    const errMessage = err.message || '';

    if (errMessage.includes("Couldn't find any data") || errMessage.includes('404') || errMessage.includes('Not Found')) {
      return sendError(res, 'PLAYLIST_NOT_FOUND', 'Could not find this playlist on Spotify. Please verify the URL and ensure the playlist is public.', 404);
    }

    if (errMessage.includes("Couldn't parse") || errMessage.includes('Not an')) {
      return sendError(res, 'NOT_A_PLAYLIST', 'The provided URL could not be parsed as a valid Spotify playlist.', 400);
    }

    return sendError(res, 'EXTRACTION_FAILED', `Failed to extract Spotify playlist: ${errMessage || 'Network error'}`, 500);
  }
});

// ================= HEALTH CHECK =================
app.get('/api/health', (req, res) => {
  res.status(200).json({
    success: true,
    data: {
      status: 'healthy',
      timestamp: new Date().toISOString(),
      extractor: 'spotify-url-info',
      version: '1.0.0'
    }
  });
});

// ================= OPENGRAPH METADATA & PUBLIC WEB PAGES FOR TRACKS & LYRICS =================
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

async function getTrackMetadata(trackId, titleParam, artistParam, imageParam) {
  let title = titleParam || 'Song on Spotiz';
  let artist = artistParam || 'Artist';
  let image = imageParam || 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80';

  if (trackId && trackId.length > 2) {
    try {
      const res = await fetch(`${REMOTE_API_BASE}/track/${trackId}`, {
        headers: { 'Accept': 'application/json' }
      });
      if (res.ok) {
        const data = await res.json();
        const t = data.track || data;
        if (t.title) title = t.title;
        if (t.artist) artist = t.artist;
        if (t.images?.large || t.images?.medium || t.images?.small) {
          image = t.images.large || t.images.medium || t.images.small;
        }
      }
    } catch (e) {
      console.error('[OG METADATA FETCH ERROR]', e.message);
    }
  }

  return { title, artist, image };
}

app.get(['/track/:id', '/share/track/:id'], async (req, res) => {
  const trackId = req.params.id;
  const { title: queryTitle, artist: queryArtist, image: queryImage } = req.query;
  const meta = await getTrackMetadata(trackId, queryTitle, queryArtist, queryImage);

  const ogTitle = meta.title;
  const ogDesc = `${meta.artist} • Listen on Spotiz`;
  const ogImage = meta.image;
  const canonicalUrl = `https://spotify2.ai.studio/track/${trackId}`;

  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${escapeHtml(ogTitle)} - ${escapeHtml(meta.artist)} | Spotiz</title>
  
  <meta property="og:site_name" content="Spotiz">
  <meta property="og:type" content="music.song">
  <meta property="og:title" content="${escapeHtml(ogTitle)}">
  <meta property="og:description" content="${escapeHtml(ogDesc)}">
  <meta property="og:image" content="${escapeHtml(ogImage)}">
  <meta property="og:image:width" content="640">
  <meta property="og:image:height" content="640">
  <meta property="og:url" content="${escapeHtml(canonicalUrl)}">
  
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="${escapeHtml(ogTitle)}">
  <meta name="twitter:description" content="${escapeHtml(ogDesc)}">
  <meta name="twitter:image" content="${escapeHtml(ogImage)}">
  <style>
    body { background-color: #121212; color: #ffffff; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; padding: 20px; box-sizing: border-box; }
    .card { background: #181818; border-radius: 16px; padding: 24px; max-width: 380px; width: 100%; text-align: center; box-shadow: 0 12px 32px rgba(0,0,0,0.6); }
    .art { width: 100%; aspect-ratio: 1; border-radius: 12px; object-fit: cover; box-shadow: 0 8px 20px rgba(0,0,0,0.4); }
    .title { font-size: 22px; font-weight: 700; margin: 16px 0 4px 0; color: #ffffff; }
    .artist { font-size: 15px; color: #b3b3b3; margin: 0 0 20px 0; }
    .btn { display: inline-block; background: #1DB954; color: #000000; font-weight: 700; font-size: 15px; text-decoration: none; padding: 12px 32px; border-radius: 24px; transition: transform 0.2s; }
    .btn:hover { transform: scale(1.04); }
  </style>
</head>
<body>
  <div class="card">
    <img class="art" src="${escapeHtml(ogImage)}" alt="${escapeHtml(ogTitle)}" />
    <div class="title">${escapeHtml(ogTitle)}</div>
    <div class="artist">${escapeHtml(meta.artist)}</div>
    <a class="btn" href="spotiz://track/${trackId}">Play on Spotiz</a>
  </div>
</body>
</html>`;

  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  return res.send(html);
});

app.get(['/lyrics/:id', '/share/lyrics/:id'], async (req, res) => {
  const trackId = req.params.id;
  const { text: queryText, title: queryTitle, artist: queryArtist, image: queryImage } = req.query;
  const meta = await getTrackMetadata(trackId, queryTitle, queryArtist, queryImage);

  const selectedLyrics = queryText ? decodeURIComponent(queryText) : 'Listen with real-time synchronized lyrics on Spotiz';
  const ogTitle = `Lyrics from ${meta.title}`;
  const ogDesc = `"${selectedLyrics}" • ${meta.artist}`;
  const ogImage = meta.image;
  const canonicalUrl = `https://spotify2.ai.studio/lyrics/${trackId}`;

  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${escapeHtml(ogTitle)} - ${escapeHtml(meta.artist)} | Spotiz</title>
  
  <meta property="og:site_name" content="Spotiz">
  <meta property="og:type" content="music.song">
  <meta property="og:title" content="${escapeHtml(ogTitle)}">
  <meta property="og:description" content="${escapeHtml(ogDesc)}">
  <meta property="og:image" content="${escapeHtml(ogImage)}">
  <meta property="og:image:width" content="640">
  <meta property="og:image:height" content="640">
  <meta property="og:url" content="${escapeHtml(canonicalUrl)}">
  
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="${escapeHtml(ogTitle)}">
  <meta name="twitter:description" content="${escapeHtml(ogDesc)}">
  <meta name="twitter:image" content="${escapeHtml(ogImage)}">
  <style>
    body { background-color: #121212; color: #ffffff; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; padding: 20px; box-sizing: border-box; }
    .card { background: linear-gradient(180deg, #2a2a32 0%, #181818 100%); border-radius: 16px; padding: 24px; max-width: 400px; width: 100%; box-shadow: 0 12px 32px rgba(0,0,0,0.6); }
    .header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
    .art { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; }
    .title { font-size: 16px; font-weight: 700; color: #ffffff; margin: 0; }
    .artist { font-size: 13px; color: #b3b3b3; margin: 2px 0 0 0; }
    .lyrics-box { font-size: 22px; font-weight: 800; line-height: 1.4; color: #ffffff; margin-bottom: 24px; white-space: pre-wrap; word-break: break-word; }
    .btn { display: inline-block; background: #1DB954; color: #000000; font-weight: 700; font-size: 15px; text-decoration: none; padding: 12px 32px; border-radius: 24px; text-align: center; width: 100%; box-sizing: border-box; }
  </style>
</head>
<body>
  <div class="card">
    <div class="header">
      <img class="art" src="${escapeHtml(ogImage)}" alt="${escapeHtml(meta.title)}" />
      <div>
        <div class="title">${escapeHtml(meta.title)}</div>
        <div class="artist">${escapeHtml(meta.artist)}</div>
      </div>
    </div>
    <div class="lyrics-box">${escapeHtml(selectedLyrics)}</div>
    <a class="btn" href="spotiz://lyrics/${trackId}">Listen on Spotiz</a>
  </div>
</body>
</html>`;

  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  return res.send(html);
});

// ================= PROXY OTHER API ROUTES TO REMOTE =================
app.use('/api', async (req, res, next) => {
  // If the route was already handled (like /api/playlist/extract or /api/health), this won't be reached
  try {
    const targetUrl = `${REMOTE_API_BASE}${req.url}`;
    const options = {
      method: req.method,
      headers: {
        'Accept': 'application/json',
        'Content-Type': req.headers['content-type'] || 'application/json',
        ...(req.headers['authorization'] ? { 'Authorization': req.headers['authorization'] } : {})
      }
    };

    if (req.method !== 'GET' && req.method !== 'HEAD' && req.body && Object.keys(req.body).length > 0) {
      options.body = JSON.stringify(req.body);
    }

    const proxyRes = await fetch(targetUrl, options);
    const contentType = proxyRes.headers.get('content-type') || 'application/json';
    res.status(proxyRes.status);
    res.setHeader('content-type', contentType);

    const bodyText = await proxyRes.text();
    res.send(bodyText);
  } catch (err) {
    console.error(`[PROXY ERROR] ${req.method} ${req.url}:`, err.message);
    sendError(res, 'GATEWAY_ERROR', 'Failed to communicate with music service', 502);
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Spotify Playlist Extractor API Server running on port ${PORT}`);
});
