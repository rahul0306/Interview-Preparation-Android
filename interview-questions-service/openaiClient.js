import OpenAI from "openai";
import dotenv from "dotenv";
dotenv.config();

if (!process.env.OPENAI_API_KEY) throw new Error("Missing OPENAI_API_KEY");

export const openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
export const OPENAI_MODEL = process.env.OPENAI_MODEL || "gpt-4o-mini";

export const TRANSCRIBE_MODEL =
  process.env.OPENAI_TRANSCRIBE_MODEL || "gpt-4o-mini-transcribe";