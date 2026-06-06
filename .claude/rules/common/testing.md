# Testing Requirements

## Minimum Test Coverage: 80%

Test Types (ALL required):
1. **Unit Tests** - Individual functions, utilities, components
2. **Integration Tests** - API endpoints, database operations
3. **E2E Tests** - Critical user flows

## Test-Driven Development

MANDATORY workflow:
1. Write test first (RED)
2. Run test - it should FAIL
3. Write minimal implementation (GREEN)
4. Run test - it should PASS
5. Refactor (IMPROVE)
6. Verify coverage (80%+)

## Test Structure (AAA Pattern)

Prefer Arrange-Act-Assert structure for tests:

```text
test('description', () => {
  // Arrange - setup inputs and expected values
  const input = { ... }

  // Act - execute the behavior under test
  const result = execute(input)

  // Assert - verify the outcome matches expectations
  expect(result).toBe(expected)
})
```

## Test Naming

Use descriptive names that explain the behavior under test:

```text
test('returns empty array when no markets match query', () => {})
test('throws error when API key is missing', () => {})
test('falls back to substring search when Redis is unavailable', () => {})
```

## Troubleshooting Test Failures

1. Use **test-driven-development** skill
2. Check test isolation
3. Verify mocks are correct
4. Fix implementation, not tests (unless tests are wrong)

## Agent Support

- **test-driven-development** - Use PROACTIVELY for new features, enforces write-tests-first
