package com.lab.reservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.entity.Notice;
import com.lab.reservation.entity.Reservation;
import com.lab.reservation.entity.User;
import com.lab.reservation.mapper.BlacklistMapper;
import com.lab.reservation.mapper.LabMapper;
import com.lab.reservation.mapper.NoticeMapper;
import com.lab.reservation.mapper.ReservationMapper;
import com.lab.reservation.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端流程（MockMvc + Mock 持久层）：注册 → 登录 → 提交预约 → 管理员审核通过 → 用户拉取站内信列表并校验审核通知。
 * 不连真实数据库，验证各 Controller / Service 协同与 JWT 角色切换。
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
@AutoConfigureMockMvc
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
class ReservationEndToEndFlowIntegrationTest {

    private static final String USERNAME = "e2e_flow_user";
    private static final String ADMIN_NAME = "e2e_flow_admin";
    private static final String PASSWORD = "Passw0rd!";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    @MockBean
    private UserMapper userMapper;
    @MockBean
    private ReservationMapper reservationMapper;
    @MockBean
    private NoticeMapper noticeMapper;
    @MockBean
    private BlacklistMapper blacklistMapper;
    @MockBean
    private LabMapper labMapper;

    private final List<Reservation> reservations = new ArrayList<>();
    private final List<Notice> notices = new ArrayList<>();
    private final AtomicReference<User> registeredUserHolder = new AtomicReference<>();

    @Autowired
    ReservationEndToEndFlowIntegrationTest(MockMvc mockMvc,
                                           ObjectMapper objectMapper,
                                           PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void setUp() {
        reservations.clear();
        notices.clear();
        registeredUserHolder.set(null);

        when(userMapper.findByUsername(USERNAME)).thenAnswer(inv -> registeredUserHolder.get());
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2002L);
            registeredUserHolder.set(u);
            return 1;
        });

        User admin = new User();
        admin.setId(99L);
        admin.setUsername(ADMIN_NAME);
        admin.setRealName("流程管理员");
        admin.setRole(1);
        admin.setStatus(1);
        admin.setPassword(passwordEncoder.encode(PASSWORD));
        when(userMapper.findByUsername(ADMIN_NAME)).thenReturn(admin);

        when(blacklistMapper.findActiveByUserId(anyLong())).thenReturn(null);
        when(reservationMapper.countConflict(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), isNull()))
                .thenReturn(0);
        when(reservationMapper.countConflict(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), anyLong()))
                .thenReturn(0);
        when(labMapper.findByIds(any())).thenReturn(List.of());

        when(reservationMapper.insert(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            if (r.getId() == null) {
                r.setId((long) (reservations.size() + 1));
            }
            reservations.add(r);
            return 1;
        });
        when(reservationMapper.findByUserId(2002L)).thenAnswer(inv -> new ArrayList<>(reservations));
        when(reservationMapper.findPendingList()).thenAnswer(inv ->
                reservations.stream().filter(x -> x.getStatus() != null && x.getStatus() == 0).toList());
        when(reservationMapper.findById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return reservations.stream().filter(r -> id.equals(r.getId())).findFirst().orElse(null);
        });
        when(reservationMapper.updateStatus(anyLong(), any(), anyString())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            Integer st = invocation.getArgument(1);
            reservations.stream().filter(r -> id.equals(r.getId())).findFirst().ifPresent(r -> r.setStatus(st));
            return 1;
        });
        when(reservationMapper.findNeedReminderReservations()).thenReturn(List.of());

        when(noticeMapper.insert(any(Notice.class))).thenAnswer(invocation -> {
            Notice n = invocation.getArgument(0);
            n.setId((long) (notices.size() + 1));
            if (n.getIsRead() == null) {
                n.setIsRead(0);
            }
            notices.add(n);
            return 1;
        });
        when(noticeMapper.findByUserId(eq(2002L))).thenAnswer(inv ->
                notices.stream().filter(n -> n.getUserId() != null && n.getUserId().equals(2002L)).toList());
        when(noticeMapper.countUnread(2002L)).thenAnswer(inv ->
                (int) notices.stream().filter(n -> n.getUserId() != null && n.getUserId().equals(2002L)
                        && n.getIsRead() != null && n.getIsRead() == 0).count());
    }

    private String login(String username, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode root = objectMapper.readTree(r.getResponse().getContentAsString());
        String token = root.path("data").path("token").asText();
        assertTrue(token != null && !token.isBlank());
        return token;
    }

    @Test
    void shouldFlowRegisterReserveAuditAndReceiveNotice() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "e2e_flow_user",
                                  "realName": "流程用户",
                                  "password": "Passw0rd!",
                                  "email": "e2e_flow@test.com",
                                  "phone": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String userToken = login(USERNAME, PASSWORD);
        assertEquals(2002L, registeredUserHolder.get().getId());

        LocalDateTime start = LocalDateTime.now().plusHours(3).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "labId": 1,
                                  "title": "端到端预约",
                                  "startTime": "%s",
                                  "endTime": "%s",
                                  "attendees": 2,
                                  "remark": "flow"
                                }
                                """.formatted(start, end)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("端到端预约"));

        assertEquals(1, reservations.size());
        Long rid = reservations.get(0).getId();

        String adminToken = login(ADMIN_NAME, PASSWORD);
        mockMvc.perform(put("/api/reservations/" + rid + "/audit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":1,\"rejectReason\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertEquals(1, notices.size());
        assertEquals(1, notices.get(0).getType());
        assertTrue(notices.get(0).getTitle().contains("通过"));

        mockMvc.perform(get("/api/notices").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value(1))
                .andExpect(jsonPath("$.data[0].title").value("预约审核通过"));
    }
}
