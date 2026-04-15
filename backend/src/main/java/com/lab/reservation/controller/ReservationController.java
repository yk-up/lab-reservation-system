package com.lab.reservation.controller;

import com.lab.reservation.dto.AuditReservationRequest;
import com.lab.reservation.dto.ReservationRequest;
import com.lab.reservation.service.ReservationService;
import com.lab.reservation.util.UserContext;
import com.lab.reservation.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "预约模块", description = "迭代 1 - 预约申请、查询、取消与审核接口")
public class ReservationController {

    private final ReservationService reservationService;

    /** 提交预约申请 */
    @Operation(summary = "提交预约申请", description = "普通用户提交实验室预约申请，创建后状态默认为待审核")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "提交成功", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              \"code\": 200,
                              \"message\": \"操作成功\",
                              \"data\": {
                                \"id\": 1,
                                \"userId\": 3,
                                \"labId\": 1,
                                \"title\": \"Java课程项目讨论\",
                                \"startTime\": \"2026-04-20T09:00:00\",
                                \"endTime\": \"2026-04-20T11:00:00\",
                                \"attendees\": 4,
                                \"remark\": \"需要投影设备\",
                                \"status\": 0
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "参数错误或预约冲突")
    })
    @PostMapping
    public Result<?> create(@Valid @RequestBody ReservationRequest req) {
        Long userId = UserContext.getUserId();
        return Result.success(reservationService.create(userId, req));
    }

    /** 查看"我的预约" */
    @Operation(summary = "查看我的预约", description = "返回当前登录用户的预约记录列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/my")
    public Result<?> myReservations() {
        return Result.success(reservationService.listMyReservations(UserContext.getUserId()));
    }

    /** 取消预约 */
    @Operation(summary = "取消预约", description = "当前登录用户取消自己提交的预约记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "400", description = "预约不存在或无权操作")
    })
    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@Parameter(description = "预约 ID", example = "1") @PathVariable Long id) {
        reservationService.cancel(id, UserContext.getUserId());
        return Result.success("预约已取消");
    }

    /** 管理员：查看所有待审核预约 */
    @Operation(summary = "查看待审核预约", description = "管理员获取当前所有状态为待审核的预约列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/pending")
    public Result<?> pending() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        return Result.success(reservationService.listPending());
    }

    /** 管理员：审核预约 */
    @Operation(summary = "审核预约", description = "管理员审核预约申请，status=1 表示通过，status=2 表示拒绝")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "审核成功", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              \"code\": 200,
                              \"message\": \"操作成功\",
                              \"data\": \"审核操作成功\"
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "审核状态错误或预约状态不允许重复审核"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PutMapping("/{id}/audit")
    public Result<?> audit(
            @Parameter(description = "预约 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody AuditReservationRequest req) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        Integer status = req.getStatus();
        String rejectReason = req.getRejectReason();
        if (status != 1 && status != 2) {
            return Result.fail("审核状态参数错误，1=通过，2=拒绝");
        }
        reservationService.audit(id, status, rejectReason);
        return Result.success("审核操作成功");
    }
}
