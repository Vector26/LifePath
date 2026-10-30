/**
 * Garden Game Bootstrap — auth-free initialization for embedding in a React app.
 *
 * Load this script AFTER game.js. It neutralizes the DOMContentLoaded auth
 * flow and exposes window.initGardenGame() for the host app to call once the
 * DOM is ready.
 */
(function () {
  'use strict';

  // ── 1. Prevent the game.js DOMContentLoaded handler from redirecting ──────
  //
  // game.js registers a DOMContentLoaded listener that:
  //   a) checks localStorage for 'garden_game_token'
  //   b) fetches /api/auth/verify
  //   c) redirects to /login on failure
  //
  // We satisfy (a) with a dummy token and intercept (b) by patching fetch so
  // the verify call succeeds. After the handler runs and creates MenuSystem we
  // clean everything up.

  var FAKE_TOKEN = '__garden_bootstrap__';
  var hadToken = localStorage.getItem('garden_game_token');
  localStorage.setItem('garden_game_token', FAKE_TOKEN);

  var _origFetch = window.fetch;
  window.fetch = function (url, opts) {
    if (typeof url === 'string' && url.indexOf('/api/auth/verify') !== -1) {
      return Promise.resolve(new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      }));
    }
    return _origFetch.apply(this, arguments);
  };

  // Restore fetch + token once the DOMContentLoaded handler has finished.
  // We wait one microtask tick after DOMContentLoaded so the .then() chain
  // inside game.js has resolved.
  window.addEventListener('DOMContentLoaded', function _bootstrapCleanup() {
    window.removeEventListener('DOMContentLoaded', _bootstrapCleanup);
    Promise.resolve().then(function () {
      window.fetch = _origFetch;
      if (hadToken === null) {
        localStorage.removeItem('garden_game_token');
      } else {
        localStorage.setItem('garden_game_token', hadToken);
      }
    });
  });

  // ── 2. Inject styles to hide unwanted UI ──────────────────────────────────

  var style = document.createElement('style');
  style.textContent = [
    '.multiplayer-panel { display: none !important; }',
    '#adminBtn { display: none !important; }',
    '#logoutBtn { display: none !important; }',
    '#accountBtn { display: none !important; }',
    '#supportBtn { display: none !important; }',
    '#closeAdminBtn { display: none !important; }',
    '.save-slots { display: none !important; }',
    '.menu-buttons { display: none !important; }'
  ].join('\n');
  document.head.appendChild(style);

  // ── 2b. Mobile tab bar for sidebar sections ──────────────────────────────
  function isMobile() { return window.innerWidth <= 768; }

  function injectMobileTabBar() {
    var sidebar = document.querySelector('.sidebar');
    if (!sidebar || sidebar.querySelector('.mobile-tab-bar')) return;
    if (!isMobile()) return;

    var tabs = [
      { id: 'seeds', label: '🌱 Seeds', sections: ['.seed-shop'] },
      { id: 'supplies', label: '💧 Supplies', sections: ['.purchase-shop', '.sprinkler-shop'] },
      { id: 'tools', label: '🛠️ Tools', sections: ['.tools'] },
      { id: 'decor', label: '🌸 Decor', sections: ['.decoration-shop'] },
      { id: 'more', label: '📊 More', sections: ['.achievements-section', '.challenges-section', '.stats-section', '.instructions'] }
    ];

    var bar = document.createElement('div');
    bar.className = 'mobile-tab-bar';

    var allSections = [];
    tabs.forEach(function(tab) {
      tab.sections.forEach(function(sel) {
        var el = sidebar.querySelector(sel);
        if (el) allSections.push(el);
      });
    });

    function showTab(activeId) {
      allSections.forEach(function(el) { el.style.display = 'none'; });
      bar.querySelectorAll('button').forEach(function(btn) { btn.classList.remove('active'); });

      tabs.forEach(function(tab) {
        if (tab.id === activeId) {
          tab.sections.forEach(function(sel) {
            var el = sidebar.querySelector(sel);
            if (el) el.style.display = '';
          });
        }
      });

      var activeBtn = bar.querySelector('[data-tab="' + activeId + '"]');
      if (activeBtn) activeBtn.classList.add('active');
    }

    tabs.forEach(function(tab) {
      var btn = document.createElement('button');
      btn.textContent = tab.label;
      btn.setAttribute('data-tab', tab.id);
      btn.addEventListener('click', function() { showTab(tab.id); });
      bar.appendChild(btn);
    });

    sidebar.insertBefore(bar, sidebar.firstChild);
    showTab('seeds');
  }

  window.addEventListener('DOMContentLoaded', function() {
    setTimeout(injectMobileTabBar, 300);
  });
  window.addEventListener('resize', function() {
    var bar = document.querySelector('.mobile-tab-bar');
    if (isMobile()) {
      if (!bar) injectMobileTabBar();
    } else if (bar) {
      bar.remove();
      document.querySelectorAll('.sidebar > *').forEach(function(el) { el.style.display = ''; });
    }
  });

  // ── 3. Expose window.initGardenGame() ─────────────────────────────────────
  //
  // The host React app should call this after the garden DOM is mounted.
  // It waits for MenuSystem to be available (created by the DOMContentLoaded
  // handler) then auto-starts slot 1.

  window.initGardenGame = function initGardenGame(slot) {
    slot = slot || 1;

    function boot() {
      var menuSystem = window.menuSystem;
      if (!menuSystem) {
        console.error('[garden-bootstrap] window.menuSystem not found — is game.js loaded?');
        return;
      }

      var mainMenu = document.getElementById('mainMenu');
      var gameContainer = document.getElementById('gameContainer');

      var saveKey = 'gardenGameSave_' + slot;
      if (localStorage.getItem(saveKey)) {
        menuSystem.loadGame(slot);
      } else {
        menuSystem.startNewGame(slot);
      }

      if (mainMenu) mainMenu.style.display = 'none';
      if (gameContainer) gameContainer.style.display = 'block';
    }

    if (window.menuSystem) {
      boot();
    } else {
      // MenuSystem may not be ready yet — poll briefly.
      var attempts = 0;
      var poll = setInterval(function () {
        attempts++;
        if (window.menuSystem) {
          clearInterval(poll);
          boot();
        } else if (attempts > 50) {
          clearInterval(poll);
          console.error('[garden-bootstrap] Timed out waiting for MenuSystem');
        }
      }, 100);
    }
  };
})();
