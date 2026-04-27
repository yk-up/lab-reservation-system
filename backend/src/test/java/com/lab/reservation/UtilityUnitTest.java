package com.lab.reservation;

import com.lab.reservation.util.JwtUtil;
import com.lab.reservation.vo.Result;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityUnitTest {

    @Test
    void resultSuccessShouldContainExpectedFields() {
        Result<String> result = Result.success("ok");

        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertEquals("ok", result.getData());
    }

    @Test
    void resultFailShouldContainExpectedFields() {
        Result<?> result = Result.fail("出错了");

        assertEquals(400, result.getCode());
        assertEquals("出错了", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void resultUnauthorizedShouldContainExpectedFields() {
        Result<?> result = Result.unauthorized();

        assertEquals(401, result.getCode());
        assertEquals("未登录或登录已过期", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void jwtUtilShouldGenerateAndParseToken() throws Exception {
        JwtUtil jwtUtil = createJwtUtil(
                "lab-reservation-system-jwt-secret-key-2024-very-long-string",
                60_000L
        );

        String token = jwtUtil.generateToken(1L, "admin", 1);
        Claims claims = jwtUtil.parseToken(token);

        assertEquals("admin", claims.getSubject());
        assertEquals(Long.valueOf(1L), jwtUtil.getUserId(token));
        assertEquals(Integer.valueOf(1), jwtUtil.getRole(token));
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void jwtUtilShouldTreatExpiredTokenAsExpired() throws Exception {
        JwtUtil jwtUtil = createJwtUtil(
                "lab-reservation-system-jwt-secret-key-2024-very-long-string",
                -1L
        );

        String token = jwtUtil.generateToken(2L, "tester", 0);

        assertTrue(jwtUtil.isTokenExpired(token));
    }

    private JwtUtil createJwtUtil(String secret, Long expiration) throws Exception {
        JwtUtil jwtUtil = new JwtUtil();
        setField(jwtUtil, "secret", secret);
        setField(jwtUtil, "expiration", expiration);
        return jwtUtil;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
