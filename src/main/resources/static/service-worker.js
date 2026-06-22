const CACHE_NAME = 'mind-companion-v2';
const OFFLINE_URL = '/offline.html';

const PRECACHE_URLS = [
    '/',
    '/chat',
    '/mood',
    '/journal',
    '/dashboard',
    '/manifest.json',
    '/icons/icon-192.png',
    '/icons/icon-512.png',
    OFFLINE_URL
];

// Install — precache core assets
self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => cache.addAll(PRECACHE_URLS))
            .then(() => self.skipWaiting())
    );
});

// Activate — clean up old caches
self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(cacheNames =>
            Promise.all(
                cacheNames
                    .filter(name => name !== CACHE_NAME)
                    .map(name => caches.delete(name))
            )
        ).then(() => self.clients.claim())
    );
});

// Fetch — network first, fall back to cache, then offline page
self.addEventListener('fetch', event => {
    // Skip non-GET and non-HTTP requests
    if (event.request.method !== 'GET') return;
    if (!event.request.url.startsWith('http')) return;

    // Skip API calls — always go to network
    if (event.request.url.includes('/api/')) return;
    if (event.request.url.includes('/ws')) return;

    event.respondWith(
        fetch(event.request)
            .then(response => {
                // Cache a copy of successful responses
                if (response.ok) {
                    const clone = response.clone();
                    caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone));
                }
                return response;
            })
            .catch(() =>
                caches.match(event.request)
                    .then(cached => cached || caches.match(OFFLINE_URL))
            )
    );
});