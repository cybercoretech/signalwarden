import { access, readFile, stat } from 'node:fs/promises';
import { resolve } from 'node:path';

const siteRoot = resolve('site');
const html = await readFile(resolve(siteRoot, 'index.html'), 'utf8');
const iconLinks = [...html.matchAll(/<link\b[^>]*\brel=["'](?:icon|apple-touch-icon|manifest)["'][^>]*>/gi)];
const required = ['assets/favicon.svg', 'assets/favicon-32x32.png', 'assets/favicon.ico', 'assets/apple-touch-icon.png', 'site.webmanifest'];

if (iconLinks.length < required.length) {
  throw new Error(`Expected ${required.length} browser icon links, found ${iconLinks.length}`);
}

for (const asset of required) {
  await access(resolve(siteRoot, asset));
  const { size } = await stat(resolve(siteRoot, asset));
  if (size === 0) throw new Error(`Browser icon asset is empty: ${asset}`);
}

const manifest = JSON.parse(await readFile(resolve(siteRoot, 'site.webmanifest'), 'utf8'));
for (const icon of manifest.icons ?? []) {
  await access(resolve(siteRoot, icon.src));
}

console.log(`Verified ${required.length} browser icon assets and ${manifest.icons?.length ?? 0} manifest icon(s).`);
