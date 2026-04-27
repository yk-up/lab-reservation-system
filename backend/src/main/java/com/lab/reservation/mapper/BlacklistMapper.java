package com.lab.reservation.mapper;

import com.lab.reservation.entity.Blacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BlacklistMapper {
    int insert(Blacklist blacklist);
    /** 查询用户是否处于有效封禁状态（未过期） */
    Blacklist findActiveByUserId(@Param("userId") Long userId);
    List<Blacklist> findAll();
    int countAll();
    int deleteById(@Param("id") Long id);
}
