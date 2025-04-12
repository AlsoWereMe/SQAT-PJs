package com.demo.pj1.demo.controller.user;

import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.service.MessageService;
import com.demo.service.UserService;
import com.demo.exception.LoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;  // Add this import
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.NestedServletException;

import java.time.LocalDateTime;
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
public class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    private List<Message> testMessages;
    private User testUser;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setUserID("testUser");
        testUser.setUserName("Test User");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setPhone("12345678901");
        userService.create(testUser);

        // 创建会话并添加用户
        session = new MockHttpSession();
        session.setAttribute("user", testUser);

        // 创建测试留言
        testMessages = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Message message = new Message();
            message.setContent("Test Message " + i);
            message.setTime(LocalDateTime.now().minusDays(i));
            message.setState(i <= 5 ? MessageService.STATE_PASS : MessageService.STATE_NO_AUDIT);
            message.setUserID(testUser.getUserID());
            testMessages.add(message);
            messageService.create(message);
        }
    }

    @Test
    void testMessageList_WithLogin() throws Exception {
        mockMvc.perform(get("/message_list").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("message_list"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attributeExists("user_total"))
                .andDo(print());
    }

    @Test
    void testMessageList_WithoutLogin() throws Exception {
        try {
            mockMvc.perform(get("/message_list"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception, "Expected exception to be thrown");
                        assertTrue(exception instanceof LoginException, 
                                "Expected LoginException but got " + exception.getClass().getName());
                        assertEquals("请登录！", exception.getMessage());
                    })
                    .andDo(print());
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof LoginException);
            assertEquals("请登录！", e.getCause().getMessage());
        }
    }

    @Test
    void testGetMessageList() throws Exception {
        mockMvc.perform(get("/message/getMessageList")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].content", startsWith("Test Message")))
                .andExpect(jsonPath("$[0].userName", is("Test User")))
                .andDo(print());
    }

    @Test
    void testFindUserList_WithLogin() throws Exception {
        mockMvc.perform(get("/message/findUserList")
                .session(session)
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].userID", is(testUser.getUserID())))
                .andDo(print());
    }

    @Test
    void testFindUserList_WithoutLogin() throws Exception {
        try {
            mockMvc.perform(get("/message/findUserList")
                    .param("page", "1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception, "Expected exception to be thrown");
                        assertTrue(exception instanceof LoginException, 
                                "Expected LoginException but got " + exception.getClass().getName());
                        assertEquals("请登录！", exception.getMessage());
                    })
                    .andDo(print());
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof LoginException);
            assertEquals("请登录！", e.getCause().getMessage());
        }
    }

    @Test
    void testSendMessage() throws Exception {
        String content = "New Test Message";
        mockMvc.perform(post("/sendMessage")
                .param("userID", testUser.getUserID())
                .param("content", content)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/message_list"));

        // 验证留言是否成功创建
        List<Message> userMessages = messageService.findByUser(testUser.getUserID(), 
            PageRequest.of(0, 1, Sort.by("time").descending())).getContent();
        assertFalse(userMessages.isEmpty());
        assertEquals(content, userMessages.get(0).getContent());
    }

    @Test
    void testModifyMessage_Success() throws Exception {
        Message testMessage = testMessages.get(0);
        String updatedContent = "Updated Message Content";

        mockMvc.perform(post("/modifyMessage.do")
                .param("messageID", String.valueOf(testMessage.getMessageID()))
                .param("content", updatedContent)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 验证留言是否更新
        Message updatedMessage = messageService.findById(testMessage.getMessageID());
        assertEquals(updatedContent, updatedMessage.getContent());
        assertEquals(MessageService.STATE_NO_AUDIT, updatedMessage.getState());
    }

    @Test
    void testDelMessage_Success() throws Exception {
        Message testMessage = testMessages.get(0);

        mockMvc.perform(post("/delMessage.do")
                .param("messageID", String.valueOf(testMessage.getMessageID()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 验证留言是否被删除
        assertThrows(Exception.class, () -> messageService.findById(testMessage.getMessageID()));
    }

    @Test
    void testDelMessage_NonexistentMessage() throws Exception {
        try {
            mockMvc.perform(post("/delMessage.do")
                    .param("messageID", "999")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception instanceof RuntimeException);
                    })
                    .andDo(print());
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RuntimeException);
        }
    }

    // 边界测试 - 空内容留言
    @Test
    void testSendMessage_EmptyContent() throws Exception {
        mockMvc.perform(post("/sendMessage")
                .param("userID", testUser.getUserID())
                .param("content", "")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Message content cannot be empty")));
    }

    // 边界测试 - 超长内容留言
    @Test
    void testSendMessage_ContentTooLong() throws Exception {
        String longContent = "a".repeat(1001); // 假设最大长度为1000
        mockMvc.perform(post("/sendMessage")
                .param("userID", testUser.getUserID())
                .param("content", longContent)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Message content too long")));
    }

    // 并发测试
    @Test
    void testMessageOperations_ConcurrentModification() throws Exception {
        Message testMessage = testMessages.get(0);
        
        // 并发修改
        mockMvc.perform(post("/modifyMessage.do")
                .param("messageID", String.valueOf(testMessage.getMessageID()))
                .param("content", "First Update"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/modifyMessage.do")
                .param("messageID", String.valueOf(testMessage.getMessageID()))
                .param("content", "Second Update"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Message being processed")));
    }

    // 性能测试
    @Test
    void testMessageList_Performance() throws Exception {
        // 创建大量测试数据
        for (int i = 0; i < 100; i++) {
            Message message = new Message();
            message.setContent("Performance Test Message " + i);
            message.setTime(LocalDateTime.now());
            message.setState(MessageService.STATE_PASS);
            message.setUserID(testUser.getUserID());
            messageService.create(message);
        }

        long startTime = System.currentTimeMillis();
        
        // 多次请求测试
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(get("/message/getMessageList")
                    .param("page", String.valueOf(i)))
                    .andExpect(status().isOk());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 2000, 
                "Performance test failed. Expected duration < 2000ms but was " + duration + "ms");
    }

    // 异常情况测试 - XSS注入
    @Test
    void testSendMessage_XSSContent() throws Exception {
        String xssContent = "<script>alert('xss')</script>";
        mockMvc.perform(post("/sendMessage")
                .param("userID", testUser.getUserID())
                .param("content", xssContent)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk());

        // 验证内容是否被转义
        List<Message> userMessages = messageService.findByUser(testUser.getUserID(), 
            PageRequest.of(0, 1, Sort.by("time").descending())).getContent();
        assertFalse(userMessages.isEmpty());
        assertNotEquals(xssContent, userMessages.get(0).getContent());
        assertTrue(userMessages.get(0).getContent().contains("&lt;script&gt;"));
    }

    // 分页边界测试
    @Test
    void testMessageList_PaginationBoundary() throws Exception {
        // 测试第一页
        mockMvc.perform(get("/message/getMessageList")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andDo(print());

        // 测试无效页码 (负数)
        assertThrows(NestedServletException.class, () -> {
            mockMvc.perform(get("/message/getMessageList")
                    .param("page", "-1"))
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception.getCause() instanceof IllegalArgumentException);
                        assertEquals("Page index must not be less than zero!", 
                            exception.getCause().getMessage());
                    });
        });

        // 测试无效页码 (零)
        assertThrows(NestedServletException.class, () -> {
            mockMvc.perform(get("/message/getMessageList")
                    .param("page", "0"))
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception.getCause() instanceof IllegalArgumentException);
                        assertEquals("Page index must not be less than zero!", 
                            exception.getCause().getMessage());
                    });
        });

        // 测试非数字页码
        assertThrows(NestedServletException.class, () -> {
            mockMvc.perform(get("/message/getMessageList")
                    .param("page", "abc"))
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception.getCause() instanceof NumberFormatException);
                    });
        });

        // 测试不存在的页码（超出范围）
        mockMvc.perform(get("/message/getMessageList")
                .param("page", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andDo(print());
    }

    // 留言状态转换测试
    @Test
    void testMessageStateTransition() throws Exception {
        Message message = new Message();
        message.setContent("State Transition Test");
        message.setTime(LocalDateTime.now());
        message.setState(MessageService.STATE_NO_AUDIT);
        message.setUserID(testUser.getUserID());
        int messageId = messageService.create(message);

        // 验证初始状态
        Message createdMessage = messageService.findById(messageId);
        assertEquals(MessageService.STATE_NO_AUDIT, createdMessage.getState());

        // 修改留言后状态应重置为待审核
        mockMvc.perform(post("/modifyMessage.do")
                .param("messageID", String.valueOf(messageId))
                .param("content", "Updated Content")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk());

        Message updatedMessage = messageService.findById(messageId);
        assertEquals(MessageService.STATE_NO_AUDIT, updatedMessage.getState());
    }
}