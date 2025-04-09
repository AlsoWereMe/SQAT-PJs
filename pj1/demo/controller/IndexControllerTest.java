package com.demo.pj1.demo.controller;

import com.demo.entity.Message;
import com.demo.entity.News;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.service.MessageService;
import com.demo.service.NewsService;
import com.demo.service.UserService;
import com.demo.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
public class IndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NewsService newsService;

    @Autowired
    private VenueService venueService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    private List<News> testNewsList;
    private List<Venue> testVenueList;
    private List<Message> testMessageList;

    @BeforeEach
    void setUp() {
        // 创建测试新闻数据
        testNewsList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            News news = new News();
            news.setTitle("Test News " + i);
            news.setContent("Test Content " + i);
            news.setTime(LocalDateTime.now().minusDays(i));
            testNewsList.add(news);
            newsService.create(news);
        }

        // 创建测试场馆数据
        testVenueList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Venue venue = new Venue();
            venue.setVenueName("Test Venue " + i);
            venue.setAddress("Test Address " + i);
            venue.setDescription("Test Description " + i);
            venue.setPrice(100 + i);
            venue.setPicture("");
            venue.setOpen_time("09:00");
            venue.setClose_time("22:00");
            testVenueList.add(venue);
            venueService.create(venue);
        }

        // 创建测试用户数据
        for (int i = 1; i <= 10; i++) {
            User user = new User();
            user.setUserID("testUser" + i);
            user.setUserName("Test User " + i);
            user.setPassword("password" + i);
            user.setEmail("test" + i + "@example.com");
            user.setPhone("1234567890" + i);
            userService.create(user);
        }

        // 创建测试留言数据
        testMessageList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Message message = new Message();
            message.setContent("Test Message " + i);
            message.setTime(LocalDateTime.now().minusDays(i));
            message.setState(MessageService.STATE_PASS);  // 已通过状态
            message.setUserID("testUser" + i);
            testMessageList.add(message);
            messageService.create(message);
        }
    }

    @Test
    void testIndex_PageLoad() throws Exception {
        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("news_list"))
                .andExpect(model().attributeExists("venue_list"))
                .andExpect(model().attributeExists("message_list"))
                .andExpect(model().attribute("user", nullValue()))
                .andDo(print());
    }

    @Test
    void testIndex_NewsListContent() throws Exception {
        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("news_list", hasSize(5)))  // 验证只返回5条新闻
                .andExpect(model().attribute("news_list", 
                    hasItem(hasProperty("title", startsWith("Test News")))))
                .andDo(print());
    }

    @Test
    void testIndex_VenueListContent() throws Exception {
        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("venue_list", hasSize(5)))  // 验证只返回5个场馆
                .andExpect(model().attribute("venue_list", 
                    hasItem(hasProperty("venueName", startsWith("Test Venue")))))
                .andDo(print());
    }

    @Test
    void testIndex_MessageListContent() throws Exception {
        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("message_list", hasSize(5)))  // 验证只返回5条留言
                .andExpect(model().attribute("message_list", 
                    hasItem(allOf(
                        hasProperty("content", startsWith("Test Message")),
                        hasProperty("userID", startsWith("testUser"))
                    ))))
                .andDo(print());
    }

    @Test
    void testIndex_EmptyData() throws Exception {
        // 清空所有测试数据
        for (News news : testNewsList) {
            try {
                newsService.delById(news.getNewsID());
            } catch (Exception e) {
                // 忽略删除失败的异常
            }
        }
        for (Venue venue : testVenueList) {
            try {
                venueService.delById(venue.getVenueID());
            } catch (Exception e) {
                // 忽略删除失败的异常
            }
        }
        for (Message message : testMessageList) {
            try {
                messageService.delById(message.getMessageID());
            } catch (Exception e) {
                // 忽略删除失败的异常
            }
        }

        // 测试空数据情况
        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("news_list", empty()))
                .andExpect(model().attribute("venue_list", empty()))
                .andExpect(model().attribute("message_list", empty()))
                .andDo(print());
    }

    @Test
    void testAdminIndex() throws Exception {
        mockMvc.perform(get("/admin_index"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin_index"))
                .andDo(print());
    }
}