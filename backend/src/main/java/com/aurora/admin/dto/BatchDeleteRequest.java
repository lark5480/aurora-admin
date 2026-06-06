package com.aurora.admin.dto;

import java.util.List;

public record BatchDeleteRequest(List<Long> ids) {}
