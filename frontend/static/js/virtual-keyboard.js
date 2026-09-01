/* ===================================================================
   Mari-Fariyaad - Reusable Virtual Keyboard Component
   ---------------------------------------------------------------
   TWO SEPARATE, DECOUPLED STATES (do not merge these):
     1. currentLang            -> read from localStorage "gvp_lang"
                                   (same key main.js already owns).
                                   Controls website language AND which
                                   keyboard layout is shown.
     2. keyboardEnabled        -> read from localStorage "gvp_vk_enabled".
                                   Defaults to OFF. Controls ONLY whether
                                   the virtual keyboard is allowed to
                                   appear at all. Independent of language.

   The keyboard NEVER auto-opens just because an input received focus.
   It only opens when: keyboardEnabled === true AND a supported input
   is focused. The physical/device keyboard is never blocked, disabled,
   or interfered with, regardless of this setting.

   - Attaches to any <input type="text|search|email">/<textarea> marked
     data-virtual-keyboard="true"
   - Listens for "gvp:languagechange" (fired by switchLanguage() in
     main.js) so an already-open keyboard updates its layout instantly,
     no reload - but this NEVER opens the keyboard by itself.
   - Inserts characters at the exact caret position (not just at the end)
   - Unicode/grapheme-safe backspace (matras, conjuncts, combining marks)
   - Self-contained: injects its own <style>, floating ON/OFF toggle, and
     keyboard panel - a page only needs this one <script> tag.
   =================================================================== */

