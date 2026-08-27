export interface AgentJob {
    jobId: string;
    userId: string;
    repository: string;
    task: string;
    status: "QUEUED" | "PLANNING" | "WAITING_APPROVAL" | "EXECUTING" | "VERIFYING" | "COMPLETED" | "FAILED" | "CANCELLED";
    createdAt: number;
    updatedAt: number;
    currentStep: string;
    result: string | null;
    cancellationState: string | null;
}
export declare const getConfiguration: import("firebase-functions/v2/https").CallableFunction<any, Promise<{
    capabilities: string[];
    version: string;
    message: string;
}>>;
export declare const modelGateway: import("firebase-functions/v2/https").CallableFunction<any, Promise<{
    status: string;
    provider: any;
    secretStatus: string;
    message: string;
}>>;
export declare const createAgentJob: import("firebase-functions/v2/https").CallableFunction<any, Promise<AgentJob>>;
export declare const getAgentJobStatus: import("firebase-functions/v2/https").CallableFunction<any, Promise<AgentJob>>;
export declare const cancelAgentJob: import("firebase-functions/v2/https").CallableFunction<any, Promise<{
    status: string;
}>>;
//# sourceMappingURL=index.d.ts.map