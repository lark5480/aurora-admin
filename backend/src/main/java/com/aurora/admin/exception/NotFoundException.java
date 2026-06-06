package com.aurora.admin.exception;

/**
 * 资源未找到异常（404）
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String resource) {
        super(404, resource + "不存在");
    }

    public NotFoundException(String resource, Object id) {
        super(404, resource + "不存在: " + id);
    }
}
