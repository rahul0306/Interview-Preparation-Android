import { auth } from "./firebase.js";

export async function requireFirebaseUser(req, res, next) {
  try {
    const header = req.headers.authorization || "";
    const [, token] = header.split(" ");
    if (!token) return res.status(401).json({ error: "Missing Bearer token" });

    const decoded = await auth.verifyIdToken(token);
    req.user = { uid: decoded.uid, email: decoded.email };
    next();
  } catch (err) {
    res.status(401).json({ error: "Invalid or expired ID token" });
  }
}
