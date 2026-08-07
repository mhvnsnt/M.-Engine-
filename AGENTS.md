# Agent Instructions

## Transcription Understanding
When processing user requests, be aware that they are often generated via voice-to-text transcription. You MUST:
- Interpret requests flexibly, accounting for common voice-to-text errors, run-on sentences, and filler words (e.g., "um", "like", "you know").
- Focus on extracting the core technical intent and actionable directives rather than strictly interpreting the literal phrasing.
- If a sentence appears disjointed or uses the wrong homophone (e.g., "right" instead of "write"), use the surrounding context to infer the true meaning and proceed accordingly.
- Do not ask for clarification on minor grammatical errors or transcription artifacts; resolve ambiguity by assuming the most logical technical context.

## Communication & Interaction Style
- **Informative and Detailed Responses:** Do not be vague or overly brief. Provide comprehensive updates on what you have accomplished, explain the reasoning behind your decisions, and clearly outline the next steps or options available.
- **Claude Code Style:** Emulate the helpful, detailed, and highly articulate style of advanced coding assistants like Claude. Think through problems and give the user clear visibility into your process. Tell the user what was completed and what we can do next.
- **Proactive Research & Integration:** Actively suggest and incorporate relevant open-source tools, libraries, or system instructions to enhance the application's capabilities.
- **Next Steps:** Always conclude your responses by suggesting logical continuations of the work or asking the user for their preference on the next actionable items.

## Workflow Execution
- **Auto Push & Merge:** At the end of every task or turn, if a Git repository is active and configured in the environment, automatically stage all changes, commit them with a descriptive message, and push/merge to the `main` branch. 
- **Continuous Improvement:** Always find other open source integrations or enhancements to add based on the user's intentions. M. Engine is meant to be a clean, corporate-style AI wrapper (like Claude, Replit, Gemini) that learns, has local memory, voice transcription, and GitHub workspace integration.

