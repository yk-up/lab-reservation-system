package com.lab.reservation.controller;

import com.lab.reservation.dto.ReservationRequest;
import com.lab.reservation.service.ReservationService;
import com.lab.reservation.util.UserContext;
import com.lab.reservation.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** 提交预约申请 */
    @PostMapping
    public Result<?> create(@Valid @RequestBody ReservationRequest req) {
        Long userId = UserContext.getUserId();
        return Result.success(reservationService.create(userId, req));
    }

    /** 查看"我的预约" */
    @GetMapping("/my")
    public Result<?> myReservations() {
        return Result.success(reservationService.listMyReservations(UserContext.getUserId()));
    }

    /** 取消预约 */
    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id) {
        reservationService.cancel(id, UserContext.getUserId());
        return Result.success("预约已取消");
    }

    /** 管理员：查看所有待审核预约 */
    @GetMapping("/pending")
    public Result<?> pending() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        return Result.success(reservationService.listPending());
    }

    /** 管理员：审核预约 */
    @PutMapping("/{id}/audit")
    public Result<?> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        Integer status = (Integer) body.get("status");
        String rejectReason = (String) body.get("rejectReason");
        if (status == null || (status != 1 && status != 2)) {
            return Result.fail("审核状态参数错误，1=通过，2=拒绝");
        }
        reservationService.audit(id, status, rejectReason);
        return Result.success("审核操作成功");
    }
}
