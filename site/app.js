const root = document.documentElement;
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

const signalDialog = document.querySelector("#signalDialog");
const dialogTitle = document.querySelector("#dialogTitle");
const dialogContext = document.querySelector("#dialogContext");
const defaultDialogTitle = "How the check runs.";
const defaultDialogContext = "This is an explainer, not a live scan. A test begins only when you tap the diagnostic button in SignalWarden.";
let dialogOpener;

document.querySelectorAll("[data-open-signal-dialog]").forEach((trigger) => {
  trigger.addEventListener("click", () => {
    dialogOpener = trigger;
    dialogTitle.textContent = trigger.dataset.detailTitle || defaultDialogTitle;
    dialogContext.textContent = trigger.dataset.detailText || defaultDialogContext;
    signalDialog.showModal();
  });
});

signalDialog.querySelector(".dialog-close").addEventListener("click", () => signalDialog.close());
signalDialog.addEventListener("click", (event) => {
  if (event.target === signalDialog) signalDialog.close();
});
signalDialog.addEventListener("close", () => dialogOpener?.focus());

if (!matchMedia("(prefers-reduced-motion: reduce)").matches && "IntersectionObserver" in window) {
  const revealTargets = document.querySelectorAll(".section-head, .feature, .phase-list article, .privacy-card, .terminal, .trust-strip > div");
  const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add("is-visible");
      observer.unobserve(entry.target);
    });
  }, { threshold: 0.12, rootMargin: "0px 0px -35px 0px" });
  revealTargets.forEach((target, index) => {
    target.classList.add("reveal-on-scroll");
    target.style.transitionDelay = `${Math.min(index % 4, 3) * 65}ms`;
    revealObserver.observe(target);
  });
}
