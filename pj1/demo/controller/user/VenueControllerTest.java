package com.demo.pj1.demo.controller.user;

import com.demo.entity.Venue;
import com.demo.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
public class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VenueService venueService;

    private List<Venue> testVenues;

    @BeforeEach
    void setUp() {
        testVenues = new ArrayList<>();
        // 创建测试场馆数据
        for (int i = 1; i <= 10; i++) {
            Venue venue = new Venue();
            venue.setVenueName("Test Venue " + i);
            venue.setAddress("Test Address " + i);
            venue.setDescription("Test Description " + i);
            venue.setPrice(100 + i);
            venue.setPicture("");
            venue.setOpen_time("09:00");
            venue.setClose_time("22:00");
            testVenues.add(venue);
            venueService.create(venue);
        }
    }

    @Test
    void testToGymPage_Success() throws Exception {
        // 获取第一个测试场馆
        Venue testVenue = testVenues.get(0);

        mockMvc.perform(get("/venue")
                .param("venueID", String.valueOf(testVenue.getVenueID())))
                .andExpect(status().isOk())
                .andExpect(view().name("venue"))
                .andExpect(model().attributeExists("venue"))
                .andExpect(model().attribute("venue", 
                    hasProperty("venueName", is(testVenue.getVenueName()))))
                .andDo(print());
    }

    @Test
    void testToGymPage_NonexistentVenue() throws Exception {
        try {
            mockMvc.perform(get("/venue")
                    .param("venueID", "999"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception.getMessage()
                            .contains("No class com.demo.entity.Venue entity with id 999 exists"));
                    })
                    .andDo(print());
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertTrue(e.getCause().getMessage()
                .contains("No class com.demo.entity.Venue entity with id 999 exists"));
        }
    }

    @Test
    void testVenueList_FirstPage() throws Exception {
        mockMvc.perform(get("/venuelist/getVenueList")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[0].venueName", startsWith("Test Venue")))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andDo(print());
    }

    @Test
    void testVenueList_SecondPage() throws Exception {
        mockMvc.perform(get("/venuelist/getVenueList")
                .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[0].venueName", startsWith("Test Venue")))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andDo(print());
    }

    @Test
    void testVenueList_EmptyPage() throws Exception {
        mockMvc.perform(get("/venuelist/getVenueList")
                .param("page", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", empty()))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andDo(print());
    }

    @Test
    void testVenueListPage() throws Exception {
        mockMvc.perform(get("/venue_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue_list"))
                .andExpect(model().attributeExists("venue_list"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("venue_list", hasSize(5)))
                .andExpect(model().attribute("venue_list", 
                    hasItem(hasProperty("venueName", startsWith("Test Venue")))))
                .andDo(print());
    }

    // 边界测试 - 分页参数验证
    @Test
    void testVenueList_InvalidPage() throws Throwable {
        try {
            // 测试负数页码
            mockMvc.perform(get("/venuelist/getVenueList")
                    .param("page", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception instanceof IllegalArgumentException);
                        assertEquals("Page index must not be less than zero!", 
                            exception.getMessage());
                    })
                    .andDo(print());

            // 测试零页码
            mockMvc.perform(get("/venuelist/getVenueList")
                    .param("page", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception instanceof IllegalArgumentException);
                        assertEquals("Page index must not be less than zero!", 
                            exception.getMessage());
                    });
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertEquals(e.getMessage(), "Page index must not be less than zero!");
        }
    }

    // 性能测试 - 大量场馆数据查询
    @Test
    void testVenueList_Performance() throws Exception {
        // 创建大量测试数据
        for (int i = 11; i <= 100; i++) {
            Venue venue = new Venue();
            venue.setVenueName("Performance Test Venue " + i);
            venue.setAddress("Performance Test Address " + i);
            venue.setDescription("Performance Test Description " + i);
            venue.setPrice(100 + i);
            venue.setPicture("");
            venue.setOpen_time("09:00");
            venue.setClose_time("22:00");
            venueService.create(venue);
        }

        long startTime = System.currentTimeMillis();
        
        // 测试多页查询性能
        for (int i = 1; i <= 20; i++) {
            mockMvc.perform(get("/venuelist/getVenueList")
                    .param("page", String.valueOf(i)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").exists());
        }

        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 2000, 
            "Performance test failed: " + duration + "ms > 2000ms");
    }

    // 并发测试 - 同时访问场馆详情
    @Test
    void testVenue_ConcurrentAccess() throws Exception {
        Venue testVenue = testVenues.get(0);
        List<Thread> threads = new ArrayList<>();
        
        // 创建多个并发请求
        for (int i = 0; i < 10; i++) {
            threads.add(new Thread(() -> {
                try {
                    mockMvc.perform(get("/venue")
                            .param("venueID", String.valueOf(testVenue.getVenueID())))
                            .andExpect(status().isOk())
                            .andExpect(model().attributeExists("venue"));
                } catch (Exception e) {
                    fail("Concurrent access test failed: " + e.getMessage());
                }
            }));
        }

        // 启动所有线程
        threads.forEach(Thread::start);
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
    }

    // 数据完整性测试
    @Test
    void testVenue_DataIntegrity() throws Exception {
        Venue testVenue = testVenues.get(0);
        String originalName = testVenue.getVenueName();
        String originalAddress = testVenue.getAddress();
        String originalDescription = testVenue.getDescription();
        double originalPrice = testVenue.getPrice();

        mockMvc.perform(get("/venue")
                .param("venueID", String.valueOf(testVenue.getVenueID())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("venue", allOf(
                    hasProperty("venueName", is(originalName)),
                    hasProperty("address", is(originalAddress)),
                    hasProperty("description", is(originalDescription)),
                    hasProperty("price", is(originalPrice))
                )))
                .andDo(print());
    }

    // 营业时间边界测试
    @Test
    void testVenue_BusinessHours() throws Throwable {
        // 创建特殊营业时间的场馆
        Venue specialVenue = new Venue();
        specialVenue.setVenueName("Special Hours Venue");
        specialVenue.setAddress("Special Address");
        specialVenue.setDescription("Special Description");
        specialVenue.setPrice(100);
        specialVenue.setPicture("");
        specialVenue.setOpen_time("00:00");
        specialVenue.setClose_time("24:00");
        int venueId = venueService.create(specialVenue);

        try {
            mockMvc.perform(get("/venue")
                    .param("venueID", String.valueOf(venueId)))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("venue", 
                        hasProperty("open_time", is("00:00"))))
                    .andExpect(model().attribute("venue", 
                        hasProperty("close_time", is("24:00"))))
                    .andDo(print());
        } catch (Exception e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                throw e.getCause();
            }
            throw e;
        }
    }

    // 缓存测试
    @Test
    void testVenueList_CacheValidation() throws Exception {
        // 首次请求
        mockMvc.perform(get("/venue_list"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("venue_list"));

        // 添加新场馆
        Venue newVenue = new Venue();
        newVenue.setVenueName("Cache Test Venue");
        newVenue.setAddress("Cache Test Address");
        newVenue.setDescription("Cache Test Description");
        newVenue.setPrice(150);
        newVenue.setPicture("");
        newVenue.setOpen_time("09:00");
        newVenue.setClose_time("22:00");
        venueService.create(newVenue);

        // 再次请求，验证是否包含新添加的场馆
        mockMvc.perform(get("/venue_list"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("venue_list", 
                    hasItem(hasProperty("venueName", is("Cache Test Venue")))))
                .andDo(print());
    }
}