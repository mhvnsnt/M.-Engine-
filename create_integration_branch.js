const fs = require('fs');
const pat = process.env.Mengine_Github_PAT;
const headers = { 
    "Authorization": "token " + pat, 
    "User-Agent": "NodeJS",
    "Content-Type": "application/json"
};
const repoUrl = "https://api.github.com/repos/mhvnsnt/Bannon";

async function request(url, method, body) {
    const res = await fetch(repoUrl + url, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined
    });
    if (!res.ok) {
        throw new Error(`API Error ${res.status}: ${await res.text()}`);
    }
    return res.json();
}

async function run() {
    console.log("1. Fetching main branch ref...");
    const ref = await request("/git/refs/heads/main", "GET");
    const baseCommitSha = ref.object.sha;

    console.log("2. Fetching base commit...");
    const commit = await request(`/git/commits/${baseCommitSha}`, "GET");
    const baseTreeSha = commit.tree.sha;

    console.log("3. Fetching Bannon.uproject content to modify...");
    const uprojectRes = await fetch("https://raw.githubusercontent.com/mhvnsnt/Bannon/main/unreal/Bannon.uproject", {headers});
    const uprojectText = await uprojectRes.text();
    let uprojectObj = JSON.parse(uprojectText);
    
    // Add module
    let modules = uprojectObj.Modules || [];
    if (!modules.some(m => m.Name === "BannonEngine")) {
        modules.push({
            "Name": "BannonEngine",
            "Type": "Runtime",
            "LoadingPhase": "Default",
            "AdditionalDependencies": ["Engine", "BannonCore"]
        });
        uprojectObj.Modules = modules;
    }

    const bannonEngineBuildCs = `// Copyright BANNON.

using UnrealBuildTool;
using System.IO;

public class BannonEngine : ModuleRules
{
    public BannonEngine(ReadOnlyTargetRules Target) : base(Target)
    {
        PCHUsage = PCHUsageMode.UseExplicitOrSharedPCHs;
        CppStandard = CppStandardVersion.Cpp20;

        PublicDependencyModuleNames.AddRange(new string[] {
            "Core", "CoreUObject", "Engine", "InputCore", "EnhancedInput",
            "BannonCore"
        });

        PrivateDependencyModuleNames.AddRange(new string[] {
            "ControlRig", "RigVM", "AnimGraphRuntime"
        });
        
        string NativeInclude = Path.GetFullPath(Path.Combine(ModuleDirectory, "..", "..", "..", "native", "include"));
        PublicIncludePaths.Add(NativeInclude);
    }
}
`;

    const bannonEngineH = `// Copyright BANNON.
#pragma once
#include "CoreMinimal.h"
`;

    const bannonEngineCpp = `// Copyright BANNON.
#include "BannonEngine.h"
#include "Modules/ModuleManager.h"

IMPLEMENT_MODULE(FDefaultModuleImpl, BannonEngine);
`;

    console.log("4. Creating blobs...");
    const blobUproject = await request("/git/blobs", "POST", { content: JSON.stringify(uprojectObj, null, 2), encoding: "utf-8" });
    const blobBuildCs = await request("/git/blobs", "POST", { content: bannonEngineBuildCs, encoding: "utf-8" });
    const blobH = await request("/git/blobs", "POST", { content: bannonEngineH, encoding: "utf-8" });
    const blobCpp = await request("/git/blobs", "POST", { content: bannonEngineCpp, encoding: "utf-8" });

    console.log("5. Creating new tree...");
    const tree = [
        { path: "unreal/Bannon.uproject", mode: "100644", type: "blob", sha: blobUproject.sha },
        { path: "unreal/Source/BannonEngine/BannonEngine.Build.cs", mode: "100644", type: "blob", sha: blobBuildCs.sha },
        { path: "unreal/Source/BannonEngine/Public/BannonEngine.h", mode: "100644", type: "blob", sha: blobH.sha },
        { path: "unreal/Source/BannonEngine/Private/BannonEngine.cpp", mode: "100644", type: "blob", sha: blobCpp.sha }
    ];
    
    const newTree = await request("/git/trees", "POST", { base_tree: baseTreeSha, tree: tree });

    console.log("6. Creating commit...");
    const newCommit = await request("/git/commits", "POST", {
        message: "Correct BannonEngine module registration\n\nIntegrates BannonEngine into the Unreal project properly. Adds Build.cs and module boilerplate, and updates Bannon.uproject.",
        tree: newTree.sha,
        parents: [baseCommitSha]
    });

    console.log("7. Creating branch integration/bannon-engine-content-recovery...");
    try {
        await request("/git/refs", "POST", {
            ref: "refs/heads/integration/bannon-engine-content-recovery",
            sha: newCommit.sha
        });
        console.log("Branch created successfully!");
    } catch (e) {
        if (e.message.includes("Reference already exists")) {
            console.log("Branch already exists. Updating it...");
            await request("/git/refs/heads/integration/bannon-engine-content-recovery", "PATCH", {
                sha: newCommit.sha,
                force: true
            });
            console.log("Branch updated successfully!");
        } else {
            throw e;
        }
    }
}
run().catch(console.error);
