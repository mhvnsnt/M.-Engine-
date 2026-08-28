with open(".github/workflows/alpha-release.yml", "r") as f:
    code = f.read()

permissions_block = """
jobs:
  build-and-verify:
    name: Build & Verify Reality Evidence
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: read
"""

code = code.replace("jobs:\n  build-and-verify:\n    name: Build & Verify Reality Evidence\n    runs-on: ubuntu-latest", permissions_block.strip())

with open(".github/workflows/alpha-release.yml", "w") as f:
    f.write(code)
