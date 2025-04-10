package com.demo.pj1.demo.controller.user;

import com.demo.entity.News;
import com.demo.service.NewsService;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
public class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NewsService newsService;

    private List<News> testNewsList;

    @BeforeEach
    void setUp() {
        testNewsList = new ArrayList<>();
        // 创建测试新闻数据
        for (int i = 1; i <= 10; i++) {
            News news = new News();
            news.setTitle("Test News " + i);
            news.setContent("Test Content " + i);
            news.setTime(LocalDateTime.now().minusDays(i));
            testNewsList.add(news);
            newsService.create(news);
        }
    }

    @Test
    void testNewsList() throws Exception {
        mockMvc.perform(get("/news_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("news_list"))
                .andExpect(model().attributeExists("news_list"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("news_list", hasSize(5)))  // 验证只返回5条新闻
                .andExpect(model().attribute("news_list", 
                    hasItem(hasProperty("title", startsWith("Test News")))))
                .andDo(print());
    }

    @Test
    void testGetNewsList_FirstPage() throws Exception {
        mockMvc.perform(get("/news/getNewsList")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[0].title", startsWith("Test News")))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andDo(print());
    }

    @Test
    void testGetNewsList_SecondPage() throws Exception {
        mockMvc.perform(get("/news/getNewsList")
                .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[0].title", startsWith("Test News")))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andDo(print());
    }

    @Test
    void testGetNewsList_EmptyPage() throws Exception {
        mockMvc.perform(get("/news/getNewsList")
                .param("page", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", empty()))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andDo(print());
    }

    @Test
    void testGetNews_Success() throws Exception {
        News testNews = testNewsList.get(0);
        
        mockMvc.perform(get("/news")
                .param("newsID", String.valueOf(testNews.getNewsID())))
                .andExpect(status().isOk())
                .andExpect(view().name("news"))
                .andExpect(model().attributeExists("news"))
                .andExpect(model().attribute("news", 
                    hasProperty("title", is(testNews.getTitle()))))
                .andDo(print());
    }

    @Test
    void testGetNews_NonexistentNews() throws Exception {
        try {
            mockMvc.perform(get("/news")
                    .param("newsID", "999"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception, "Expected exception to be thrown");
                        assertTrue(exception instanceof RuntimeException);
                        assertTrue(exception.getMessage()
                            .contains("No class com.demo.entity.News entity with id 999 exists"));
                    })
                    .andDo(print());
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertTrue(e.getCause().getMessage()
                .contains("No class com.demo.entity.News entity with id 999 exists"));
        }
    }
}