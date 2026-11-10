// SPDX-License-Identifier: AGPL-3.0-only
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import type { ParcelData, ReproEvent, Semantics } from "./parcel.js";

export function escapeHtml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function renderEvents(events: ReproEvent[]): string {
  if (events.length === 0) return "<p class=\"muted\">No events collected.</p>";
  const rows = events
    .map(
      (event) => `<tr>
        <td>${event.sequence}</td>
        <td>${event.offsetMs}</td>
        <td>${escapeHtml(event.category)}</td>
        <td>${escapeHtml(event.actionId ?? "")}</td>
        <td>${escapeHtml(event.screenId ?? "")}</td>
        <td>${escapeHtml(event.outcome ?? "")}</td>
        <td><code>${escapeHtml(JSON.stringify(event.attributes ?? {}))}</code></td>
      </tr>`,
    )
    .join("\n");
  return `<table><thead><tr><th>Seq</th><th>Offset ms</th><th>Category</th><th>Action</th><th>Screen</th><th>Outcome</th><th>Attributes</th></tr></thead><tbody>${rows}</tbody></table>`;
}

function renderSemantics(semantics: Semantics | null): string {
  if (!semantics) return "<p class=\"muted\">No semantic evidence.</p>";
  const rows = semantics.nodes
    .map(
      (node) => `<li>
        <strong>${escapeHtml(node.role)}</strong>
        ${node.label ? ` — ${escapeHtml(node.label)}` : ""}
        ${node.testTag ? ` <code>${escapeHtml(node.testTag)}</code>` : ""}
        <span class="muted">[${node.bounds.left},${node.bounds.top} ${node.bounds.right},${node.bounds.bottom}]</span>
      </li>`,
    )
    .join("\n");
  return `<ul>${rows}</ul>${semantics.truncated ? "<p class=\"warn\">Semantic evidence was truncated.</p>" : ""}`;
}

export async function renderParcel(parcel: ParcelData, outputDirectory: string): Promise<void> {
  await mkdir(outputDirectory, { recursive: true });
  if (parcel.imagePng) {
    await writeFile(path.join(outputDirectory, "screen.png"), parcel.imagePng);
  }
  const manifest = parcel.manifest;
  const html = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src 'self'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'">
<title>ReproParcel ${escapeHtml(manifest.reportId)}</title>
<style>
:root { color-scheme: light dark; }
body { font-family: system-ui, sans-serif; max-width: 1000px; margin: 2rem auto; padding: 0 1rem; line-height: 1.45; }
h1, h2 { line-height: 1.2; }
table { border-collapse: collapse; width: 100%; margin: 1rem 0; }
th, td { border: 1px solid #8888; padding: .4rem .5rem; vertical-align: top; text-align: left; }
code { white-space: pre-wrap; overflow-wrap: anywhere; }
.muted { opacity: .75; }
.warn { border-left: 4px solid #b26a00; padding-left: .75rem; }
.card { border: 1px solid #8886; border-radius: 10px; padding: 1rem; margin: 1rem 0; }
img { max-width: 100%; height: auto; border: 1px solid #8886; border-radius: 8px; }
</style>
</head>
<body>
<h1>ReproParcel report</h1>
<div class="card">
<p><strong>Report ID:</strong> <code>${escapeHtml(manifest.reportId)}</code></p>
<p><strong>Created:</strong> ${escapeHtml(manifest.createdAt)}</p>
<p><strong>App:</strong> ${escapeHtml(manifest.app.packageName)} ${escapeHtml(manifest.app.versionName)} (${escapeHtml(manifest.app.variant)})</p>
<p><strong>Device:</strong> ${escapeHtml(manifest.device.manufacturer)} ${escapeHtml(manifest.device.model)}, API ${manifest.device.apiLevel}, ${manifest.device.windowWidthPx}×${manifest.device.windowHeightPx}</p>
<p><strong>Privacy policy:</strong> ${escapeHtml(manifest.privacy.policyVersion)} / ${escapeHtml(manifest.privacy.redactionMode)}</p>
<p class="muted">This page renders report content as untrusted text. It does not execute scripts or load external URLs.</p>
</div>

<h2>Description</h2>
<p>${escapeHtml(parcel.report.description)}</p>
${parcel.report.expectedBehavior ? `<h2>Expected behavior</h2><p>${escapeHtml(parcel.report.expectedBehavior)}</p>` : ""}
${parcel.report.actualBehavior ? `<h2>Actual behavior</h2><p>${escapeHtml(parcel.report.actualBehavior)}</p>` : ""}
${parcel.report.reproSteps ? `<h2>Steps to reproduce</h2><p>${escapeHtml(parcel.report.reproSteps)}</p>` : ""}

${parcel.imagePng ? `<h2>Approved image</h2><p><img src="screen.png" alt="Approved sanitized screenshot"></p>` : `<h2>Image</h2><p class="muted">No approved image is present.</p>`}

<h2>Events</h2>
${renderEvents(parcel.events)}

<h2>Semantics</h2>
${renderSemantics(parcel.semantics)}

<h2>Evidence status</h2>
<ul>${(parcel.report.evidence ?? []).map((e) => `<li>${escapeHtml(e.state)}${e.reason ? ` — ${escapeHtml(e.reason)}` : ""}${e.detail ? ` (${escapeHtml(e.detail)})` : ""}</li>`).join("") || "<li class=\"muted\">No statuses recorded.</li>"}</ul>
</body>
</html>`;
  await writeFile(path.join(outputDirectory, "index.html"), html, "utf8");
}
