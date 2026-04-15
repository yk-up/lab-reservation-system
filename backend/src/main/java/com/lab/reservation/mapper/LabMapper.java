package com.lab.reservation.mapper;

import com.lab.reservation.entity.Lab;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface LabMapper {
    List<Lab> findAll(@Param("status") Integer status);
    Lab findById(@Param("id") Long id);
    int insert(Lab lab);
    int update(Lab lab);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
