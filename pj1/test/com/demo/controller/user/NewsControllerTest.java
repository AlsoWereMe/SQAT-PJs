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
import org.springframework.web.util.NestedServletException;

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

    // 边界测试 - 验证新闻排序
    @Test
    void testNewsList_SortOrder() throws Exception {
        mockMvc.perform(get("/news/getNewsList")
                .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[*].time", 
                    containsInRelativeOrder(LocalDateTime.now().toString())));
    }

    // 边界测试 - 零条新闻的情况
    @Test
    void testNewsList_NoNews() throws Exception {
        // 清空所有新闻
        for (News news : testNewsList) {
            newsService.delById(news.getNewsID());
        }

        mockMvc.perform(get("/news_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("news_list"))
                .andExpect(model().attribute("news_list", empty()))
                .andExpect(model().attribute("total", is(0)));
    }

    // 边界测试 - 分页参数验证
    @Test
    void testGetNewsList_InvalidPageParameters() throws Exception {
        // 测试负数页码
        assertThrows(NestedServletException.class, () -> {
            mockMvc.perform(get("/news/getNewsList")
                    .param("page", "-1"))
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception.getCause() instanceof IllegalArgumentException);
                        assertEquals("Page index must not be less than zero!", 
                            exception.getCause().getMessage());
                    });
        });

        // 测试零页码
        assertThrows(NestedServletException.class, () -> {
            mockMvc.perform(get("/news/getNewsList")
                    .param("page", "0"))
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception.getCause() instanceof IllegalArgumentException);
                        assertEquals("Page index must not be less than zero!", 
                            exception.getCause().getMessage());
                    });
        });

        // 测试非数字页码
        assertThrows(NestedServletException.class, () -> {
            mockMvc.perform(get("/news/getNewsList")
                    .param("page", "abc"))
                    .andExpect(result -> {
                        Exception exception = result.getResolvedException();
                        assertNotNull(exception);
                        assertTrue(exception.getCause() instanceof NumberFormatException);
                    });
        });
    }

    // 性能测试 - 大量数据场景
    @Test
    void testNewsList_Performance() throws Exception {
        // 创建大量测试数据
        List<News> largeNewsList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            News news = new News();
            news.setTitle("Performance Test News " + i);
            news.setContent("Performance Test Content " + i);
            news.setTime(LocalDateTime.now().minusHours(i));
            largeNewsList.add(news);
            newsService.create(news);
        }

        long startTime = System.currentTimeMillis();
        
        // 测试多页请求的响应时间
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(get("/news/getNewsList")
                    .param("page", String.valueOf(i)))
                    .andExpect(status().isOk());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 2000, 
                "Performance test failed. Expected duration < 2000ms but was " + duration + "ms");
    }

    // 并发测试 - 同时访问新闻详情
    @Test
    void testGetNews_ConcurrentAccess() throws Exception {
        News testNews = testNewsList.get(0);
        
        // 创建多个并发请求
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            threads.add(new Thread(() -> {
                try {
                    mockMvc.perform(get("/news")
                            .param("newsID", String.valueOf(testNews.getNewsID())))
                            .andExpect(status().isOk())
                            .andExpect(model().attributeExists("news"));
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

    // 安全测试 - XSS防御
    @Test
    void testGetNews_XSSProtection() throws Exception {
        // 创建包含潜在XSS内容的新闻
        News xssNews = new News();
        xssNews.setTitle("<script>alert('xss')</script>");
        xssNews.setContent("<img src='x' onerror='alert(1)'>");
        xssNews.setTime(LocalDateTime.now());
        int newsId = newsService.create(xssNews);

        mockMvc.perform(get("/news")
                .param("newsID", String.valueOf(newsId)))
                .andExpect(status().isOk())
                .andExpect(view().name("news"))
                .andExpect(model().attribute("news", hasProperty("title", 
                    not(containsString("<script>")))));
    }

    // 缓存测试 - 验证新闻更新后的缓存失效
    @Test
    void testNewsList_CacheInvalidation() throws Exception {
        // 首次请求
        mockMvc.perform(get("/news_list"))
                .andExpect(status().isOk());

        // 添加新新闻
        News newNews = new News();
        newNews.setTitle("Cache Test News");
        newNews.setContent("Cache Test Content");
        newNews.setTime(LocalDateTime.now());
        newsService.create(newNews);

        // 再次请求，验证是否包含新添加的新闻
        mockMvc.perform(get("/news_list"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("news_list", 
                    hasItem(hasProperty("title", is("Cache Test News")))));
    }

    // 数据完整性测试
    @Test
    void testGetNews_DataIntegrity() throws Exception {
        News testNews = testNewsList.get(0);
        String originalTitle = testNews.getTitle();
        String originalContent = testNews.getContent();
        LocalDateTime originalTime = testNews.getTime();

        // 验证返回的新闻数据是否完整且准确
        mockMvc.perform(get("/news")
                .param("newsID", String.valueOf(testNews.getNewsID())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("news", allOf(
                    hasProperty("title", is(originalTitle)),
                    hasProperty("content", is(originalContent)),
                    hasProperty("time", is(originalTime))
                )));
    }
}