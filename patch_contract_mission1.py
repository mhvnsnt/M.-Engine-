with open("REALITY_CONTRACT.md", "a") as f:
    f.write("\n## REPOSITORY INSPECTION AUTHORIZATION\n")
    f.write("> M. Engine is authorized to recursively inspect repositories that the user has explicitly connected, subject to the Reality Contract and connector permissions. Inspection is read-only by default. Any mutation requires the appropriate mission/risk authorization and evidence gates.\n")
    f.write("\n## ANTI-SIMULATION & EXPLICIT FAILURE (Phase 20)\n")
    f.write("> Simulation is an explicit failure state for missions that request real execution.\n")
    f.write("> REAL_REQUEST -> REAL EXECUTION AVAILABLE? YES -> execute. NO -> BLOCKED (explain dependency).\n")
    f.write("> NEVER: REAL_REQUEST -> execution unavailable -> pretend/mock/simulate -> SUCCESS.\n")
    f.write("> A mission is complete ONLY when the requested improvement has been implemented in the real repository and the Evidence Engine has independently verified the resulting behavior.\n")
