export interface SpeechRecognitionLike extends EventTarget {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  start: () => void;
  stop: () => void;
  abort: () => void;
  onresult: ((event: SpeechRecognitionEventLike) => void) | null;
  onerror: ((event: SpeechRecognitionErrorEventLike) => void) | null;
  onend: (() => void) | null;
}

export interface SpeechRecognitionAlternativeLike {
  transcript: string;
}

export interface SpeechRecognitionResultLike {
  isFinal: boolean;
  0: SpeechRecognitionAlternativeLike;
}

export interface SpeechRecognitionEventLike {
  resultIndex: number;
  results: {
    length: number;
    [index: number]: SpeechRecognitionResultLike;
  };
}

export interface SpeechRecognitionErrorEventLike {
  error: string;
  message?: string;
}

export type SpeechRecognitionConstructor = new () => SpeechRecognitionLike;

export function getSpeechRecognitionConstructor() {
  const browserWindow = window as Window & {
    SpeechRecognition?: SpeechRecognitionConstructor;
    webkitSpeechRecognition?: SpeechRecognitionConstructor;
  };
  return browserWindow.SpeechRecognition ?? browserWindow.webkitSpeechRecognition;
}

export function getSupportedRecordingMimeType() {
  const candidates = [
    "audio/webm;codecs=opus",
    "audio/webm",
    "audio/mp4",
    "audio/ogg;codecs=opus"
  ];
  return candidates.find((item) => MediaRecorder.isTypeSupported(item)) ?? "";
}

export function getRecordingExtension(mimeType: string) {
  if (mimeType.includes("mp4")) {
    return "m4a";
  }
  if (mimeType.includes("ogg")) {
    return "ogg";
  }
  return "webm";
}

export function buildRecordingFile(chunks: BlobPart[], mimeType: string) {
  const extension = getRecordingExtension(mimeType);
  const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
  return new File(chunks, `live-recording-${timestamp}.${extension}`, {
    type: mimeType || "audio/webm"
  });
}

export function cleanLiveTranscript(text: string) {
  return text
    .replace(/\s+/g, " ")
    .replace(/(^|[，。！？、,.!?\s])查([，。！？、,.!?\s]|$)/g, "$1$2")
    .replace(/\s+([，。！？、,.!?])/g, "$1")
    .trim();
}
