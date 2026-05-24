type PreparedAudioUpload = {
  file: File;
  durationSeconds: number;
  normalized: boolean;
};

const NORMALIZED_SAMPLE_RATE = 16_000;

export async function prepareAudioUpload(file: File): Promise<PreparedAudioUpload> {
  if (!needsNormalization(file)) {
    return {
      file,
      durationSeconds: await resolveAudioDuration(file),
      normalized: false
    };
  }

  const AudioContextCtor = window.AudioContext ?? (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!AudioContextCtor) {
    throw new Error("当前浏览器不支持音频转码，请改用 WAV、MP3、FLAC 或 OGG 文件。");
  }

  const context = new AudioContextCtor();
  try {
    const sourceBytes = await file.arrayBuffer();
    const decoded = await context.decodeAudioData(sourceBytes.slice(0));
    const rendered = await renderMonoWav(decoded);
    const normalizedFile = new File([rendered], replaceExtension(file.name, "wav"), { type: "audio/wav" });

    return {
      file: normalizedFile,
      durationSeconds: Number(decoded.duration.toFixed(2)),
      normalized: true
    };
  } catch (error) {
    console.error(error);
    throw new Error("当前音频格式与 Xiaomi MiMo 兼容性较差，且浏览器转码失败。请改用 WAV、MP3、FLAC 或 OGG 文件。");
  } finally {
    void context.close();
  }
}

async function renderMonoWav(source: AudioBuffer): Promise<Blob> {
  const frameCount = Math.max(1, Math.ceil(source.duration * NORMALIZED_SAMPLE_RATE));
  const offlineContext = new OfflineAudioContext(1, frameCount, NORMALIZED_SAMPLE_RATE);
  const bufferSource = offlineContext.createBufferSource();
  bufferSource.buffer = source;
  bufferSource.connect(offlineContext.destination);
  bufferSource.start(0);

  const rendered = await offlineContext.startRendering();
  const samples = rendered.getChannelData(0);
  return encodeMono16BitWav(samples, rendered.sampleRate);
}

function encodeMono16BitWav(samples: Float32Array, sampleRate: number): Blob {
  const bytesPerSample = 2;
  const dataLength = samples.length * bytesPerSample;
  const buffer = new ArrayBuffer(44 + dataLength);
  const view = new DataView(buffer);

  writeAscii(view, 0, "RIFF");
  view.setUint32(4, 36 + dataLength, true);
  writeAscii(view, 8, "WAVE");
  writeAscii(view, 12, "fmt ");
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * bytesPerSample, true);
  view.setUint16(32, bytesPerSample, true);
  view.setUint16(34, 16, true);
  writeAscii(view, 36, "data");
  view.setUint32(40, dataLength, true);

  let offset = 44;
  for (let index = 0; index < samples.length; index += 1) {
    const clamped = Math.max(-1, Math.min(1, samples[index]));
    const value = clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff;
    view.setInt16(offset, value, true);
    offset += bytesPerSample;
  }

  return new Blob([buffer], { type: "audio/wav" });
}

function writeAscii(view: DataView, offset: number, value: string) {
  for (let index = 0; index < value.length; index += 1) {
    view.setUint8(offset + index, value.charCodeAt(index));
  }
}

function replaceExtension(fileName: string, nextExtension: string) {
  const baseName = fileName.replace(/\.[^.]+$/, "");
  return `${baseName || "audio"}.${nextExtension}`;
}

function needsNormalization(file: File) {
  const normalizedType = file.type.toLowerCase();
  const normalizedName = file.name.toLowerCase();

  if (normalizedName.endsWith(".wav") || normalizedName.endsWith(".mp3") || normalizedName.endsWith(".flac") || normalizedName.endsWith(".ogg")) {
    return false;
  }

  return normalizedType.includes("m4a")
    || normalizedType.includes("webm")
    || normalizedType.includes("mp4")
    || normalizedType.includes("aac")
    || normalizedType.includes("x-m4a")
    || normalizedName.endsWith(".webm")
    || normalizedName.endsWith(".m4a")
    || normalizedName.endsWith(".mp4")
    || normalizedName.endsWith(".aac");
}

async function resolveAudioDuration(file: File) {
  return new Promise<number>((resolve) => {
    const audio = document.createElement("audio");
    audio.preload = "metadata";
    audio.onloadedmetadata = () => {
      URL.revokeObjectURL(audio.src);
      resolve(Number(audio.duration.toFixed(2)));
    };
    audio.onerror = () => resolve(0);
    audio.src = URL.createObjectURL(file);
  });
}

export type { PreparedAudioUpload };
