const sharp = require('sharp');
const piexif = require('piexifjs');
const fs = require('fs');
const path = require('path');

// 테스트 이미지 출력 디렉토리
const outputDir = path.join(__dirname, '..', 'test-images');

// 디렉토리 생성
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

// EXIF DateTimeOriginal 형식으로 변환 (YYYY:MM:DD HH:MM:SS)
function formatExifDateTime(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}:${pad(date.getMonth() + 1)}:${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

// 사람이 읽기 쉬운 날짜 형식
function formatDisplayDateTime(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

// 파일명용 시간 형식 (HHmm)
function formatFileTime(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(date.getHours())}${pad(date.getMinutes())}`;
}

// 파일명용 날짜 형식 (MMdd)
function formatFileDate(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(date.getMonth() + 1)}${pad(date.getDate())}`;
}

// SVG 텍스트 오버레이 생성
function createTextOverlay(text, label, width, height, bgColor) {
  // 배경색에 따라 텍스트 색상 결정 (밝으면 검정, 어두우면 흰색)
  const brightness = (bgColor.r * 299 + bgColor.g * 587 + bgColor.b * 114) / 1000;
  const textColor = brightness > 128 ? '#000000' : '#FFFFFF';

  return Buffer.from(`
    <svg width="${width}" height="${height}">
      <rect width="100%" height="100%" fill="rgba(0,0,0,0.3)"/>
      <text x="50%" y="35%" font-family="Arial, sans-serif" font-size="28" font-weight="bold"
            fill="${textColor}" text-anchor="middle" dominant-baseline="middle">${label}</text>
      <text x="50%" y="65%" font-family="Arial, sans-serif" font-size="22"
            fill="${textColor}" text-anchor="middle" dominant-baseline="middle">${text}</text>
    </svg>
  `);
}

// EXIF 데이터가 포함된 JPEG 이미지 생성 (텍스트 오버레이 포함)
async function createImageWithExif(filename, dateTime, color, label) {
  const width = 400;
  const height = 300;
  const displayText = formatDisplayDateTime(dateTime);

  // 1. 배경색 이미지 생성
  const baseImage = await sharp({
    create: {
      width,
      height,
      channels: 3,
      background: color
    }
  }).png().toBuffer();

  // 2. 텍스트 오버레이 합성
  const textOverlay = createTextOverlay(displayText, label, width, height, color);

  const compositeImage = await sharp(baseImage)
    .composite([{
      input: textOverlay,
      top: 0,
      left: 0
    }])
    .jpeg({ quality: 95 })
    .toBuffer();

  // 3. EXIF 데이터 생성
  const exifObj = {
    '0th': {},
    'Exif': {
      [piexif.ExifIFD.DateTimeOriginal]: formatExifDateTime(dateTime),
      [piexif.ExifIFD.DateTimeDigitized]: formatExifDateTime(dateTime),
    },
    'GPS': {},
    '1st': {},
    'thumbnail': null
  };

  const exifBytes = piexif.dump(exifObj);

  // 4. 이미지에 EXIF 삽입
  const imageData = compositeImage.toString('binary');
  const newImageData = piexif.insert(exifBytes, imageData);
  const newImageBuffer = Buffer.from(newImageData, 'binary');

  // 5. 파일 저장
  const outputPath = path.join(outputDir, filename);
  fs.writeFileSync(outputPath, newImageBuffer);

  return { path: outputPath, dateTime: formatExifDateTime(dateTime), displayTime: displayText };
}

// 운동 점수 계산 (백엔드 로직과 동일)
const INTENSITY = {
  '산책': 0.5, '스트레칭': 0.5, '요가': 0.5,
  '자전거': 0.7, '등산': 0.8, '헬스': 0.8,
  '배드민턴': 0.9, '테니스': 0.9, '축구': 1.0,
  '농구': 1.0, '수영': 1.0, '달리기': 1.5,
  'HIIT': 1.7, '크로스핏': 1.7
};

function calculateScore(activity, durationMinutes) {
  const intensity = INTENSITY[activity] || 1.0;
  return Math.min(30, Math.round(durationMinutes * intensity));
}

async function main() {
  console.log('===========================================');
  console.log('  EXIF 테스트 이미지 생성기');
  console.log('===========================================\n');

  const baseDate = new Date();
  baseDate.setHours(10, 0, 0, 0);

  // 테스트 시나리오 정의
  const scenarios = [
    // 정상 케이스들
    { name: '30min', duration: 30, color: { r: 76, g: 175, b: 80 }, desc: '30분 운동' },
    { name: '1hour', duration: 60, color: { r: 33, g: 150, b: 243 }, desc: '1시간 운동' },
    { name: '2hour', duration: 120, color: { r: 156, g: 39, b: 176 }, desc: '2시간 운동' },
    { name: '3hour', duration: 180, color: { r: 255, g: 152, b: 0 }, desc: '3시간 운동' },
    // 에러 케이스
    { name: 'invalid', duration: -30, color: { r: 244, g: 67, b: 54 }, desc: '잘못된 순서 (에러)' },
    // 과거 사진 (당일 검증 실패)
    { name: 'old', duration: 60, color: { r: 158, g: 158, b: 158 }, desc: '과거 사진 (에러)', daysAgo: 7 },
  ];

  console.log('생성된 테스트 이미지:\n');

  for (const scenario of scenarios) {
    const startTime = new Date(baseDate);
    const endTime = new Date(baseDate);

    // 과거 날짜 시나리오
    if (scenario.daysAgo) {
      startTime.setDate(startTime.getDate() - scenario.daysAgo);
      endTime.setDate(endTime.getDate() - scenario.daysAgo);
    }

    if (scenario.duration > 0) {
      endTime.setMinutes(endTime.getMinutes() + scenario.duration);
    } else {
      // 잘못된 케이스: 시작이 끝보다 늦음
      startTime.setMinutes(startTime.getMinutes() + Math.abs(scenario.duration));
    }

    // 파일명: [날짜-]시작시간-끝시간-start/end.jpg
    // 과거 사진은 날짜 포함 (예: 1124-1000-1100-start.jpg)
    // 당일 사진은 시간만 (예: 1000-1030-start.jpg)
    const startTimeStr = formatFileTime(startTime);
    const endTimeStr = formatFileTime(endTime);
    const datePrefix = scenario.daysAgo ? `${formatFileDate(startTime)}-` : '';
    const timeRange = `${datePrefix}${startTimeStr}-${endTimeStr}`;

    const startResult = await createImageWithExif(
      `${timeRange}-start.jpg`,
      startTime,
      scenario.color,
      '🏃 운동 시작'
    );

    const endColor = {
      r: Math.max(0, scenario.color.r - 30),
      g: Math.max(0, scenario.color.g - 30),
      b: Math.max(0, scenario.color.b - 30)
    };
    const endResult = await createImageWithExif(
      `${timeRange}-end.jpg`,
      endTime,
      endColor,
      '🎉 운동 완료'
    );

    // 점수 계산 (정상 케이스만)
    const durationMinutes = scenario.duration > 0 ? scenario.duration : 0;

    console.log(`📁 ${scenario.desc} (${timeRange})`);
    console.log(`   파일: ${timeRange}-start.jpg, ${timeRange}-end.jpg`);
    console.log(`   시작: ${startResult.displayTime} [EXIF: ${startResult.dateTime}]`);
    console.log(`   끝:   ${endResult.displayTime} [EXIF: ${endResult.dateTime}]`);

    if (scenario.duration > 0 && !scenario.daysAgo) {
      console.log(`   운동시간: ${durationMinutes}분`);
      console.log(`   예상 점수:`);
      console.log(`     - 산책(0.5):    ${calculateScore('산책', durationMinutes)}점`);
      console.log(`     - 헬스(0.8):    ${calculateScore('헬스', durationMinutes)}점`);
      console.log(`     - 달리기(1.5):  ${calculateScore('달리기', durationMinutes)}점`);
    } else if (scenario.daysAgo) {
      console.log(`   ⚠️  에러 테스트용 (${scenario.daysAgo}일 전 사진 - 당일 검증 실패)`);
    } else {
      console.log(`   ⚠️  에러 테스트용 (시작 시간이 끝 시간보다 늦음)`);
    }
    console.log('');
  }

  console.log('===========================================');
  console.log(`✅ 총 ${scenarios.length * 2}개 이미지 생성 완료`);
  console.log(`📂 경로: ${outputDir}`);
  console.log('===========================================');
  console.log('\n사용 방법:');
  console.log('  1. 정상 테스트: 30min, 1hour, 2hour, 3hour 이미지 사용');
  console.log('  2. 시간순서 에러: invalid 이미지 사용 (시작>끝)');
  console.log('  3. 당일검증 에러: old 이미지 사용 (과거 사진)');
}

main().catch(console.error);
