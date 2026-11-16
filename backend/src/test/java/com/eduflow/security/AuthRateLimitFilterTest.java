package com.eduflow.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthRateLimitFilterTest {

    private AuthRateLimitFilter filter(StringRedisTemplate redis) {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(redis);
        ReflectionTestUtils.setField(filter, "loginLimit", 10);
        ReflectionTestUtils.setField(filter, "registerLimit", 5);
        ReflectionTestUtils.setField(filter, "refreshLimit", 30);
        ReflectionTestUtils.setField(filter, "windowSeconds", 60L);
        return filter;
    }

    private MockHttpServletRequest loginRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServletPath("/api/v1/auth/login");
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    void blocksLoginWhenRedisWindowLimitIsExceeded() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any())).thenReturn(11L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(redis).doFilter(loginRequest("192.0.2.10"), response, chain);

        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
        assertNull(chain.getRequest());
        verify(redis).execute(any(RedisScript.class), eq(java.util.List.of(
                "edupilot:rate-limit:auth:login:192.0.2.10")), eq("60"));
    }

    @Test
    void failsClosedWhenRedisIsUnavailable() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(redis).doFilter(loginRequest("192.0.2.11"), response, chain);

        assertEquals(503, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void failsClosedWhenRedisReturnsNoCounterValue() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any())).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(redis).doFilter(loginRequest("192.0.2.12"), response, chain);

        assertEquals(503, response.getStatus());
        assertTrue(response.getContentAsString().contains("rate.limit.unavailable"));
        assertNull(chain.getRequest());
    }

    @Test
    void normalizesNonPositiveWindowToOneSecond() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);
        AuthRateLimitFilter filter = filter(redis);
        ReflectionTestUtils.setField(filter, "windowSeconds", 0L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(loginRequest("192.0.2.13"), response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        verify(redis).execute(any(RedisScript.class), anyList(), eq("1"));
    }
}
