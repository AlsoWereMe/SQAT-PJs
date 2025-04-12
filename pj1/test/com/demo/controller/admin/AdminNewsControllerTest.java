package com.demo.pj1.demo.controller.admin;

import com.demo.entity.News;
import com.demo.service.NewsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
public class AdminNewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NewsService newsService;

    private List<News> testNews;

    @BeforeEach
    void setUp() {
        testNews = new ArrayList<>();
        // 创建测试新闻数据
        for (int i = 1; i <= 15; i++) {
            News news = new News();
            news.setTitle("Test News " + i);
            news.setContent("Test Content " + i);
            news.setTime(LocalDateTime.now());
            testNews.add(news);
            newsService.create(news);
        }
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        for (News news : testNews) {
            try {
                newsService.delById(news.getNewsID());
            } catch (Exception e) {
                // 忽略删除失败的异常
            }
        }
    }

    @Test
    void testNewsManage() throws Exception {
        mockMvc.perform(get("/news_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/news_manage"))
                .andExpect(model().attributeExists("total"))
                .andDo(print());
    }

    @Test
    void testNewsList_NormalCase() throws Exception {
        mockMvc.perform(get("/newsList.do")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].title", startsWith("Test News")))
                .andExpect(jsonPath("$[0].content", startsWith("Test Content")));
    }

    @Test
    void testNewsList_EmptyPage() throws Exception {
        mockMvc.perform(get("/newsList.do")
                .param("page", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testNewsAdd() throws Exception {
        mockMvc.perform(get("/news_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_add"));
    }

    @Test
    void testNewsEdit() throws Exception {
        // 创建一条测试新闻
        News news = new News();
        news.setTitle("Test Edit News");
        news.setContent("Test Edit Content");
        news.setTime(LocalDateTime.now());
        int newsId = newsService.create(news);

        mockMvc.perform(get("/news_edit")
                .param("newsID", String.valueOf(newsId)))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_edit"))
                .andExpect(model().attributeExists("news"));
    }

    @Test
    void testAddNews_Success() throws Exception {
        mockMvc.perform(post("/addNews.do")
                .param("title", "New Test News")
                .param("content", "New Test Content")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("news_manage"));

        // 验证新闻是否成功添加
        List<News> allNews = newsService.findAll(org.springframework.data.domain.PageRequest.of(0, 1)).getContent();
        assertTrue(allNews.stream().anyMatch(n -> n.getTitle().equals("New Test News")));
    }

    @Test
    void testModifyNews_Success() throws Exception {
        // 创建测试新闻
        News news = new News();
        news.setTitle("Original Title");
        news.setContent("Original Content");
        news.setTime(LocalDateTime.now());
        int newsId = newsService.create(news);

        // 修改新闻
        mockMvc.perform(post("/modifyNews.do")
                .param("newsID", String.valueOf(newsId))
                .param("title", "Modified Title")
                .param("content", "Modified Content")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("news_manage"));

        // 验证修改是否成功
        News modifiedNews = newsService.findById(newsId);
        assertEquals("Modified Title", modifiedNews.getTitle());
        assertEquals("Modified Content", modifiedNews.getContent());
    }

    @Test
    void testDelNews_Success() throws Exception {
        // 创建测试新闻
        News news = new News();
        news.setTitle("Test Delete News");
        news.setContent("Test Delete Content");
        news.setTime(LocalDateTime.now());
        int newsId = newsService.create(news);

        // 删除新闻
        mockMvc.perform(post("/delNews.do")
                .param("newsID", String.valueOf(newsId))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 验证新闻是否已被删除
        assertThrows(Exception.class, () -> newsService.findById(newsId));
    }

    @Test
    void testDelNews_NonexistentNews() throws Exception {
        try {
            mockMvc.perform(post("/delNews.do")
                    .param("newsID", "999")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isInternalServerError())
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception, "Expected exception to be thrown");
                        assertTrue(exception instanceof RuntimeException);
                        assertEquals("No class com.demo.entity.News entity with id 999 exists!",
                                exception.getMessage());
                    })
                    .andDo(print());
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("No class com.demo.entity.News entity with id 999 exists!",
                    e.getCause().getMessage());
        }
    }

    @Test
    void testNewsList_Performance() throws Exception {
        // 准备大量测试数据
        for (int i = 0; i < 100; i++) {
            News news = new News();
            news.setTitle("Performance Test News " + i);
            news.setContent("Performance Test Content " + i);
            news.setTime(LocalDateTime.now());
            newsService.create(news);
        }

        // 执行性能测试
        long startTime = System.currentTimeMillis();
        
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(get("/newsList.do")
                    .param("page", String.valueOf(i)))
                    .andExpect(status().isOk());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 2000, 
                "Performance test failed. Expected duration < 2000ms but was " + duration + "ms");
    }
}