
import { promises as fsp } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { execa } from "execa";
import Tesseract from "tesseract.js";
import { createRequire } from "node:module";
import { readdirSync, existsSync } from "node:fs";


const isWin = process.platform === "win32";
const POPPLER_BIN = process.env.POPPLER_BIN || "";     
const TESSERACT_BIN = process.env.TESSERACT_BIN || ""; 

function resolveCmd(binDir, name) {
  if (!binDir) return name; 
  const exe = isWin ? `${name}.exe` : name;
  return join(binDir, exe);
}


const require = createRequire(import.meta.url);
const pdf = require("pdf-parse");
const mammoth = require("mammoth");


const MIN_SELECTABLE_TEXT = parseInt(process.env.MIN_SELECTABLE_TEXT || "120", 10);
const OCR_MAX_PAGES = parseInt(process.env.OCR_MAX_PAGES || "3", 10); 
const OCR_DPI = parseInt(process.env.OCR_DPI || "300", 10);           
const TEXT_MAX = parseInt(process.env.TEXT_MAX || "60000", 10);    
const OCR_PAGE_MAX = parseInt(process.env.OCR_PAGE_MAX || "20000", 10); 


try {
  const root = "C:\\ProgramData\\chocolatey\\lib\\poppler\\tools";
  const match = readdirSync(root, { withFileTypes: true })
    .find(d => d.isDirectory() && d.name.startsWith("poppler-"));
  if (match) {
    const bin = join(root, match.name, "Library", "bin");
    if (existsSync(bin) && !process.env.PATH.includes(bin)) {
      process.env.PATH += ";" + bin;
    }
  }
} catch {}


try {
  const tess = "C:\\Program Files\\Tesseract-OCR";
  if (existsSync(tess) && !process.env.PATH.includes(tess)) {
    process.env.PATH += ";" + tess;
  }
} catch {}


export async function extractResumeText(buffer, mime) {
  mime = (mime || "").toLowerCase();

  if (mime.startsWith("text/")) {
    return normalizeText(buffer.toString("utf8"));
  }

  if (mime === "application/pdf") {
    const txt = await extractTextFromPdf(buffer);
    if (txt && txt.length >= MIN_SELECTABLE_TEXT) {
      return normalizeText(txt);
    }

    
    const ocrTxt = await ocrPdfFallback(buffer);
    if (ocrTxt) return normalizeText(ocrTxt);

    
    throw new Error("PDF had little/no selectable text and OCR is unavailable or failed.");
  }

  if (mime === "application/vnd.openxmlformats-officedocument.wordprocessingml.document") {
    const txt = await extractTextFromDocx(buffer);
    if (txt && txt.length >= 40) return normalizeText(txt);
    throw new Error("Could not extract text from DOCX.");
  }

  
  const asUtf8 = buffer.toString("utf8");
  const printableRatio = ratioPrintable(buffer);
  if (printableRatio > 0.9 && asUtf8.trim().length > 10) {
    return normalizeText(asUtf8);
  }

  throw new Error(`Unsupported or binary file type: ${mime}`);
}


async function extractTextFromPdf(buffer) {
  try {
    const data = await pdf(buffer);
    return (data.text || "").trim();
  } catch {
    return "";
  }
}


async function extractTextFromDocx(buffer) {
  try {
    const { value } = await mammoth.extractRawText({ buffer });
    return (value || "").trim();
  } catch {
    return "";
  }
}


async function ocrPdfFallback(buffer) {
  const tools = await hasPdfAndTesseract();
  if (!tools.ok) {
    console.warn("OCR disabled:", tools.reason);
    return "";
  }
  const { pdftoppmCmd } = tools;

  const dir = await fsp.mkdtemp(join(tmpdir(), "ocrpdf-"));
  const pdfPath = join(dir, "in.pdf");
  await fsp.writeFile(pdfPath, buffer);

  try {
   
    await execa(pdftoppmCmd, ["-png", "-r", "300", "-f", "1", "-l", "3", pdfPath, join(dir, "in")]);

    const files = (await fsp.readdir(dir))
      .filter((f) => f.startsWith("in-") && f.endsWith(".png"))
      .sort((a, b) => pageNum(a) - pageNum(b));

    if (!files.length) return "";

    let out = "";
    for (const f of files) {
      const imgPath = join(dir, f);
      const { data: { text } } = await Tesseract.recognize(imgPath, "eng", {
        tessedit_char_whitelist:
          "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,;:-_/()+&@%’'\"“”$#*[]{}",
      });
      out += "\n" + (text || "").slice(0, 20000);
      if (out.length > 60000) break;
    }
    return out.trim();
  } finally {
    try { await fsp.rm(dir, { recursive: true, force: true }); } catch {}
  }
}


function pageNum(name) {
  const m = name.match(/in-(\d+)\.png$/);
  return m ? parseInt(m[1], 10) : 0;
}

async function hasPdfAndTesseract() {
  const pdftoppmCmd = resolveCmd(POPPLER_BIN, "pdftoppm");
  const tesseractCmd = resolveCmd(TESSERACT_BIN, "tesseract");
  try { await execa(pdftoppmCmd, ["-v"]); } catch { return { ok: false, reason: "pdftoppm-missing" }; }
  try { await execa(tesseractCmd, ["--version"]); } catch { return { ok: false, reason: "tesseract-missing" }; }
  return { ok: true, pdftoppmCmd, tesseractCmd };
}

function normalizeText(s) {
  s = s.replace(/\r/g, "\n");
  s = s.replace(/[ \t]+/g, " ");
  s = s.replace(/\n{3,}/g, "\n\n");
  s = s.trim();
  s = dropFrequentShortLines(s, 0.9);
  if (s.length > TEXT_MAX) s = s.slice(0, TEXT_MAX);
  return s;
}

function dropFrequentShortLines(s, threshold = 0.9) {
  const lines = s.split(/\n/);
  const counts = new Map();
  for (const ln of lines) {
    const key = ln.length <= 80 ? ln.trim().toLowerCase() : null;
    if (!key || key.length === 0) continue;
    counts.set(key, (counts.get(key) || 0) + 1);
  }
  const N = lines.length || 1;
  const frequent = new Set(
    [...counts.entries()]
      .filter(([, c]) => c / N >= threshold)
      .map(([k]) => k)
  );
  if (frequent.size === 0) return s;
  const filtered = lines.filter((ln) => !frequent.has(ln.trim().toLowerCase()));
  return filtered.join("\n");
}

function ratioPrintable(buf) {
  let printable = 0;
  for (const b of buf) {
    const c = b & 0xff;
    if (c === 0x0a || c === 0x0d || (c >= 0x09 && c <= 0x7e)) printable++;
  }
  return buf.length ? printable / buf.length : 0;
}
