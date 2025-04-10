package com.demo.service;

import com.demo.dao.MessageDao;
import com.demo.entity.Message;
import com.demo.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageDao messageDao;

    @InjectMocks
    private MessageServiceImpl messageService;

    /**
     * 创建样例留言用以测试
     * @return 样例留言
     */
    private Message createSampleMessage() {
        return new Message(1, "user1", "Test content", LocalDateTime.now(), MessageService.STATE_NO_AUDIT);
    }

    // ------------------------- 1. findById -------------------------

    /**
     * TC:1.0
     * 类型：黑盒测试-有效等价类
     * 描述：根据存在的留言ID查找留言
     * 预期：查找成功，返回对应留言
     */
    @Test
    void testFindById_exist() {
        when(messageDao.getOne(1)).thenReturn(createSampleMessage());

        Message result = messageService.findById(1);
        assertNotNull(result);
        assertEquals(1, result.getMessageID());
        verify(messageDao).getOne(1);
    }

    /**
     * TC:1.1
     * 类型：黑盒测试-无效等价类
     * 描述：根据不存在的留言ID查找留言
     * 预期：查找不成功，返回null
     */
    @Test
    void testFindById_nonExist() {
        assertNull(messageService.findById(999));
    }

    // ------------------------- 2. findByUser -------------------------

    /**
     * TC:2.0
     * 类型：黑盒测试-有效等价类&有效等价类
     * 描述：根据存在的用户与有效的分页参数查找用户留言
     * 预期：查找成功，返回分页后的留言
     */
    @Test
    void testFindByUser_existIdAndPageable() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> mockPage = new PageImpl<>(List.of(createSampleMessage()));

        when(messageDao.findAllByUserID(eq("user"), eq(pageable))).thenReturn(mockPage);

        Page<Message> result = messageService.findByUser("user", pageable);
        assertEquals(1, result.getContent().size());
        verify(messageDao).findAllByUserID("user", pageable);
    }

    /**
     * TC:2.1
     * 类型：黑盒测试-无效等价类&有效等价类
     * 描述：根据不存在的用户ID查找用户留言
     * 预期：查找失败，返回null
     */
    @Test
    void testFindByUser_notExistId() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Message> result = messageService.findByUser("invalidUser", pageable);
        assertNull(result);
    }

    /**
     * TC:2.2
     * 类型：黑盒测试-无效等价类&有效等价类
     * 描述：根据null查找用户留言
     * 预期：抛出异常
     */
    @Test
    void testFindByUser_nullId() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(NullPointerException.class, () -> messageService.findByUser(null, pageable));
    }

    /**
     * TC:2.3
     * 类型：黑盒测试-有效等价类&无效等价类
     * 描述：根据有效用户ID与null分页参数查找用户留言
     * 预期：抛出异常
     */
    @Test
    void testFindByUser_existIdAndNullPageable() {
        assertThrows(NullPointerException.class, () -> messageService.findByUser("user", null));
    }

    // ------------------------- 3. create -------------------------

    /**
     * TC:3.0
     * 类型：黑盒测试-有效等价类
     * 描述：创建一个有内容的有效留言
     * 预期：创建成功，返回其ID
     */
    @Test
    void testCreate_wellMsg() {
        Message message = createSampleMessage();
        when(messageDao.save(any(Message.class))).thenReturn(message);

        int messageId = messageService.create(message);
        assertEquals(1, messageId);
        verify(messageDao).save(message);
    }

    /**
     * TC:3.1
     * 类型：黑盒测试-无效等价类
     * 描述：创建一个null留言
     * 预期：创建失败，抛出异常
     */
    @Test
    void testCreate_nullMsg() {
        assertThrows(NullPointerException.class, () -> messageService.create(null));
    }

    // ------------------------- 4. delById -------------------------

    /**
     * TC:4.0
     * 类型：黑盒测试-有效等价类
     * 描述：删除一个已存在的留言
     * 预期：删除成功，无任何返回
     */
    @Test
    void testDelById_existId() {
        doNothing().when(messageDao).deleteById(1);

        messageService.delById(1);
        verify(messageDao).deleteById(1);
    }

    /**
     * TC:4.1
     * 类型：黑盒测试-无效等价类
     * 描述：删除一个不存在的留言
     * 预期：删除失败，抛出异常
     */
    @Test
    void testDelById_notExistId() {
        assertThrows(RuntimeException.class, () -> messageService.delById(999));
    }

    // ------------------------- 5. update -------------------------

    /**
     * TC:5.0
     * 类型：黑盒测试-有效等价类
     * 描述：更新一个正常的留言
     * 预期：更新成功，无返回值
     */
    @Test
    void testUpdate_wellMsg() {
        Message message = createSampleMessage();
        when(messageDao.save(message)).thenReturn(message);

        messageService.update(message);
        verify(messageDao).save(message);
    }

    /**
     * TC:5.1
     * 类型：黑盒测试-无效等价类
     * 描述：更新一个空留言
     * 预期：更新失败，且抛出空异常
     */
    @Test
    void testUpdate_nullMsg() {
        assertThrows(NullPointerException.class, () -> messageService.update(null));
    }

    // ------------------------- 6. confirmMessage -------------------------

    /**
     * TC:6.0
     * 类型：黑盒测试-有效等价类
     * 描述：通过一个存在的留言
     * 预期：通过成功，无返回值
     */
    @Test
    void testConfirmMessage_existId() {
        when(messageDao.findByMessageID(1)).thenReturn(createSampleMessage());

        messageService.confirmMessage(1);
        verify(messageDao).updateState(eq(MessageService.STATE_PASS), eq(1));
    }

    /**
     * TC:6.1
     * 类型：黑盒测试-无效等价类
     * 描述：通过一个不存在的留言
     * 预期：通过失败，抛出异常
     */
    @Test
    void testConfirmMessage_notExistId() {
        when(messageDao.findByMessageID(999)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> messageService.confirmMessage(999));
    }

    // ------------------------- 7. findWaitState -------------------------

    /**
     * TC:7.0
     * 类型：黑盒测试-有效等价类
     * 描述：用有效参数分页
     * 预期：查找成功
     */
    @Test
    void testFindWaitState_wellPageable() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> mockPage = new PageImpl<>(List.of(createSampleMessage()));

        when(messageDao.findAllByState(eq(MessageService.STATE_NO_AUDIT), eq(pageable))).thenReturn(mockPage);

        Page<Message> result = messageService.findWaitState(pageable);
        assertEquals(1, result.getContent().size());
        verify(messageDao).findAllByState(eq(MessageService.STATE_NO_AUDIT), eq(pageable));
    }

    /**
     * TC:7.1
     * 类型：黑盒测试-无效等价类
     * 描述：用null参数分页
     * 预期：抛出异常
     */
    @Test
    void testFindWaitState_nullPageable() {
        assertThrows(IllegalArgumentException.class, () -> messageService.findWaitState(null));
    }

    // ------------------------- 8. findPassState -------------------------

    /**
     * TC:8.0
     * 类型：黑盒测试-有效等价类
     * 描述：用有效参数分页
     * 预期：查找成功
     */
    @Test
    void testFindPassState_wellPageable() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> mockPage = new PageImpl<>(List.of(createSampleMessage()));

        when(messageDao.findAllByState(eq(MessageService.STATE_PASS), eq(pageable))).thenReturn(mockPage);

        Page<Message> result = messageService.findPassState(pageable);
        assertEquals(1, result.getContent().size());
        verify(messageDao).findAllByState(eq(MessageService.STATE_PASS), eq(pageable));
    }

    /**
     * TC:8.1
     * 类型：黑盒测试-无效等价类
     * 描述：用null参数分页
     * 预期：抛出异常
     */
    @Test
    void testFindPassState_nullPageable() {
        assertThrows(IllegalArgumentException.class, () -> messageService.findPassState(null));
    }

}
