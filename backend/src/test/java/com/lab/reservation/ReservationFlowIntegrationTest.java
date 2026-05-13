package com.lab.reservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
@AutoConfigureMockMvc
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
class ReservationFlowIntegrationTest {

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

    private final List<Reservation> userReservations = new ArrayList<>();

    @Autowired
    ReservationFlowIntegrationTest(MockMvc mockMvc,
                                   ObjectMapper objectMapper,
                                   PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void setUp() {
        userReservations.clear();

        User normalUser = new User();
        normalUser.setId(1001L);
        normalUser.setUsername("flow_user");
        normalUser.setRealName("流程测试用户");
        normalUser.setRole(0);
        normalUser.setStatus(1);
        normalUser.setPassword(passwordEncoder.encode("Passw0rd!"));

        when(userMapper.findByUsername("flow_user")).thenReturn(normalUser);
        when(blacklistMapper.findActiveByUserId(1001L)).thenReturn(null);
        when(reservationMapper.countConflict(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), isNull()))
                .thenReturn(0);
        when(reservationMapper.findByUserId(1001L)).thenAnswer(invocation -> new ArrayList<>(userReservations));
        when(reservationMapper.insert(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            if (reservation.getId() == null) {
                reservation.setId((long) (userReservations.size() + 1));
            }
            userReservations.add(reservation);
            return 1;
        });
    }

    @Test
    void shouldCompleteReservationFlowWithJwt() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "flow_user",
                                  "password": "Passw0rd!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginJson.path("data").path("token").asText();
        assertTrue(token != null && !token.isBlank());

        LocalDateTime start = LocalDateTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        String createBody = """
                {
                  "labId": 1,
                  "title": "集成测试预约",
                  "startTime": "%s",
                  "endTime": "%s",
                  "attendees": 3,
                  "remark": "E2E flow test"
                }
                """.formatted(start, end);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("集成测试预约"));

        MvcResult myReservationsResult = mockMvc.perform(get("/api/reservations/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("集成测试预约"))
                .andReturn();

        JsonNode listJson = objectMapper.readTree(myReservationsResult.getResponse().getContentAsString());
        assertEquals(1, listJson.path("data").size());
    }

    @Test
    void shouldRejectReservationWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/reservations/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }
}
