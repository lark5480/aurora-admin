---
paths:
  - "**/*.java"
---
# Java Security

> This file extends [common/security.md](../common/security.md) with Java-specific content.

## Secrets Management

- Never hardcode API keys, tokens, or credentials in source code
- Use environment variables: `System.getenv("API_KEY")`
- Use a secret manager (Vault, AWS Secrets Manager) for production secrets
- Keep local config files with secrets in `.gitignore`

```java
// BAD
private static final String API_KEY = "sk-abc123...";

// GOOD — environment variable
String apiKey = System.getenv("PAYMENT_API_KEY");
Objects.requireNonNull(apiKey, "PAYMENT_API_KEY must be set");
```

## SQL Injection Prevention

**CRITICAL**: Always use parameterized queries — never concatenate user input into SQL.

### MyBatis-Plus `#{}` vs `${}`

```java
// SAFE — #{} uses PreparedStatement parameter binding
@Select("SELECT * FROM t_user WHERE username = #{username}")
User findByUsername(String username);

// UNSAFE — ${} is string-replaced, vulnerable to SQL injection
@Select("SELECT * FROM t_user WHERE username = '${username}'")  // NEVER DO THIS
```

Only use `${}` for safe, controlled values like table names or ORDER BY columns (never user input):

```java
// ACCEPTABLE — dynamic ORDER BY column (not user data)
@Select("SELECT * FROM t_user ORDER BY ${sortColumn} ${sortDirection}")
List<User> findAllSorted(@Param("sortColumn") String sortColumn, @Param("sortDirection") String sortDirection);

// NEVER do this with user input
@Select("SELECT * FROM t_user WHERE name = '${name}'")  // SQL INJECTION!
```

### JDBC Template

```java
// GOOD — parameterized query
jdbcTemplate.query("SELECT * FROM orders WHERE name = ?", mapper, name);

// BAD — string concatenation
jdbcTemplate.query("SELECT * FROM orders WHERE name = '" + name + "'", mapper);  // SQL INJECTION!
```

## Input Validation

- Validate all user input at system boundaries before processing
- Use Bean Validation (`@NotNull`, `@NotBlank`, `@Size`) on DTOs
- Sanitize file paths and user-provided strings before use
- Reject input that fails validation with clear error messages

```java
public Order createOrder(String customerName, BigDecimal amount) {
    if (customerName == null || customerName.isBlank()) {
        throw new IllegalArgumentException("Customer name is required");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Amount must be positive");
    }
    return new Order(customerName, amount);
}
```

## Authentication and Authorization

- Never implement custom auth crypto — use established libraries
- Store passwords with bcrypt or Argon2, never MD5/SHA1
- Enforce authorization checks at service boundaries
- Clear sensitive data from logs — never log passwords, tokens, or PII

## Dependency Security

- Run `mvn dependency:tree` or `./gradlew dependencies` to audit transitive dependencies
- Use OWASP Dependency-Check or Snyk to scan for known CVEs
- Keep dependencies updated — set up Dependabot or Renovate

## Error Messages

- Never expose stack traces, internal paths, or SQL errors in API responses
- Map exceptions to safe, generic client messages at handler boundaries
- Log detailed errors server-side; return generic messages to clients

```java
// Log the detail, return a generic message
try {
    return orderService.findById(id);
} catch (OrderNotFoundException ex) {
    log.warn("Order not found: id={}", id);
    return ApiResponse.error("Resource not found");
} catch (Exception ex) {
    log.error("Unexpected error processing order id={}", id, ex);
    return ApiResponse.error("Internal server error");
}
```

## References

See skill: `springboot-security` for Spring Security authentication and authorization patterns.
See skill: `security-review` for general security checklists.
