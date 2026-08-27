import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import { defineSecret } from "firebase-functions/params";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

// Define secrets
const openRouterApiKey = defineSecret("OPENROUTER_API_KEY");
const groqApiKey = defineSecret("GROQ_API_KEY");
const openaiApiKey = defineSecret("OPENAI_API_KEY");

// ==========================================
// TYPES & INTERFACES
// ==========================================
export interface AgentJob {
  jobId: string;
  userId: string;
  repository: string;
  task: string;
  status: "QUEUED" | "UNDERSTAND" | "RESEARCH" | "RETRIEVE" | "PLAN" | "RISK_EVALUATION" | "DELEGATE" | "SANDBOX_CREATING" | "REPOSITORY_LOADING" | "WORKER_STARTING" | "EXECUTING" | "BUILDING" | "TESTING" | "INSPECTING" | "VERIFYING" | "REFLECTING" | "ADAPTING" | "WAITING_APPROVAL" | "COMPLETED" | "FAILED" | "CANCELLING" | "CANCELLED";
  createdAt: number;
  updatedAt: number;
  currentStep: string;
  result: string | null;
  cancellationState: string | null;
  evidence: ExecutionEvidence | null;
  selectedWorker: string | null;
  riskLevel: string | null;
}

export interface ExecutionEvidence {
  buildPass: boolean;
  unitTestsPass: boolean;
  staticAnalysisPass: boolean;
  securityChecksPass: boolean;
  requestedBehaviorVerified: boolean;
  diffReviewPass: boolean;
  unresolvedWarnings: number;
}

// ==========================================
// MIDDLEWARE / HELPERS
// ==========================================
function ensureAuthenticated(request: any) {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "The function must be called while authenticated.");
  }
}

// ==========================================
// FUNCTIONS
// ==========================================

export const getConfiguration = onCall({ enforceAppCheck: true }, async (request) => {
  ensureAuthenticated(request);
  logger.info("Fetching M. Engine configuration", { uid: request.auth?.uid });
  return {
    capabilities: ["MODEL", "REMOTE_AGENT", "JOB_SYSTEM", "RESEARCH_ENGINE", "SELF_IMPROVEMENT"],
    version: "1.0.0",
    message: "Cloud Infrastructure Ready",
  };
});

export const modelGateway = onCall(
  { secrets: [openRouterApiKey, groqApiKey, openaiApiKey], enforceAppCheck: true },
  async (request) => {
    ensureAuthenticated(request);
    const data = request.data;
    const provider = data.provider;
    
    logger.info(`Model gateway request for provider: ${provider}`, { uid: request.auth?.uid });
    
    let keyToUse = "UNKNOWN";
    if (provider === "OPENROUTER") keyToUse = openRouterApiKey.value() ? "SET" : "UNSET";
    if (provider === "GROQ") keyToUse = groqApiKey.value() ? "SET" : "UNSET";
    if (provider === "OPENAI") keyToUse = openaiApiKey.value() ? "SET" : "UNSET";
    
    return {
      status: "success",
      provider,
      secretStatus: keyToUse,
      message: `Gateway accepted request for ${provider}.`
    };
  }
);

export const createAgentJob = onCall({ enforceAppCheck: true }, async (request) => {
  ensureAuthenticated(request);
  
  const { repository, task } = request.data;
  if (!repository || !task) {
    throw new HttpsError("invalid-argument", "Repository and task are required.");
  }

  const userId = request.auth!.uid;
  const newJobRef = db.collection(`users/${userId}/jobs`).doc();
  const now = Date.now();

  const job: AgentJob = {
    jobId: newJobRef.id,
    userId,
    repository,
    task,
    status: "QUEUED",
    createdAt: now,
    updatedAt: now,
    currentStep: "Initializing",
    result: null,
    cancellationState: null,
    evidence: null,
    selectedWorker: null,
    riskLevel: null
  };

  await newJobRef.set(job);
  logger.info(`Created new job ${job.jobId} for user ${userId}`);
  return job;
});

