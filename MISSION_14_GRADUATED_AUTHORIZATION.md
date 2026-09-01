# Mission 14 — Graduated Authorization & Autonomous Research Governance

## Architectural Expansion: Navigating Ambiguity
M. Engine should not confuse uncertainty with wrongdoing, nor should it confuse lack of proof with unlimited permission. **Mission 14** introduces a nuanced, graduated authorization model that allows M. Engine to evaluate evidence (including owner assertions) and dynamically downgrade its autonomy rather than simply halting at the first sign of ambiguity.

## 1. Graduated Authorization Engine
Instead of a rigid `ALLOWED` / `REFUSED` binary, authorization is now evaluated across a gradient:
- `EXPLICITLY_AUTHORIZED`
- `PUBLICLY_PERMITTED`
- `OWNER_ASSERTED_AUTHORIZATION`
- `UNCERTAIN`
- `RESTRICTED`
- `PROHIBITED`

`UNCERTAIN` no longer equates to illegal. It simply means M. Engine must rely on a more restrictive autonomy mode.

## 2. Least-Restrictive Agency
If M. Engine cannot determine explicit public permission but you have asserted ownership or requested analysis, it dynamically selects the least-restrictive justifiable autonomy level:
`FULL_AUTONOMY` -> `BOUNDED_AUTOMATION` -> `SANDBOXED_EXPERIMENT` -> `OWNER_CONFIRMATION` -> `WAITING_FOR_CAPABILITY` -> `HALT`

This prevents M. Engine from freezing up unnecessarily. For example, if scraping is restricted but metadata or transcripts are allowed, it gracefully degrades to metadata analysis rather than halting completely.

## 3. Authorization Evidence Ledger
Owner assertions matter. When you say "I own this," M. Engine records an `OWNER_ASSERTION` as empirical evidence rather than treating you with suspicion. It records its decisions in the `AgencyBoundaryStateMachine` to ensure there is a clear, auditable lineage for why it proceeded with bounded automation.

## 4. Research Memory Governance Integration
The Autonomous Research Memory (Mission 13) is directly linked to Mission 14. Every `PersistentResearchArtifact` and `ResearchArtifact` now permanently records its `acquisitionMethod` and `authorizationStatusAtAcquisition`. If future evidence invalidates a previous authorization assumption, M. Engine can track down and flag all downstream beliefs that were generated from that compromised data.

## Conclusion
> *Uncertainty is not guilt. Absence of evidence is not unlimited authorization. M. Engine should choose the least restrictive action justified by available evidence.*

Mission 14 creates an architecture that is less brittle, less annoyingly restrictive, and vastly more capable of safely navigating the real-world ambiguity of research and development.
