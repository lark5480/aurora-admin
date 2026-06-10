# Generate API Documentation
分析当前选中的 Spring Boot Controller 文件，解析 @RequestMapping/@PostMapping/@GetMapping 等注解，生成标准 Markdown 格式文档，包括：
*   接口路径（URL）和请求方法。
*   请求参数（名称、类型、是否必填、描述），从 @RequestParam/@PathVariable/@RequestBody 提取。
*   请求/响应 JSON 示例（统一使用 `ApiResponse<T>` 包装）。
*   错误码说明（对应 `exception/` 包中的异常定义）。