# Bannon Remote Unreal Worker Import Manifest

**Target Engine:** Unreal Engine 5.3
**Project:** `unreal/Bannon.uproject`

## Asset Import Instructions

The following `.glb` assets have been physically recovered and are staged in the repository. They must be imported via the Unreal Editor (or via Python/Commandlet import scripts) into the `Content/` directory.

### Asset 1: JAGER (Default)
- **Source Path:** `assets/models/JAGER.glb`
- **Target Unreal Path:** `/Game/Bannon/Characters/JAGER/`
- **Import Type:** Skeletal Mesh
- **Expected Artifacts:**
  - `SK_JAGER` (Skeletal Mesh)
  - `SK_JAGER_Skeleton` (Skeleton)
  - `PA_JAGER` (Physics Asset)
  - Associated Materials and Textures

### Asset 2: JAGER (Coat Variant)
- **Source Path:** `assets/models/JAGER_coat.glb`
- **Target Unreal Path:** `/Game/Bannon/Characters/JAGER_Coat/`
- **Import Type:** Skeletal Mesh (Share skeleton with JAGER if compatible)
- **Expected Artifacts:**
  - `SK_JAGER_Coat` (Skeletal Mesh)
  - Associated Materials and Textures

## Verification Requirements

The remote Unreal worker must provide physical evidence of the following upon completion of this manifest:
1. **Import Success/Failure:** A log of the import operation.
2. **Skeleton Detection:** Did Unreal successfully parse the `.glb` bone hierarchy?
3. **Mesh Validity:** Are the vertex counts and material slots correct?
4. **Material Validity:** Were materials created and assigned correctly?
5. **Animation Compatibility:** Is the generated skeleton compatible with Bannon's expected retargeting or standard humanoid rigs?

**DO NOT** mark these assets as "Imported" in M. Engine until the remote Unreal worker reports success and generates the `.uasset` files.
