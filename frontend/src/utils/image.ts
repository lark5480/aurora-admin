/**
 * 解析图片 URL，兼容两种存储格式：
 * - 旧格式: /api/files/{id}/view (走 controller 查 DB)
 * - 新格式: /uploads/{relativePath} (走静态资源，性能更好)
 */
export const resolveImageUrl = (url: string): string => {
  if (!url) return ''
  return url
}
