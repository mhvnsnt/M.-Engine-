import fs from 'fs'

const path = '/app/applet/m-engine-web/src/screens/Chat.jsx'
let content = fs.readFileSync(path, 'utf-8')

// Add syncFromCanonical import
content = content.replace("import { appendMessage, clearMessages, isAvailable, loadMessages }", "import { appendMessage, clearMessages, isAvailable, loadMessages, syncFromCanonical }")

// Add sync call to loadMessages
content = content.replace(
"    loadMessages()\n      .then(setMessages)", 
"    syncFromCanonical().then(() => loadMessages()).then(setMessages)"
)

fs.writeFileSync(path, content)
