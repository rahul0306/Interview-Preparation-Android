import express from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import pino from "pino";
import dotenv from "dotenv";
import multer from "multer";
import router from "./routes.js";

dotenv.config();
const app = express();
const log = pino();

const PORT = Number(process.env.PORT || 8080);
const ORIGIN = process.env.CORS_ORIGIN || "*";
const JSON_LIMIT = process.env.JSON_LIMIT || "1mb";
const RATE_LIMIT_PER_MIN = Number(process.env.RATE_LIMIT_PER_MIN || 30);

app.use(helmet());
app.use(cors({ origin: ORIGIN }));
app.use(express.json({ limit: JSON_LIMIT }));

app.use(rateLimit({
  windowMs: 60_000,
  max: RATE_LIMIT_PER_MIN,
  standardHeaders: true,
  legacyHeaders: false
}));

app.get("/health", (_req, res) => res.json({ ok: true, ts: Date.now() }));
app.use("/api", router);

app.use((err, _req, res, _next) => {
  
  if (err instanceof multer.MulterError && err.code === "LIMIT_FILE_SIZE") {
    log.warn({ err }, "Uploaded file too large");
    return res.status(413).json({
      error: "TOO_LARGE",
      detail:
        "Recording is larger than the 25 MB limit. Please record a shorter or lower-resolution answer.",
    });
  }

  log.error(err);
  res.status(500).json({ error: "SERVER_ERROR" });
});


app.use((req, _res, next) => {
  console.log(`[REQ] ${req.method} ${req.originalUrl} - auth=${req.headers.authorization ? 'yes' : 'no'}`);
  next();
});
app.listen(PORT, () => log.info(`Server running on port ${PORT}`));

