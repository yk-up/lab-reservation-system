package com.lab.reservation.mapper;

import com.lab.reservation.entity.User;
import com.lab.reservation.vo.UserSearchItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    User findById(@Param("id") Long id);

    /** 按用户名/姓名模糊检索，条数上限由调用方限制 */
    List<UserSearchItem> searchUsersByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
    int insert(User user);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
