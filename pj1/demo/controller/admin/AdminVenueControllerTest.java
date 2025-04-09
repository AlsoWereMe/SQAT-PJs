package com.demo.pj1.demo.controller.admin;

import com.demo.entity.Venue;
import com.demo.service.VenueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;

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
public class AdminVenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VenueService venueService;

    private List<Venue> testVenues;

    @BeforeEach
    void setUp() {
        testVenues = new ArrayList<>();
        // 创建测试场馆数据
        for (int i = 1; i <= 15; i++) {
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

    @AfterEach
    void tearDown() {
        // 清理测试数据
        for (Venue venue : testVenues) {
            try {
                venueService.delById(venue.getVenueID());
            } catch (Exception e) {
                // 忽略删除失败的异常
            }
        }
    }

    @Test
    void testVenueManage() throws Exception {
        mockMvc.perform(get("/venue_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/venue_manage"))
                .andExpect(model().attributeExists("total"))
                .andDo(print());
    }

    @Test
    void testVenueList_NormalCase() throws Exception {
        mockMvc.perform(get("/venueList.do")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].venueName", startsWith("Test Venue")))
                .andDo(print());
    }

    @Test
    void testVenueList_EmptyPage() throws Exception {
        mockMvc.perform(get("/venueList.do")
                .param("page", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testVenueAdd() throws Exception {
        mockMvc.perform(get("/venue_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_add"));
    }

    @Test
    void testVenueEdit() throws Exception {
        // 获取第一个测试场馆
        Venue testVenue = testVenues.get(0);

        mockMvc.perform(get("/venue_edit")
                .param("venueID", String.valueOf(testVenue.getVenueID())))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_edit"))
                .andExpect(model().attributeExists("venue"));
    }

    @Test
    void testAddVenue_Success() throws Exception {
        MockMultipartFile picture = new MockMultipartFile(
                "picture",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        mockMvc.perform(multipart("/addVenue.do")
                .file(picture)
                .param("venueName", "New Test Venue")
                .param("address", "New Test Address")
                .param("description", "New Test Description")
                .param("price", "150")
                .param("open_time", "09:00")
                .param("close_time", "22:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        // 验证场馆是否成功添加
        Venue addedVenue = venueService.findByVenueName("New Test Venue");
        assertNotNull(addedVenue);
        assertEquals("New Test Address", addedVenue.getAddress());
    }

    @Test
    void testModifyVenue_Success() throws Exception {
        // 获取第一个测试场馆
        Venue testVenue = testVenues.get(0);
        String updatedVenueName = "Updated Venue Name";

        MockMultipartFile picture = new MockMultipartFile(
                "picture",
                "",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/modifyVenue.do")
                .file(picture)
                .param("venueID", String.valueOf(testVenue.getVenueID()))
                .param("venueName", updatedVenueName)
                .param("address", testVenue.getAddress())
                .param("description", testVenue.getDescription())
                .param("price", String.valueOf(testVenue.getPrice()))
                .param("open_time", testVenue.getOpen_time())
                .param("close_time", testVenue.getClose_time()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        // 验证场馆信息是否更新
        Venue updatedVenue = venueService.findByVenueID(testVenue.getVenueID());
        assertEquals(updatedVenueName, updatedVenue.getVenueName());
    }

    @Test
    void testDelVenue_Success() throws Exception {
        // 获取第一个测试场馆
        Venue testVenue = testVenues.get(0);

        mockMvc.perform(post("/delVenue.do")
                .param("venueID", String.valueOf(testVenue.getVenueID()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 验证场馆是否已被删除
        assertThrows(Exception.class, () -> venueService.findByVenueID(testVenue.getVenueID()));
    }

    @Test
    void testDelVenue_NonexistentVenue() throws Exception {
        try {
            mockMvc.perform(post("/delVenue.do")
                    .param("venueID", "999")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception.getMessage().contains("No class com.demo.entity.Venue entity with id 999 exists"));
                    })
                    .andDo(print());
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertTrue(e.getCause().getMessage().contains("No class com.demo.entity.Venue entity with id 999 exists"));
        }
    }

    @Test
    void testCheckVenueName_Unique() throws Exception {
        mockMvc.perform(post("/checkVenueName.do")
                .param("venueName", "UniqueVenueName")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckVenueName_Duplicate() throws Exception {
        // 使用已存在的测试场馆名称
        String existingVenueName = testVenues.get(0).getVenueName();
        
        mockMvc.perform(post("/checkVenueName.do")
                .param("venueName", existingVenueName)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}