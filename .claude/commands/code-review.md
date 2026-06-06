# Code Review Command
作为资深代码审查专家，审查当前选中或变更的代码，参照项目 `.claude/rules/` 规范重点检查：
*   Java 代码：命名规范（camelCase/PascalCase）、不可变性、分层职责、JPA 查询性能、异常处理。
*   安全性：SQL 注入（MyBatis-Plus 参数化）、JWT Token 处理、输入校验、敏感信息泄露。
*   前端代码：Vue 3 Composition API、Pinia Setup Store 规范、Element Plus 使用、TypeScript 类型安全。
*   通用：KISS/DRY/YAGNI 原则、文件行数（<800）、函数行数（<50）。
输出问题列表（严重/一般/建议）及修改方案。