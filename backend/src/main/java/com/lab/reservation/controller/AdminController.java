package com.lab.reservation.controller;

import com.lab.reservation.entity.Blacklist;
import com.lab.reservation.entity.Notice;
import com.lab.reservation.mapper.BlacklistMapper;
import com.lab.reservation.mapper.NoticeMapper;
import com.lab.reservation.mapper.LabMapper;
import com.lab.reservation.mapper.ReservationMapper;
import com.lab.reservation.mapper.UserMapper;
import com.lab.reservation.service.LabService;
import com.lab.reservation.util.UserContext;
import com.lab.reservation.vo.Result;
import com.lab.reservation.vo.UserSearchItem;
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
public class AdminController {

    private static final int NOTICE_TYPE_SYSTEM = 4;

    private final BlacklistMapper blacklistMapper;
    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final LabMapper labMapper;
    private final LabService labService;
    private final NoticeMapper noticeMapper;

    /** 数据看板 */
    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        Map<String, Object> data = new HashMap<>();
        data.put("totalLabs", labMapper.countAll(null));
        data.put("openLabs", labMapper.countAll(1));
        data.put("pendingCount", reservationMapper.countPending());
        data.put("blacklistCount", blacklistMapper.countAll());
        return Result.success(data);
    }

    /** 工作台首页公告条（系统通知 type=4，多用户群发按标题+正文去重） */
    @GetMapping("/dashboard-announcements")
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
    public Result<?> labUsage() {
        if (!UserContext.isAdmin()) return Result.forbidden();
        return Result.success(labService.usageStatsSummary());
    }

    /** 预约趋势统计 */
    @GetMapping("/reservation-trend")
    public Result<?> reservationTrend(@RequestParam(defaultValue = "7") Integer days) {
        if (!UserContext.isAdmin()) return Result.forbidden();

        int safeDays = Math.min(Math.max(days, 7), 30);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1L).atStartOfDay();

        List<Map<String, Object>> rows = reservationMapper.findDailyTrend(startTime, endTime);
        Map<String, Map<String, Object>> trendMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            trendMap.put(String.valueOf(row.get("stat_date")), row);
        }

        List<Map<String, Object>> series = new java.util.ArrayList<>();
        long totalCount = 0L;
        long approvedCount = 0L;
        long rejectedCount = 0L;
        Map<String, Object> peakDay = null;

        for (int i = 0; i < safeDays; i++) {
            LocalDate current = startDate.plusDays(i);
            Map<String, Object> raw = trendMap.get(current.toString());
            long dayTotal = raw == null ? 0L : ((Number) raw.getOrDefault("total_count", 0L)).longValue();
            long dayApproved = raw == null ? 0L : ((Number) raw.getOrDefault("approved_count", 0L)).longValue();
            long dayRejected = raw == null ? 0L : ((Number) raw.getOrDefault("rejected_count", 0L)).longValue();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", current.toString());
            item.put("label", String.format("%02d-%02d", current.getMonthValue(), current.getDayOfMonth()));
            item.put("totalCount", dayTotal);
            item.put("approvedCount", dayApproved);
            item.put("rejectedCount", dayRejected);
            series.add(item);

            totalCount += dayTotal;
            approvedCount += dayApproved;
            rejectedCount += dayRejected;

            if (peakDay == null || dayTotal > ((Number) peakDay.get("totalCount")).longValue()) {
                peakDay = item;
            }
        }

        double passRate = totalCount == 0 ? 0D : (approvedCount * 100.0 / totalCount);
        double avgDaily = safeDays == 0 ? 0D : (totalCount * 1.0 / safeDays);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("days", safeDays);
        summary.put("totalCount", totalCount);
        summary.put("approvedCount", approvedCount);
        summary.put("rejectedCount", rejectedCount);
        summary.put("passRate", Math.round(passRate * 10) / 10.0);
        summary.put("avgDaily", Math.round(avgDaily * 10) / 10.0);
        summary.put("peakDate", peakDay == null ? null : peakDay.get("date"));
        summary.put("peakLabel", peakDay == null ? null : peakDay.get("label"));
        summary.put("peakCount", peakDay == null ? 0L : peakDay.get("totalCount"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("series", series);
        result.put("summary", summary);
        return Result.success(result);
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
