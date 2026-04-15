package com.lab.reservation.controller;

import com.lab.reservation.entity.Lab;
import com.lab.reservation.service.LabService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/labs")
@RequiredArgsConstructor
@Tag(name = "实验室模块", description = "迭代 1 - 实验室查询与管理接口")
public class LabController {

    private final LabService labService;

    /** 获取所有开放实验室（不需要登录） */
    @Operation(summary = "获取开放实验室列表", description = "返回当前状态为开放的实验室列表")
    @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(value = """
                    {
                      \"code\": 200,
                      \"message\": \"操作成功\",
                      \"data\": [
                        {
                          \"id\": 1,
                          \"name\": \"计算机实验室A\",
                          \"location\": \"教学楼3楼301\",
                          \"capacity\": 40,
                          \"description\": \"配备高性能工作站，适合编程实践\",
                          \"status\": 1,
                          \"adminId\": 1
                        }
                      ]
                    }
                    """)))
    @GetMapping
    public Result<?> list() {
        return Result.success(labService.listOpenLabs());
    }

    /** 获取指定实验室某天的空闲时段（日历视图） */
    @Operation(summary = "查询实验室空闲时段", description = "按 30 分钟粒度返回指定实验室某一天的可用/占用时段")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}/slots")
    public Result<?> availableSlots(
            @Parameter(description = "实验室 ID", example = "1") @PathVariable Long id,
            @Parameter(description = "查询日期，格式 yyyy-MM-dd", example = "2026-04-16")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(labService.getAvailableSlots(id, date));
    }

    /** 管理员：获取所有实验室（含停用） */
    @Operation(summary = "管理员获取全部实验室", description = "返回全部实验室，包括停用状态的实验室")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/all")
    public Result<?> listAll() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        return Result.success(labService.listAllLabs());
    }

    /** 管理员：新增实验室 */
    @Operation(summary = "新增实验室", description = "管理员新增实验室，创建后默认状态为开放")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "新增成功", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              \"code\": 200,
                              \"message\": \"操作成功\",
                              \"data\": \"实验室添加成功\"
                            }
                            """))),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping
    public Result<?> add(@RequestBody Lab lab) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        labService.addLab(lab);
        return Result.success("实验室添加成功");
    }

    /** 管理员：编辑实验室信息 */
    @Operation(summary = "编辑实验室", description = "管理员更新实验室名称、位置、容量、描述等信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PutMapping("/{id}")
    public Result<?> update(@Parameter(description = "实验室 ID", example = "1") @PathVariable Long id, @RequestBody Lab lab) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        lab.setId(id);
        labService.updateLab(lab);
        return Result.success("更新成功");
    }

    /** 管理员：启用/停用实验室 */
    @Operation(summary = "启用或停用实验室", description = "通过 status 参数控制实验室是否开放，1 表示开放，0 表示停用")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PutMapping("/{id}/status")
    public Result<?> toggleStatus(
            @Parameter(description = "实验室 ID", example = "1") @PathVariable Long id,
            @Parameter(description = "实验室状态，1=开放，0=停用", example = "1") @RequestParam Integer status) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        labService.toggleStatus(id, status);
        return Result.success("操作成功");
    }
}
