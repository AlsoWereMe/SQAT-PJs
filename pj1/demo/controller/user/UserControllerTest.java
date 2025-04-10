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