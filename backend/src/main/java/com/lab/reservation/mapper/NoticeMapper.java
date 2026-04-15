package com.lab.reservation.mapper;

import com.lab.reservation.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface NoticeMapper {
    int insert(Notice notice);
    List<Notice> findByUserId(@Param("userId") Long userId);
    int countUnread(@Param("userId") Long userId);
    int markAllRead(@Param("userId") Long userId);
    int markRead(@Param("id") Long id);
}
