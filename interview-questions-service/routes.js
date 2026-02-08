import express from "express";
import multer from "multer";
import { z } from "zod";
import { requireFirebaseUser } from "./authMiddleware.js";
import { openai, OPENAI_MODEL, TRANSCRIBE_MODEL } from "./openaiClient.js";
import { extractResumeText } from "./textExtractor.js";
import fs from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { promises as fsp } from "node:fs";
import { execa } from "execa";

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 15 * 1024 * 1024 },
});

const videoUpload = multer({
  storage: multer.memoryStorage(),
});

const router = express.Router();

const QuerySchema = z.object({
  role: z.string().min(2).max(120).optional(),
  count: z.preprocess((v) => (typeof v === "string" ? Number(v) : v),
    z.number().int().min(3).max(30).default(8)
  ),
});

const DAILY_TRANSCRIPT_LIMIT = Number(process.env.DAILY_TRANSCRIPT_LIMIT || 10);

const transcriptCounts = new Map();

function todayKeyFor(uid) {
  const day = new Date().toISOString().slice(0, 10); 
  return `${uid}:${day}`;
}

function getTranscriptUsage(uid) {
  const key = todayKeyFor(uid);
  return transcriptCounts.get(key) || 0;
}

function incrementTranscriptUsage(uid) {
  const key = todayKeyFor(uid);
  const current = transcriptCounts.get(key) || 0;
  const next = current + 1;
  transcriptCounts.set(key, next);
  return next;
}

router.post(
  "/generate-questions-file",
  requireFirebaseUser,
  upload.single("resume"),
  async (req, res) => {
    try {
      console.log("[UPLOAD] uid:", req.user?.uid);
      if (!req.file) return res.status(400).json({ error: "No file uploaded" });

      console.log("[UPLOAD] file:", {
        field: req.file.fieldname,
        name: req.file.originalname,
        size: req.file.size,
        mime: req.file.mimetype,
      });
      const parsed = QuerySchema.safeParse(req.body);
      if (!parsed.success) {
        return res.status(400).json({ error: parsed.error.flatten() });
      }
      const { role, count } = parsed.data;

      const f = req.file;
      if (!f) return res.status(400).json({ error: "No file uploaded" });

      
      const text = await extractResumeText(f.buffer, f.mimetype);
      if (!text || text.length < 80) {
        return res.status(400).json({
          error: "NO_TEXT",
          detail: "Could not extract enough text from the file (even after OCR).",
        });
      }

      
      const system =
        "You are an expert technical interviewer who crafts concise, specific, resume-grounded questions. " +
        "Return strict JSON only: {\"questions\":[{\"question\":\"...\"}]}";

      const userPrompt = [
        `Generate ${count} interview questions from this resume` + (role ? ` for the role: ${role}` : "") + ".",
        "Project/deep-dive, fundamentals, and role-specific items.",
        "Keep each question 1-2 sentences.",
        "",
        "RESUME TEXT:",
        text
      ].join("\n");


      console.log("[OPENAI] Calling", OPENAI_MODEL, "for uid:", req.user?.uid);
      const t0 = Date.now();

      
      const response = await openai.responses.create({
        model: OPENAI_MODEL,
        input: [
          { role: "system", content: system },
          { role: "user", content: userPrompt }
        ],
        temperature: 0.6,
        max_output_tokens: 1200,
        text: { format: { type: "json_object" } }
      });

      console.log("[OPENAI] Done in", Date.now() - t0, "ms");

      const json = JSON.parse(response.output_text);

      if (process.env.DEBUG_QUESTIONS === "true") {
        const preview = (json?.questions ?? []).slice(0, 10).map((q, i) => `${i + 1}. ${q.question}`);
        console.log("\n[RESULT] uid:", req.user?.uid, "role:", role || "none", "count:", (json?.questions || []).length);
        console.log(preview.join("\n"));
        console.log("— end —\n");
      }

      return res.json({ uid: req.user.uid, role: role || null, ...json });
    } catch (e) {
      console.error("FILE_ROUTE_ERROR", e);
      return res.status(500).json({
        error: "PROCESSING_FAILED",
        detail: e?.message ?? String(e),
      });
    }
  }
);

router.post(
  "/transcribe",
  requireFirebaseUser,
  videoUpload.single("video"),
  async (req, res) => {
    try {
      const uid = req.user?.uid;
      if (!uid) {
        return res.status(401).json({ error: "UNAUTHENTICATED" });
      }

      
      const used = getTranscriptUsage(uid);
      if (used >= DAILY_TRANSCRIPT_LIMIT) {
        return res.status(429).json({
          error: "TRANSCRIPT_LIMIT",
          detail: `Daily transcription limit of ${DAILY_TRANSCRIPT_LIMIT} reached. Please try again tomorrow.`,
        });
      }

      if (!req.file) {
        return res.status(400).json({
          error: "NO_FILE",
          detail: "No video file uploaded.",
        });
      }

      const f = req.file;
      console.log("[TRANSCRIBE] uid:", uid, "file:", {
        name: f.originalname,
        mime: f.mimetype,
        sizeMB: (f.size / (1024 * 1024)).toFixed(2),
      });

      incrementTranscriptUsage(uid);

      
      const dir = await fsp.mkdtemp(join(tmpdir(), "transcribe-"));
      const videoPath = join(dir, "input.mp4");
      const audioPath = join(dir, "audio.mp3"); 

      await fsp.writeFile(videoPath, f.buffer);

      try {
        console.log("[TRANSCRIBE] extracting audio with ffmpeg");

        
        await execa("ffmpeg", [
          "-i",
          videoPath,
          "-vn",
          "-acodec",
          "libmp3lame",
          "-b:a",
          "64k",
          "-y",
          audioPath,
        ]);

        const audioStats = await fsp.stat(audioPath);
        console.log(
          "[TRANSCRIBE] audio size MB:",
          (audioStats.size / (1024 * 1024)).toFixed(2)
        );

        const MAX_AUDIO_BYTES = 25 * 1024 * 1024;
        if (audioStats.size > MAX_AUDIO_BYTES) {
          return res.status(413).json({
            error: "AUDIO_TOO_LONG",
            detail:
              "Recording is too long to transcribe in one go. Please keep answers under about 1 hour.",
          });
        }

        const transcription = await openai.audio.transcriptions.create({
          file: fs.createReadStream(audioPath),
          model: TRANSCRIBE_MODEL,
          response_format: "json",
        });

        const text =
          transcription.text ??
          transcription?.data?.text ??
          "";

        if (!text || !text.trim()) {
          return res.status(422).json({
            error: "EMPTY_TRANSCRIPT",
            detail: "Could not extract any speech from this recording.",
          });
        }

        return res.json({ uid, text });
      } finally {
      
        try {
          await fsp.rm(dir, { recursive: true, force: true });
        } catch {
         
        }
      }
    } catch (e) {
      console.error("FILE_ROUTE_ERROR /transcribe", e);
      const msg = String(e?.message || "");

      if (msg.includes("ffmpeg")) {
        return res.status(500).json({
          error: "FFMPEG_ERROR",
          detail:
            "Server-side media tools are not available. Please ensure ffmpeg is installed and on PATH.",
        });
      }

      if (e?.status === 429 || msg.includes("quota")) {
        return res.status(429).json({
          error: "OPENAI_QUOTA",
          detail:
            "Transcription quota or billing limit reached for the OpenAI API key.",
        });
      }

      return res.status(500).json({
        error: "TRANSCRIBE_FAILED",
        detail: msg,
      });
    }
  }
);

export default router;
