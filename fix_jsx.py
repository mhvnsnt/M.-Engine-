import re

path = "m-engine-web/src/App.jsx"
with open(path, "r") as f:
    content = f.read()

content = content.replace("> Executing Universal Reality Loop...", "&gt; Executing Universal Reality Loop...")
content = content.replace("> UNDERSTAND: Parsing mission objectives...", "&gt; UNDERSTAND: Parsing mission objectives...")
content = content.replace("> RETRIEVE: Checking capability graph for existing Web UI bounds...", "&gt; RETRIEVE: Checking capability graph for existing Web UI bounds...")
content = content.replace("> Awaiting external worker delegation...", "&gt; Awaiting external worker delegation...")

with open(path, "w") as f:
    f.write(content)
