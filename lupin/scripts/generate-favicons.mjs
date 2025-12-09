import sharp from 'sharp';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const publicDir = join(__dirname, '..', 'public');
const sourceImage = join(publicDir, 'Lupin.webp');

const sizes = [
  { name: 'favicon-16x16.png', size: 16 },
  { name: 'favicon-32x32.png', size: 32 },
  { name: 'android-icon-36x36.png', size: 36 },
  { name: 'android-icon-48x48.png', size: 48 },
  { name: 'android-icon-72x72.png', size: 72 },
  { name: 'android-icon-96x96.png', size: 96 },
  { name: 'android-icon-144x144.png', size: 144 },
  { name: 'android-icon-192x192.png', size: 192 },
  { name: 'apple-icon-180x180.png', size: 180 },
];

async function generateFavicons() {
  console.log('Generating favicons from Lupin.webp...');

  for (const { name, size } of sizes) {
    await sharp(sourceImage)
      .resize(size, size, { fit: 'contain', background: { r: 255, g: 255, b: 255, alpha: 0 } })
      .png()
      .toFile(join(publicDir, name));
    console.log(`  Created: ${name}`);
  }

  console.log('Done!');
}

generateFavicons().catch(console.error);
