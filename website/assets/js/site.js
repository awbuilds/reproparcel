// SPDX-License-Identifier: Apache-2.0
// ReproParcel website — theme, navigation and copy controls. No tracking.

(function () {
  "use strict";

  var root = document.documentElement;
  var STORAGE_KEY = "reproparcel-theme";

  function storedTheme() {
    try {
      var value = window.localStorage.getItem(STORAGE_KEY);
      return value === "light" || value === "dark" ? value : null;
    } catch (error) {
      return null;
    }
  }

  function systemTheme() {
    try {
      return window.matchMedia("(prefers-color-scheme: light)").matches
        ? "light"
        : "dark";
    } catch (error) {
      return "dark";
    }
  }

  function applyTheme(theme) {
    root.setAttribute("data-theme", theme);
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) {
      meta.setAttribute("content", theme === "light" ? "#f7f8fc" : "#070c16");
    }
    var toggle = document.querySelector("[data-theme-toggle]");
    if (toggle) {
      var isLight = theme === "light";
      toggle.setAttribute("aria-pressed", String(isLight));
      toggle.setAttribute(
        "aria-label",
        isLight ? "Switch to dark theme" : "Switch to light theme"
      );
    }
  }

  applyTheme(storedTheme() || systemTheme());
  root.classList.add("js");

  function onReady(callback) {
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", callback, { once: true });
    } else {
      callback();
    }
  }

  onReady(function () {
    var toggle = document.querySelector("[data-theme-toggle]");
    if (toggle) {
      toggle.addEventListener("click", function () {
        var next = root.getAttribute("data-theme") === "light" ? "dark" : "light";
        applyTheme(next);
        try {
          window.localStorage.setItem(STORAGE_KEY, next);
        } catch (error) {
          /* Storage may be unavailable; the theme still applies for this view. */
        }
      });
    }

    var navButton = document.querySelector("[data-nav-toggle]");
    var nav = document.getElementById("site-nav");
    if (navButton && nav) {
      navButton.addEventListener("click", function () {
        var open = nav.classList.toggle("is-open");
        navButton.setAttribute("aria-expanded", String(open));
      });
    }

    var copyButtons = document.querySelectorAll("[data-copy]");
    Array.prototype.forEach.call(copyButtons, function (button) {
      button.addEventListener("click", function () {
        var target = document.getElementById(button.getAttribute("data-copy"));
        if (!target) {
          return;
        }
        var text = target.innerText;
        var done = function () {
          var label = button.textContent;
          button.textContent = "Copied";
          window.setTimeout(function () {
            button.textContent = label;
          }, 1600);
        };
        try {
          if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(done, function () {
              button.textContent = "Copy failed";
            });
            return;
          }
        } catch (error) {
          /* Fall through to the selection fallback. */
        }
        try {
          var range = document.createRange();
          range.selectNodeContents(target);
          var selection = window.getSelection();
          selection.removeAllRanges();
          selection.addRange(range);
          document.execCommand("copy");
          selection.removeAllRanges();
          done();
        } catch (error) {
          button.textContent = "Copy failed";
        }
      });
    });
  });
})();
