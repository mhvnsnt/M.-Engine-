import os
import re
import json

def get_directory_size(path):
    total = 0
    file_count = 0
    for dirpath, _, filenames in os.walk(path):
        for f in filenames:
            fp = os.path.join(dirpath, f)
            if not os.path.islink(fp):
                total += os.path.getsize(fp)
                file_count += 1
    return total, file_count

def scan_gradle_dependencies():
    deps = []
    try:
        with open("app/build.gradle.kts", "r") as f:
            content = f.read()
            # Simple regex to find dependencies
            matches = re.findall(r'implementation\s*\((.*?)\)', content)
            deps.extend(matches)
    except Exception as e:
        pass
    return deps

def analyze_capabilities():
    cap_dir = "app/src/main/java/com/example/ai/capabilities"
    capabilities = {}
    if os.path.exists(cap_dir):
        for f in os.listdir(cap_dir):
            if f.endswith(".kt"):
                with open(os.path.join(cap_dir, f), "r") as kt_file:
                    content = kt_file.read()
                    # Identify if it contains mocked/simulated comments
                    is_mock = "mock" in content.lower() or "simulate" in content.lower() or "throw UnsupportedOperationException" in content
                    capabilities[f] = "SIMULATED/BLOCKED" if is_mock else "IMPLEMENTED"
    return capabilities

def main():
    print("=== M. ENGINE SELF-AUDIT REPORT ===")
    
    # 1. Structure
    android_size, android_files = get_directory_size("app/src")
    web_size, web_files = get_directory_size("m-engine-web/src")
    print(f"Android Core: {android_files} files")
    print(f"Web Client: {web_files} files")
    
    # 2. Dependencies
    deps = scan_gradle_dependencies()
    print(f"\nDiscovered {len(deps)} Gradle Dependencies.")
    has_jgit = any("jgit" in d.lower() for d in deps)
    has_room = any("room" in d.lower() for d in deps)
    has_ast = any("tree-sitter" in d.lower() or "javaparser" in d.lower() or "psi" in d.lower() for d in deps)
    
    # 3. Capabilities
    caps = analyze_capabilities()
    print("\nCapability Graph Mapping:")
    for k, v in caps.items():
        print(f" - {k}: {v}")
        
    # 4. Blocker Identification
    print("\n=== IDENTIFIED BLOCKERS PREVENTING INDEPENDENT DEVELOPMENT ===")
    blockers = []
    
    if not has_ast:
        blockers.append({
            "gap": "No AST or Code Parsing capability",
            "impact": 9,
            "feasibility": 8,
            "confidence": 9,
            "description": "M. Engine cannot surgically read/modify syntax trees of its own codebase, forcing reliance on fragile string replacement."
        })
        
    # Check testing
    test_dir = "app/src/test"
    if not os.path.exists(test_dir) or len(os.listdir(test_dir)) == 0:
        blockers.append({
            "gap": "No Automated Unit Test Suite for Evidence Engine",
            "impact": 10,
            "feasibility": 10,
            "confidence": 10,
            "description": "Evidence Engine requires UNIT_TEST validation, but the repo lacks a verifiable JUnit/Robolectric test suite for its core AI classes."
        })
        
    # Check execution sandbox
    if not os.path.exists("m-engine-sandbox"):
        blockers.append({
            "gap": "No Local Isolated Execution Sandbox",
            "impact": 8,
            "feasibility": 5,
            "confidence": 7,
            "description": "Cannot securely run untrusted worker code locally without Docker or process isolation."
        })

    for b in blockers:
        b['score'] = b['impact'] * b['feasibility'] * b['confidence']

    blockers.sort(key=lambda x: x['score'], reverse=True)
    
    for idx, b in enumerate(blockers):
        print(f"\n{idx+1}. {b['gap']} (Score: {b['score']})")
        print(f"   Impact: {b['impact']}, Feasibility: {b['feasibility']}, Confidence: {b['confidence']}")
        print(f"   Desc: {b['description']}")

if __name__ == '__main__':
    main()
