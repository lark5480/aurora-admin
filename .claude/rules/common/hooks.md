# Hooks System

## Hook Types

- **PreToolUse**: Before tool execution (validation, parameter modification)
- **PostToolUse**: After tool execution (auto-format, checks)
- **Stop**: When session ends (final verification)

## Auto-Accept Permissions

Use with caution:
- Enable for trusted, well-defined plans
- Disable for exploratory work
- Never use dangerously-skip-permissions flag
- Configure `allowedTools` in `~/.claude.json` instead

## TodoWrite Best Practices

Use TodoWrite tool to:
- Track progress on multi-step tasks
- Verify understanding of instructions
- Enable real-time steering
- Show granular implementation steps

Todo list reveals:
- Out of order steps
- Missing items
- Extra unnecessary items
- Wrong granularity
- Misinterpreted requirements

## Settings Configuration

Configure PostToolUse hooks in `~/.claude/settings.json`:

```json
{
  "allowedTools": ["Bash", "Read", "Edit", "Write", "Glob", "Grep"],
  "hooks": {
    "PostToolUse": {
      "Edit": [
        {
          "name": "prettier-format",
          "description": "Format JS/TS files after edit",
          "pattern": ["**/*.{ts,tsx,js,jsx}"],
          "command": "prettier --write ${file_path}"
        }
      ]
    },
    "Stop": [
      {
        "name": "console-log-audit",
        "description": "Check for console.log before session ends",
        "command": "grep -r 'console\\.log' ${modified_files}"
      }
    ]
  }
}
```
