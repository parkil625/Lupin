const piexif = require('piexifjs');
const fs = require('fs');
const path = require('path');

function readExif(filepath) {
  const jpeg = fs.readFileSync(filepath);
  const data = jpeg.toString('binary');

  try {
    const exif = piexif.load(data);
    const dateTimeOriginal = exif['Exif'][piexif.ExifIFD.DateTimeOriginal];
    console.log(`${path.basename(filepath)}: DateTimeOriginal = ${dateTimeOriginal}`);
    return dateTimeOriginal;
  } catch (e) {
    console.log(`${path.basename(filepath)}: No EXIF data found`);
    return null;
  }
}

const testImagesDir = path.join(__dirname, '..', 'test-images');
readExif(path.join(testImagesDir, 'start.jpg'));
readExif(path.join(testImagesDir, 'end.jpg'));
