package com.demo.pj1.demo.service;

import com.demo.dao.OrderDao;
import com.demo.dao.VenueDao;
import com.demo.entity.Order;
import com.demo.entity.Venue;
import com.demo.service.OrderService;
import com.demo.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OrderServiceTest {
    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderDao orderDao;

    @Mock
    private VenueDao venueDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // 等价类划分：正常提交订单
    @Test
    void testSubmitOrder_NormalCase() {
        Venue venue = new Venue();
        venue.setVenueID(1);
        venue.setPrice(100);
        when(venueDao.findByVenueName("testVenue")).thenReturn(venue);

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        orderService.submit("testVenue", startTime, 2, "testUser");

        verify(orderDao).save(argThat(order -> 
            order.getVenueID() == 1 &&
            order.getHours() == 2 &&
            order.getTotal() == 200.0 &&
            order.getState() == OrderService.STATE_NO_AUDIT
        ));
    }

    // 边界值测试：订单时长边界值
    @ParameterizedTest
    @CsvSource({
        "1",    // 最小时长
        "24"    // 最大时长
    })
    void testSubmitOrder_HoursBoundary(int hours) {
        Venue venue = new Venue();
        venue.setVenueID(1);
        venue.setPrice(100);
        when(venueDao.findByVenueName("testVenue")).thenReturn(venue);

        orderService.submit("testVenue", LocalDateTime.now(), hours, "testUser");
        
        verify(orderDao).save(any(Order.class));
    }

    // 判定覆盖：订单状态更新的各种情况
    @Test
    void testConfirmOrder_OrderExists() {
        Order order = new Order();
        order.setOrderID(1);
        when(orderDao.findByOrderID(1)).thenReturn(order);

        orderService.confirmOrder(1);
        verify(orderDao).updateState(OrderService.STATE_WAIT, 1);
    }

    @Test
    void testConfirmOrder_OrderNotExists() {
        when(orderDao.findByOrderID(1)).thenReturn(null);
        
        assertThrows(RuntimeException.class, () -> orderService.confirmOrder(1));
    }

    // 等价类划分：查找特定日期范围内的订单
    @Test
    void testFindDateOrder() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        List<Order> expectedOrders = new ArrayList<>();
        when(orderDao.findByVenueIDAndStartTimeIsBetween(1, start, end))
            .thenReturn(expectedOrders);

        List<Order> result = orderService.findDateOrder(1, start, end);

        assertEquals(expectedOrders, result);
    }

    // 语句覆盖：更新订单信息
    @Test
    void testUpdateOrder() {
        Venue venue = new Venue();
        venue.setVenueID(2);
        venue.setPrice(150);
        Order order = new Order();
        order.setOrderID(1);

        when(venueDao.findByVenueName("newVenue")).thenReturn(venue);
        when(orderDao.findByOrderID(1)).thenReturn(order);

        LocalDateTime startTime = LocalDateTime.now();
        orderService.updateOrder(1, "newVenue", startTime, 3, "user1");

        verify(orderDao).save(argThat(updatedOrder -> 
            updatedOrder.getVenueID() == 2 &&
            updatedOrder.getHours() == 3 &&
            updatedOrder.getTotal() == 450.0 &&
            updatedOrder.getState() == OrderService.STATE_NO_AUDIT
        ));
    }

    // 分支覆盖：不同状态的订单查询
    @Test
    void testFindNoAuditOrder() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> expectedPage = new PageImpl<>(new ArrayList<>());
        when(orderDao.findAllByState(OrderService.STATE_NO_AUDIT, pageable))
            .thenReturn(expectedPage);

        Page<Order> result = orderService.findNoAuditOrder(pageable);

        assertEquals(expectedPage, result);
    }
}