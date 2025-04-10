package com.demo.service;

import com.demo.dao.MessageDao;
import com.demo.dao.UserDao;
import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.entity.vo.MessageVo;
import com.demo.service.impl.MessageVoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageVoServiceTest {

    @Mock
    private MessageDao messageDao;

    @Mock
    private UserDao userDao;

    @InjectMocks
    private MessageVoServiceImpl messageVoService;

    /**
     * 创建样例留言用以测试
     * @return 样例留言
     */
    private Message createSampleMessage() {
        return new Message(1, "user", "Test content", LocalDateTime.now(), MessageService.STATE_NO_AUDIT);
    }

    /**
     * 创建样例用户用以测试
     * @return 样例用户
     */
    private User createSampleUser() {
        return new User(1,"user","User","password","user1@example.com","13800138000",0,"avatar.jpg");
    }

    // ------------------------- 1. returnMessageVoByMessageID -------------------------

    /**
     * TC:1.0
     * 类型：黑盒测试-有效等价类
     * 描述：留言存在时返回消息视图
     * 预期：返回有效且与创建时信息相等的视图
     */
    @Test
    void testReturnMessageVoByMessageID_existId() {
        when(messageDao.findByMessageID(1)).thenReturn(createSampleMessage());
        when(userDao.findByUserID("user")).thenReturn(createSampleUser());

        MessageVo result = messageVoService.returnMessageVoByMessageID(1);

        assertNotNull(result);
        assertEquals(1, result.getMessageID());
        assertEquals("user", result.getUserID());
        assertEquals("User", result.getUserName());
        assertEquals("avatar.jpg", result.getPicture());

        verify(messageDao).findByMessageID(1);
        verify(userDao).findByUserID("user");
    }

    /**
     * TC:1.1
     * 类型：黑盒测试-无效等价类
     * 描述：用不存在的留言构建视图
     * 预期：抛出空指针异常
     */
    @Test
    void testReturnMessageVoByMessageID_NonExistId() {
        when(messageDao.findByMessageID(999)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> messageVoService.returnMessageVoByMessageID(999));

        verify(messageDao).findByMessageID(999);
    }

    // ------------------------- returnVo -------------------------

    /**
     * TC:2.0
     * 类型：黑盒测试-有效等价类
     * 描述：列表有留言时返回视图列表
     * 预期：返回正确的视图列表
     */
    @Test
    void testReturnVo_validMessages() {
        List<Message> messages = List.of(createSampleMessage());
        User user = createSampleUser();

        when(messageDao.findByMessageID(1)).thenReturn(createSampleMessage());
        when(userDao.findByUserID("user")).thenReturn(user);

        List<MessageVo> result = messageVoService.returnVo(messages);

        assertEquals(1, result.size());
        MessageVo vo = result.get(0);
        assertEquals(1, vo.getMessageID());
        assertEquals("user", vo.getUserID());

        verify(messageDao).findByMessageID(anyInt());
        verify(userDao).findByUserID(any());
    }

    /**
     * TC:2.1
     * 类型：黑盒测试-无效等价类
     * 描述：用null作为列表参数
     * 预期：抛出空指针异常
     */
    @Test
    void testReturnVo_null() {
        assertThrows(NullPointerException.class, () -> messageVoService.returnVo(null));

        verify(messageDao, never()).findByMessageID(anyInt());
        verify(userDao, never()).findByUserID(any());
    }

    /**
     * TC:2.2
     * 类型：黑盒测试-无效等价类
     * 描述：列表中包含的留言为null
     * 预期：抛出空指针异常
     */
    @Test
    void testReturnVo_nullInList() {
        List<Message> messages = Collections.singletonList(null);

        assertThrows(NullPointerException.class, () -> messageVoService.returnVo(messages));

        verify(messageDao, never()).findByMessageID(anyInt());
        verify(userDao, never()).findByUserID(any());
    }
}

