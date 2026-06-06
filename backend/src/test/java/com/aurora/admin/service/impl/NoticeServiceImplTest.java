package com.aurora.admin.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aurora.admin.dto.NoticeRequest;
import com.aurora.admin.dto.NoticeResponse;
import com.aurora.admin.entity.Notice;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.NoticeMapper;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {

    @Mock
    private NoticeMapper noticeMapper;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private Notice draftNotice;
    private Notice publishedNotice;

    @BeforeEach
    void setUp() {
        draftNotice = new Notice();
        draftNotice.setId(1L);
        draftNotice.setTitle("测试公告");
        draftNotice.setContent("公告内容");
        draftNotice.setTargetType("ALL");
        draftNotice.setTargetIds(null);
        draftNotice.setStatus("DRAFT");
        draftNotice.setCreateBy(1L);
        draftNotice.setCreateTime(LocalDateTime.now());

        publishedNotice = new Notice();
        publishedNotice.setId(2L);
        publishedNotice.setTitle("已发布公告");
        publishedNotice.setContent("已发内容");
        publishedNotice.setTargetType("ALL");
        publishedNotice.setStatus("PUBLISHED");
        publishedNotice.setCreateTime(LocalDateTime.now().minusDays(1));
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnNotice_whenExists() {
            when(noticeMapper.selectById(1L)).thenReturn(draftNotice);
            NoticeResponse result = noticeService.findById(1L);
            assertNotNull(result);
            assertEquals("测试公告", result.title());
        }

        @Test
        void shouldReturnNull_whenNotExists() {
            when(noticeMapper.selectById(999L)).thenReturn(null);
            NoticeResponse result = noticeService.findById(999L);
            assertNull(result);
        }
    }

    @Nested
    class Create {

        @Test
        void shouldCreateDraftNotice() {
            NoticeRequest request = new NoticeRequest(
                    "新公告", "新内容", "ALL",
                    List.of(), LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(7));

            when(noticeMapper.insert(any(Notice.class))).thenReturn(1);

            NoticeResponse result = noticeService.create(request, 1L);

            assertNotNull(result);
            assertEquals("新公告", result.title());
            assertEquals("DRAFT", result.status());
            verify(noticeMapper).insert(argThat(n ->
                    n.getStatus().equals("DRAFT") && n.getCreateBy().equals(1L)));
        }

        @Test
        void shouldSerializeTargetIdsToJson() {
            NoticeRequest request = new NoticeRequest(
                    "定向公告", "内容", "USER",
                    List.of(1L, 2L, 3L), null, null);

            noticeService.create(request, 1L);

            verify(noticeMapper).insert(argThat(n ->
                    n.getTargetIds() != null && n.getTargetIds().contains("1") && n.getTargetIds().contains("3")));
        }
    }

    @Nested
    class Update {

        @Test
        void shouldUpdateDraftNotice() {
            when(noticeMapper.selectById(1L)).thenReturn(draftNotice);

            NoticeRequest request = new NoticeRequest(
                    "更新标题", "更新内容", "DEPT",
                    List.of(5L), null, null);

            NoticeResponse result = noticeService.update(1L, request);

            assertEquals("更新标题", result.title());
            verify(noticeMapper).updateById(draftNotice);
        }

        @Test
        void shouldThrowNotFoundException_whenNotExists() {
            when(noticeMapper.selectById(999L)).thenReturn(null);

            NoticeRequest request = new NoticeRequest("标题", "内容", "ALL", List.of(), null, null);

            assertThrows(NotFoundException.class, () -> noticeService.update(999L, request));
        }

        @Test
        void shouldThrowException_whenUpdatingPublishedNotice() {
            when(noticeMapper.selectById(2L)).thenReturn(publishedNotice);

            NoticeRequest request = new NoticeRequest("标题", "内容", "ALL", List.of(), null, null);

            assertThrows(IllegalStateException.class, () -> noticeService.update(2L, request));
            verify(noticeMapper, never()).updateById(any());
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteDraftNotice() {
            when(noticeMapper.selectById(1L)).thenReturn(draftNotice);

            noticeService.delete(1L);

            verify(noticeMapper).deleteById(1L);
        }

        @Test
        void shouldThrowNotFoundException_whenNotExists() {
            when(noticeMapper.selectById(999L)).thenReturn(null);

            assertThrows(NotFoundException.class, () -> noticeService.delete(999L));
        }

        @Test
        void shouldThrowException_whenDeletingPublishedNotice() {
            when(noticeMapper.selectById(2L)).thenReturn(publishedNotice);

            assertThrows(IllegalStateException.class, () -> noticeService.delete(2L));
            verify(noticeMapper, never()).deleteById(anyLong());
        }
    }

    @Nested
    class Publish {

        @Test
        void shouldPublishDraftNotice() {
            when(noticeMapper.selectById(1L)).thenReturn(draftNotice);

            NoticeResponse result = noticeService.publish(1L);

            assertEquals("PUBLISHED", result.status());
            verify(noticeMapper).updateById(argThat(n -> n.getStatus().equals("PUBLISHED")));
        }

        @Test
        void shouldThrowException_whenPublishingNonDraftNotice() {
            when(noticeMapper.selectById(2L)).thenReturn(publishedNotice);

            assertThrows(IllegalStateException.class, () -> noticeService.publish(2L));
        }

        @Test
        void shouldSetPublishTime_whenNull() {
            draftNotice.setPublishTime(null);
            when(noticeMapper.selectById(1L)).thenReturn(draftNotice);

            noticeService.publish(1L);

            assertNotNull(draftNotice.getPublishTime());
        }
    }

    @Nested
    class Withdraw {

        @Test
        void shouldWithdrawPublishedNotice() {
            when(noticeMapper.selectById(2L)).thenReturn(publishedNotice);

            NoticeResponse result = noticeService.withdraw(2L);

            assertEquals("WITHDRAWN", result.status());
            verify(noticeMapper).updateById(publishedNotice);
        }

        @Test
        void shouldThrowException_whenWithdrawingNonPublishedNotice() {
            when(noticeMapper.selectById(1L)).thenReturn(draftNotice);

            assertThrows(IllegalStateException.class, () -> noticeService.withdraw(1L));
        }
    }

    @Nested
    class Visibility {

        @Test
        void shouldFilterByTargetType() {
            Notice userTargeted = new Notice();
            userTargeted.setId(3L);
            userTargeted.setTitle("定向公告");
            userTargeted.setStatus("PUBLISHED");
            userTargeted.setPublishTime(LocalDateTime.now().minusDays(1));
            userTargeted.setTargetType("USER");
            userTargeted.setTargetIds("[1,5,10]");

            Notice deptTargeted = new Notice();
            deptTargeted.setId(4L);
            deptTargeted.setTitle("部门公告");
            deptTargeted.setStatus("PUBLISHED");
            deptTargeted.setPublishTime(LocalDateTime.now().minusDays(1));
            deptTargeted.setTargetType("DEPT");
            deptTargeted.setTargetIds("[2,3]");

            when(noticeMapper.selectList(any())).thenReturn(List.of(userTargeted, deptTargeted));

            // userId=5, deptId=1 → only userTargeted matches
            List<NoticeResponse> result = noticeService.getVisibleNotices(5L, 1L);

            assertEquals(1, result.size());
            assertEquals("定向公告", result.get(0).title());
        }

        @Test
        void shouldReturnAllForTargetAll() {
            Notice allTarget = new Notice();
            allTarget.setId(3L);
            allTarget.setTitle("全员公告");
            allTarget.setStatus("PUBLISHED");
            allTarget.setPublishTime(LocalDateTime.now().minusDays(1));
            allTarget.setTargetType("ALL");

            when(noticeMapper.selectList(any())).thenReturn(List.of(allTarget));

            List<NoticeResponse> result = noticeService.getVisibleNotices(1L, 1L);

            assertEquals(1, result.size());
            assertEquals("全员公告", result.get(0).title());
        }
    }
}
