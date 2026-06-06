# Generate Unit Test Command
请针对当前选中的 Java 类，基于 JUnit 5 和 Mockito，生成高覆盖率的单元测试类。
*   📋 测试类的命名需符合规范（`目标类名 + Test`）。
*   📝 测试方法结构需要清晰，并遵循 `given` / `when` / `then` 的风格。
*   🛠️ 对于外部依赖，使用 `@Mock` 注解进行模拟。
*   🔑 对于 MyBatis-Plus 的操作，直接使用实际 Service。