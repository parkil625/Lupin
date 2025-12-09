import sharp from 'sharp';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const publicDir = join(__dirname, '..', 'public');

// 소셜 로그인 로고 최적화: 40x40 표시 → 80x80 생성 (2x retina 대응)
const logos = [
  { name: 'kakao-logo.png', size: 80 },
  { name: 'google-logo.png', size: 80 },
  { name: 'naver-logo.png', size: 80 },
];

async function optimizeLogos() {
  console.log('Optimizing social login logos...\n');

  for (const { name, size } of logos) {
    const inputPath = join(publicDir, name);
    const outputPath = join(publicDir, name);

    // 원본 파일 정보
    const metadata = await sharp(inputPath).metadata();
    console.log(`${name}:`);
    console.log(`  Before: ${metadata.width}x${metadata.height}`);

    // 리사이즈 및 최적화
    await sharp(inputPath)
      .resize(size, size, { fit: 'contain', background: { r: 255, g: 255, b: 255, alpha: 0 } })
      .png({ quality: 90, compressionLevel: 9 })
      .toFile(join(publicDir, `${name}.tmp`));

    // 임시 파일을 원본으로 교체
    const fs = await import('fs/promises');
    await fs.rename(join(publicDir, `${name}.tmp`), outputPath);

    // 새 파일 정보
    const newMetadata = await sharp(outputPath).metadata();
    const stats = await fs.stat(outputPath);
    console.log(`  After: ${newMetadata.width}x${newMetadata.height} (${(stats.size / 1024).toFixed(1)} KiB)\n`);
  }

  console.log('Done! Social logos optimized to 80x80 for retina displays.');
}

optimizeLogos().catch(console.error);
