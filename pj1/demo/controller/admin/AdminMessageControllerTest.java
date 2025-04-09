package com.demo.pj1.demo.controller.admin;

import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import com.demo.dao.UserDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static com.demo.service.MessageService.STATE_NO_AUDIT;
import static com.demo.service.MessageService.STATE_PASS;
import static com.demo.service.MessageService.STATE_REJECT;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
public class AdminMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserDao userDao;  // Add UserDao injection

    private List<Message> testMessages;

    @BeforeEach
    void setUp() {
        // Create test user first
        User testUser = new User();
        testUser.setUserID("testUser");
        testUser.setUserName("Test User");
        testUser.setPassword("password");
        testUser.setPicture("default.jpg");
        userDao.save(testUser);

        // Then create messages
        testMessages = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Message message = new Message();
            message.setMessageID(i);
            message.setContent("Test Message " + i);
            message.setTime(LocalDateTime.now());
            message.setState(MessageService.STATE_NO_AUDIT);
            message.setUserID("testUser");  // Use the same userID as created above
            testMessages.add(message);
            messageService.create(message);
        }
    }

    @AfterEach
    void tearDown() {
        for (Message message : testMessages) {
            try {
                messageService.delById(message.getMessageID());
            } catch (Exception e) {
                // 忽略删除失败的异常
            }
        }
    }

    @Test
    void testMessageList_NormalCase() throws Exception {
        mockMvc.perform(get("/messageList.do")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10))) // 每页10条
                .andExpect(jsonPath("$[0].content", startsWith("Test Message")))
                .andExpect(jsonPath("$[0].state", is(MessageService.STATE_NO_AUDIT)));  // 验证状态是否为 1
    }

    @Test
    void testMessageList_InvalidPage() throws Exception {
        // 测试各种无效页码
        String[] invalidPages = {"-1", "0", "abc", String.valueOf(Integer.MAX_VALUE)};
        
        for (String page : invalidPages) {
            try {
                mockMvc.perform(get("/messageList.do")
                        .param("page", page))
                        .andExpect(status().isInternalServerError())
                        .andExpect(result -> {
                            Exception exception = result.getResolvedException();
                            assertNotNull(exception, "Expected exception to be thrown");
                            if (page.equals("-1") || page.equals("0")) {
                                assertTrue(exception instanceof IllegalArgumentException);
                                assertEquals("Page index must not be less than zero!", exception.getMessage());
                            } else {
                                assertTrue(exception instanceof RuntimeException);
                                assertEquals("Invalid page number", exception.getMessage());
                            }
                        })
                        .andDo(print());
            } catch (Exception e) {
                if (page.equals("-1") || page.equals("0")) {
                    assertTrue(e.getCause() instanceof IllegalArgumentException);
                    assertEquals("Page index must not be less than zero!", e.getCause().getMessage());
                } else {
                    assertTrue(e.getCause() instanceof RuntimeException);
                    assertEquals("Invalid page number", e.getCause().getMessage());
                }
            }
        }
    }

    @Test
    void testMessageList_EmptyPage() throws Exception {
        mockMvc.perform(get("/messageList.do")
                .param("page", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testPassMessage_Success() throws Exception {
        // 1. 创建待审核状态的消息
        Message message = new Message();
        message.setContent("Test Pass Message");
        message.setTime(LocalDateTime.now());
        message.setState(MessageService.STATE_NO_AUDIT);  // 设置初始状态为待审核(1)
        message.setUserID("testUser");
        int messageId = messageService.create(message);

        // 2. 执行通过操作
        mockMvc.perform(post("/passMessage.do")
                .param("messageID", String.valueOf(messageId))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andDo(print());  // 打印请求和响应详情，方便调试

        // 3. 验证消息状态已更新
        Message updatedMessage = messageService.findById(messageId);
        assertNotNull(updatedMessage, "Message should exist after approval");
        assertEquals(MessageService.STATE_PASS, updatedMessage.getState(), 
                String.format("Message state should be PASS(%d) but was %d", 
                        MessageService.STATE_PASS, updatedMessage.getState()));
    }

    @Test
    void testPassMessage_NonexistentMessage() throws Exception {
        try {
            mockMvc.perform(post("/passMessage.do")
                    .param("messageID", "999")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception, "Expected exception to be thrown");
                        assertTrue(exception instanceof RuntimeException);
                        assertEquals("留言不存在", exception.getMessage());
                    })
                    .andDo(print());
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("留言不存在", e.getCause().getMessage());
        }
    }

    @Test
    void testRejectMessage_Success() throws Exception {
        // 1. 创建待审核状态的消息
        Message message = new Message();
        message.setContent("Test Reject Message");
        message.setTime(LocalDateTime.now());
        message.setState(MessageService.STATE_NO_AUDIT);  // 状态为1
        message.setUserID("testUser");
        int messageId = messageService.create(message);

        // 2. 执行拒绝操作
        mockMvc.perform(post("/rejectMessage.do")
                .param("messageID", String.valueOf(messageId))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 3. 验证消息状态已更新为拒绝状态
        Message updatedMessage = messageService.findById(messageId);
        assertNotNull(updatedMessage, "Message should exist");
        assertEquals(MessageService.STATE_REJECT, updatedMessage.getState());  // 状态应该为3
    }

    @Test
    void testRejectMessage_NonexistentMessage() throws Exception {
        try {
            mockMvc.perform(post("/rejectMessage.do")
                    .param("messageID", "999")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception, "Expected exception to be thrown");
                        assertTrue(exception instanceof RuntimeException);
                        assertEquals("留言不存在", exception.getMessage());
                    })
                    .andDo(print());
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("留言不存在", e.getCause().getMessage());
        }
    }

    @Test
    void testDelMessage_Success() throws Exception {
        // 1. 创建要删除的消息
        Message message = new Message();
        message.setContent("Test Delete Message");
        message.setTime(LocalDateTime.now());
        message.setState(MessageService.STATE_NO_AUDIT);
        message.setUserID("testUser");
        int messageId = messageService.create(message);

        // 2. 执行删除操作
        mockMvc.perform(post("/delMessage.do")
                .param("messageID", String.valueOf(messageId))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 3. 验证消息已被删除
        assertThrows(RuntimeException.class, () -> messageService.findById(messageId),
                "Message should not exist after deletion");
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
                        assertNotNull(exception, "Expected exception to be thrown");
                        assertTrue(exception instanceof RuntimeException);
                        assertEquals("No class com.demo.entity.Message entity with id 999 exists", 
                                exception.getMessage());
                    })
                    .andDo(print());
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("No class com.demo.entity.Message entity with id 999 exists", 
                    e.getCause().getMessage());
        }
    }

    // 边界条件测试
    @Test
    void testMessageList_BoundaryPages() throws Exception {
        // 准备测试数据：确保有足够的消息进行分页测试
        for (int i = 0; i < 25; i++) {
            Message message = new Message();
            message.setContent("Test Message " + i);
            message.setTime(LocalDateTime.now());
            message.setState(MessageService.STATE_NO_AUDIT);
            message.setUserID("testUser");
            messageService.create(message);
        }

        // 测试第一页
        mockMvc.perform(get("/messageList.do")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].state", is(MessageService.STATE_NO_AUDIT)))
                .andExpect(jsonPath("$[0].content", startsWith("Test Message")));

        // 测试最后一页
        mockMvc.perform(get("/messageList.do")
                .param("page", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    // 异常情况测试
    @Test
    void testMessageOperations_ConcurrentModification() throws Exception {
        // 1. 创建测试消息
        Message message = new Message();
        message.setContent("Test Concurrent Message");
        message.setTime(LocalDateTime.now());
        message.setState(MessageService.STATE_NO_AUDIT);
        message.setUserID("testUser");
        int messageId = messageService.create(message);

        // 2. 模拟并发操作
        mockMvc.perform(post("/passMessage.do")
                .param("messageID", String.valueOf(messageId)))
                .andExpect(status().isOk());

        // 3. 尝试对已处理的消息进行操作
        mockMvc.perform(post("/rejectMessage.do")
                .param("messageID", String.valueOf(messageId)))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Message already processed")));
    }

    // 性能测试
    @Test
    void testMessageList_Performance() throws Exception {
        // 准备大量测试数据
        for (int i = 0; i < 100; i++) {
            Message message = new Message();
            message.setContent("Performance Test Message " + i);
            message.setTime(LocalDateTime.now());
            message.setState(MessageService.STATE_NO_AUDIT);
            message.setUserID("testUser");
            messageService.create(message);
        }

        // 执行性能测试
        long startTime = System.currentTimeMillis();
        
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(get("/messageList.do")
                    .param("page", String.valueOf(i)))
                    .andExpect(status().isOk());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 2000, 
                "Performance test failed. Expected duration < 2000ms but was " + duration + "ms");
    }
}