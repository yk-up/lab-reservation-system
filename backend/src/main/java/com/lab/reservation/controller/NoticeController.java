package com.lab.reservation.controller;

import com.lab.reservation.mapper.NoticeMapper;
import com.lab.reservation.util.UserContext;
import com.lab.reservation.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@Tag(name = "通知模块", description = "迭代 1 - 站内信通知查询与已读管理接口")
public class NoticeController {

    private final NoticeMapper noticeMapper;

    @Operation(summary = "获取通知列表", description = "返回当前登录用户最近的通知列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              \"code\": 200,
                              \"message\": \"操作成功\",
                              \"data\": [
                                {
                                  \"id\": 1,
                                  \"userId\": 3,
                                  \"type\": 1,
                                  \"title\": \"预约审核通过\",
                                  \"content\": \"您的预约已通过审核，请按时使用实验室。\",
                                  \"reservationId\": 1,
                                  \"isRead\": 0
                                }
                              ]
                            }
                            """)))
    })
    @GetMapping
    public Result<?> list() {
        return Result.success(noticeMapper.findByUserId(UserContext.getUserId()));
    }

    @Operation(summary = "获取未读通知数", description = "返回当前登录用户未读通知总数")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              \"code\": 200,
                              \"message\": \"操作成功\",
                              \"data\": 3
                            }
                            """)))
    })
    @GetMapping("/unread-count")
    public Result<?> unreadCount() {
        return Result.success(noticeMapper.countUnread(UserContext.getUserId()));
    }

    @Operation(summary = "全部标记已读", description = "将当前登录用户的所有通知批量标记为已读")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              \"code\": 200,
                              \"message\": \"操作成功\",
                              \"data\": \"已全部标为已读\"
                            }
                            """)))
    })
    @PutMapping("/read-all")
    public Result<?> readAll() {
        noticeMapper.markAllRead(UserContext.getUserId());
        return Result.success("已全部标为已读");
    }

    @Operation(summary = "单条通知标记已读", description = "根据通知 ID 将单条通知标记为已读")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              \"code\": 200,
                              \"message\": \"操作成功\",
                              \"data\": null
                            }
                            """)))
    })
    @PutMapping("/{id}/read")
    public Result<?> read(@Parameter(description = "通知 ID", example = "1") @PathVariable Long id) {
        noticeMapper.markRead(id);
        return Result.success();
    }
}
