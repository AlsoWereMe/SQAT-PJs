package com.demo.pj1.demo.controller.admin;

import com.demo.entity.Order;
import com.demo.entity.Venue;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
import com.demo.dao.VenueDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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
public class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    @Autowired
    private VenueDao venueDao;

    private List<Order> testOrders;
    private Venue testVenue;

    @BeforeEach
    void setUp() {
        // 创建测试场地
        testVenue = new Venue();
        testVenue.setVenueName("Test Venue");
        testVenue.setPrice(100);
        testVenue.setDescription("Test Description");
        venueDao.save(testVenue);

        // 创建测试订单
        testOrders = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Order order = new Order();
            order.setUserID("testUser");
            order.setVenueID(testVenue.getVenueID());
            order.setState(OrderService.STATE_NO_AUDIT);
            order.setOrderTime(LocalDateTime.now());
            order.setStartTime(LocalDateTime.now().plusDays(1));
            order.setHours(2);
            order.setTotal(200);
            testOrders.add(order);
        }
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        for (Order order : testOrders) {
            try {
                orderService.delOrder(order.getOrderID());
            } catch (Exception e) {
                // 忽略删除失败的异常
            }
        }
        try {
            venueDao.delete(testVenue);
        } catch (Exception e) {
            // 忽略删除失败的异常
        }
    }

    @Test
    void testReservationManage() throws Exception {
        mockMvc.perform(get("/reservation_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation_manage"))
                .andExpect(model().attributeExists("order_list"))
                .andExpect(model().attributeExists("total"))
                .andDo(print());
    }

    @Test
    void testGetNoAuditOrder_NormalCase() throws Exception {
        mockMvc.perform(get("/admin/getOrderList.do")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(10))))
                .andExpect(jsonPath("$[0].state", is(OrderService.STATE_NO_AUDIT)))
                .andDo(print());
    }

    @Test
    void testGetNoAuditOrder_EmptyPage() throws Exception {
        mockMvc.perform(get("/admin/getOrderList.do")
                .param("page", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testConfirmOrder_Success() throws Exception {
        // 创建测试订单
        orderService.submit("Test Venue", LocalDateTime.now().plusDays(1), 2, "testUser");
        
        // 获取最新创建的订单
        Pageable pageable = PageRequest.of(0, 1, Sort.by("orderTime").descending());
        List<Order> orders = orderService.findNoAuditOrder(pageable).getContent();
        assertFalse(orders.isEmpty(), "Order should be created");
        int orderId = orders.get(0).getOrderID();

        // 执行确认操作
        mockMvc.perform(post("/passOrder.do")
                .param("orderID", String.valueOf(orderId))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 验证订单状态已更新
        Order updatedOrder = orderService.findById(orderId);
        assertNotNull(updatedOrder, "Order should exist after confirmation");
        assertEquals(OrderService.STATE_WAIT, updatedOrder.getState(), 
                "Order state should be WAIT after confirmation");
    }

    @Test
    void testConfirmOrder_NonexistentOrder() throws Exception {
        try {
            mockMvc.perform(post("/passOrder.do")
                    .param("orderID", "999")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> {
                        // 获取异常并验证异常信息
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception, "Expected exception to be thrown");
                        assertTrue(exception instanceof RuntimeException);
                        assertEquals("订单不存在", exception.getMessage());
                    })
                    .andDo(print());
        } catch (Exception e) {
            // 捕获并验证异常
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("订单不存在", e.getCause().getMessage());
        }
    }

    @Test
    void testRejectOrder_Success() throws Exception {
        // 创建测试订单并获取ID
        orderService.submit("Test Venue", LocalDateTime.now().plusDays(1), 2, "testUser");
        
        // 获取最新创建的订单
        Pageable pageable = PageRequest.of(0, 1, Sort.by("orderTime").descending());
        List<Order> orders = orderService.findNoAuditOrder(pageable).getContent();
        assertFalse(orders.isEmpty(), "Order should be created");
        int orderId = orders.get(0).getOrderID();

        // 执行拒绝操作
        mockMvc.perform(post("/rejectOrder.do")
                .param("orderID", String.valueOf(orderId))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 验证订单状态已更新
        Order updatedOrder = orderService.findById(orderId);
        assertNotNull(updatedOrder, "Order should exist after rejection");
        assertEquals(OrderService.STATE_REJECT, updatedOrder.getState(), 
                "Order state should be REJECT after rejection");
    }

    @Test
    void testRejectOrder_NonexistentOrder() throws Exception {
        try {
            mockMvc.perform(post("/rejectOrder.do")
                    .param("orderID", "999")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> {
                        // 获取异常并验证异常信息
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception, "Expected exception to be thrown");
                        assertTrue(exception instanceof RuntimeException);
                        assertEquals("订单不存在", exception.getMessage());
                    })
                    .andDo(print());
        } catch (Exception e) {
            // 捕获并验证异常
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("订单不存在", e.getCause().getMessage());
        }
    }

    @Test
    void testOrderOperations_ConcurrentModification() throws Exception {
        // 创建测试订单并获取ID
        orderService.submit("Test Venue", LocalDateTime.now().plusDays(1), 2, "testUser");
        
        // 获取最新创建的订单
        Pageable pageable = PageRequest.of(0, 1, Sort.by("orderTime").descending());
        List<Order> orders = orderService.findNoAuditOrder(pageable).getContent();
        assertFalse(orders.isEmpty(), "Order should be created");
        int orderId = orders.get(0).getOrderID();

        // 先通过订单
        mockMvc.perform(post("/passOrder.do")
                .param("orderID", String.valueOf(orderId)))
                .andExpect(status().isOk());

        // 尝试拒绝已经通过的订单
        mockMvc.perform(post("/rejectOrder.do")
                .param("orderID", String.valueOf(orderId)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("订单状态已改变")));
    }

    @Test
    void testGetOrderList_Performance() throws Exception {
        // 准备大量测试数据
        for (int i = 0; i < 100; i++) {
            Order order = new Order();
            order.setUserID("testUser");
            order.setVenueID(testVenue.getVenueID());
            order.setState(OrderService.STATE_NO_AUDIT);
            order.setOrderTime(LocalDateTime.now());
            order.setStartTime(LocalDateTime.now().plusDays(1));
            order.setHours(2);
            order.setTotal(200);
            orderService.submit("Test Venue", 
                LocalDateTime.now().plusDays(1), 2, "testUser");
        }

        // 执行性能测试
        long startTime = System.currentTimeMillis();
        
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(get("/admin/getOrderList.do")
                    .param("page", String.valueOf(i)))
                    .andExpect(status().isOk());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 2000, 
                "Performance test failed. Expected duration < 2000ms but was " + duration + "ms");
    }
}