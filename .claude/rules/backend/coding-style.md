---
paths:
  - "**/*.java"
---
# Java Coding Style

> This file extends [common/coding-style.md](../common/coding-style.md) with Java-specific content.

## Formatting

- **google-java-format** or **Checkstyle** (Google or Sun style) for enforcement
- One public top-level type per file
- Consistent indent: 2 or 4 spaces (match project standard)
- Member order: constants, fields, constructors, public methods, protected, private

## Immutability

- Prefer `record` for value types (Java 16+)
- Mark fields `final` by default — use mutable state only when required
- Return defensive copies from public APIs: `List.copyOf()`, `Map.copyOf()`, `Set.copyOf()`
- Copy-on-write: return new instances rather than mutating existing ones

```java
// GOOD — immutable value type
public record OrderSummary(Long id, String customerName, BigDecimal total) {}

// GOOD — final fields, no setters
public class Order {
    private final Long id;
    private final List<LineItem> items;

    public List<LineItem> getItems() {
        return List.copyOf(items);
    }
}
```

## Naming

Follow standard Java conventions:
- `PascalCase` for classes, interfaces, records, enums
- `camelCase` for methods, fields, parameters, local variables
- `SCREAMING_SNAKE_CASE` for `static final` constants
- Packages: all lowercase, reverse domain (`com.example.app.service`)

## Modern Java Features

Use modern language features where they improve clarity:
- **Records** for DTOs and value types (Java 16+)
- **Sealed classes** for closed type hierarchies (Java 17+)
- **Pattern matching** with `instanceof` — no explicit cast (Java 16+)
- **Text blocks** for multi-line strings — SQL, JSON templates (Java 15+)
- **Switch expressions** with arrow syntax (Java 14+)
- **Pattern matching in switch** — exhaustive sealed type handling (Java 21+)

```java
// Pattern matching instanceof
if (shape instanceof Circle c) {
    return Math.PI * c.radius() * c.radius();
}

// Sealed type hierarchy
public sealed interface PaymentMethod permits CreditCard, BankTransfer, Wallet {}

// Switch expression
String label = switch (status) {
    case ACTIVE -> "Active";
    case SUSPENDED -> "Suspended";
    case CLOSED -> "Closed";
};
```

## Optional Usage

- Return `Optional<T>` from finder methods that may have no result
- Use `map()`, `flatMap()`, `orElseThrow()` — never call `get()` without `isPresent()`
- Never use `Optional` as a field type or method parameter

```java
// GOOD
return repository.findById(id)
    .map(ResponseDto::from)
    .orElseThrow(() -> new OrderNotFoundException(id));

// BAD — Optional as parameter
public void process(Optional<String> name) {}
```

## Error Handling

- Prefer unchecked exceptions for domain errors
- Create domain-specific exceptions extending `RuntimeException`
- Avoid broad `catch (Exception e)` unless at top-level handlers
- Include context in exception messages

```java
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("Order not found: id=" + id);
    }
}
```

## Streams

- Use streams for transformations; keep pipelines short (3-4 operations max)
- Prefer method references when readable: `.map(Order::getTotal)`
- Avoid side effects in stream operations
- For complex logic, prefer a loop over a convoluted stream pipeline

## MyBatis-Plus Entity

All entities MUST have MyBatis-Plus annotations:

```java
@TableName("t_order")           // Required - maps to table name
public class Order {
    @TableId(type = IdType.AUTO)  // Required for auto-increment PK
    private Long id;

    @TableField("customer_name")  // Only when column name differs from field
    private String customerName;

    private BigDecimal total;
}
```

- Use `@TableName` on class level
- Use `@TableId(type = IdType.AUTO)` for auto-increment primary key
- Use `@TableField` only when column name differs from Java field name (e.g., snake_case vs camelCase)
- Do NOT use `@Data` on Entity — use `@Getter @Setter @ToString(exclude = "password")` to avoid exposing sensitive fields in toString()

## DTO

Use class with `@Data` OR `record`:
- **record** (recommended for simple DTOs): `public record UserRequest(String name, String email) {}`
- **class + @Data** (acceptable for complex DTOs with builder patterns)

```java
// record DTO - preferred for simple cases
public record RegisterRequest(
    @NotBlank(message = "用户名不能为空")
    String username,

    @NotBlank(message = "密码不能为空")
    String password
) {}

// class DTO - acceptable for validation with groups
public class CreateOrderRequest {
    @NotNull(message = "Customer required")
    private String customerName;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal total;
}
```

## Dependency Injection

Prefer **constructor injection** for testability and immutability:

```java
// GOOD — constructor injection
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public OrderService(OrderRepository orderRepository, PaymentGateway paymentGateway) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
    }
}

// ACCEPTABLE — @Autowired field injection (legacy code)
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
}
```

New code should use constructor injection. Field injection is acceptable only for existing code.

## References

See skill: `java-coding-standards` for full coding standards with examples.
See skill: `jpa-patterns` for JPA/Hibernate entity design patterns.
