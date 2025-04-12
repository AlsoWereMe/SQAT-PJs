package com.demo.pj1.demo.service;

import com.demo.dao.OrderDao;
import com.demo.dao.VenueDao;
import com.demo.entity.Order;
import com.demo.entity.Venue;
import com.demo.entity.vo.OrderVo;
import com.demo.service.impl.OrderVoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OrderVoServiceTest {

    @InjectMocks
    private OrderVoServiceImpl orderVoService;

    @Mock
    private OrderDao orderDao;

    @Mock
    private VenueDao venueDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // 测试用例分组1: returnOrderVoByOrderID方法
    // TC1.1: 获取单个订单视图 - 正常场景
    @Test
    void testReturnOrderVoByOrderID_Success() {
        // Arrange
        Order order = createTestOrder(1, "user1", 1, 2, 200);
        Venue venue = createTestVenue(1, "TestVenue");
        
        when(orderDao.findByOrderID(1)).thenReturn(order);
        when(venueDao.findByVenueID(1)).thenReturn(venue);

        // Act
        OrderVo result = orderVoService.returnOrderVoByOrderID(1);

        // Assert
        assertNotNull(result);
        assertEquals("TestVenue", result.getVenueName());
        assertEquals("user1", result.getUserID());
        assertEquals(200.0, result.getTotal());
    }

    // TC1.2: 获取单个订单视图 - 订单不存在
    @Test
    void testReturnOrderVoByOrderID_OrderNotFound() {
        // Arrange
        when(orderDao.findByOrderID(999)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(NullPointerException.class, () -> 
            orderVoService.returnOrderVoByOrderID(999));
    }

    // TC1.3: 获取单个订单视图 - 场馆不存在
    @Test
    void testReturnOrderVoByOrderID_VenueNotFound() {
        // Arrange
        Order order = createTestOrder(1, "user1", 1, 2, 200);
        when(orderDao.findByOrderID(1)).thenReturn(order);
        when(venueDao.findByVenueID(1)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(NullPointerException.class, () -> 
            orderVoService.returnOrderVoByOrderID(1));
    }

    // 语句覆盖：订单列表转换
    @Test
    void testReturnVo_MultipleOrders() {
        // Arrange
        Order order1 = createTestOrder(1, "user1", 1, 2, 200);
        Order order2 = createTestOrder(2, "user2", 2, 3, 300);
        List<Order> orders = Arrays.asList(order1, order2);

        Venue venue1 = createTestVenue(1, "Venue1");
        Venue venue2 = createTestVenue(2, "Venue2");

        when(orderDao.findByOrderID(1)).thenReturn(order1);
        when(orderDao.findByOrderID(2)).thenReturn(order2);
        when(venueDao.findByVenueID(1)).thenReturn(venue1);
        when(venueDao.findByVenueID(2)).thenReturn(venue2);

        // Act
        List<OrderVo> result = orderVoService.returnVo(orders);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Venue1", result.get(0).getVenueName());
        assertEquals("Venue2", result.get(1).getVenueName());
    }

    // 测试用例分组2: returnVo方法
    // TC2.1: 获取多个订单视图 - 正常场景
    @Test
    void testReturnVo_Success() {
        // Arrange
        Order order1 = createTestOrder(1, "user1", 1, 2, 200);
        Order order2 = createTestOrder(2, "user2", 2, 3, 300);
        List<Order> orders = Arrays.asList(order1, order2);

        Venue venue1 = createTestVenue(1, "Venue1");
        Venue venue2 = createTestVenue(2, "Venue2");

        when(orderDao.findByOrderID(1)).thenReturn(order1);
        when(orderDao.findByOrderID(2)).thenReturn(order2);
        when(venueDao.findByVenueID(1)).thenReturn(venue1);
        when(venueDao.findByVenueID(2)).thenReturn(venue2);

        // Act
        List<OrderVo> result = orderVoService.returnVo(orders);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Venue1", result.get(0).getVenueName());
        assertEquals("Venue2", result.get(1).getVenueName());
    }

    // TC2.2: 获取多个订单视图 - 空列表
    @Test
    void testReturnVo_EmptyList() {
        List<Order> emptyList = Arrays.asList();
        List<OrderVo> result = orderVoService.returnVo(emptyList);
        assertTrue(result.isEmpty());
    }

    // TC2.3: 获取多个订单视图 - 列表中包含无效订单
    @Test
    void testReturnVo_InvalidOrderInList() {
        // Arrange
        Order validOrder = createTestOrder(1, "user1", 1, 2, 200);
        Order invalidOrder = createTestOrder(2, "user2", 999, 3, 300);
        List<Order> orders = Arrays.asList(validOrder, invalidOrder);

        Venue venue1 = createTestVenue(1, "Venue1");
        
        when(orderDao.findByOrderID(1)).thenReturn(validOrder);
        when(orderDao.findByOrderID(2)).thenReturn(invalidOrder);
        when(venueDao.findByVenueID(1)).thenReturn(venue1);
        when(venueDao.findByVenueID(999)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(NullPointerException.class, () -> 
            orderVoService.returnVo(orders));
    }

    // 边界值测试：不同状态的订单
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3}) // 不同订单状态
    void testReturnOrderVoByOrderID_DifferentStates(int state) {
        // Arrange
        Order order = createTestOrder(1, "user1", 1, 2, 200);
        order.setState(state);
        Venue venue = createTestVenue(1, "TestVenue");

        when(orderDao.findByOrderID(1)).thenReturn(order);
        when(venueDao.findByVenueID(1)).thenReturn(venue);

        // Act
        OrderVo result = orderVoService.returnOrderVoByOrderID(1);

        // Assert
        assertEquals(state, result.getState());
    }

    // 等价类划分：多个场次时间的订单
    @Test
    void testReturnVo_DifferentTimeSlots() {
        // Arrange
        Order order1 = createTestOrder(1, "user1", 1, 2, 200);
        Order order2 = createTestOrder(2, "user1", 1, 3, 300);
        order1.setStartTime(LocalDateTime.now().plusDays(1).withHour(9));
        order2.setStartTime(LocalDateTime.now().plusDays(1).withHour(14));
        List<Order> orders = Arrays.asList(order1, order2);

        Venue venue = createTestVenue(1, "TestVenue");

        when(orderDao.findByOrderID(anyInt())).thenReturn(order1, order2);
        when(venueDao.findByVenueID(1)).thenReturn(venue);

        // Act
        List<OrderVo> result = orderVoService.returnVo(orders);

        // Assert
        assertEquals(2, result.size());
        assertNotEquals(
            result.get(0).getStartTime(), 
            result.get(1).getStartTime()
        );
    }

    // 性能测试：大量订单转换
    @Test
    void testReturnVo_LargeOrderList() {
        // Arrange
        List<Order> orders = new ArrayList<>();
        int orderCount = 100;
        
        for (int i = 1; i <= orderCount; i++) {
            Order order = createTestOrder(i, "user" + i, 1, 2, 200);
            orders.add(order);
            when(orderDao.findByOrderID(i)).thenReturn(order);
        }
        
        Venue venue = createTestVenue(1, "TestVenue");
        when(venueDao.findByVenueID(1)).thenReturn(venue);

        // Act
        long startTime = System.currentTimeMillis();
        List<OrderVo> result = orderVoService.returnVo(orders);
        long endTime = System.currentTimeMillis();

        // Assert
        assertEquals(orderCount, result.size());
        assertTrue((endTime - startTime) < 1000); // 确保处理时间在1秒内
    }

    // Helper methods
    private Order createTestOrder(int orderId, String userId, int venueId, 
                                int hours, int total) {
        Order order = new Order();
        order.setOrderID(orderId);
        order.setUserID(userId);
        order.setVenueID(venueId);
        order.setHours(hours);
        order.setTotal(total);
        order.setState(0); // 默认状态
        order.setOrderTime(LocalDateTime.now());
        order.setStartTime(LocalDateTime.now().plusDays(1));
        return order;
    }

    private Venue createTestVenue(int venueId, String venueName) {
        Venue venue = new Venue();
        venue.setVenueID(venueId);
        venue.setVenueName(venueName);
        return venue;
    }
}