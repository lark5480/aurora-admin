package com.aurora.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.admin.dto.NoticeRequest;
import com.aurora.admin.dto.NoticeResponse;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.entity.Notice;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.NoticeMapper;
import com.aurora.admin.service.NoticeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 公告管理服务实现。提供公告的增删改查、发布撤回、定时发布与过期处理、用户可见范围过滤等业务逻辑。
 * 公告状态流转：DRAFT -> PUBLISHED -> EXPIRED / WITHDRAWN，WITHDRAWN 可重新发布为 PUBLISHED。
 */
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeServiceImpl.class);

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";
    private static final String TARGET_TYPE_ALL = "ALL";
    private static final String TARGET_TYPE_DEPT = "DEPT";
    private static final String TARGET_TYPE_USER = "USER";

    private final NoticeMapper noticeMapper;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public PageResult<NoticeResponse> findAllForManage(int page, int size, String status, String keyword) {
        Page<Notice> pagination = new Page<>(page, size);
        LambdaQueryWrapper<Notice> wrapper = Wrappers.lambdaQuery();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Notice::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Notice::getTitle, keyword);
        }
        wrapper.orderByDesc(Notice::getCreateTime);
        Page<Notice> result = noticeMapper.selectPage(pagination, wrapper);
        List<NoticeResponse> records = result.getRecords().stream().map(this::toResponse).toList();
        return PageResult.of(records, result.getTotal(), page, size);
    }

    @Override
    public NoticeResponse findById(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            return null;
        }
        return toResponse(notice);
    }

    @Override
    @Transactional
    public NoticeResponse create(NoticeRequest request, Long createBy) {
        Notice notice = new Notice();
        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setTargetType(request.targetType());
        notice.setTargetIds(toJson(request.targetIds()));
        notice.setPublishTime(request.publishTime());
        notice.setExpireTime(request.expireTime());
        notice.setStatus(STATUS_DRAFT);
        notice.setCreateBy(createBy);
        notice.setCreateTime(LocalDateTime.now());

        noticeMapper.insert(notice);
        return toResponse(notice);
    }

    @Override
    @Transactional
    public NoticeResponse update(Long id, NoticeRequest request) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new NotFoundException("公告", id);
        }
        if (STATUS_PUBLISHED.equals(notice.getStatus()) || STATUS_EXPIRED.equals(notice.getStatus())) {
            throw new IllegalStateException("已发布的公告不可编辑");
        }

        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setTargetType(request.targetType());
        notice.setTargetIds(toJson(request.targetIds()));
        notice.setPublishTime(request.publishTime());
        notice.setExpireTime(request.expireTime());

        noticeMapper.updateById(notice);
        return toResponse(notice);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new NotFoundException("公告", id);
        }
        if (STATUS_PUBLISHED.equals(notice.getStatus())) {
            throw new IllegalStateException("已发布的公告不可删除");
        }
        noticeMapper.deleteById(id);
    }

    @Override
    @Transactional
    public NoticeResponse publish(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new NotFoundException("公告", id);
        }
        if (!STATUS_DRAFT.equals(notice.getStatus()) && !STATUS_WITHDRAWN.equals(notice.getStatus())) {
            throw new IllegalStateException("只有草稿或已撤回的公告可以发布");
        }

        if (notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }

        notice.setStatus(STATUS_PUBLISHED);
        noticeMapper.updateById(notice);
        return toResponse(notice);
    }

    @Override
    @Transactional
    public NoticeResponse withdraw(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new NotFoundException("公告", id);
        }
        if (!STATUS_PUBLISHED.equals(notice.getStatus())) {
            throw new IllegalStateException("只有已发布的公告可以撤回");
        }

        notice.setStatus(STATUS_WITHDRAWN);
        noticeMapper.updateById(notice);
        return toResponse(notice);
    }

    @Override
    @Transactional
    public void processScheduledNotices() {
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Notice> publishWrapper = Wrappers.lambdaQuery();
        publishWrapper.eq(Notice::getStatus, STATUS_DRAFT)
                .le(Notice::getPublishTime, now)
                .isNotNull(Notice::getPublishTime);
        List<Notice> toPublish = noticeMapper.selectList(publishWrapper);
        for (Notice notice : toPublish) {
            notice.setStatus(STATUS_PUBLISHED);
            noticeMapper.updateById(notice);
            log.info("定时发布公告: id={}, title={}", notice.getId(), notice.getTitle());
        }

        LambdaQueryWrapper<Notice> expireWrapper = Wrappers.lambdaQuery();
        expireWrapper.eq(Notice::getStatus, STATUS_PUBLISHED)
                .le(Notice::getExpireTime, now)
                .isNotNull(Notice::getExpireTime);
        List<Notice> toExpire = noticeMapper.selectList(expireWrapper);
        for (Notice notice : toExpire) {
            notice.setStatus(STATUS_EXPIRED);
            noticeMapper.updateById(notice);
            log.info("公告已过期: id={}, title={}", notice.getId(), notice.getTitle());
        }
    }

    @Override
    public List<NoticeResponse> getVisibleNotices(Long userId, Long deptId) {
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Notice> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Notice::getStatus, STATUS_PUBLISHED)
                .and(w -> w.le(Notice::getPublishTime, now).or().isNull(Notice::getPublishTime))
                .and(w -> w.isNull(Notice::getExpireTime).or().gt(Notice::getExpireTime, now));

        List<Notice> notices = noticeMapper.selectList(wrapper);

        return notices.stream()
                .filter(notice -> isVisible(notice, userId, deptId))
                .map(this::toResponse)
                .toList();
    }

    private boolean isVisible(Notice notice, Long userId, Long deptId) {
        String targetType = notice.getTargetType();
        if (TARGET_TYPE_ALL.equals(targetType)) {
            return true;
        }
        if (TARGET_TYPE_DEPT.equals(targetType)) {
            List<Long> targetIds = fromJson(notice.getTargetIds());
            return targetIds.contains(deptId);
        }
        if (TARGET_TYPE_USER.equals(targetType)) {
            List<Long> targetIds = fromJson(notice.getTargetIds());
            return targetIds.contains(userId);
        }
        return false;
    }

    private NoticeResponse toResponse(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getTargetType(),
                fromJson(notice.getTargetIds()),
                notice.getPublishTime(),
                notice.getExpireTime(),
                notice.getStatus(),
                notice.getCreateTime()
        );
    }

    private String toJson(List<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(targetIds);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return null;
        }
    }

    private List<Long> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON反序列化失败: {}", json, e);
            return Collections.emptyList();
        }
    }
}
