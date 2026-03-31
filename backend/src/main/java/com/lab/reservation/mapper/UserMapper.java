package com.lab.reservation.mapper;

import com.lab.reservation.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    User findById(@Param("id") Long id);
    int insert(User user);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
