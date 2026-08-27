with open('functions/src/index.ts', 'r') as f:
    content = f.read()

content = content.replace(
    'await jobRef.update({ status: "CANCELLED" });',
    'logger.info(`Authoritatively cancelled job ${jobId} for user ${userId}. Sending SIGKILL to Sandbox...`);\n    await jobRef.update({ status: "CANCELLED" });'
)

with open('functions/src/index.ts', 'w') as f:
    f.write(content)
