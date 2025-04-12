package com.demo.service.impl;

import com.demo.dao.VenueDao;
import com.demo.entity.Venue;
import com.demo.exception.LoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
        validVenue = new Venue(1,"Test Stadium",
                "A modern stadium for sports events",
                100,"test.jpg","123 Test Street",
                "08:00","22:00");

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
    void testCreate_WhenNameIsUnique() {
        when(venueDao.save(validVenue)).thenReturn(validVenue);
        int result = venueService.create(validVenue);
        assertEquals(1, result);
        verify(venueDao).save(validVenue);
    }

    @Test
    void testCreate_WhenIdIsDuplicate() {
        Venue duplicateVenue = new Venue();
        duplicateVenue.setVenueID(1);
        when(venueDao.findByVenueID(1)).thenReturn(validVenue);
        when(venueDao.save(duplicateVenue)).thenReturn(duplicateVenue);
        int result = venueService.create(duplicateVenue);
        assertNotEquals(1,result);
        verify(venueDao,never()).save(duplicateVenue);

    }
    @Test
    void testCreate_WhenNameIsDuplicate() {
        Venue duplicateVenue = new Venue();
        duplicateVenue.setVenueName("Test Stadium");
        when(venueDao.countByVenueName("Test Stadium")).thenReturn(1);
        when(venueDao.save(duplicateVenue)).thenReturn(duplicateVenue);
        int result = venueService.create(duplicateVenue);
        assertNotEquals(1,result);
        verify(venueDao,never()).save(duplicateVenue);

    }
    @ParameterizedTest
    @ValueSource(ints = {-1,0, Integer.MAX_VALUE})
    void testCreate_WithPriceBoundaryValues(int price) {
        Venue venue = new Venue();
        venue.setVenueName("Price Test Stadium");
        venue.setPrice(price);
        when(venueDao.save(venue)).thenReturn(venue);
        if (price < 0) {
            venueService.create(venue);
            verify(venueDao, never()).save(any());
        } else {
            int result = venueService.create(venue);
            assertEquals(venue.getVenueID(), result);
            assertEquals(venue.getPrice(),price);
        }
    }
    @ParameterizedTest
    @CsvSource({
            "00:00, 23:59",
            "00:01, 24:00",
            "25:00, 26:00",
            "00:70, 23:80"
    })
    void testCreate_WithInvalidTimeFormat(String open_time, String close_time){
        Venue venue = new Venue();
        venue.setOpen_time(open_time);
        venue.setClose_time(close_time);
        when(venueDao.save(venue)).thenReturn(venue);
        if(open_time.equals("00:00") ){
            int result = venueService.create(venue);
            assertEquals(venue.getVenueID(), result);
            assertEquals(venue.getOpen_time(), open_time);
            assertEquals(venue.getClose_time(), close_time);
        }
        else{
            venueService.create(venue);
            verify(venueDao, never()).save(any());
        }
    }





    //------------------------ 查询方法测试 ------------------------

    /**
     * 测试场景：通过存在的场馆ID查询场馆详情
     * 测试类型：黑盒（有效等价类） + 白盒（字段完整性验证）
     * 覆盖目标：
     * 1. 验证返回的场馆对象非空
     */
    @Test
    void testFindByVenueID_WhenExists() {
        when(venueDao.getOne(1)).thenReturn(validVenue);
        Venue result = venueService.findByVenueID(1);
        assertNotNull(result);
        assertEquals(result,validVenue);
    }

    /**
     * 测试场景：通过不存在的场馆ID查询
     * 测试类型：黑盒（无效等价类） + 白盒（空值处理）
     * 覆盖目标：
     * 1. 验证返回结果为 null
     */
    @Test
    void testFindByVenueID_WhenNotExists() {
        when(venueDao.getOne(999)).thenReturn(null);
        Venue result = venueService.findByVenueID(999);
        assertNull(result);
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
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdate_WhenNameIsUnique() {
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
    @ParameterizedTest
    @ValueSource(strings = {"Itself", "Other Stadium"})
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdate_WhenNameIsDuplicatt(String venueName) {
        Venue updatedVenue = new Venue();
        updatedVenue.setVenueID(1);
        updatedVenue.setVenueName(venueName); // 与已有场馆名称重复
        when(venueDao.countByVenueName(venueName)).thenReturn(1);
        if(venueName.equals("Itself")){
            //这个重名的就是它自己
            Venue oldVenue = new Venue();
            oldVenue.setVenueID(1);
            when(venueDao.findByVenueName(venueName)).thenReturn(oldVenue);
            venueService.update(updatedVenue);
            verify(venueDao).save(updatedVenue);
        }
        else{
            //这个重名的是别的场馆
            Venue otherVenue = new Venue();
            otherVenue.setVenueID(2);
            when(venueDao.findByVenueName(venueName)).thenReturn(otherVenue);
            venueService.update(updatedVenue);
            verify(venueDao,never()).save(updatedVenue);
        }

        // 模拟名称重复



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
    void testFindAll_WithoutPage() {
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
    void testFindAll_WithPage() {
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
    void testFindAllWithPage_WhenDataIsEmpty() {
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
    void testDelById_WhenIdExists() {
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
    void testDelById_WhenIdNotExists() {
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
    void testCountVenueName_WhenNameExists() {
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
    void testCountVenueName_WhenNameNotExists() {
        when(venueDao.countByVenueName("Non-existent Stadium")).thenReturn(0);
        int count = venueService.countVenueName("Non-existent Stadium");
        assertEquals(0, count);
    }



    /**
        被删除的用例：
     * 测试场景：创建地址为空的场馆
     * 测试类型：黑盒（无效等价类）
     * 覆盖目标：
     * 1. 验证地址为空时返回失败（0）
     * 2. 确保必填字段校验逻辑生效
    @Test
    void create_WithEmptyAddress_ShouldReturnFailure() {
        Venue invalidVenue = new Venue();
        invalidVenue.setVenueName("Empty Address Stadium");
        invalidVenue.setAddress(""); // 空地址
        int result = venueService.create(invalidVenue);
        assertEquals(0, result);
    }
     删除原因：没有要求地址必须非空

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
     删除原因：该方法与测试Create和测试Update的方法冗余
     */

}