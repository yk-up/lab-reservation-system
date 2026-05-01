package com.lab.reservation.mapper;

import com.lab.reservation.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface NoticeMapper {
    int insert(Notice notice);
    List<Notice> findByUserId(@Param("userId") Long userId);
    /** 按类型倒序取样（含多用户同内容广播，需在业务层按标题+正文去重） */
    List<Notice> findByTypeOrderByCreateTimeDesc(@Param("type") int type, @Param("limit") int limit);
    Notice findById(@Param("id") Long id);
    int countUnread(@Param("userId") Long userId);
    int markAllRead(@Param("userId") Long userId);
    int markRead(@Param("id") Long id, @Param("userId") Long userId);
}
