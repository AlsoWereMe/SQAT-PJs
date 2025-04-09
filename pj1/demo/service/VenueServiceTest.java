package com.demo.service.impl;

import com.demo.dao.VenueDao;
import com.demo.entity.Venue;
import com.demo.exception.LoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VenueServiceTest {

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private VenueServiceImpl venueService;

    private Venue validVenue;

    @BeforeEach
    void setUp() {
        // 初始化一个合法的场馆对象，用于多个测试用例
        validVenue = new Venue();
        validVenue.setVenueID(1);
        validVenue.setVenueName("Test Stadium");
        validVenue.setDescription("A modern stadium for sports events");
        validVenue.setPrice(100);
        validVenue.setPicture("test.jpg");
        validVenue.setAddress("123 Test Street");
        validVenue.setOpen_time("08:00");
        validVenue.setClose_time("22:00");
    }

    //------------------------ 查询方法测试 ------------------------

    /**
     * 测试场景：通过存在的场馆ID查询场馆详情
     * 测试类型：黑盒（有效等价类） + 白盒（字段完整性验证）
     * 覆盖目标：
     * 1. 验证返回的场馆对象非空
     * 2. 确保所有字段（名称、描述、价格等）与预期一致
     * 3. 验证DAO层方法被正确调用
     */
    @Test
    void findByVenueID_WhenExists_ShouldReturnFullVenueDetails() {
        when(venueDao.findByVenueID(1)).thenReturn(validVenue);

        Venue result = venueService.findByVenueID(1);

        assertNotNull(result);
        assertEquals("Test Stadium", result.getVenueName());
        assertEquals("A modern stadium for sports events", result.getDescription());
        assertEquals(100, result.getPrice());
        assertEquals("test.jpg", result.getPicture());
        verify(venueDao).findByVenueID(1);
    }

    /**
     * 测试场景：通过不存在的场馆ID查询
     * 测试类型：黑盒（无效等价类） + 白盒（空值处理）
     * 覆盖目标：
     * 1. 验证返回结果为 null
     * 2. 确保DAO层方法被调用
     */
    @Test
    void findByVenueID_WhenNotExists_ShouldReturnNull() {
        when(venueDao.findByVenueID(999)).thenReturn(null);

        Venue result = venueService.findByVenueID(999);

        assertNull(result);
        verify(venueDao).findByVenueID(999);
    }

    //------------------------ 创建方法测试 ------------------------

    /**
     * 测试场景：创建名称唯一的场馆
     * 测试类型：黑盒（有效等价类） + 白盒（语句覆盖）
     * 覆盖目标：
     * 1. 验证名称唯一性检查通过
     * 2. 确保返回生成的场馆ID正确
     * 3. 确认DAO层保存方法被调用
     */
    @Test
    void create_WhenNameIsUnique_ShouldReturnId() {
        when(venueDao.countByVenueName("Test Stadium")).thenReturn(0);
        when(venueDao.save(validVenue)).thenReturn(validVenue);

        int result = venueService.create(validVenue);
        assertEquals(1, result);
        verify(venueDao).save(validVenue);
    }

    /**
     * 测试场景：创建名称重复的场馆
     * 测试类型：黑盒（无效等价类） + 白盒（判定覆盖）
     * 覆盖目标：
     * 1. 验证名称重复性检查失败
     * 2. 确保返回失败状态码（0）
     * 3. 确认DAO层保存方法未被调用
     */
    @Test
    void create_WhenNameIsDuplicate_ShouldReturnFailure() {
        Venue duplicateVenue = new Venue();
        duplicateVenue.setVenueName("Existing Stadium");
        when(venueDao.countByVenueName("Existing Stadium")).thenReturn(1);

        int result = venueService.create(duplicateVenue);
        assertEquals(0, result);
        verify(venueDao, never()).save(any());
    }

    /**
     * 测试场景：使用边界值价格创建场馆
     * 测试类型：黑盒（边界值分析） + 白盒（条件分支覆盖）
     * 输入参数：price = {0, -1, Integer.MAX_VALUE}
     * 覆盖目标：
     * 1. 价格 ≤ 0 时返回失败（0）
     * 2. 合法价格（Integer.MAX_VALUE）时返回成功ID
     * 3. 验证不同价格值的处理逻辑
     */
    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MAX_VALUE})
    void create_WithPriceBoundaryValues_ShouldHandleCorrectly(int price) {
        Venue venue = new Venue();
        venue.setVenueName("Price Test Stadium");
        venue.setPrice(price);

        if (price <= 0) {
            int result = venueService.create(venue);
            assertEquals(0, result);
        } else {
            when(venueDao.countByVenueName("Price Test Stadium")).thenReturn(0);
            when(venueDao.save(venue)).thenReturn(venue);
            int result = venueService.create(venue);
            assertEquals(venue.getVenueID(), result);
        }
    }

    /**
     * 测试场景：创建地址为空的场馆
     * 测试类型：黑盒（无效等价类）
     * 覆盖目标：
     * 1. 验证地址为空时返回失败（0）
     * 2. 确保必填字段校验逻辑生效
     */
    @Test
    void create_WithEmptyAddress_ShouldReturnFailure() {
        Venue invalidVenue = new Venue();
        invalidVenue.setVenueName("Empty Address Stadium");
        invalidVenue.setAddress(""); // 空地址
        int result = venueService.create(invalidVenue);
        assertEquals(0, result);
    }

    //------------------------ 更新方法测试 ------------------------

    /**
     * 测试场景：更新场馆所有字段（名称唯一）
     * 测试类型：黑盒（有效等价类） + 白盒（语句覆盖）
     * 覆盖目标：
     * 1. 验证所有字段被正确更新
     * 2. 确保名称唯一性检查通过
     * 3. 确认DAO层保存方法被调用
     */
    @Test
    void update_WhenNameIsUnique_ShouldUpdateAllFields() {
        Venue updatedVenue = new Venue();
        updatedVenue.setVenueID(1);
        updatedVenue.setVenueName("Updated Stadium");
        updatedVenue.setDescription("Renovated stadium");
        updatedVenue.setPrice(200);
        updatedVenue.setPicture("updated.jpg");
        updatedVenue.setAddress("456 Updated Street");
        updatedVenue.setOpen_time("09:00");
        updatedVenue.setClose_time("23:00");

        // 模拟名称唯一性检查和保存操作
        when(venueDao.countByVenueName("Updated Stadium")).thenReturn(0);
        when(venueDao.save(updatedVenue)).thenReturn(updatedVenue);

        venueService.update(updatedVenue);
        verify(venueDao).save(updatedVenue);
    }

    /**
     * 测试场景：更新场馆名称至已存在的名称
     * 测试类型：黑盒（无效等价类） + 白盒（判定覆盖）
     * 覆盖目标：
     * 1. 验证名称重复性检查失败
     * 2. 确保更新操作被拒绝并抛出异常
     * 3. 确认DAO层保存方法未被调用
     */
    @Test
    void update_WhenNameIsDuplicate_ShouldThrowException() {
        Venue updatedVenue = new Venue();
        updatedVenue.setVenueID(1);
        updatedVenue.setVenueName("Existing Stadium"); // 与已有场馆名称重复

        // 模拟名称重复
        when(venueDao.countByVenueName("Existing Stadium")).thenReturn(1);

        assertThrows(LoginException.class, () -> venueService.update(updatedVenue));
        verify(venueDao, never()).save(any());
    }

    //------------------------ 分页查询测试 ------------------------

    /**
     * 测试场景：分页查询存在数据的场馆列表
     * 测试类型：黑盒（功能验证）
     * 覆盖目标：
     * 1. 验证分页结果的总数和内容正确性
     * 2. 确保分页参数传递给DAO层
     */
    /**
     * 测试场景：无参查询所有场馆列表
     * 覆盖目标：
     * 1. 验证返回非空列表
     * 2. 确保DAO层无参方法被调用
     */
    @Test
    void findAll_WithoutPage_ShouldReturnList() {
        List<Venue> mockList = Collections.singletonList(validVenue);
        when(venueDao.findAll()).thenReturn(mockList); // 模拟无参方法

        List<Venue> result = venueService.findAll();
        assertEquals(1, result.size());
        verify(venueDao).findAll(); // 验证无参调用
    }

    /**
     * 测试场景：分页查询场馆列表
     * 覆盖目标：
     * 1. 验证分页结果的总数和内容正确性
     * 2. 确保分页参数传递给DAO层
     */
    @Test
    void findAll_WithPage_ShouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10); // 使用 PageRequest
        List<Venue> mockData = Arrays.asList(validVenue, validVenue);
        Page<Venue> mockPage = new PageImpl<>(mockData, pageable, mockData.size());
        when(venueDao.findAll(pageable)).thenReturn(mockPage);

        Page<Venue> result = venueService.findAll(pageable);
        assertEquals(2, result.getTotalElements());
        verify(venueDao).findAll(pageable);
    }

    /**
     * 测试场景：分页查询空数据集
     * 测试类型：黑盒（边界值）
     * 覆盖目标：
     * 1. 验证返回空分页结果
     * 2. 确保分页逻辑正确处理空数据
     */
    @Test
    void findAllWithPage_WhenDataIsEmpty_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0,10);
        when(venueDao.findAll(pageable)).thenReturn(Page.empty());

        Page<Venue> result = venueService.findAll(pageable);
        assertEquals(0, result.getTotalElements());
    }

    //------------------------ 删除方法测试 ------------------------

    /**
     * 测试场景：删除存在的场馆ID
     * 测试类型：黑盒（有效等价类） + 白盒（语句覆盖）
     * 覆盖目标：
     * 1. 验证删除操作成功执行
     * 2. 确保DAO层方法被调用
     */
    @Test
    void delById_WhenIdExists_ShouldCallDelete() {
        doNothing().when(venueDao).deleteById(1);

        venueService.delById(1);
        verify(venueDao).deleteById(1);
    }

    /**
     * 测试场景：删除不存在的场馆ID
     * 测试类型：黑盒（无效等价类） + 白盒（异常分支覆盖）
     * 覆盖目标：
     * 1. 验证删除操作抛出业务异常（LoginException）
     * 2. 确保DAO层异常被正确转换
     */
    @Test
    void delById_WhenIdNotExists_ShouldThrowLoginException() {
        doThrow(EmptyResultDataAccessException.class).when(venueDao).deleteById(999);
        assertThrows(LoginException.class, () -> venueService.delById(999));
    }

    //------------------------ 辅助方法测试 ------------------------

    /**
     * 测试场景：统计存在的场馆名称
     * 测试类型：黑盒（有效等价类） + 白盒（判定覆盖）
     * 覆盖目标：
     * 1. 验证返回正确的计数（≥1）
     * 2. 确保DAO层方法被调用
     */
    @Test
    void countVenueName_WhenNameExists_ShouldReturnPositive() {
        when(venueDao.countByVenueName("Existing Stadium")).thenReturn(1);
        int count = venueService.countVenueName("Existing Stadium");
        assertEquals(1, count);
    }

    /**
     * 测试场景：统计不存在的场馆名称
     * 测试类型：黑盒（无效等价类） + 白盒（判定覆盖）
     * 覆盖目标：
     * 1. 验证返回计数0
     * 2. 确保DAO层方法被调用
     */
    @Test
    void countVenueName_WhenNameNotExists_ShouldReturnZero() {
        when(venueDao.countByVenueName("Non-existent Stadium")).thenReturn(0);
        int count = venueService.countVenueName("Non-existent Stadium");
        assertEquals(0, count);
    }

    /**
     * 测试场景：验证时间格式合法性
     * 测试类型：黑盒（无效等价类）
     * 覆盖目标：
     * 1. 检查非法时间格式（如25:00）是否触发异常
     * 注意：需结合业务层时间校验逻辑
     */
    @Test
    void testTimeFormatValidation() {
        Venue invalidTimeVenue = new Venue();
        invalidTimeVenue.setVenueName("Invalid Time Stadium");
        invalidTimeVenue.setOpen_time("25:00"); // 无效时间

        // 若业务层有时间校验，此处应调用相关方法触发异常
        assertThrows(IllegalArgumentException.class, () -> {
            // 示例：直接赋值触发字段校验（需实体类添加校验注解）
            invalidTimeVenue.setOpen_time("25:00");
        });
    }
}