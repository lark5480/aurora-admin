---
paths:
  - "**/*.java"
---
# Java Testing

> This file extends [common/testing.md](../common/testing.md) with Java-specific content.

## Test Framework

- **JUnit 5** (`@Test`, `@ParameterizedTest`, `@Nested`, `@DisplayName`)
- **AssertJ** for fluent assertions (`assertThat(result).isEqualTo(expected)`)
- **Mockito** for mocking dependencies
- **Testcontainers** for integration tests requiring databases or services

## Test Organization

```
src/test/java/com/example/app/
  service/           # Unit tests for service layer
  controller/        # Web layer / API tests
  repository/        # Data access tests
  integration/       # Cross-layer integration tests
```

Mirror the `src/main/java` package structure in `src/test/java`.

## Unit Test Pattern

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("findById returns order when exists")
    void findById_existingOrder_returnsOrder() {
        var order = new Order(1L, "Alice", BigDecimal.TEN);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var result = orderService.findById(1L);

        assertThat(result.customerName()).isEqualTo("Alice");
        verify(orderRepository).findById(1L);
    }

    @Test
    @DisplayName("findById throws when order not found")
    void findById_missingOrder_throws() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L))
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessageContaining("99");
    }
}
```

## Parameterized Tests

```java
@ParameterizedTest
@CsvSource({
    "100.00, 10, 90.00",
    "50.00, 0, 50.00",
    "200.00, 25, 150.00"
})
@DisplayName("discount applied correctly")
void applyDiscount(BigDecimal price, int pct, BigDecimal expected) {
    assertThat(PricingUtils.discount(price, pct)).isEqualByComparingTo(expected);
}
```

## Spring Boot Integration Tests

Use `@SpringBootTest` with `MockMvc` for web layer tests:

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /orders returns 201 with valid request")
    void createOrder_validRequest_returns201() throws Exception {
        var request = new CreateOrderRequest("Alice", BigDecimal.TEN);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.customer").value("Alice"));
    }

    @Test
    @DisplayName("POST /orders returns 400 with invalid request")
    void createOrder_invalidRequest_returns400() throws Exception {
        var request = Map.of("customerName", "", "total", -1);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
```

For repository integration tests, use `@DataJpaTest` or Testcontainers.

## Mapper/Repository Tests

Test MyBatis-Plus mappers with an in-memory database:

```java
@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    @DisplayName("insert generates id and findByUsername works")
    void insert_and_findByUsername() {
        var user = new User();
        user.setUsername("testuser");
        user.setPassword("encoded");
        user.setEmail("test@example.com");

        userMapper.insert(user);

        var found = userMapper.findByUsername("testuser");
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("test@example.com");
    }
}
```

## Test Naming

Use descriptive names with `@DisplayName`:
- `methodName_scenario_expectedBehavior()` for method names
- `@DisplayName("human-readable description")` for reports

## Coverage

- Target 80%+ line coverage
- Use JaCoCo for coverage reporting
- Focus on service and domain logic — skip trivial getters/config classes

## References

See skill: `springboot-tdd` for Spring Boot TDD patterns with MockMvc and Testcontainers.
See skill: `java-coding-standards` for testing expectations.
