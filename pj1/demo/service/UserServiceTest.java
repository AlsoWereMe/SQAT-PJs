package com.demo.service;

import com.demo.dao.UserDao;
import com.demo.entity.User;
import com.demo.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // 测试有效用户ID
    @Test
    void testFindByUserID_Valid() {
        String userID = "testUser";
        User mockUser = new User();
        mockUser.setUserID(userID);

        when(userDao.findByUserID(userID)).thenReturn(mockUser);

        User result = userService.findByUserID(userID);

        assertNotNull(result);
        assertEquals(userID, result.getUserID());
        verify(userDao, times(1)).findByUserID(userID);
    }

    // 测试无效用户ID
    @Test
    void testFindByUserID_Invalid() {
        String userID = "invalidUser";

        when(userDao.findByUserID(userID)).thenReturn(null);

        User result = userService.findByUserID(userID);

        assertNull(result);
        verify(userDao, times(1)).findByUserID(userID);
    }

    // 测试空用户ID
    @Test
    void testFindByUserID_Empty() {
        String userID = "";

        when(userDao.findByUserID(userID)).thenReturn(null);

        User result = userService.findByUserID(userID);

        assertNull(result);
        verify(userDao, times(1)).findByUserID(userID);
    }

    // 测试null用户ID
    @Test
    void testFindByUserID_Null() {
        String userID = null;

        when(userDao.findByUserID(userID)).thenReturn(null);

        User result = userService.findByUserID(userID);

        assertNull(result);
        verify(userDao, times(1)).findByUserID(userID);
    }

    // 测试有效ID
    @Test
    void testFindById_Valid() {
        int id = 1;
        User mockUser = new User();
        mockUser.setId(id);

        when(userDao.findById(id)).thenReturn(mockUser);

        User result = userService.findById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(userDao, times(1)).findById(id);
    }

    // 测试无效ID
    @Test
    void testFindById_Invalid() {
        int id = 999;

        when(userDao.findById(id)).thenReturn(null);

        User result = userService.findById(id);

        assertNull(result);
        verify(userDao, times(1)).findById(id);
    }

    // 测试最小值ID
    @Test
    void testFindById_MinValue() {
        int id = 0;

        when(userDao.findById(id)).thenReturn(null);

        User result = userService.findById(id);

        assertNull(result);
        verify(userDao, times(1)).findById(id);
    }

    // 测试负数ID
    @Test
    void testFindById_Negative() {
        int id = -1;

        when(userDao.findById(id)).thenReturn(null);

        User result = userService.findById(id);

        assertNull(result);
        verify(userDao, times(1)).findById(id);
    }

    // 测试带分页的通过用户ID查找用户的方法
    @Test
    void testFindByUserIDWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        User user1 = new User();
        user1.setUserID("user1");
        User user2 = new User();
        user2.setUserID("user2");
        Page<User> mockPage = new PageImpl<>(Arrays.asList(user1, user2));

        when(userDao.findAllByIsadmin(0, pageable)).thenReturn(mockPage);

        Page<User> result = userService.findByUserID(pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(userDao, times(1)).findAllByIsadmin(0, pageable);
    }

    // 测试正确的用户ID和密码
    @Test
    void testCheckLogin_Valid() {
        String userID = "testUser";
        String password = "password123";
        User mockUser = new User();
        mockUser.setUserID(userID);
        mockUser.setPassword(password);

        when(userDao.findByUserIDAndPassword(userID, password)).thenReturn(mockUser);

        User result = userService.checkLogin(userID, password);

        assertNotNull(result);
        assertEquals(userID, result.getUserID());
        assertEquals(password, result.getPassword());
        verify(userDao, times(1)).findByUserIDAndPassword(userID, password);
    }

    // 测试错误的用户ID或密码
    @Test
    void testCheckLogin_Invalid() {
        String userID = "testUser";
        String password = "wrongPassword";

        when(userDao.findByUserIDAndPassword(userID, password)).thenReturn(null);

        User result = userService.checkLogin(userID, password);

        assertNull(result);
        verify(userDao, times(1)).findByUserIDAndPassword(userID, password);
    }

    // 测试空用户ID或密码
    @Test
    void testCheckLogin_Empty() {
        String userID = "";
        String password = "";

        when(userDao.findByUserIDAndPassword(userID, password)).thenReturn(null);

        User result = userService.checkLogin(userID, password);

        assertNull(result);
        verify(userDao, times(1)).findByUserIDAndPassword(userID, password);
    }

    // 测试null用户ID或密码
    @Test
    void testCheckLogin_Null() {
        String userID = null;
        String password = null;

        when(userDao.findByUserIDAndPassword(userID, password)).thenReturn(null);

        User result = userService.checkLogin(userID, password);

        assertNull(result);
        verify(userDao, times(1)).findByUserIDAndPassword(userID, password);
    }

    // 测试无效用户对象
    @Test
    void testCreateUser_Invalid() {
        User newUser = null;

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            if (newUser == null) {
                throw new IllegalArgumentException("User cannot be null");
            }
        });

        assertEquals("User cannot be null", exception.getMessage());
        verify(userDao, times(0)).save(any(User.class));
    }

    // 测试通过ID删除用户的方法
    @Test
    void testDelByID() {
        int id = 1;

        doNothing().when(userDao).deleteById(id);

        userService.delByID(id);

        verify(userDao, times(1)).deleteById(id);
    }

    // 测试更新用户信息的方法
    @Test
    void testUpdateUser() {
        User updatedUser = new User();
        updatedUser.setUserID("updatedUser");

        when(userDao.save(updatedUser)).thenReturn(updatedUser);

        userService.updateUser(updatedUser);

        verify(userDao, times(1)).save(updatedUser);
    }

    // 测试统计用户ID数量的方法
    @Test
    void testCountUserID() {
        String userID = "testUser";

        when(userDao.countByUserID(userID)).thenReturn(1);

        int result = userService.countUserID(userID);

        assertEquals(1, result);
        verify(userDao, times(1)).countByUserID(userID);
    }
}

