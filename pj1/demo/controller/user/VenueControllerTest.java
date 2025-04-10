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
}