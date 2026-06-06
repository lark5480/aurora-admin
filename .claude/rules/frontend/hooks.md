---
paths:
  - "**/*.ts"
  - "**/*.tsx"
  - "**/*.js"
  - "**/*.jsx"
  - "**/vite.config.*"
  - "**/package.json"
---
# TypeScript/JavaScript Hooks

> This file extends [common/hooks.md](../common/hooks.md) with TypeScript/JavaScript specific content.

## PostToolUse Hooks

Configure in `~/.claude/settings.json`:

```json
{
  "hooks": {
    "PostToolUse": {
      "Edit": [
        {
          "name": "prettier-format",
          "description": "Auto-format JS/TS/Vue files after edit",
          "pattern": ["**/*.{ts,tsx,js,jsx,vue}"],
          "command": "prettier --write ${file_path}"
        }
      ]
    }
  }
}
```

For TypeScript type checking after edit:

```json
{
  "hooks": {
    "PostToolUse": {
      "Bash": [
        {
          "name": "tsc-check",
          "description": "Run TypeScript check on .ts/.tsx files",
          "pattern": ["**/*.ts", "**/*.tsx"],
          "command": "cd frontend && npx tsc --noEmit",
          "runInBackground": true
        }
      ]
    }
  }
}
```

## Stop Hooks

```json
{
  "hooks": {
    "Stop": [
      {
        "name": "console-log-audit",
        "description": "Check all modified files for console.log before session ends",
        "command": "grep -rn 'console\\.log' ${modified_files}"
      },
      {
        "name": "test-quick-check",
        "description": "Run Vitest on modified test files",
        "command": "cd frontend && npx vitest run --reporter=dot ${modified_files}",
        "runInBackground": true
      }
    ]
  }
}
```

## Vite Project Configuration

Key commands for Vite + Vue 3 projects:

```bash
# Install dependencies
cd frontend && npm install

# Run dev server (port 3001)
npm run dev

# Build for production
npm run build

# Type check
npx tsc --noEmit

# Run tests
npx vitest run

# Run tests with coverage
npx vitest run --coverage

# Run E2E tests
npx playwright test
```