(function () {
  "use strict";

  const LANG_KEY = "gvp_lang";
  const ENABLED_KEY = "gvp_vk_enabled";
  const SUPPORTED_INPUT_TYPES = ["text", "search", "email", ""]; // "" = no type attr, defaults to text

  /* ===================== Keyboard Layouts ===================== */
  const KEY_LAYOUTS = {
    en: {
      label: "English",
      rows: [
        ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"],
        ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"],
        ["a", "s", "d", "f", "g", "h", "j", "k", "l"],
        ["z", "x", "c", "v", "b", "n", "m"],
      ],
      shiftable: true,
    },
    gu: {
      label: "ગુજરાતી",
      rows: [
        ["અ", "આ", "ઇ", "ઈ", "ઉ", "ઊ", "એ", "ઐ", "ઓ", "ઔ"],
        ["ક", "ખ", "ગ", "ઘ", "ઙ", "ચ", "છ", "જ", "ઝ", "ઞ"],
        ["ટ", "ઠ", "ડ", "ઢ", "ણ", "ત", "થ", "દ", "ધ", "ન"],
        ["પ", "ફ", "બ", "ભ", "મ", "ય", "ર", "લ", "વ", "શ"],
        ["ષ", "સ", "હ", "ળ", "ક્ષ", "જ્ઞ"],
        ["ા", "િ", "ી", "ુ", "ૂ", "ે", "ૈ", "ો", "ૌ", "ં", "ઃ", "્"],
      ],
      shiftable: false,
    },
    hi: {
      label: "हिन्दी",
      rows: [
        ["अ", "आ", "इ", "ई", "उ", "ऊ", "ए", "ऐ", "ओ", "औ"],
        ["क", "ख", "ग", "घ", "ङ", "च", "छ", "ज", "झ", "ञ"],
        ["ट", "ठ", "ड", "ढ", "ण", "त", "थ", "द", "ध", "न"],
        ["प", "फ", "ब", "भ", "म", "य", "र", "ल", "व", "श"],
        ["ष", "स", "ह", "ळ", "क्ष", "ज्ञ"],
        ["ा", "ि", "ी", "ु", "ू", "े", "ै", "ो", "ौ", "ं", "ः", "्"],
      ],
      shiftable: false,
    },
  };

  const SYMBOL_ROW = [".", ",", "?", "!", "@", "#", "$", "%", "&", "(", ")", "-", "_", "/"];

  /* ===================== Internal State ===================== */
  let activeInput = null;
  let currentLang = localStorage.getItem(LANG_KEY) || "gu";
  let keyboardEnabled = localStorage.getItem(ENABLED_KEY) === "true"; // OFF by default
  let shiftOn = false;
  let symbolsMode = false;
  let keyboardEl = null;
  let keysAreaEl = null;
  let titleEl = null;
  let toggleBtnEl = null;

  function isSupportedInput(el) {
    if (!el || !el.matches) return false;
    if (!el.matches('[data-virtual-keyboard="true"]')) return false;
    if (el.tagName === "TEXTAREA") return true;
    if (el.tagName === "INPUT") {
      const type = (el.getAttribute("type") || "").toLowerCase();
      return SUPPORTED_INPUT_TYPES.indexOf(type) !== -1;
    }
    return false;
  }

  /* ===================== Unicode-safe editing helpers ===================== */
  function insertAtCursor(input, text) {
    const start = input.selectionStart != null ? input.selectionStart : input.value.length;
    const end = input.selectionEnd != null ? input.selectionEnd : input.value.length;
    const val = input.value;
    input.value = val.slice(0, start) + text + val.slice(end);
    const newPos = start + text.length;
    input.setSelectionRange(newPos, newPos);
    input.focus();
    input.dispatchEvent(new Event("input", { bubbles: true }));
  }

  function graphemeSafeBackspace(input) {
    const start = input.selectionStart;
    const end = input.selectionEnd;
    const val = input.value;

    if (start == null) return;

    if (start !== end) {
      input.value = val.slice(0, start) + val.slice(end);
      input.setSelectionRange(start, start);
    } else if (start > 0) {
      let deleteFrom = start - 1;
      if (typeof Intl !== "undefined" && Intl.Segmenter) {
        const segmenter = new Intl.Segmenter(undefined, { granularity: "grapheme" });
        const segments = Array.from(segmenter.segment(val.slice(0, start)));
        if (segments.length) {
          deleteFrom = segments[segments.length - 1].index;
        }
      } else {
        const arr = Array.from(val.slice(0, start));
        arr.pop();
        deleteFrom = arr.join("").length;
      }
      input.value = val.slice(0, deleteFrom) + val.slice(start);
      input.setSelectionRange(deleteFrom, deleteFrom);
    }
    input.focus();
    input.dispatchEvent(new Event("input", { bubbles: true }));
  }

  /* ===================== Styles ===================== */
  function injectStyles() {
    if (document.getElementById("vk-styles")) return;
    const style = document.createElement("style");
    style.id = "vk-styles";
    style.textContent = `
      #vkToggleBtn {
        position: fixed;
        right: 16px; bottom: 16px;
        z-index: 1090;
        display: flex; align-items: center; gap: 6px;
        background: var(--gvp-card-bg, #fff);
        border: 1px solid var(--gvp-border, #e5e2d9);
        color: var(--gvp-text, #3d3d29);
        border-radius: 30px;
        padding: 8px 14px;
        font-size: .8rem; font-weight: 600;
        box-shadow: var(--shadow-md, 0 8px 24px rgba(0,0,0,.1));
        cursor: pointer;
      }
      #vkToggleBtn .vk-dot {
        width: 9px; height: 9px; border-radius: 50%;
        background: var(--status-rejected, #dc2626);
        display: inline-block;
      }
      #vkToggleBtn.vk-enabled .vk-dot { background: var(--status-resolved, #059669); }
      #vkPanel {
        position: fixed;
        left: 0; right: 0; bottom: 0;
        z-index: 1080;
        background: var(--gvp-card-bg, #fff);
        border-top: 1px solid var(--gvp-border, #e5e2d9);
        box-shadow: var(--shadow-lg, 0 -8px 24px rgba(0,0,0,.12));
        padding: 10px 12px 14px;
        font-family: 'Hind','Noto Sans Gujarati','Noto Sans Devanagari',sans-serif;
        transform: translateY(100%);
        transition: transform .22s ease;
        max-height: 46vh;
        overflow-y: auto;
      }
      #vkPanel.vk-open { transform: translateY(0); }
      #vkPanel .vk-header {
        display: flex; align-items: center; justify-content: space-between;
        margin-bottom: 8px;
      }
      #vkPanel .vk-title {
        font-weight: 600; font-size: .85rem; color: var(--gvp-text-muted, #8a8a6f);
      }
      #vkPanel .vk-controls { display: flex; gap: 6px; align-items: center; }
      #vkPanel .vk-btn-icon {
        border: 1px solid var(--gvp-border, #e5e2d9);
        background: transparent; border-radius: 8px;
        width: 30px; height: 30px; line-height: 1;
        color: var(--gvp-text, #3d3d29); cursor: pointer;
      }
      #vkPanel .vk-row { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; margin-bottom: 6px; }
      #vkPanel .vk-key {
        min-width: 38px; height: 42px; padding: 0 8px;
        display: flex; align-items: center; justify-content: center;
        background: var(--gvp-warm-bg, #f9f8f4);
        border: 1px solid var(--gvp-border, #e5e2d9);
        border-radius: 8px; cursor: pointer; user-select: none;
        font-size: 1rem; color: var(--gvp-text, #3d3d29);
        transition: background .15s ease, transform .1s ease;
      }
      #vkPanel .vk-key:hover { background: var(--gvp-border-hover, #dedbcf); }
      #vkPanel .vk-key:active { transform: scale(0.94); background: var(--gvp-accent-sand, #d4a373); }
      #vkPanel .vk-key.vk-wide { min-width: 90px; font-size: .8rem; font-weight: 600; }
      #vkPanel .vk-key.vk-space { flex: 1 1 auto; max-width: 320px; }
      #vkPanel .vk-key.vk-active { background: var(--gvp-saffron, #5a5a40); color: #fff; }
      @media (max-width: 576px) {
        #vkPanel .vk-key { min-width: 30px; height: 38px; font-size: .9rem; }
        #vkPanel { max-height: 52vh; }
        #vkToggleBtn { right: 10px; bottom: 10px; }
      }
    `;
    document.head.appendChild(style);
  }

  /* ===================== Floating ON/OFF toggle ===================== */
  function buildToggleButton() {
    if (toggleBtnEl) return;
    injectStyles();
    toggleBtnEl = document.createElement("button");
    toggleBtnEl.type = "button";
    toggleBtnEl.id = "vkToggleBtn";
    toggleBtnEl.addEventListener("click", () => setKeyboardEnabled(!keyboardEnabled));
    updateToggleButton();
    document.body.appendChild(toggleBtnEl);
  }

  function updateToggleButton() {
    if (!toggleBtnEl) return;
    toggleBtnEl.classList.toggle("vk-enabled", keyboardEnabled);
    toggleBtnEl.innerHTML =
      '<span class="vk-dot"></span> Virtual Keyboard: ' + (keyboardEnabled ? "ON" : "OFF");
    toggleBtnEl.setAttribute(
      "aria-label",
      "Virtual keyboard is " + (keyboardEnabled ? "on" : "off") + ". Click to toggle."
    );
  }

  function setKeyboardEnabled(value) {
    keyboardEnabled = !!value;
    localStorage.setItem(ENABLED_KEY, keyboardEnabled ? "true" : "false");
    updateToggleButton();
    if (!keyboardEnabled) {
      hideKeyboard();
    } else if (isSupportedInput(document.activeElement)) {
      showKeyboard(document.activeElement);
    }
  }

  window.setVirtualKeyboardEnabled = setKeyboardEnabled;
  window.isVirtualKeyboardEnabled = function () {
    return keyboardEnabled;
  };

  /* ===================== Keyboard panel ===================== */
  function buildPanel() {
    if (keyboardEl) return;
    injectStyles();

    keyboardEl = document.createElement("div");
    keyboardEl.id = "vkPanel";
    keyboardEl.setAttribute("role", "group");
    keyboardEl.setAttribute("aria-label", "Virtual Keyboard");

    const header = document.createElement("div");
    header.className = "vk-header";

    titleEl = document.createElement("span");
    titleEl.className = "vk-title";
    header.appendChild(titleEl);

    const controls = document.createElement("div");
    controls.className = "vk-controls";

    const toggleModeBtn = document.createElement("button");
    toggleModeBtn.type = "button";
    toggleModeBtn.className = "vk-btn-icon";
    toggleModeBtn.title = "Toggle letters / symbols";
    toggleModeBtn.setAttribute("aria-label", "Toggle letters and symbols");
    toggleModeBtn.textContent = "#+=";
    toggleModeBtn.addEventListener("mousedown", (e) => e.preventDefault());
    toggleModeBtn.addEventListener("click", () => {
      symbolsMode = !symbolsMode;
      render();
    });
    controls.appendChild(toggleModeBtn);

    const closeBtn = document.createElement("button");
    closeBtn.type = "button";
    closeBtn.className = "vk-btn-icon";
    closeBtn.title = "Close keyboard";
    closeBtn.setAttribute("aria-label", "Close virtual keyboard");
    closeBtn.innerHTML = "&#10005;";
    closeBtn.addEventListener("mousedown", (e) => e.preventDefault());
    // Closes the panel for this field only; does NOT flip the global toggle.
    closeBtn.addEventListener("click", hideKeyboard);
    controls.appendChild(closeBtn);

    header.appendChild(controls);
    keyboardEl.appendChild(header);

    keysAreaEl = document.createElement("div");
    keyboardEl.appendChild(keysAreaEl);

    document.body.appendChild(keyboardEl);
  }

  function makeKey(label, opts) {
    opts = opts || {};
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "vk-key" + (opts.extraClass ? " " + opts.extraClass : "");
    btn.textContent = opts.display != null ? opts.display : label;
    btn.setAttribute("aria-label", opts.ariaLabel || ("Letter " + label));
    btn.addEventListener("mousedown", (e) => e.preventDefault());
    btn.addEventListener("click", () => {
      if (!activeInput) return;
      if (opts.onClick) {
        opts.onClick();
      } else {
        insertAtCursor(activeInput, label);
      }
    });
    return btn;
  }

  function render() {
    if (!keysAreaEl) return;
    keysAreaEl.innerHTML = "";
    const layout = KEY_LAYOUTS[currentLang] || KEY_LAYOUTS.en;
    titleEl.textContent = layout.label + " Virtual Keyboard";

    if (symbolsMode) {
      const row = document.createElement("div");
      row.className = "vk-row";
      SYMBOL_ROW.forEach((sym) => row.appendChild(makeKey(sym)));
      keysAreaEl.appendChild(row);
    } else {
      layout.rows.forEach((rowChars) => {
        const row = document.createElement("div");
        row.className = "vk-row";
        rowChars.forEach((ch) => {
          const display = layout.shiftable && shiftOn ? ch.toUpperCase() : ch;
          row.appendChild(makeKey(display, { display, ariaLabel: (layout.label + " letter " + display) }));
        });
        keysAreaEl.appendChild(row);
      });
    }

    const bottom = document.createElement("div");
    bottom.className = "vk-row";

    if (layout.shiftable && !symbolsMode) {
      bottom.appendChild(
        makeKey("Shift", {
          display: "⇧ Shift",
          extraClass: "vk-wide" + (shiftOn ? " vk-active" : ""),
          ariaLabel: "Toggle uppercase",
          onClick: () => {
            shiftOn = !shiftOn;
            render();
          },
        })
      );
    }

    bottom.appendChild(
      makeKey("123", {
        display: symbolsMode ? "ABC" : "123",
        extraClass: "vk-wide",
        ariaLabel: "Toggle numbers and symbols",
        onClick: () => {
          symbolsMode = !symbolsMode;
          render();
        },
      })
    );

    bottom.appendChild(
      makeKey(" ", { display: "Space", extraClass: "vk-key vk-space", ariaLabel: "Space" })
    );

    bottom.appendChild(
      makeKey("Backspace", {
        display: "⌫ Backspace",
        extraClass: "vk-wide",
        ariaLabel: "Backspace",
        onClick: () => activeInput && graphemeSafeBackspace(activeInput),
      })
    );

    bottom.appendChild(
      makeKey("Enter", {
        display: "⏎ Enter",
        extraClass: "vk-wide",
        ariaLabel: "Enter",
        onClick: () => {
          if (!activeInput) return;
          if (activeInput.tagName === "TEXTAREA") {
            insertAtCursor(activeInput, "\n");
          } else if (activeInput.form) {
            if (activeInput.form.requestSubmit) activeInput.form.requestSubmit();
            else activeInput.form.submit();
          }
        },
      })
    );

    keysAreaEl.appendChild(bottom);
  }

  function showKeyboard(input) {
    // Guard belongs here too (not just at the call site) so no future
    // caller can bypass the enabled check.
    if (!keyboardEnabled) return;
    activeInput = input;
    buildPanel();
    shiftOn = false;
    symbolsMode = false;
    render();
    requestAnimationFrame(() => keyboardEl.classList.add("vk-open"));
  }

  function hideKeyboard() {
    if (keyboardEl) keyboardEl.classList.remove("vk-open");
    activeInput = null;
  }

  /* ===================== Wiring ===================== */
  // The ONLY place the keyboard is opened. Requires BOTH: the global
  // toggle is ON, and the focused element is a supported field. Focus
  // alone, on its own, never opens anything and never touches normal
  // typing/keydown/input behavior.
  document.addEventListener("focusin", (e) => {
    if (!keyboardEnabled) return;
    if (isSupportedInput(e.target)) showKeyboard(e.target);
  });

  document.addEventListener("mousedown", (e) => {
    if (!keyboardEl || !keyboardEl.classList.contains("vk-open")) return;
    const target = e.target;
    const insideKeyboard = keyboardEl.contains(target);
    const insideToggle = toggleBtnEl && toggleBtnEl.contains(target);
    if (!insideKeyboard && !insideToggle && !isSupportedInput(target)) hideKeyboard();
  });

  // Only re-renders an already-open keyboard; never opens one. Keeps
  // language and keyboard-enabled fully independent of each other.
  document.addEventListener("gvp:languagechange", (e) => {
    currentLang = (e.detail && e.detail.lang) || localStorage.getItem(LANG_KEY) || "gu";
    shiftOn = false;
    symbolsMode = false;
    if (keyboardEl && keyboardEl.classList.contains("vk-open")) render();
  });

  document.addEventListener("DOMContentLoaded", () => {
    currentLang = localStorage.getItem(LANG_KEY) || "gu";
    keyboardEnabled = localStorage.getItem(ENABLED_KEY) === "true";
    buildToggleButton();
  });
})();
