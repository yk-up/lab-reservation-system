package com.lab.reservation.controller;

import com.lab.reservation.entity.Blacklist;
import com.lab.reservation.mapper.BlacklistMapper;
import com.lab.reservation.mapper.LabMapper;
import com.lab.reservation.mapper.ReservationMapper;
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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理后台模块", description = "迭代 1 - 管理员数据看板与黑名单管理接口")
public class AdminController {

    private final BlacklistMapper blacklistMapper;
    private final ReservationMapper reservationMapper;
    private final LabMapper labMapper;

    /** 数据看板 */
    @Operation(summary = "获取管理看板数据", description = "返回实验室总数、开放数、待审核预约数、黑名单人数等汇总信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              \"code\": 200,
                              \"message\": \"操作成功\",
                              \"data\": {
                                \"totalLabs\": 4,
                                \"openLabs\": 3,
                                \"pendingCount\": 1,
                                \"blacklistCount\": 1
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        Map<String, Object> data = new HashMap<>();
        data.put("totalLabs", labMapper.findAll(null).size());
        data.put("openLabs", labMapper.findAll(1).size());
        data.put("pendingCount", reservationMapper.findPendingList().size());
        data.put("blacklistCount", blacklistMapper.findAll().size());
        return Result.success(data);
    }

    /** 黑名单列表 */
    @Operation(summary = "获取黑名单列表", description = "返回当前系统内所有黑名单记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/blacklist")
    public Result<?> blacklist() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        return Result.success(blacklistMapper.findAll());
    }

    /** 加入黑名单 */
    @Operation(summary = "添加黑名单", description = "管理员将指定用户加入黑名单，operatorId 由后端自动填充")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "添加成功", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              \"code\": 200,
                              \"message\": \"操作成功\",
                              \"data\": \"已加入黑名单\"
                            }
                            """))),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping("/blacklist")
    public Result<?> addBlacklist(@RequestBody Blacklist blacklist) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        blacklist.setOperatorId(UserContext.getUserId());
        blacklistMapper.insert(blacklist);
        return Result.success("已加入黑名单");
    }

    /** 移出黑名单 */
    @Operation(summary = "移出黑名单", description = "管理员根据黑名单记录 ID 删除黑名单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "移除成功", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              \"code\": 200,
                              \"message\": \"操作成功\",
                              \"data\": \"已移出黑名单\"
                            }
                            """))),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @DeleteMapping("/blacklist/{id}")
    public Result<?> removeBlacklist(@Parameter(description = "黑名单记录 ID", example = "1") @PathVariable Long id) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        blacklistMapper.deleteById(id);
        return Result.success("已移出黑名单");
    }
}
