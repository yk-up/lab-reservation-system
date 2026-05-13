package com.lab.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.entity.Notice;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 站内信（通知）模块：MockMvc + Mock Mapper，覆盖列表、未读数、单条已读、全部已读的正常与异常路径。
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
@AutoConfigureMockMvc
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
class NoticeControllerIntegrationTest {

    private final MockMvc mockMvc;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

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

    @Autowired
    NoticeControllerIntegrationTest(MockMvc mockMvc, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    private String loginAsNoticeUser() throws Exception {
        User u = new User();
        u.setId(3001L);
        u.setUsername("notice_user");
        u.setRealName("通知测试");
        u.setRole(0);
        u.setStatus(1);
        u.setPassword(passwordEncoder.encode("Passw0rd!"));
        when(userMapper.findByUsername("notice_user")).thenReturn(u);

        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"notice_user\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    @BeforeEach
    void baseStubs() {
        when(blacklistMapper.findActiveByUserId(anyLong())).thenReturn(null);
    }

    @Test
    void shouldListNoticesWhenLoggedIn() throws Exception {
        Notice n = new Notice();
        n.setId(10L);
        n.setUserId(3001L);
        n.setType(1);
        n.setTitle("预约审核通过");
        n.setContent("内容");
        n.setIsRead(0);
        n.setCreateTime(LocalDateTime.now());
        when(noticeMapper.findByUserId(3001L)).thenReturn(List.of(n));

        String token = loginAsNoticeUser();
        mockMvc.perform(get("/api/notices").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("预约审核通过"));
    }

    @Test
    void shouldReturnUnreadCount() throws Exception {
        when(noticeMapper.countUnread(3001L)).thenReturn(4);
        String token = loginAsNoticeUser();
        mockMvc.perform(get("/api/notices/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(4));
    }

    @Test
    void shouldFailMarkReadWhenNoRowUpdated() throws Exception {
        when(noticeMapper.markRead(eq(999L), eq(3001L))).thenReturn(0);
        String token = loginAsNoticeUser();
        mockMvc.perform(put("/api/notices/999/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("通知不存在或无权限操作"));
    }

    @Test
    void shouldSucceedMarkReadWhenRowUpdated() throws Exception {
        when(noticeMapper.markRead(eq(2L), eq(3001L))).thenReturn(1);
        String token = loginAsNoticeUser();
        mockMvc.perform(put("/api/notices/2/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldMarkAllRead() throws Exception {
        when(noticeMapper.markAllRead(3001L)).thenReturn(2);
        String token = loginAsNoticeUser();
        mockMvc.perform(put("/api/notices/read-all").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").value("已全部标为已读"));
    }
}
