# Capability Gap Matrix (Phase 18)

## 1. M. Engine Capability Graph (Self-Audit)
*   **Mission Engine**: Present (Durable via Room DB)
*   **Personal Context Engine**: Present (Memory fragment storage)
*   **Evidence Engine**: Present (Records verification outcomes)
*   **Universal Reality Loop**: Present (Core 18-step workflow)
*   **GitHub API**: Present (REST-based branch/commit management)
*   **Physical Actuators**: Stubbed/Simulated (Cannot physically execute UI testing on emulator from container)
*   **Web Client / PWA**: Missing (`BLOCKED_BY_EXTERNAL_DEPENDENCY` - requires Node.js/web deployment environment)
*   **AST/Code Parser**: Missing (Relies on simple regex or LLM rather than a robust syntax tree parser like Tree-sitter)
*   **Agent Evaluation**: Missing (No local benchmark suite for SWE-agent/OpenHands-style evaluation)
*   **Local Vector DB**: Missing (Relying on simple Room queries, no exact local vector search like pgvector or specialized SQLite extension)

## 2. External Ecosystem Research (Pre-August 2026)
*   **Coding Agents**: OpenHands, SWE-agent, Aider (Python/Node based, not easily embeddable in Kotlin Android).
*   **Code Parsing**: Tree-sitter (C/Java/JNI based, robust AST generation).
*   **Git Operations**: Eclipse JGit (Mature, pure Java, highly embeddable).
*   **Vector Search**: sqlite-vss (SQLite vector search extension) or Room full-text search as fallback.
*   **Browser Automation**: Playwright, Puppeteer (Node.js/Python - difficult to run in Android without remote sandbox).

## 3. Gap Matrix & Priorities

| CURRENT CAPABILITY | BEST VERIFIED CANDIDATE | CURRENT IMPLEMENTATION | EVIDENCE | GAP | INTEGRATION STRATEGY | SECURITY RISK | BENCHMARK | PRIORITY | REALITY CLASSIFICATION |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **AST Code Parsing** | Tree-sitter | String/Regex parsing | Fails on complex nested brackets | High | Embed Kotlin binding or rewrite simple native Kotlin AST parser | Low | High | P1 | CAN_IMPLEMENT |
| **Vector Search** | sqlite-vss / Room FTS | Room LIKE queries | Poor semantic matching | Med | Implement Room FTS4/FTS5 for semantic text matching | Low | Med | P2 | CAN_IMPLEMENT |
| **Web Client** | Next.js / Compose Web | Android Native Only | No web URL available | High | Wait for deployment env | Low | N/A | P1 | BLOCKED_BY_EXTERNAL_DEPENDENCY |
| **Git Operations** | Eclipse JGit | GitHub REST API | Slow, rate-limited | Med | Direct local clone/commit via JGit | Low | High | P3 | CAN_IMPLEMENT |
| **Physical Actuators**| UIAutomator/Espresso | Simulated | Cannot run in build container | High | Remote Device Farm via API | Med | Low | P4 | BLOCKED_BY_EXTERNAL_DEPENDENCY |

## 4. Closing the Highest-Value Gap
The highest value, implementable gap is **Robust AST/Code Parsing (or Room FTS for memory)**. Since we are building an autonomous coding agent, robustly parsing classes/methods is critical. However, embedding Tree-sitter via JNI in Android is complex and might break the build. 

An alternative high-value gap is **Local Git Operations via Eclipse JGit**. Relying on the GitHub REST API for every file change is slow and rate-limited. By implementing a real `JGitRepositoryManager`, M. Engine can clone, branch, commit, and push locally, acting exactly like a real developer on the filesystem before pushing.
Another option is **Room FTS4/FTS5 for Memory Retrieval**, upgrading `MemoryFragmentDao` to use Full Text Search for high-speed, semantic-like retrieval.

Let's prioritize: **Local Git Operations via Eclipse JGit**. It massively improves autonomy, speed, and reality (operating on local files). We already have `org.eclipse.jgit` in `build.gradle.kts`.
