package com.demo.pj1.demo.controller.user;

import com.demo.entity.User;
import com.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    private User testUser;
    private User testAdmin;
    private MockHttpSession userSession;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() {
        // 创建测试普通用户
        testUser = new User();
        testUser.setUserID("testUser");
        testUser.setUserName("Test User");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setPhone("12345678901");
        testUser.setIsadmin(0);
        userService.create(testUser);

        // 创建测试管理员
        testAdmin = new User();
        testAdmin.setUserID("testAdmin");
        testAdmin.setUserName("Test Admin");
        testAdmin.setPassword("adminpass");
        testAdmin.setEmail("admin@example.com");
        testAdmin.setPhone("12345678902");
        testAdmin.setIsadmin(1);
        userService.create(testAdmin);

        // 创建会话
        userSession = new MockHttpSession();
        userSession.setAttribute("user", testUser);
        
        adminSession = new MockHttpSession();
        adminSession.setAttribute("admin", testAdmin);
    }

    @Test
    void testSignUp() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andDo(print());
    }

    @Test
    void testLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andDo(print());
    }

    @Test
    void testLoginCheck_UserSuccess() throws Exception {
        mockMvc.perform(post("/loginCheck.do")
                .param("userID", testUser.getUserID())
                .param("password", testUser.getPassword())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("/index"))
                .andDo(print());
    }

    @Test
    void testLoginCheck_AdminSuccess() throws Exception {
        mockMvc.perform(post("/loginCheck.do")
                .param("userID", testAdmin.getUserID())
                .param("password", testAdmin.getPassword())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("/admin_index"))
                .andDo(print());
    }

    @Test
    void testLoginCheck_Fail() throws Exception {
        mockMvc.perform(post("/loginCheck.do")
                .param("userID", "wrongUser")
                .param("password", "wrongPass")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("false"))
                .andDo(print());
    }

    @Test
    void testRegister() throws Exception {
        mockMvc.perform(post("/register.do")
                .param("userID", "newUser")
                .param("userName", "New User")
                .param("password", "newpass")
                .param("email", "new@example.com")
                .param("phone", "12345678903")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("login"));

        // 验证用户是否成功创建
        User newUser = userService.findByUserID("newUser");
        assertNotNull(newUser);
        assertEquals("New User", newUser.getUserName());
    }

    // 用户注册边界测试
    @Test
    void testRegister_InvalidInput() throws Throwable {
        try {
            // 测试空用户名
            mockMvc.perform(post("/register.do")
                    .param("userID", "")
                    .param("userName", "Test User")
                    .param("password", "password")
                    .param("email", "test@example.com")
                    .param("phone", "12345678901")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception instanceof IllegalArgumentException);
                        assertEquals("UserID cannot be empty", exception.getMessage());
                    })
                    .andDo(print());

            // 测试无效邮箱格式
            mockMvc.perform(post("/register.do")
                    .param("userID", "testUser")
                    .param("userName", "Test User")
                    .param("password", "password")
                    .param("email", "invalid-email")
                    .param("phone", "12345678901")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception instanceof IllegalArgumentException);
                        assertEquals("Invalid email format", exception.getMessage());
                    });

            // 测试无效手机号
            mockMvc.perform(post("/register.do")
                    .param("userID", "testUser")
                    .param("userName", "Test User")
                    .param("password", "password")
                    .param("email", "test@example.com")
                    .param("phone", "123")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception instanceof IllegalArgumentException);
                        assertEquals("Invalid phone number format", exception.getMessage());
                    });
        } catch (Exception e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                throw e.getCause();
            }
            throw e;
        }
    }

    // 并发测试 - 用户注册
    @Test
    void testRegister_ConcurrentUsers() throws Exception {
        String baseUserID = "concurrent_user_";
        List<Thread> threads = new ArrayList<>();
        
        // 创建多个并发注册请求
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads.add(new Thread(() -> {
                try {
                    mockMvc.perform(post("/register.do")
                            .param("userID", baseUserID + index)
                            .param("userName", "Concurrent User " + index)
                            .param("password", "password" + index)
                            .param("email", "user" + index + "@example.com")
                            .param("phone", "1234567890" + index)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                            .andExpect(status().is3xxRedirection());
                } catch (Exception e) {
                    fail("Concurrent registration failed: " + e.getMessage());
                }
            }));
        }

        // 启动所有线程
        threads.forEach(Thread::start);
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证所有用户是否成功注册
        for (int i = 0; i < 10; i++) {
            User user = userService.findByUserID(baseUserID + i);
            assertNotNull(user);
            assertEquals("Concurrent User " + i, user.getUserName());
        }
    }

    // 安全测试 - XSS防护
    @Test
    void testRegister_XSSAttempt() throws Exception {
        String xssUserName = "<script>alert('xss')</script>";
        
        mockMvc.perform(post("/register.do")
                .param("userID", "xssUser")
                .param("userName", xssUserName)
                .param("password", "password")
                .param("email", "xss@example.com")
                .param("phone", "12345678901")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection());

        User user = userService.findByUserID("xssUser");
        assertNotNull(user);
        assertNotEquals(xssUserName, user.getUserName());
        assertTrue(user.getUserName().contains("&lt;script&gt;"));
    }

    @Test
    void testLogout() throws Exception {
        mockMvc.perform(get("/logout.do")
                .session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"))
                .andDo(print());
    }

    @Test
    void testQuit() throws Exception {
        mockMvc.perform(get("/quit.do")
                .session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"))
                .andDo(print());
    }

    @Test
    void testUpdateUser() throws Exception {
        MockMultipartFile picture = new MockMultipartFile(
                "picture",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        mockMvc.perform(multipart("/updateUser.do")
                .file(picture)
                .param("userID", testUser.getUserID())
                .param("userName", "Updated User")
                .param("passwordNew", "newpassword")
                .param("email", "updated@example.com")
                .param("phone", "12345678904")
                .session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_info"));

        // 验证用户信息是否更新
        User updatedUser = userService.findByUserID(testUser.getUserID());
        assertEquals("Updated User", updatedUser.getUserName());
        assertEquals("updated@example.com", updatedUser.getEmail());
    }

    // 文件上传测试
    @Test
    void testUpdateUser_InvalidFile() throws Throwable {
        // 测试不支持的文件类型
        MockMultipartFile invalidFile = new MockMultipartFile(
                "picture",
                "test.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "test content".getBytes()
        );

        try {
            mockMvc.perform(multipart("/updateUser.do")
                    .file(invalidFile)
                    .param("userID", testUser.getUserID())
                    .param("userName", "Updated User")
                    .param("passwordNew", "newpassword")
                    .param("email", "updated@example.com")
                    .param("phone", "12345678904")
                    .session(userSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception instanceof IllegalArgumentException);
                        assertEquals("Unsupported file type", exception.getMessage());
                    });
        } catch (Exception e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                throw e.getCause();
            }
            throw e;
        }
    }

    // 会话管理测试
    @Test
    void testSession_Expiry() throws Exception {
        // 创建过期会话
        MockHttpSession expiredSession = new MockHttpSession();
        expiredSession.setAttribute("user", testUser);
        expiredSession.setMaxInactiveInterval(1); // 1秒后过期
        
        Thread.sleep(2000); // 等待会话过期

        mockMvc.perform(get("/user_info")
                .session(expiredSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andDo(print());
    }

    // 性能测试
    @Test
    void testLogin_Performance() throws Exception {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(post("/loginCheck.do")
                    .param("userID", testUser.getUserID())
                    .param("password", testUser.getPassword())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isOk());
        }

        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 2000, 
            "Performance test failed: " + duration + "ms > 2000ms");
    }

    @Test
    void testCheckPassword_Success() throws Exception {
        mockMvc.perform(get("/checkPassword.do")
                .param("userID", testUser.getUserID())
                .param("password", testUser.getPassword()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andDo(print());
    }

    @Test
    void testCheckPassword_Fail() throws Exception {
        mockMvc.perform(get("/checkPassword.do")
                .param("userID", testUser.getUserID())
                .param("password", "wrongpass"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"))
                .andDo(print());
    }

    @Test
    void testUserInfo() throws Exception {
        mockMvc.perform(get("/user_info")
                .session(userSession))  // 添加用户会话
                .andExpect(status().isOk())
                .andExpect(view().name("user_info"))
                .andExpect(model().attributeExists("user"))  // 验证模型中有用户信息
                .andDo(print());
    }
}