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
}