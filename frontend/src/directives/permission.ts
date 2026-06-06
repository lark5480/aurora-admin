import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '../stores/user'

export const vPermission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const userStore = useUserStore()
    const { value } = binding

    if (!value) return

    // 支持多种格式：单个权限 'system:user:delete' 或数组 ['system:user:add', 'system:user:delete']
    const permissions = Array.isArray(value) ? value : [value]

    const hasPermission = permissions.some((p) => userStore.hasPermission(p))

    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  },
}
