# M. Engine Unreal Remote Worker

Unreal Engine cannot run on the phone and cannot run inside M. Engine's own
container — it is licence-gated and roughly 100 GB. So it is federated the only
honest way: **the phone governs and observes; a machine that actually has the
engine does the work.**

This worker is that machine's half. Node stdlib only — no dependencies to
install.

## Run it

On the machine with Unreal Engine installed:

```bash
node server.js --port 8770 --projects /path/to/your/projects
```

Options:

| Flag | Meaning |
| --- | --- |
| `--port` | Listen port (default 8770) |
| `--projects` | Root(s) it may touch. Repeatable. **Everything outside is refused.** |
| `--engine` | Engine root override; otherwise it discovers one |
| `--token` | Shared secret. Also read from `MENGINE_WORKER_TOKEN` |

Then in M. Engine → Capability Fabric, point `unrealWorker` at
`http://<that-machine>:8770`.

## What it will and will not do

**It refuses to run arbitrary commands.** There is no endpoint that accepts a
command string. Operations are a fixed allowlist of named functions:

```
probe · build · automationTest · inspectContent
```

Adding one is a deliberate code change. A worker that accepts arbitrary commands
is a remote shell with extra steps, and this one is not that.

Every operation is additionally bounded by:

- **path confinement** — a `.uproject` outside `--projects` is refused, not sanitised
- **timeouts** — builds 45 min, tests 30 min, probes seconds
- **evidence on failure** — a failed build returns its log; failure is data

## Capability states are probed, never configured

Telling the worker "Unreal is installed" is not evidence. It looks for
`UnrealEditor-Cmd`, **executes it with `-Version`**, and reports what happened.

A reachable worker on a machine *without* an engine reports
`PARTIALLY_VERIFIED`, never `AVAILABLE` — because "the worker is up" and "Unreal
can build" are different facts, and conflating them is how a green light stops
meaning anything.

Verified output from this worker running on a host with no engine:

```
UNREAL_RUNTIME_DISCOVERED     CAPABILITY_GAP   no UnrealEditor-Cmd binary found
UNREAL_BUILD_CAPABLE          CAPABILITY_GAP   no engine root; build tool cannot exist
UNREAL_PROJECT_AVAILABLE      VERIFIED         1 .uproject file(s) found
ANDROID_TOOLCHAIN_AVAILABLE   CAPABILITY_GAP   missing: Android SDK, Android NDK
PHYSICAL_DEVICE_AVAILABLE     CAPABILITY_GAP   adb present but no authorised device
```

## `inspectContent` — the operation to run first on Bannon

```bash
curl -X POST http://localhost:8770/op/inspectContent \
  -H 'Content-Type: application/json' \
  -d '{"uproject":"/path/to/Bannon/unreal/Bannon.uproject"}'
```

Run against the real Bannon repository it returns:

```json
{"counts":{"uasset":0,"umap":0,"uplugin":0,"fbx":0},
 "state":"CAPABILITY_GAP",
 "evidence":"no Unreal content: project cannot render or animate regardless of C++ correctness"}
```

That is the whole Bannon diagnosis in one call, and it is why debugging the
animation C++ before a content layer exists is likely wasted effort.

## Security posture

- `/health` is unauthenticated by design — it is how the fabric discovers the
  worker exists, and reveals nothing else.
- Everything else requires the token when one is configured, compared in
  constant time.
- **Bind to a trusted network.** With no token the worker is open to anyone who
  can reach the port. It performs no privilege escalation and cannot run
  arbitrary commands, but it can start long builds.
