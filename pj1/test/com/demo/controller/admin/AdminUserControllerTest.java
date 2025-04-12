package com.demo.pj1.demo.controller.admin;

import com.demo.entity.User;
import com.demo.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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
public class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        testUsers = new ArrayList<>();
        // 创建测试用户数据
        for (int i = 1; i <= 15; i++) {
            User user = new User();
            user.setUserID("testUser" + i);
            user.setUserName("Test User " + i);
            user.setPassword("password" + i);
            user.setEmail("test" + i + "@example.com");
            user.setPhone("1234567890" + i);
            user.setPicture("");
            testUsers.add(user);
            userService.create(user);
        }
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        for (User user : testUsers) {
            try {
                userService.delByID(user.getId());
            } catch (Exception e) {
                // 忽略删除失败的异常
            }
        }
    }

    @Test
    void testUserManage() throws Exception {
        mockMvc.perform(get("/user_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_manage"))
                .andExpect(model().attributeExists("total"))
                .andDo(print());
    }

    @Test
    void testUserList_NormalCase() throws Exception {
        mockMvc.perform(get("/userList.do")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].userID", startsWith("testUser")))
                .andDo(print());
    }

    @Test
    void testUserList_EmptyPage() throws Exception {
        mockMvc.perform(get("/userList.do")
                .param("page", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testUserAdd() throws Exception {
        mockMvc.perform(get("/user_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_add"));
    }

    @Test
    void testUserEdit() throws Exception {
        // 获取第一个测试用户
        User testUser = testUsers.get(0);

        mockMvc.perform(get("/user_edit")
                .param("id", String.valueOf(testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_edit"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void testAddUser_Success() throws Exception {
        String newUserId = "newTestUser";
        mockMvc.perform(post("/addUser.do")
                .param("userID", newUserId)
                .param("userName", "New Test User")
                .param("password", "newpassword")
                .param("email", "newtest@example.com")
                .param("phone", "1234567890")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        // 验证用户是否成功添加
        User addedUser = userService.findByUserID(newUserId);
        assertNotNull(addedUser);
        assertEquals("New Test User", addedUser.getUserName());
    }

    @Test
    void testModifyUser_Success() throws Exception {
        // 获取第一个测试用户
        User testUser = testUsers.get(0);
        String updatedUserName = "Updated User Name";

        mockMvc.perform(post("/modifyUser.do")
                .param("userID", testUser.getUserID())
                .param("oldUserID", testUser.getUserID())
                .param("userName", updatedUserName)
                .param("password", testUser.getPassword())
                .param("email", testUser.getEmail())
                .param("phone", testUser.getPhone())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        // 验证用户信息是否更新
        User updatedUser = userService.findByUserID(testUser.getUserID());
        assertEquals(updatedUserName, updatedUser.getUserName());
    }

    @Test
    void testCheckUserID_Unique() throws Exception {
        mockMvc.perform(post("/checkUserID.do")
                .param("userID", "uniqueUserID")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckUserID_Duplicate() throws Exception {
        // 使用已存在的测试用户ID
        String existingUserId = testUsers.get(0).getUserID();
        
        mockMvc.perform(post("/checkUserID.do")
                .param("userID", existingUserId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testDelUser_Success() throws Exception {
        // 获取第一个测试用户
        User testUser = testUsers.get(0);

        mockMvc.perform(post("/delUser.do")
                .param("id", String.valueOf(testUser.getId()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 验证用户是否已被删除
        assertThrows(Exception.class, () -> userService.findById(testUser.getId()));
    }

    @Test
    void testDelUser_NonexistentUser() throws Exception {
        try {
            mockMvc.perform(post("/delUser.do")
                    .param("id", "999")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception.getMessage().contains("No class com.demo.entity.User entity with id 999 exists"));
                    })
                    .andDo(print());
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertTrue(e.getCause().getMessage().contains("No class com.demo.entity.User entity with id 999 exists"));
        }
    }
}