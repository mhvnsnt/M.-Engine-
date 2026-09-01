# Mission #7 — Capability Acquisition Engine

## The Boundary Dilemma (Java 17 Toolchain)
During Mission #6, M. Engine encountered an environment dependency failure (`WAITING_FOR_EXTERNAL_CAPABILITY`) when a target repository required a Java 17 toolchain. Instead of blindly halting, M. Engine now leverages the **Capability Acquisition Engine** to evaluate, acquire, and provision missing requirements.

## Capability Investigation Report

1. **What Java runtimes are actually installed?** 
   - `openjdk 21.0.12` (Temurin) is the only provisioned runtime in `/usr/lib/jvm`.
2. **Can Java 17 be provisioned legitimately?** 
   - Yes. The execution environment possesses `root` authority (UID 0).
3. **Is network/toolchain download available?** 
   - Yes. The `apt` package manager successfully communicates with `deb.debian.org`.
4. **Is a compatible local JDK already present but undiscovered?** 
   - No. `ls -la /usr/lib/jvm` confirms only Java 21 is present.
5. **Can an authorized package/tool installation provide it?** 
   - Yes. `apt-cache search openjdk-17` confirmed the availability of `openjdk-17-jdk-headless`.
6. **What does acquiring it cost?** 
   - ~200MB disk space, minor network bandwidth, and roughly 15 seconds of execution time.
7. **What security implications exist?** 
   - Sourcing from the official Debian signed repository mitigates immediate supply chain risks. However, installing additional JVMs increases the environment's attack surface.
8. **Would installing it alter the reproducibility of the environment?** 
   - Yes. It alters the baseline state. A repository that succeeds here would fail in a pristine reproduction of the original container unless this acquisition step is codified into the environment's bootstrap schema.
9. **Can the capability be persisted for future missions?** 
   - Within this specific container instance, yes. For durable systemic persistence, the acquisition must be promoted to the orchestration layer (e.g., Dockerfile/infrastructure definition).
10. **If acquisition isn't possible, what alternative environments exist?**
    - Though possible here, if `apt` were unauthorized, the fallback would be leveraging the `CiCdPipeline` integration to delegate the compilation to a remote GitHub Actions runner via the `Mengine_Github_PAT`.

## Engine Implementation

I have implemented `CapabilityAcquisitionEngine` and integrated it into the `AutonomousAgencyRuntime`. 
When the runtime enters `WAITING_FOR_EXTERNAL_CAPABILITY`, it now automatically suspends execution and attempts to acquire the required capability. 
- If acquisition succeeds (Status: `PROVISIONED`), it transitions back to `ACTING` and resumes the task.
- If acquisition fails (Status: `FAILED_UNAUTHORIZED` or `FAILED_TECHNICAL`), it halts and logs the exact economic and technical failure reason into the `AgencyLedger`.

M. Engine now manages its capability economy.
