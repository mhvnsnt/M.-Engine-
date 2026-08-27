"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.cancelAgentJob = exports.getAgentJobStatus = exports.createAgentJob = exports.modelGateway = exports.getConfiguration = void 0;
const https_1 = require("firebase-functions/v2/https");
const logger = __importStar(require("firebase-functions/logger"));
const params_1 = require("firebase-functions/params");
const admin = __importStar(require("firebase-admin"));
admin.initializeApp();
const db = admin.firestore();
// Define secrets
const openRouterApiKey = (0, params_1.defineSecret)("OPENROUTER_API_KEY");
const groqApiKey = (0, params_1.defineSecret)("GROQ_API_KEY");
const openaiApiKey = (0, params_1.defineSecret)("OPENAI_API_KEY");
// ==========================================
// MIDDLEWARE / HELPERS
// ==========================================
function ensureAuthenticated(request) {
    if (!request.auth) {
        throw new https_1.HttpsError("unauthenticated", "The function must be called while authenticated.");
    }
}
// ==========================================
// FUNCTIONS
// ==========================================
exports.getConfiguration = (0, https_1.onCall)({ enforceAppCheck: true }, async (request) => {
    ensureAuthenticated(request);
    logger.info("Fetching M. Engine configuration", { uid: request.auth?.uid });
    return {
        capabilities: ["MODEL", "REMOTE_AGENT", "JOB_SYSTEM"],
        version: "1.0.0",
        message: "Cloud Infrastructure Ready",
    };
});
exports.modelGateway = (0, https_1.onCall)({ secrets: [openRouterApiKey, groqApiKey, openaiApiKey], enforceAppCheck: true }, async (request) => {
    ensureAuthenticated(request);
    const data = request.data;
    const provider = data.provider;
    const prompt = data.prompt;
    logger.info(`Model gateway request for provider: ${provider}`, { uid: request.auth?.uid });
    let keyToUse = "UNKNOWN";
    if (provider === "OPENROUTER")
        keyToUse = openRouterApiKey.value() ? "SET" : "UNSET";
    if (provider === "GROQ")
        keyToUse = groqApiKey.value() ? "SET" : "UNSET";
    if (provider === "OPENAI")
        keyToUse = openaiApiKey.value() ? "SET" : "UNSET";
    return {
        status: "success",
        provider,
        secretStatus: keyToUse,
        message: `Gateway accepted request for ${provider}.`
    };
});
exports.createAgentJob = (0, https_1.onCall)({ enforceAppCheck: true }, async (request) => {
    ensureAuthenticated(request);
    const { repository, task } = request.data;
    if (!repository || !task) {
        throw new https_1.HttpsError("invalid-argument", "Repository and task are required.");
    }
    const userId = request.auth.uid;
    const newJobRef = db.collection(`users/${userId}/jobs`).doc();
    const now = Date.now();
    const job = {
        jobId: newJobRef.id,
        userId,
        repository,
        task,
        status: "QUEUED",
        createdAt: now,
        updatedAt: now,
        currentStep: "Initializing",
        result: null,
        cancellationState: null
    };
    await newJobRef.set(job);
    logger.info(`Created new job ${job.jobId} for user ${userId}`);
    return job;
});
exports.getAgentJobStatus = (0, https_1.onCall)({ enforceAppCheck: true }, async (request) => {
    ensureAuthenticated(request);
    const { jobId } = request.data;
    if (!jobId) {
        throw new https_1.HttpsError("invalid-argument", "jobId is required.");
    }
    const userId = request.auth.uid;
    const jobDoc = await db.collection(`users/${userId}/jobs`).doc(jobId).get();
    if (!jobDoc.exists) {
        throw new https_1.HttpsError("not-found", "Job not found.");
    }
    return jobDoc.data();
});
exports.cancelAgentJob = (0, https_1.onCall)({ enforceAppCheck: true }, async (request) => {
    ensureAuthenticated(request);
    const { jobId } = request.data;
    if (!jobId) {
        throw new https_1.HttpsError("invalid-argument", "jobId is required.");
    }
    const userId = request.auth.uid;
    const jobRef = db.collection(`users/${userId}/jobs`).doc(jobId);
    const jobDoc = await jobRef.get();
    if (!jobDoc.exists) {
        throw new https_1.HttpsError("not-found", "Job not found.");
    }
    const job = jobDoc.data();
    if (job.status === "COMPLETED" || job.status === "FAILED" || job.status === "CANCELLED") {
        throw new https_1.HttpsError("failed-precondition", "Job is already in a terminal state.");
    }
    await jobRef.update({
        status: "CANCELLED",
        updatedAt: Date.now(),
        cancellationState: "User requested cancellation"
    });
    logger.info(`Cancelled job ${jobId} for user ${userId}`);
    return { status: "CANCELLED" };
});
//# sourceMappingURL=index.js.map