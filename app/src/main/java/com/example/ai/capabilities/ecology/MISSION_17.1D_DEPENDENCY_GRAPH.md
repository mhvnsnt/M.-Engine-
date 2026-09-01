# Mission 17.1D — Cross-Project Dependency Graph

## Core Invariant
Every graph edge in M. Engine is an epistemic object, not a generic string connection. Connections are not assumed; they are strictly categorized, evaluated, and backed by falsifiable evidence. 

## The DependencyRelationship Object
Relationships in the ecosystem graph now represent robust, time-aware epistemic claims:
```kotlin
DependencyRelationship
├── sourceNode
├── targetNode
├── relationshipType
├── epistemicClassification (OBSERVATION, INFERENCE, HYPOTHESIS)
├── confidence
├── evidence[]
├── discoveredAt
├── lastVerifiedAt
├── verificationMethod
├── falsificationCondition (FalsificationProbe)
├── status (ACTIVE, STALE, CONTESTED, CURRENTLY_UNRELATED, etc.)
└── downstreamImplications[]
```

## Relationship Categories & Types
Graph edges are strictly typed to distinguish between structural, operational, semantic, and negative claims:

- **Structural:** IMPORTS, DEPENDS_ON, USES_PACKAGE, USES_API, REFERENCES_REPOSITORY, SHARES_MODULE, SHARES_WORKSPACE, SUBMODULE_OF
- **Operational:** CALLS_SERVICE, SHARES_AUTHENTICATION, EXCHANGES_DATA, DEPLOYED_WITH, TESTED_WITH, BUILT_BY
- **Semantic:** SUPPORTS_GOAL, POTENTIAL_SYNERGY, MAY_BENEFIT_FROM, ARCHITECTURALLY_SIMILAR
- **Negative:** NO_EVIDENCE_OF_RELATIONSHIP (e.g. explicitly mapping the absence of connections after deep inspection).

## Falsifiability
Instead of merely storing a natural-language description, edges embed a `FalsificationProbe` to support automatic daily/weekly re-verification:
```kotlin
FalsificationProbe(
    type = MANIFEST_SEARCH,
    target = "package.json",
    expectedReference = "@mengine/core"
)
```
If a probe fails, the relationship status degrades from `ACTIVE` to `CONTESTED` rather than silently persisting an obsolete fact.

## Next Phase Alignment (17.1D.5)
This epistemic rigor sets the stage for Evidence Reconciliation, where contradictions between Documentation (e.g., "Bannon uses X") and Structural Inspection ("X is absent in Bannon's build manifest") downgrade edge confidence and trigger an autonomous investigation flag.
