const palettes = {
  grass: ['#6BA43A', '#7CB843', '#5E9532', '#86C44C', '#73AE3E'],
  dirt: ['#7A5636', '#8B6440', '#6B4A2E', '#825C3A'],
  stone: ['#8A8F94', '#787D82', '#9AA0A6'],
  snow: ['#F2F6F8', '#E4EBEF', '#FAFCFD'],
  leaf: ['#3F7C2A', '#4C8F33', '#356B22', '#458428'],
  trunk: ['#5C3F26', '#6B4A2E'],
  water: ['#3A6FC4', '#4A7FD4', '#2F5FAE'],
  sand: ['#DCCB8E', '#CFBC7E', '#E5D69C'],
};

function hashNoise(x, y, seed) {
  let h = Math.imul(x, 374761393) + Math.imul(y, 668265263) + Math.imul(seed, 2246822519);
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
}

function pick(list, x, y, seed) {
  return list[Math.floor(hashNoise(x, y, seed) * list.length)];
}

function terrainHeight(column, rows, seed) {
  const ridge = Math.sin(column * 0.045 + seed * 0.7) * 3.2;
  const roll = Math.sin(column * 0.13 + seed * 1.9) * 1.6;
  const grain = hashNoise(column, 0, seed) * 0.8;
  return Math.round(rows * 0.44 + ridge + roll + grain);
}

function paintSky(context, width, height, block) {
  const sky = context.createLinearGradient(0, 0, 0, height * 0.62);
  sky.addColorStop(0, '#4E92DE');
  sky.addColorStop(0.6, '#86BCEA');
  sky.addColorStop(1, '#C9E2F4');
  context.fillStyle = sky;
  context.fillRect(0, 0, width, height);

  const columns = Math.ceil(width / block);
  context.fillStyle = 'rgba(255,255,255,0.9)';
  for (let column = 0; column < columns; column++) {
    for (let row = 1; row < 9; row++) {
      const drift = Math.sin(column * 0.11 + row * 1.7 + 0.4) * 0.5 + 0.5;
      if (hashNoise(column, row, 7) * drift > 0.63) context.fillRect(column * block, row * block, block, block);
    }
  }
}

function paintTree(context, column, groundRow, block, seed) {
  const trunkHeight = 3 + Math.floor(hashNoise(column, 1, seed) * 3);
  for (let i = 0; i < trunkHeight; i++) {
    context.fillStyle = pick(palettes.trunk, column, groundRow - i, seed);
    context.fillRect(column * block, (groundRow - 1 - i) * block, block, block);
  }
  const crown = groundRow - trunkHeight;
  for (let dx = -2; dx <= 2; dx++) {
    for (let dy = -3; dy <= 1; dy++) {
      const reach = Math.abs(dx) + Math.abs(dy + 1);
      if (reach > 3) continue;
      if (hashNoise(column + dx, crown + dy, seed + 3) < 0.12) continue;
      context.fillStyle = pick(palettes.leaf, column + dx, crown + dy, seed);
      context.fillRect((column + dx) * block, (crown + dy) * block, block, block);
    }
  }
}

function renderWorld(canvas, seed = 11) {
  const context = canvas.getContext('2d');
  const block = 20;
  const columns = Math.ceil(canvas.width / block);
  const rows = Math.ceil(canvas.height / block);

  context.imageSmoothingEnabled = false;
  paintSky(context, canvas.width, canvas.height, block);

  const snowFrom = Math.floor(columns * 0.68);
  const waterFrom = Math.floor(columns * 0.2);
  const waterTo = Math.floor(columns * 0.27);
  const heights = [];
  for (let column = 0; column < columns; column++) heights.push(terrainHeight(column, rows, seed));

  for (let column = 0; column < columns; column++) {
    const ground = heights[column];
    const isSnow = column >= snowFrom;
    const isWater = column >= waterFrom && column <= waterTo;
    const isShore = (column >= waterFrom - 2 && column < waterFrom) || (column > waterTo && column <= waterTo + 2);

    for (let row = ground; row < rows; row++) {
      let family = palettes.dirt;
      if (row === ground) family = isSnow ? palettes.snow : isShore ? palettes.sand : palettes.grass;
      else if (row > ground + 4) family = palettes.stone;
      if (isWater && row <= ground + 2) family = palettes.water;
      if (isSnow && row > ground && row <= ground + 1) family = palettes.stone;
      context.fillStyle = pick(family, column, row, seed);
      context.fillRect(column * block, row * block, block, block);
    }

    const treeRoll = hashNoise(column, 99, seed);
    const tall = column > 2 && column < columns - 3 && !isWater && !isShore && !isSnow;
    if (tall && treeRoll > 0.84) paintTree(context, column, ground, block, seed);
  }

  const haze = context.createLinearGradient(0, rows * 0.36 * block, 0, canvas.height);
  haze.addColorStop(0, 'rgba(201,226,244,0.34)');
  haze.addColorStop(0.3, 'rgba(201,226,244,0.04)');
  haze.addColorStop(1, 'rgba(255,248,220,0.06)');
  context.fillStyle = haze;
  context.fillRect(0, 0, canvas.width, canvas.height);
}

window.renderWorld = renderWorld;
