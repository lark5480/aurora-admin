package com.aurora.admin.service.impl;

import com.aurora.admin.dto.CreateUserRequest;
import com.aurora.admin.dto.PageResult;
import com.aurora.admin.dto.UpdateUserRequest;
import com.aurora.admin.dto.UserVO;
import com.aurora.admin.entity.Role;
import com.aurora.admin.entity.User;
import com.aurora.admin.exception.BusinessException;
import com.aurora.admin.exception.NotFoundException;
import com.aurora.admin.mapper.RoleMapper;
import com.aurora.admin.mapper.UserMapper;
import com.aurora.admin.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setNickname("Test User");
        testUser.setRole("user");
        testUser.setStatus(1);

        userRole = new Role();
        userRole.setId(1L);
        userRole.setCode("USER");
        userRole.setName("普通用户");
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Nested
    class FindByUsername {

        @Test
        void shouldReturnUser_whenExists() {
            when(userMapper.findByUsername("testuser")).thenReturn(testUser);
            User result = userService.findByUsername("testuser");
            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
            verify(userMapper).findByUsername("testuser");
        }

        @Test
        void shouldReturnNull_whenNotExists() {
            when(userMapper.findByUsername("nonexistent")).thenReturn(null);
            User result = userService.findByUsername("nonexistent");
            assertNull(result);
        }
    }

    @Nested
    class FindByEmail {

        @Test
        void shouldReturnUser_whenExists() {
            when(userMapper.findByEmail("test@example.com")).thenReturn(testUser);
            User result = userService.findByEmail("test@example.com");
            assertNotNull(result);
            assertEquals("test@example.com", result.getEmail());
        }
    }

    @Nested
    class Register {

        @Test
        void shouldCreateUserWithEncodedPassword() {
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(roleMapper.findByCode("USER")).thenReturn(userRole);
            doAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(1L); // simulate auto-generated key
                return 1;
            }).when(userMapper).insert(any(User.class));

            User result = userService.register("newuser", "password123", "new@example.com");

            assertNotNull(result);
            assertEquals("newuser", result.getUsername());
            assertEquals("encodedPassword", result.getPassword());
            assertEquals("user", result.getRole());
            assertEquals(1, result.getStatus());
            verify(passwordEncoder).encode("password123");
            verify(userMapper).insert(any(User.class));
            verify(roleMapper).insertUserRole(eq(1L), eq(1L));
        }

        @Test
        void shouldAssignUserRole_byDefault() {
            when(passwordEncoder.encode("pass")).thenReturn("encoded");
            when(roleMapper.findByCode("USER")).thenReturn(userRole);
            doAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(2L);
                return 1;
            }).when(userMapper).insert(any(User.class));

            userService.register("user1", "pass", "user1@test.com");

            verify(roleMapper).findByCode("USER");
            verify(roleMapper).insertUserRole(eq(2L), eq(1L));
        }
    }

    @Nested
    class CreateWithDTO {

        @Test
        void shouldCreateUser_fromCreateUserRequest() {
            CreateUserRequest request = new CreateUserRequest();
            request.setUsername("admin01");
            request.setPassword("admin123");
            request.setEmail("admin@test.com");
            request.setNickname("Admin");
            request.setRole("admin");

            when(passwordEncoder.encode("admin123")).thenReturn("encodedAdmin");
            Role adminRole = new Role();
            adminRole.setId(2L);
            adminRole.setCode("ADMIN");
            when(roleMapper.findByCode("ADMIN")).thenReturn(adminRole);
            doAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(3L);
                return 1;
            }).when(userMapper).insert(any(User.class));

            User result = userService.create(request);

            assertNotNull(result);
            assertEquals("admin01", result.getUsername());
            assertEquals("encodedAdmin", result.getPassword());
            assertEquals("admin", result.getRole());
            verify(roleMapper).findByCode("ADMIN");
            verify(roleMapper).insertUserRole(eq(3L), eq(2L));
        }

        @Test
        void shouldDefaultRoleToUser_whenRoleIsNull() {
            CreateUserRequest request = new CreateUserRequest();
            request.setUsername("user01");
            request.setPassword("pass123");
            request.setEmail("user@test.com");
            request.setRole(null);

            when(passwordEncoder.encode("pass123")).thenReturn("encoded");
            when(roleMapper.findByCode("USER")).thenReturn(userRole);
            doAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(4L);
                return 1;
            }).when(userMapper).insert(any(User.class));

            User result = userService.create(request);

            assertEquals("user", result.getRole());
        }
    }

    @Nested
    class FindAll {

        @Test
        void shouldReturnPageResult() {
            List<User> users = List.of(testUser);
            when(userMapper.findAll(0, 10, "")).thenReturn(users);
            when(userMapper.count("")).thenReturn(1);

            PageResult<UserVO> result = userService.findAll(1, 10, "");

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getList().size());
            assertEquals("testuser", result.getList().get(0).getUsername());
            assertNull(result.getList().get(0).getAvatar()); // optional field
            assertEquals(1, result.getPage());
            assertEquals(10, result.getSize());
        }

        @Test
        void shouldReturnEmptyResult_whenNoUsers() {
            when(userMapper.findAll(0, 10, "notfound")).thenReturn(List.of());
            when(userMapper.count("notfound")).thenReturn(0);

            PageResult<UserVO> result = userService.findAll(1, 10, "notfound");

            assertEquals(0, result.getTotal());
            assertTrue(result.getList().isEmpty());
        }
    }

    @Nested
    class UpdateWithDTO {

        @Test
        void shouldUpdateUserFields() {
            when(userMapper.findById(1L)).thenReturn(testUser);

            UpdateUserRequest request = new UpdateUserRequest();
            request.setNickname("New Nick");
            request.setEmail("newemail@test.com");

            userService.update(1L, request);

            assertEquals("New Nick", testUser.getNickname());
            assertEquals("newemail@test.com", testUser.getEmail());
            verify(userMapper).update(testUser);
        }

        @Test
        void shouldThrowNotFoundException_whenUserNotExists() {
            when(userMapper.findById(999L)).thenReturn(null);

            UpdateUserRequest request = new UpdateUserRequest();
            request.setNickname("Nick");

            assertThrows(NotFoundException.class, () -> userService.update(999L, request));
        }

        @Test
        void shouldNotUpdateNullFields() {
            User existing = new User();
            existing.setId(1L);
            existing.setUsername("user1");
            existing.setEmail("old@test.com");
            existing.setNickname("Old Nick");
            when(userMapper.findById(1L)).thenReturn(existing);

            UpdateUserRequest request = new UpdateUserRequest();
            // only set email, nickname stays null

            userService.update(1L, request);

            // nickname should remain unchanged because request.nickname is null
            assertEquals("Old Nick", existing.getNickname());
            // email should also remain unchanged because request.email is null
            assertEquals("old@test.com", existing.getEmail());
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteUser() {
            when(userMapper.findById(1L)).thenReturn(testUser);

            userService.delete(1L);

            verify(userMapper).delete(1L);
        }

        @Test
        void shouldThrowNotFoundException_whenUserNotExists() {
            when(userMapper.findById(999L)).thenReturn(null);

            assertThrows(NotFoundException.class, () -> userService.delete(999L));
            verify(userMapper, never()).delete(anyLong());
        }
    }

    @Nested
    class AssignRoles {

        @Test
        void shouldAssignRolesCorrectly() {
            List<Long> roleIds = List.of(1L, 2L, 3L);

            userService.assignRoles(1L, roleIds);

            verify(roleMapper).deleteUserRoles(1L);
            verify(roleMapper).insertUserRole(1L, 1L);
            verify(roleMapper).insertUserRole(1L, 2L);
            verify(roleMapper).insertUserRole(1L, 3L);
        }
    }

    @Nested
    class ChangePassword {

        @Test
        void shouldChangePassword_whenOldPasswordCorrect() {
            when(userMapper.findById(1L)).thenReturn(testUser);
            when(passwordEncoder.matches("oldPass", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.encode("newPass")).thenReturn("newEncoded");

            userService.changePassword(1L, "oldPass", "newPass");

            verify(userMapper).updatePassword(1L, "newEncoded");
        }

        @Test
        void shouldThrowNotFoundException_whenUserNotExists() {
            when(userMapper.findById(999L)).thenReturn(null);

            assertThrows(NotFoundException.class,
                    () -> userService.changePassword(999L, "old", "new"));
        }

        @Test
        void shouldThrowBusinessException_whenOldPasswordIncorrect() {
            when(userMapper.findById(1L)).thenReturn(testUser);
            when(passwordEncoder.matches("wrongPass", "encodedPassword")).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.changePassword(1L, "wrongPass", "newPass"));
            assertEquals("原密码不正确", ex.getMessage());
            verify(userMapper, never()).updatePassword(anyLong(), anyString());
        }
    }

    @Nested
    class UpdateProfile {

        @Test
        void shouldUpdateProfileFields() {
            when(userMapper.findById(1L)).thenReturn(testUser);

            userService.updateProfile(1L, "New Nick", "new@test.com", "/avatar.png");

            verify(userMapper).updateProfile(eq(1L), eq("New Nick"), eq("new@test.com"), eq("/avatar.png"));
        }

        @Test
        void shouldThrowNotFoundException_whenUserNotExists() {
            when(userMapper.findById(999L)).thenReturn(null);

            assertThrows(NotFoundException.class,
                    () -> userService.updateProfile(999L, "Nick", "e@e.com", null));
        }
    }

    @Nested
    class GetProfile {

        @Test
        void shouldReturnCurrentUserProfile() {
            securityUtilsMock.when(SecurityUtils::getCurrentUsername).thenReturn("testuser");
            when(userMapper.findByUsername("testuser")).thenReturn(testUser);

            User result = userService.getProfile();

            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
        }
    }
}
