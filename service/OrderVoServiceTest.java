package com.demo.service;

import com.demo.dao.OrderDao;
import com.demo.dao.VenueDao;
import com.demo.entity.Order;
import com.demo.entity.Venue;
import com.demo.entity.vo.OrderVo;
import com.demo.service.impl.OrderVoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
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

    // 等价类划分：正常订单视图转换
    @Test
    void testReturnOrderVoByOrderID_NormalCase() {
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

    // 边界值测试：空订单列表
    @Test
    void testReturnVo_EmptyList() {
        List<Order> emptyList = Arrays.asList();
        List<OrderVo> result = orderVoService.returnVo(emptyList);
        assertTrue(result.isEmpty());
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