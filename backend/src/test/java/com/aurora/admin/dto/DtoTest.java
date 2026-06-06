package com.aurora.admin.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void shouldCreatePageResult() {
        List<String> items = List.of("a", "b", "c");
        PageResult<String> result = PageResult.of(items, 3, 1, 10);

        assertEquals(3, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(3, result.getList().size());
    }

    @Test
    void shouldCreateEmptyPageResult() {
        PageResult<String> result = PageResult.of(List.of(), 0, 1, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getList().isEmpty());
    }
}

class UserVOTest {

    @Test
    void shouldCreateFromUser() {
        UserVO userVO = new UserVO();
        userVO.setId(1L);
        userVO.setUsername("test");
        userVO.setEmail("test@test.com");
        userVO.setNickname("Test");
        userVO.setRole("user");
        userVO.setStatus(1);

        assertNotNull(userVO);
        assertEquals("test", userVO.getUsername());
        // UserVO should NOT have a password field - it's intentionally excluded
        assertDoesNotThrow(() -> {
            try {
                UserVO.class.getDeclaredField("password");
                fail("password field should not exist in UserVO");
            } catch (NoSuchFieldException expected) {
                // expected
            }
        });
    }

    @Test
    void shouldReturnNull_fromNullUser() {
        assertNull(UserVO.from(null));
    }
}

class NoticeRequestTest {

    @Test
    void shouldCreateNoticeRequest() {
        NoticeRequest request = new NoticeRequest(
                "标题", "内容", "ALL",
                List.of(1L, 2L),
                LocalDateTime.now(), LocalDateTime.now().plusDays(7));

        assertEquals("标题", request.title());
        assertEquals("ALL", request.targetType());
        assertEquals(2, request.targetIds().size());
    }
}
