package com.lab.reservation.mapper;

import com.lab.reservation.entity.Lab;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface LabMapper {
    List<Lab> findAll(@Param("status") Integer status);
    int countAll(@Param("status") Integer status);
    Lab findById(@Param("id") Long id);

    List<Lab> findByIds(@Param("ids") List<Long> ids);
    List<Map<String, Object>> findUsageStats();
    int insert(Lab lab);
    int update(Lab lab);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
