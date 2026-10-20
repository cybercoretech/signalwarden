const root = document.documentElement;
const picker = document.querySelector("#themeSelect");
const button = document.querySelector("#themeButton");
const menu = document.querySelector("#themeMenu");
const themeLabels = {
  "cybercore-tech": "CYBERCORE TECH / MASTER MIX", cyberdyne: "CYBERDYNE",
  cyberpunk: "CYBERPUNK", default: "DEFAULT / CLASSICS", dystopian: "DYSTOPIAN",
  neosynth: "NEOSYNTH", "omarchy-live": "OMARCHY LIVE", synthwave: "SYNTHWAVE",
};
const catalogUrl = "https://cybercoretech.github.io/data/cybergrid.json";
const paletteCache = new Map();
const displayName = (name) => name.replaceAll("-", " ").toUpperCase();

function syncMenu() {
  button.textContent = picker.selectedOptions[0]?.textContent || "THEMES OFFLINE";
  menu.querySelectorAll(".theme-option").forEach((item) => {
    item.setAttribute("aria-selected", String(item.dataset.value === picker.value));
  });
}

function closeMenu() {
  menu.hidden = true;
  button.setAttribute("aria-expanded", "false");
}

function buildMenu() {
  menu.replaceChildren();
  for (const group of picker.querySelectorAll("optgroup")) {
    const label = document.createElement("div");
    label.className = "theme-group-label";
    label.textContent = group.label;
    menu.append(label);
    for (const source of group.querySelectorAll("option")) {
      const item = document.createElement("button");
      item.type = "button";
      item.className = "theme-option";
      item.setAttribute("role", "option");
      item.dataset.value = source.value;
      item.textContent = source.textContent;
      item.addEventListener("click", () => {
        picker.value = source.value;
        picker.dispatchEvent(new Event("change"));
        closeMenu();
        button.focus();
      });
      menu.append(item);
    }
  }
  syncMenu();
}

async function getPalette(name, family) {
  const key = `${family}/${name}`;
  if (!paletteCache.has(key)) {
    const url = `https://cybercoretech.github.io/data/themes/${encodeURIComponent(family)}/${encodeURIComponent(name)}.json`;
    paletteCache.set(key, fetch(url).then((response) => {
      if (!response.ok) throw new Error(`Palette request failed (${response.status})`);
      return response.json();
    }));
  }
  return paletteCache.get(key);
}

function applyPalette(name, palette) {
  const variables = {
    bg: "--bg", white: "--white", acid_green: "--acid", hot_pink: "--pink",
    purple: "--purple", cyan: "--cyan", orange: "--orange", red: "--red",
    panel: "--panel", line: "--line", muted: "--muted",
  };
  for (const [key, variable] of Object.entries(variables)) {
    if (palette[key]) root.style.setProperty(variable, `#${palette[key].replace(/^#/, "")}`);
  }
  root.dataset.theme = name;
  try { localStorage.setItem("signalwarden-site-theme", name); } catch { /* Storage may be disabled. */ }
  syncMenu();
}

function fallbackTheme() {
  const group = document.createElement("optgroup");
  group.label = "CYBERCORE TECH";
  const option = document.createElement("option");
  option.value = "cybercore-tech";
  option.textContent = "CYBERCORE TECH";
  option.dataset.family = "cybercore-tech";
  group.append(option);
  picker.replaceChildren(group);
  picker.value = "cybercore-tech";
  buildMenu();
}

async function startThemeChooser() {
  const response = await fetch(catalogUrl);
  if (!response.ok) throw new Error(`Theme catalog request failed (${response.status})`);
  const catalog = await response.json();
  for (const [family, names] of Object.entries(catalog.families)) {
    const group = document.createElement("optgroup");
    group.label = themeLabels[family] || family.toUpperCase();
    for (const name of names) {
      const option = document.createElement("option");
      option.value = name;
      option.textContent = displayName(name);
      option.dataset.family = family;
      group.append(option);
    }
    picker.append(group);
  }
  buildMenu();
  const params = new URLSearchParams(location.search);
  let saved;
  try { saved = localStorage.getItem("signalwarden-site-theme"); } catch { /* Use catalog default. */ }
  const requested = params.get("theme") || saved || catalog.active;
  const match = Object.entries(catalog.families).find(([, names]) => names.includes(requested));
  const [family, names] = match || Object.entries(catalog.families).find(([, names]) => names.includes(catalog.active)) || [];
  if (!family || !names) throw new Error("Active theme is missing from the theme catalog");
  picker.value = match ? requested : catalog.active;
  const selected = picker.selectedOptions[0];
  applyPalette(picker.value, await getPalette(picker.value, selected.dataset.family || family));
  picker.addEventListener("change", async () => {
    const option = picker.selectedOptions[0];
    try {
      applyPalette(option.value, await getPalette(option.value, option.dataset.family));
      history.replaceState(null, "", `?theme=${encodeURIComponent(option.value)}`);
    } catch (error) {
      console.error("Cybercore theme could not load:", error);
      syncMenu();
    }
  });
}

button.addEventListener("click", () => {
  const opening = menu.hidden;
  menu.hidden = !opening;
  button.setAttribute("aria-expanded", String(opening));
  if (opening) menu.querySelector('[aria-selected="true"]')?.focus();
});
menu.addEventListener("keydown", (event) => {
  const options = [...menu.querySelectorAll(".theme-option")];
  const current = options.indexOf(document.activeElement);
  if (event.key === "ArrowDown" || event.key === "ArrowUp") {
    event.preventDefault();
    options[(current + (event.key === "ArrowDown" ? 1 : options.length - 1)) % options.length]?.focus();
  } else if (event.key === "Home" || event.key === "End") {
    event.preventDefault();
    options[event.key === "Home" ? 0 : options.length - 1]?.focus();
  }
});
document.addEventListener("pointerdown", (event) => {
  if (!event.target.closest(".theme-picker")) closeMenu();
});
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && !menu.hidden) {
    closeMenu();
    button.focus();
  }
});
startThemeChooser().catch((error) => {
  console.error("Cybercore themes could not load:", error);
  fallbackTheme();
});

if (!matchMedia("(prefers-reduced-motion: reduce)").matches) {
  window.addEventListener("pointermove", (event) => {
    root.style.setProperty("--x", `${event.clientX}px`);
    root.style.setProperty("--y", `${event.clientY}px`);
  }, { passive: true });
}

const copyButton = document.querySelector("[data-copy-command]");
copyButton.addEventListener("click", async () => {
  const command = "git clone https://github.com/cybercoretech/signalwarden.git\ncd signalwarden\n./gradlew :app:assembleDebug :app:lintDebug";
  const status = document.querySelector("#copyStatus");
  try {
    await navigator.clipboard.writeText(command);
    status.textContent = "Copied to clipboard.";
  } catch {
    const field = document.createElement("textarea");
    field.value = command;
    field.setAttribute("readonly", "");
    field.style.position = "fixed";
    field.style.opacity = "0";
    document.body.append(field);
    field.select();
    const copied = document.execCommand("copy");
    field.remove();
    status.textContent = copied ? "Copied to clipboard." : "Copy failed. Select the commands above.";
  }
  window.setTimeout(() => { status.textContent = ""; }, 2500);
});
