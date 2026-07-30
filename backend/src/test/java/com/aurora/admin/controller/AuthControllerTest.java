package com.aurora.admin.controller;

import com.aurora.admin.config.ConfigCache;
import com.aurora.admin.dto.ApiResponse;
import com.aurora.admin.dto.LoginRequest;
import com.aurora.admin.dto.LoginResponse;
import com.aurora.admin.dto.RegisterRequest;
import com.aurora.admin.entity.User;
import com.aurora.admin.entity.Menu;
import com.aurora.admin.mapper.MenuMapper;
import com.aurora.admin.mapper.RoleMapper;
import com.aurora.admin.service.UserService;
import com.aurora.admin.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private MenuMapper menuMapper;

    @Mock
    private ConfigCache configCache;

    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        authController = new AuthController();

        // 注入 mock 依赖
        setField(authController, "userService", userService);
        setField(authController, "passwordEncoder", passwordEncoder);
        setField(authController, "jwtUtil", jwtUtil);
        setField(authController, "redisTemplate", redisTemplate);
        setField(authController, "roleMapper", roleMapper);
        setField(authController, "menuMapper", menuMapper);
        setField(authController, "configCache", configCache);

        // 默认：注册功能开启
        lenient().when(configCache.getBoolean("register.enabled", true)).thenReturn(true);

        // 默认 mock HTTP request
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(httpRequest.getHeader("Authorization")).thenReturn(null);

        // 初始化限流 buckets（避免 NPE）
        setField(authController, "loginBucketsLocal", new ConcurrentHashMap<>());
        setField(authController, "registerBucketsLocal", new ConcurrentHashMap<>());
        setField(authController, "redisAvailable", false);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void register_Success() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("new@example.com");
        when(userService.findByUsername("newuser")).thenReturn(null);
        when(userService.findByEmail("new@example.com")).thenReturn(null);
        when(userService.register("newuser", "password123", "new@example.com")).thenReturn(testUser);

        // when
        ResponseEntity<ApiResponse> response = authController.register(request, null, httpRequest);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());
        assertEquals("注册成功", response.getBody().getData());
        verify(userService).register("newuser", "password123", "new@example.com");
    }

    @Test
    void register_UsernameExists() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setEmail("new@example.com");
        when(userService.findByUsername("existinguser")).thenReturn(testUser);

        // when
        ResponseEntity<ApiResponse> response = authController.register(request, null, httpRequest);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
        assertEquals("注册失败，用户名已被使用", response.getBody().getMessage());
        verify(userService, never()).register(anyString(), anyString(), anyString());
    }

    @Test
    void register_EmailExists() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("existing@example.com");
        when(userService.findByUsername("newuser")).thenReturn(null);
        when(userService.findByEmail("existing@example.com")).thenReturn(testUser);

        // when
        ResponseEntity<ApiResponse> response = authController.register(request, null, httpRequest);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
        assertEquals("注册失败，邮箱已被使用", response.getBody().getMessage());
        verify(userService, never()).register(anyString(), anyString(), anyString());
    }

    @Test
    void login_Success() {
        // given
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(roleMapper.findCodesByUserId(1L)).thenReturn(List.of("USER"));
        when(jwtUtil.generateToken("testuser", 1L, List.of("USER"))).thenReturn("test.jwt.token");

        // mock getPermissions 调用链
        when(roleMapper.findIdsByCodes(List.of("USER"))).thenReturn(List.of(10L));
        Menu menu = new Menu();
        menu.setPermission("system:user:list");
        when(menuMapper.findByRoleIds(List.of(10L))).thenReturn(List.of(menu));

        // when
        ResponseEntity<ApiResponse> response = authController.login(request, null, httpRequest);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());
        LoginResponse loginResponse = (LoginResponse) response.getBody().getData();
        assertNotNull(loginResponse);
        assertEquals("test.jwt.token", loginResponse.getToken());
        assertEquals("testuser", loginResponse.getUsername());
        assertTrue(loginResponse.getPermissions().contains("system:user:list"));
    }

    @Test
    void login_UserNotFound() {
        // given
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");
        when(userService.findByUsername("nonexistent")).thenReturn(null);

        // when
        ResponseEntity<ApiResponse> response = authController.login(request, null, httpRequest);

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().getCode());
        assertEquals("用户名或密码错误", response.getBody().getMessage());
    }

    @Test
    void login_WrongPassword() {
        // given
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        // when
        ResponseEntity<ApiResponse> response = authController.login(request, null, httpRequest);

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().getCode());
        assertEquals("用户名或密码错误", response.getBody().getMessage());
    }

    @Test
    void logout_Success() {
        // given
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer test.jwt.token");

        // when
        ResponseEntity<ApiResponse> response = authController.logout(httpRequest);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());
        assertEquals("退出成功", response.getBody().getData());
    }

    @Test
    void logout_NoAuthHeader() {
        // given
        when(httpRequest.getHeader("Authorization")).thenReturn(null);

        // when
        ResponseEntity<ApiResponse> response = authController.logout(httpRequest);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());
        assertEquals("退出成功", response.getBody().getData());
    }

    @Test
    void logout_InvalidAuthHeader() {
        // given
        when(httpRequest.getHeader("Authorization")).thenReturn("InvalidToken");

        // when
        ResponseEntity<ApiResponse> response = authController.logout(httpRequest);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());
        assertEquals("退出成功", response.getBody().getData());
    }
}
