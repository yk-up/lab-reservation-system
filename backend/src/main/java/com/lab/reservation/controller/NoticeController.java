package com.lab.reservation.controller;

import com.lab.reservation.mapper.NoticeMapper;
import com.lab.reservation.util.UserContext;
import com.lab.reservation.vo.PageResult;
import com.lab.reservation.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeMapper noticeMapper;

    @GetMapping
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            return Result.fail(40002, "分页参数无效，page>=1 且 pageSize 范围为 1-100");
        }
        Long userId = UserContext.getUserId();
        int offset = (page - 1) * pageSize;
        var rows = noticeMapper.findByUserIdPaged(userId, offset, pageSize);
        long total = noticeMapper.countByUserId(userId);
        return Result.success(PageResult.of(page, pageSize, total, rows));
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
        noticeMapper.markRead(id, UserContext.getUserId());
        return Result.success();
    }
}
