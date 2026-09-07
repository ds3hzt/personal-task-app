const CACHE_NAME = 'personal-task-app-v1';

const APP_FILES = [
  './',
  './index.html',
  './config.js',
  './manifest.webmanifest',
  './icon.svg'
];

self.addEventListener('install', function(event) {
  event.waitUntil(
    caches.open(CACHE_NAME).then(function(cache) {
      return cache.addAll(APP_FILES);
    })
  );

  self.skipWaiting();
});

self.addEventListener('activate', function(event) {
  event.waitUntil(
    caches.keys().then(function(cacheNames) {
      return Promise.all(
        cacheNames.map(function(cacheName) {
          if (cacheName !== CACHE_NAME) {
            return caches.delete(cacheName);
          }
        })
      );
    })
  );

  self.clients.claim();
});

self.addEventListener('fetch', function(event) {
  if (event.request.method !== 'GET') {
    return;
  }

  event.respondWith(
    fetch(event.request)
      .then(function(response) {
        const copy = response.clone();

        caches.open(CACHE_NAME)
          .then(function(cache) {
            cache.put(event.request, copy);
          });

        return response;
      })
      .catch(function() {
        return caches.match(event.request);
      })
  );
});