export const getAgentJobStatus = onCall({ enforceAppCheck: true }, async (request) => {
  ensureAuthenticated(request);
  
  const { jobId } = request.data;
  if (!jobId) {
    throw new HttpsError("invalid-argument", "jobId is required.");
  }

  const userId = request.auth!.uid;
  const jobDoc = await db.collection(`users/${userId}/jobs`).doc(jobId).get();
  
  if (!jobDoc.exists) {
    throw new HttpsError("not-found", "Job not found.");
  }

  return jobDoc.data() as AgentJob;
});

export const cancelAgentJob = onCall({ enforceAppCheck: true }, async (request) => {
  ensureAuthenticated(request);
  
  const { jobId } = request.data;
  if (!jobId) {
    throw new HttpsError("invalid-argument", "jobId is required.");
  }

  const userId = request.auth!.uid;
  const jobRef = db.collection(`users/${userId}/jobs`).doc(jobId);
  const jobDoc = await jobRef.get();
  
  if (!jobDoc.exists) {
    throw new HttpsError("not-found", "Job not found.");
  }

  const job = jobDoc.data() as AgentJob;
  
  // Terminal state lockout
  if (job.status === "COMPLETED" || job.status === "FAILED" || job.status === "CANCELLED") {
    throw new HttpsError("failed-precondition", "Job is already in a terminal state.");
  }

  // Authoritative state transition to CANCELLED.
  // In a full implementation, this webhook also signals the Remote Sandbox to SIGKILL immediately.
  await jobRef.update({
    status: "CANCELLED",
    updatedAt: Date.now(),
    cancellationState: "User requested authoritative cancellation."
  });
  
  logger.info(`Authoritatively cancelled job ${jobId} for user ${userId}. Remote worker processes will be terminated.`);
  return { status: "CANCELLED" };
});

// ==========================================
// PHASE 8: REMOTE SANDBOX DISPATCHER
// ==========================================

export const provisionSandbox = onCall({ enforceAppCheck: true }, async (request) => {
  ensureAuthenticated(request);
  const { jobId, config } = request.data;
  
  if (!jobId) {
    throw new HttpsError("invalid-argument", "jobId is required.");
  }

  const userId = request.auth!.uid;
  const jobRef = db.collection(`users/${userId}/jobs`).doc(jobId);
  const jobDoc = await jobRef.get();
  
  if (!jobDoc.exists) {
    throw new HttpsError("not-found", "Job not found.");
  }

  // Update job status
  await jobRef.update({
    status: "SANDBOX_CREATING",
    updatedAt: Date.now(),
    currentStep: "Provisioning remote sandbox environment"
  });

  // Simulated Remote Worker VM creation
  const sandboxId = `sbx-${Date.now()}-${Math.floor(Math.random()*1000)}`;
  
  logger.info(`Provisioned sandbox ${sandboxId} for job ${jobId}`, { config });

  return { sandboxId, status: "provisioned" };
});

export const executeInSandbox = onCall({ enforceAppCheck: true }, async (request) => {
  ensureAuthenticated(request);
  const { sandboxId, command, timeoutMinutes } = request.data;
  
  if (!sandboxId || !command) {
    throw new HttpsError("invalid-argument", "sandboxId and command are required.");
  }

  logger.info(`Executing in sandbox ${sandboxId}: ${command}`);

  // In a real environment, we'd wait for the container to return.
  // We'll simulate a successful execution here.
  let exitCode = 0;
  let stdout = `Executed command: ${command}\nOutput: simulated success`;
  let stderr = "";
  
  if (command.includes("fail")) {
    exitCode = 1;
    stderr = "Error: command failed intentionally";
  }

  return { exitCode, stdout, stderr, timeoutTriggered: false };
});

export const destroySandbox = onCall({ enforceAppCheck: true }, async (request) => {
  ensureAuthenticated(request);
  const { sandboxId } = request.data;
  
  if (!sandboxId) {
    throw new HttpsError("invalid-argument", "sandboxId is required.");
  }

  logger.info(`Destroyed sandbox ${sandboxId}`);
  return { status: "destroyed" };
});
