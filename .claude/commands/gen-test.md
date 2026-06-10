# Generate Unit Test Command
请针对当前选中的 Java 类，基于 JUnit 5 和 Mockito，生成高覆盖率的单元测试类。
*   📋 测试类的命名需符合规范（`目标类名 + Test`）。
*   📝 测试方法结构需要清晰，并遵循 `given` / `when` / `then` 的风格。
*   🛠️ Service 层测试：对 Mapper 等外部依赖使用 `@Mock` 注解模拟。
*   🔑 Mapper 层测试：使用 `@SpringBootTest` 走真实数据库，验证 SQL 正确性。
*   📦 断言优先用 AssertJ：`assertThat(result).isEqualTo(expected)`。