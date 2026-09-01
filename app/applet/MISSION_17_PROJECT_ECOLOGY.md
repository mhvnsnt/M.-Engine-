# Mission 17 — Project Ecology & Reality Discovery

## Problem Statement
M. Engine was prioritizing opportunities based on an abstract Goal Ecology without possessing physical evidence of the user's actual environment (repositories, applications, PWAs). A system cannot claim to scan an ecology it has not verified. Doing so violates Epistemic Honesty and the Reality Contract. **Discovery ≠ Understanding**.

## 1. Reality Surface Registry (`RealitySurface.kt`)
A GitHub repository is only one representation of a project. Mission 17 introduces the `RealitySurface` which distinguishes between:
*   **Source Reality:** What the code says (GitHub).
*   **Build Reality / Evidence:** What actually happens (Tests, CI).
*   **Runtime Reality:** What the user experiences (Website, App, PWA).
*   **Knowledge Reality:** What is documented (Research, Videos).

Surfaces are explicitly tracked via an `InspectionStatus` (e.g., `UNREGISTERED`, `DISCOVERED`, `STRUCTURAL_INSPECTION_PENDING`, `MAPPED`, `STALE`).

## 2. Project Ecology Engine (`ProjectEcologyEngine.kt`)
Projects are no longer isolated names in a list. They are interconnected nodes forming a graph. They possess relationships (`DEPENDS_ON`, `SHARES_CODE_WITH`, `EXPERIMENTAL_PREDECESSOR_OF`) that allow M. Engine to spot cross-project opportunities, such as identifying a reusable component in Project A that solves a problem in Project B.

## 3. Epistemic Honesty in the Autonomous Loop
The `AutonomousOpportunityLoop` has been updated to enforce epistemic honesty.
Before attempting to prioritize abstract tasks, M. Engine calculates its `ecologyConfidence`.
If confidence is LOW (because projects are merely discovered but unmapped), the system drops all other hypotheses and explicitly states:
> *"I have insufficient information to rank your projects intelligently. Therefore, my next autonomous action is to acquire the missing information."*

The highest leverage action shifts from **Feature Development** to **Evidence Acquisition**.

## Conclusion
M. Engine now has eyes on its actual world. It does not pretend to understand what it has only discovered. The proactive system now has a physical reality to be proactive about.
