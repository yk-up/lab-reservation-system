package com.lab.reservation.controller;

import com.lab.reservation.mapper.NoticeMapper;
import com.lab.reservation.util.UserContext;
import com.lab.reservation.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeMapper noticeMapper;

    @GetMapping
    public Result<?> list() {
        return Result.success(noticeMapper.findByUserId(UserContext.getUserId()));
    }

    @GetMapping("/unread-count")
    public Result<?> unreadCount() {
        return Result.success(noticeMapper.countUnread(UserContext.getUserId()));
    }

    @PutMapping("/read-all")
    public Result<?> readAll() {
        noticeMapper.markAllRead(UserContext.getUserId());
        return Result.success("已全部标为已读");
    }

    @PutMapping("/{id}/read")
    public Result<?> read(@PathVariable Long id) {
        int updated = noticeMapper.markRead(id, UserContext.getUserId());
        if (updated == 0) {
            return Result.fail(404, "通知不存在或无权限操作");
        }
        return Result.success();
    }
}
