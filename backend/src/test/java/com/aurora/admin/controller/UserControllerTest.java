package com.aurora.admin.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.ChangePasswordRequest;
import com.aurora.admin.dto.CreateUserRequest;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.dto.UpdateUserRequest;
import com.aurora.admin.dto.UserVO;
import com.aurora.admin.entity.User;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.service.UserService;
import com.aurora.admin.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private UserController userController;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    private User testUser;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService);
        securityUtilsMock = mockStatic(SecurityUtils.class);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setNickname("Test");
        testUser.setRole("user");
        testUser.setStatus(1);

        // Clear SecurityContext before each test
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
        SecurityContextHolder.clearContext();
    }

    @Nested
    class GetUserById {

        @Test
        void shouldReturnUser_whenExists() {
            when(userService.findById(1L)).thenReturn(testUser);

            ApiResponse response = userController.getUserById(1L);

            assertEquals(200, response.getCode());
            assertNotNull(response.getData());
        }

        @Test
        void shouldThrowNotFoundException_whenNotExists() {
            when(userService.findById(999L)).thenReturn(null);

            assertThrows(NotFoundException.class, () -> userController.getUserById(999L));
        }
    }

    @Nested
    class GetUsers {

        @Test
        void shouldReturnPageResult() {
            PageResult<UserVO> pageResult = PageResult.of(
                    List.of(UserVO.from(testUser)), 1, 1, 10);
            when(userService.findAll(1, 10, "")).thenReturn(pageResult);

            ApiResponse response = userController.getUsers(1, 10, "");

            assertEquals(200, response.getCode());
            assertNotNull(response.getData());
        }
    }

    @Nested
    class Create {

        @Test
        void shouldCreateUserSuccessfully() {
            CreateUserRequest request = new CreateUserRequest();
            request.setUsername("newuser");
            request.setPassword("pass123");
            request.setEmail("new@test.com");
            request.setNickname("New");

            when(userService.create(any(CreateUserRequest.class))).thenReturn(testUser);

            ApiResponse response = userController.createUser(request);

            assertEquals(200, response.getCode());
            verify(userService).create(any(CreateUserRequest.class));
        }
    }

    @Nested
    class Update {

        @Test
        void shouldUpdateUserSuccessfully() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setNickname("Updated");

            ApiResponse response = userController.updateUser(1L, request);

            assertEquals(200, response.getCode());
            verify(userService).update(eq(1L), any(UpdateUserRequest.class));
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteUserSuccessfully() {
            ApiResponse response = userController.deleteUser(1L);

            assertEquals(200, response.getCode());
            verify(userService).delete(1L);
        }
    }

    @Nested
    class GetUserInfo {

        @Test
        void shouldReturnCurrentUser() {
            Authentication auth = new UsernamePasswordAuthenticationToken("testuser", null);
            SecurityContextHolder.getContext().setAuthentication(auth);
            when(userService.findByUsername("testuser")).thenReturn(testUser);

            ApiResponse response = userController.getUserInfo();

            assertEquals(200, response.getCode());
        }

        @Test
        void shouldThrowNotFoundException_whenNoAuthentication() {
            assertThrows(NotFoundException.class, () -> userController.getUserInfo());
        }
    }

    @Nested
    class GetProfile {

        @Test
        void shouldReturnProfile() {
            when(userService.getProfile()).thenReturn(testUser);

            ApiResponse response = userController.getProfile();

            assertEquals(200, response.getCode());
        }

        @Test
        void shouldThrowNotFoundException_whenProfileNull() {
            when(userService.getProfile()).thenReturn(null);

            assertThrows(NotFoundException.class, () -> userController.getProfile());
        }
    }

    @Nested
    class ChangePassword {

        @Test
        void shouldChangePassword() {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("oldPass");
            request.setNewPassword("newPass123");

            ApiResponse response = userController.changePassword(request);

            assertEquals(200, response.getCode());
            verify(userService).changePassword(eq(1L), eq("oldPass"), eq("newPass123"));
        }
    }

    @Nested
    class AssignRoles {

        @Test
        void shouldAssignRoles() {
            ApiResponse response = userController.assignRoles(1L, List.of(1L, 2L));

            assertEquals(200, response.getCode());
            verify(userService).assignRoles(eq(1L), eq(List.of(1L, 2L)));
        }
    }
}
