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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
/**
 * <dependency>
 *             <groupId>org.junit.platform</groupId>
 *             <artifactId>junit-platform-launcher</artifactId>
 *             <version>1.8.2</version>
 *             <scope>test</scope>
 *         </dependency>
 *         <!-- JUnit Platform Engine -->
 *         <dependency>
 *             <groupId>org.junit.platform</groupId>
 *             <artifactId>junit-platform-engine</artifactId>
 *             <version>1.9.2</version>
 *             <scope>test</scope>
 *         </dependency>
 *
 *         <!-- JUnit Platform Commons -->
 *         <dependency>
 *             <groupId>org.junit.platform</groupId>
 *             <artifactId>junit-platform-commons</artifactId>
 *             <version>1.9.2</version>
 *             <scope>test</scope>
 *         </dependency>
 *         <dependency>
 *             <groupId>org.junit.jupiter</groupId>
 *             <artifactId>junit-jupiter</artifactId>
 *             <version>RELEASE</version>
 *             <scope>test</scope>
 *         </dependency>
 *         <dependency>
 *             <groupId>org.junit.jupiter</groupId>
 *             <artifactId>junit-jupiter</artifactId>
 *             <version>5.5.2</version>
 *             <scope>test</scope>
 *         </dependency>
 *         <dependency>
 *             <groupId>org.mockito</groupId>
 *             <artifactId>mockito-core</artifactId>
 *             <version>5.12.0</version> <!-- 使用最新稳定版 -->
 *             <scope>test</scope>
 *         </dependency>
 *         <!-- ByteBuddy -->
 *         <dependency>
 *             <groupId>net.bytebuddy</groupId>
 *             <artifactId>byte-buddy</artifactId>
 *             <version>1.14.5</version>
 *         </dependency>
 *
 *         <!-- ByteBuddy Agent -->
 *         <dependency>
 *             <groupId>net.bytebuddy</groupId>
 *             <artifactId>byte-buddy-agent</artifactId>
 *             <version>1.14.5</version>
 *             <scope>test</scope>
 *         </dependency>
 **/
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

    // 测试用例设计 - findById方法
    // TC1.1: 查找存在的订单
    @Test
    void testFindById_ExistingOrder() {
        Order expectedOrder = new Order();
        expectedOrder.setOrderID(1);
        when(orderDao.getOne(1)).thenReturn(expectedOrder);

        Order result = orderService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getOrderID());
    }

    // TC1.2: 查找不存在的订单
    @Test
    void testFindById_NonExistingOrder() {
        when(orderDao.getOne(999)).thenReturn(null);
        assertNull(orderService.findById(999));
    }

    // 测试用例设计 - findDateOrder方法
    // TC2.1: 根据时间范围查询订单 - 有结果
    @Test
    void testFindDateOrder_WithResults() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        List<Order> expectedOrders = Arrays.asList(new Order(), new Order());
        when(orderDao.findByVenueIDAndStartTimeIsBetween(1, start, end))
            .thenReturn(expectedOrders);

        List<Order> result = orderService.findDateOrder(1, start, end);

        assertEquals(2, result.size());
    }

    // TC2.2: 根据时间范围查询订单 - 无结果 
    @Test
    void testFindDateOrder_NoResults() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        when(orderDao.findByVenueIDAndStartTimeIsBetween(1, start, end))
            .thenReturn(Collections.emptyList());

        List<Order> result = orderService.findDateOrder(1, start, end);

        assertTrue(result.isEmpty());
    }

    // 测试用例设计 - findUserOrder方法 
    // TC3.1: 查询用户订单 - 成功场景
    @Test
    void testFindUserOrder_Success() {
        String userId = "user1";
        Pageable pageable = PageRequest.of(0, 10);
        List<Order> orders = Arrays.asList(new Order(), new Order());
        Page<Order> expectedPage = new PageImpl<>(orders);
        
        when(orderDao.findAllByUserID(userId, pageable)).thenReturn(expectedPage);

        Page<Order> result = orderService.findUserOrder(userId, pageable);

        assertEquals(2, result.getTotalElements());
    }

    // TC3.2: 查询用户订单 - 用户无订单
    @Test 
    void testFindUserOrder_NoOrders() {
        String userId = "user2";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());
        
        when(orderDao.findAllByUserID(userId, pageable)).thenReturn(emptyPage);

        Page<Order> result = orderService.findUserOrder(userId, pageable);

        assertEquals(0, result.getTotalElements());
    }

    // 测试用例设计 - submit方法
    // TC4.1: 提交订单 - 正常场景
    @Test
    void testSubmit_Success() {
        Venue venue = new Venue();
        venue.setVenueID(1);
        venue.setPrice(100);
        when(venueDao.findByVenueName("testVenue")).thenReturn(venue);

        orderService.submit("testVenue", LocalDateTime.now(), 2, "testUser");

        verify(orderDao).save(argThat(order -> 
            order.getVenueID() == 1 &&
            order.getHours() == 2 &&
            order.getTotal() == 200.0 &&
            order.getState() == OrderService.STATE_NO_AUDIT
        ));
    }

    // TC4.2: 提交订单 - 边界值测试
    @ParameterizedTest
    @CsvSource({
        "0,  非法参数",   // 下边界-1
        "1,  合法参数",   // 下边界
        "24, 合法参数",   // 上边界  
        "25, 非法参数"    // 上边界+1
    })
    void testSubmit_HoursBoundary(int hours, String expected) {
        Venue venue = new Venue();
        venue.setVenueID(1); 
        venue.setPrice(100);
        when(venueDao.findByVenueName("testVenue")).thenReturn(venue);

        if (hours <= 0 || hours > 24) {
            assertThrows(IllegalArgumentException.class, () ->
                orderService.submit("testVenue", LocalDateTime.now(), hours, "testUser"));
        } else {
            assertDoesNotThrow(() ->
                orderService.submit("testVenue", LocalDateTime.now(), hours, "testUser"));
        }
    }

    // 测试用例分组1: confirmOrder方法
    // TC5.1: 确认存在的订单
    @Test
    void testConfirmOrder_ExistingOrder() {
        Order order = new Order();
        order.setOrderID(1);
        when(orderDao.findByOrderID(1)).thenReturn(order);

        orderService.confirmOrder(1);

        verify(orderDao).updateState(OrderService.STATE_WAIT, 1);
    }

    // TC5.2: 确认不存在的订单
    @Test
    void testConfirmOrder_NonExistingOrder() {
        when(orderDao.findByOrderID(999)).thenReturn(null);
        
        Exception exception = assertThrows(RuntimeException.class, () -> 
            orderService.confirmOrder(999));
        assertEquals("订单不存在", exception.getMessage());
    }

    // 测试用例分组2: finishOrder方法
    // TC6.1: 完成存在的订单
    @Test
    void testFinishOrder_ExistingOrder() {
        Order order = new Order();
        order.setOrderID(1);
        when(orderDao.findByOrderID(1)).thenReturn(order);

        orderService.finishOrder(1);

        verify(orderDao).updateState(OrderService.STATE_FINISH, 1);
    }

    // TC6.2: 完成不存在的订单
    @Test
    void testFinishOrder_NonExistingOrder() {
        when(orderDao.findByOrderID(999)).thenReturn(null);
        
        Exception exception = assertThrows(RuntimeException.class, () -> 
            orderService.finishOrder(999));
        assertEquals("订单不存在", exception.getMessage());
    }

    // 测试用例分组3: rejectOrder方法
    // TC7.1: 拒绝存在的订单
    @Test
    void testRejectOrder_ExistingOrder() {
        Order order = new Order();
        order.setOrderID(1);
        when(orderDao.findByOrderID(1)).thenReturn(order);

        orderService.rejectOrder(1);

        verify(orderDao).updateState(OrderService.STATE_REJECT, 1);
    }

    // TC7.2: 拒绝不存在的订单
    @Test
    void testRejectOrder_NonExistingOrder() {
        when(orderDao.findByOrderID(999)).thenReturn(null);
        
        Exception exception = assertThrows(RuntimeException.class, () -> 
            orderService.rejectOrder(999));
        assertEquals("订单不存在", exception.getMessage());
    }

    // 测试用例分组4: findNoAuditOrder方法
    // TC8.1: 查询待审核订单 - 有结果
    @Test
    void testFindNoAuditOrder_WithResults() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Order> orders = Arrays.asList(new Order(), new Order());
        Page<Order> expectedPage = new PageImpl<>(orders);
        when(orderDao.findAllByState(OrderService.STATE_NO_AUDIT, pageable))
            .thenReturn(expectedPage);

        Page<Order> result = orderService.findNoAuditOrder(pageable);

        assertEquals(2, result.getTotalElements());
    }

    // TC8.2: 查询待审核订单 - 无结果
    @Test
    void testFindNoAuditOrder_NoResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());
        when(orderDao.findAllByState(OrderService.STATE_NO_AUDIT, pageable))
            .thenReturn(emptyPage);

        Page<Order> result = orderService.findNoAuditOrder(pageable);

        assertEquals(0, result.getTotalElements());
    }

    // 测试用例分组5: findAuditOrder方法
    // TC9.1: 查询已审核订单 - 有结果
    @Test
    void testFindAuditOrder_WithResults() {
        List<Order> expectedOrders = Arrays.asList(new Order(), new Order());
        when(orderDao.findAudit(OrderService.STATE_WAIT, OrderService.STATE_FINISH))
            .thenReturn(expectedOrders);

        List<Order> result = orderService.findAuditOrder();

        assertEquals(2, result.size());
    }

    // TC9.2: 查询已审核订单 - 无结果
    @Test
    void testFindAuditOrder_NoResults() {
        when(orderDao.findAudit(OrderService.STATE_WAIT, OrderService.STATE_FINISH))
            .thenReturn(Collections.emptyList());

        List<Order> result = orderService.findAuditOrder();

        assertTrue(result.isEmpty());
    }
}