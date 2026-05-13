package com.lab.reservation.controller;

import com.lab.reservation.entity.Blacklist;
import com.lab.reservation.entity.Notice;
import com.lab.reservation.entity.Reservation;
import com.lab.reservation.mapper.BlacklistMapper;
import com.lab.reservation.mapper.NoticeMapper;
import com.lab.reservation.mapper.LabMapper;
import com.lab.reservation.mapper.ReservationMapper;
import com.lab.reservation.mapper.UserMapper;
import com.lab.reservation.service.LabService;
import com.lab.reservation.service.ReservationService;
import com.lab.reservation.util.ReservationTrendCalculator;
import com.lab.reservation.util.UserContext;
import com.lab.reservation.vo.Result;
import com.lab.reservation.vo.UserSearchItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理端", description = "数据看板、公告、统计与审批相关接口（需管理员 JWT）")
public class AdminController {

    private static final int NOTICE_TYPE_SYSTEM = 4;

    private final BlacklistMapper blacklistMapper;
    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final LabMapper labMapper;
    private final LabService labService;
    private final NoticeMapper noticeMapper;
    private final ReservationService reservationService;

    private Map<String, Object> dashboardSummary() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalLabs", labMapper.countAll(null));
        data.put("openLabs", labMapper.countAll(1));
        data.put("pendingCount", reservationMapper.countPending());
        data.put("blacklistCount", blacklistMapper.countAll());
        return data;
    }

    /** 数据看板 */
    @GetMapping("/dashboard")
    @Operation(summary = "数据看板摘要", description = "实验室数量、待审核数、黑名单数等")
    public Result<?> dashboard() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        return Result.success(dashboardSummary());
    }

    /** 工作台首页公告条（系统通知 type=4，多用户群发按标题+正文去重） */
    @GetMapping("/dashboard-announcements")
    @Operation(summary = "工作台公告条", description = "数据源：notice 表 type=4；按标题+正文去重后取最新若干条")
    public Result<List<Notice>> dashboardAnnouncements(
            @RequestParam(defaultValue = "5") Integer limit) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        int fetchRows = Math.min(400, Math.max(safeLimit * 40, 80));
        List<Notice> deduped = dedupeAnnouncementRows(
                noticeMapper.findByTypeOrderByCreateTimeDesc(NOTICE_TYPE_SYSTEM, fetchRows));
        if (deduped.size() > safeLimit) {
            deduped = deduped.subList(0, safeLimit);
        }
        return Result.success(deduped);
    }

    /** 公告中心列表（与 dashboard 数据源一致，条数可调） */
    @GetMapping("/announcements")
    @Operation(summary = "公告列表", description = "与 dashboard-announcements 相同数据源与去重规则")
    public Result<List<Notice>> announcements(
            @RequestParam(defaultValue = "50") Integer limit) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        int fetchRows = Math.min(800, Math.max(safeLimit * 40, 80));
        List<Notice> deduped = dedupeAnnouncementRows(
                noticeMapper.findByTypeOrderByCreateTimeDesc(NOTICE_TYPE_SYSTEM, fetchRows));
        if (deduped.size() > safeLimit) {
            deduped = deduped.subList(0, safeLimit);
        }
        return Result.success(deduped);
    }

    /** 公告详情（仅系统公告 type=4，供公告中心独立页使用） */
    @GetMapping("/announcements/{id}")
    @Operation(summary = "公告详情", description = "仅允许 type=4 系统公告；非系统类型返回 404")
    public Result<Notice> announcementDetail(@PathVariable Long id) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        if (id == null || id <= 0) {
            return Result.fail(400, "无效的公告 ID");
        }
        Notice n = noticeMapper.findById(id);
        if (n == null || n.getType() == null || n.getType() != NOTICE_TYPE_SYSTEM) {
            return Result.fail(404, "公告不存在或不是系统公告");
        }
        return Result.success(n);
    }

    /** 群发公告库内去重（保留最新一条）。 */
    private static List<Notice> dedupeAnnouncementRows(List<Notice> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<String, Notice> byContent = new LinkedHashMap<>();
        for (Notice n : rows) {
            String title = n.getTitle() == null ? "" : n.getTitle();
            String content = n.getContent() == null ? "" : n.getContent();
            byContent.putIfAbsent(title + "\0" + content, n);
        }
        List<Notice> sorted = new ArrayList<>(byContent.values());
        sorted.sort(Comparator.comparing(Notice::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return sorted;
    }

    /** 实验室使用率统计 */
    @GetMapping("/lab-usage")
    @Operation(summary = "实验室使用率（管理端）", description = "与 GET /api/labs/usage 统计口径一致，需管理员")
    public Result<?> labUsage() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        return Result.success(labService.usageStatsSummary());
    }

    /** 预约趋势统计 */
    @GetMapping("/reservation-trend")
    @Operation(summary = "预约趋势", description = "按申请创建日聚合近 7–30 天通过/拒绝/总量，与数据大屏趋势图一致。")
    public Result<?> reservationTrend(@RequestParam(defaultValue = "7") Integer days) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        return Result.success(buildReservationTrend(days));
    }

    /**
     * 数据大屏统计聚合：复用 dashboard、lab-usage、reservation-trend 及公告条数据；
     * 后续若指标增多，可在此扩展字段或新增专用聚合接口。
     */
    @GetMapping("/stats/screen")
    @Operation(summary = "数据大屏聚合", description = "一次返回看板摘要、使用率、趋势与公告预览（notice.type=4）")
    public Result<Map<String, Object>> screenStats(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(defaultValue = "5") Integer announcementLimit) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        int safeTrendDays = Math.min(Math.max(days, 7), 30);
        int annLimit = Math.min(Math.max(announcementLimit, 1), 20);
        int fetchRows = Math.min(400, Math.max(annLimit * 40, 80));
        List<Notice> annTop = dedupeAnnouncementRows(
                noticeMapper.findByTypeOrderByCreateTimeDesc(NOTICE_TYPE_SYSTEM, fetchRows));
        if (annTop.size() > annLimit) {
            annTop = annTop.subList(0, annLimit);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dashboard", dashboardSummary());
        payload.put("usage", labService.usageStatsSummary());
        payload.put("trend", buildReservationTrend(safeTrendDays));
        payload.put("announcementsTop", annTop);
        payload.put("_meta", Map.of(
                "description", "聚合字段分别对应 GET /admin/dashboard、/admin/lab-usage、/admin/reservation-trend 与公告条逻辑",
                "announcementDataSource",
                "notice 表 type=4 系统公告；与 GET /admin/dashboard-announcements、GET /admin/announcements 使用相同去重键（标题+正文），保留最新一条"
        ));
        return Result.success(payload);
    }

    /**
     * 审批中心聚合：看板摘要 + 待审核预览列表 + 关联接口说明。
     */
    @GetMapping("/stats/approval-center")
    @Operation(summary = "审批中心聚合", description = "摘要指标与待审核预览；完整列表请用 /reservations/admin-list 或 /pending")
    public Result<Map<String, Object>> approvalCenterStats(
            @RequestParam(defaultValue = "30") Integer pendingPreviewLimit) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        int limit = Math.min(Math.max(pendingPreviewLimit, 1), 100);
        List<Reservation> pending = reservationService.listPending();
        int total = pending.size();
        List<Reservation> preview = total > limit ? pending.subList(0, limit) : pending;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("summary", dashboardSummary());
        body.put("pendingPreview", preview);
        body.put("pendingTotal", total);
        body.put("_meta", Map.of(
                "adminListEndpoint", "GET /api/reservations/admin-list",
                "pendingEndpoint", "GET /api/reservations/pending",
                "singleAuditEndpoint", "PUT /api/reservations/{id}/audit",
                "batchAuditEndpoint", "PUT /api/reservations/audit/batch",
                "announcementListEndpoint", "GET /api/admin/announcements",
                "announcementDetailEndpoint", "GET /api/admin/announcements/{id}",
                "announcementSource", "notice 表 type=4 系统公告；列表/详情与公告中心页面一致"
        ));
        return Result.success(body);
    }

    private Map<String, Object> buildReservationTrend(Integer days) {
        int safeDays = Math.min(Math.max(days, 7), 30);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1L).atStartOfDay();
        List<Map<String, Object>> rows = reservationMapper.findDailyTrend(startTime, endTime);
        return ReservationTrendCalculator.build(startDate, safeDays, rows);
    }

    /** 按用户名/姓名/精确 ID 检索用户（加入黑名单等场景） */
    @GetMapping("/users/search")
    public Result<List<UserSearchItem>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer limit) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        String k = keyword == null ? "" : keyword.trim();
        if (k.isEmpty()) {
            return Result.success(List.of());
        }
        int safeLimit = limit == null ? 20 : Math.min(Math.max(limit, 1), 50);
        return Result.success(userMapper.searchUsersByKeyword(k, safeLimit));
    }

    /** 黑名单列表 */
    @GetMapping("/blacklist")
    public Result<?> blacklist() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        return Result.success(blacklistMapper.findAll());
    }

    /** 加入黑名单 */
    @PostMapping("/blacklist")
    public Result<?> addBlacklist(@RequestBody Blacklist blacklist) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        Long userId = blacklist.getUserId();
        if (userId == null) {
            return Result.fail("请指定用户ID");
        }
        if (userMapper.findById(userId) == null) {
            return Result.fail("用户不存在");
        }
        if (blacklistMapper.findActiveByUserId(userId) != null) {
            return Result.fail("该用户已在有效黑名单中");
        }
        blacklist.setOperatorId(UserContext.getUserId());
        blacklistMapper.insert(blacklist);
        return Result.success("已加入黑名单");
    }

    /** 移出黑名单 */
    @DeleteMapping("/blacklist/{id}")
    public Result<?> removeBlacklist(@PathVariable Long id) {
        if (!UserContext.isAdmin()) return Result.forbidden();
        blacklistMapper.deleteById(id);
        return Result.success("已移出黑名单");
    }
}
