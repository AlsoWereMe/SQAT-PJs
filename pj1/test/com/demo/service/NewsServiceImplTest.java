package com.demo.service.impl;

import com.demo.dao.NewsDao;
import com.demo.entity.News;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import javax.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {

    @Mock
    private NewsDao newsDao;

    @InjectMocks
    private NewsServiceImpl newsService;

    private News validNews;
    private News invalidNews;
    private Pageable validPageable;
    private Pageable invalidPageable;

    @BeforeEach
    void setUp() {
        // 准备有效测试数据
        validNews = new News();
        validNews.setNewsID(1);
        validNews.setTitle("Valid News");
        validNews.setContent("Valid Content");

        // 准备无效测试数据
        invalidNews = new News(); // 缺少必要字段

        // 准备分页参数
        validPageable = PageRequest.of(0, 10);
        invalidPageable = null;
    }

    // =============== findAll 测试 ===============
    @Test
    void findAll_WithValidPageable_ShouldReturnPage() {
        // 准备模拟数据
        List<News> newsList = Collections.singletonList(validNews);
        Page<News> expectedPage = new PageImpl<>(newsList, validPageable, newsList.size());

        // 模拟DAO行为
        when(newsDao.findAll(validPageable)).thenReturn(expectedPage);

        // 执行测试
        Page<News> result = newsService.findAll(validPageable);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(validNews, result.getContent().get(0));

        // 验证DAO调用
        verify(newsDao, times(1)).findAll(validPageable);
    }

    @Test
    void findAll_WithNullPageable_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> newsService.findAll(invalidPageable));

        // 验证DAO未被调用
        verify(newsDao, never()).findAll((Sort) any());
    }

    // =============== findById 测试 ===============
    @Test
    void findById_WithValidId_ShouldReturnNews() {
        // 模拟DAO行为
        when(newsDao.getOne(1)).thenReturn(validNews);

        // 执行测试
        News result = newsService.findById(1);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getNewsID());
        assertEquals("Valid News", result.getTitle());

        // 验证DAO调用
        verify(newsDao, times(1)).getOne(1);
    }

    @Test
    void findById_WithZeroId_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> newsService.findById(0));

        // 验证DAO未被调用
        verify(newsDao, never()).getOne(anyInt());
    }

    @Test
    void findById_WithNegativeId_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> newsService.findById(-1));

        // 验证DAO未被调用
        verify(newsDao, never()).getOne(anyInt());
    }

    @Test
    void findById_WithNonExistentId_ShouldThrowException() {
        // 模拟DAO行为
        when(newsDao.getOne(999)).thenThrow(EntityNotFoundException.class);

        // 执行测试并验证异常
        assertThrows(EntityNotFoundException.class, () -> newsService.findById(999));

        // 验证DAO调用
        verify(newsDao, times(1)).getOne(999);
    }

    // =============== create 测试 ===============
    @Test
    void create_WithValidNews_ShouldReturnId() {
        // 模拟DAO行为
        when(newsDao.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setNewsID(1); // 模拟ID生成
            return news;
        });

        // 执行测试
        int resultId = newsService.create(validNews);

        // 验证结果
        assertEquals(1, resultId);

        // 验证DAO调用
        verify(newsDao, times(1)).save(validNews);
    }

    @Test
    void create_WithNullNews_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> newsService.create(null));

        // 验证DAO未被调用
        verify(newsDao, never()).save(any());
    }

    @Test
    void create_WithInvalidNews_ShouldThrowException() {
        // 模拟DAO行为
        when(newsDao.save(invalidNews)).thenThrow(IllegalArgumentException.class);

        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> newsService.create(invalidNews));

        // 验证DAO调用
        verify(newsDao, times(1)).save(invalidNews);
    }

    // =============== delById 测试 ===============
    @Test
    void delById_WithValidId_ShouldExecute() {
        // 模拟DAO行为
        doNothing().when(newsDao).deleteById(1);

        // 执行测试
        newsService.delById(1);

        // 验证DAO调用
        verify(newsDao, times(1)).deleteById(1);
    }

    @Test
    void delById_WithZeroId_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> newsService.delById(0));

        // 验证DAO未被调用
        verify(newsDao, never()).deleteById(anyInt());
    }

    @Test
    void delById_WithNegativeId_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> newsService.delById(-1));

        // 验证DAO未被调用
        verify(newsDao, never()).deleteById(anyInt());
    }

    @Test
    void delById_WithNonExistentId_ShouldExecute() {
        // 模拟DAO行为
        doNothing().when(newsDao).deleteById(999);

        // 执行测试
        newsService.delById(999);

        // 验证DAO调用
        verify(newsDao, times(1)).deleteById(999);
    }

    // =============== update 测试 ===============
    @Test
    void update_WithValidNews_ShouldExecute() {
        // 模拟DAO行为
        when(newsDao.save(validNews)).thenReturn(validNews);

        // 执行测试
        newsService.update(validNews);

        // 验证DAO调用
        verify(newsDao, times(1)).save(validNews);
    }

    @Test
    void update_WithNullNews_ShouldThrowException() {
        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> newsService.update(null));

        // 验证DAO未被调用
        verify(newsDao, never()).save(any());
    }

    @Test
    void update_WithInvalidNews_ShouldThrowException() {
        // 模拟DAO行为
        when(newsDao.save(invalidNews)).thenThrow(IllegalArgumentException.class);

        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> newsService.update(invalidNews));

        // 验证DAO调用
        verify(newsDao, times(1)).save(invalidNews);
    }
}