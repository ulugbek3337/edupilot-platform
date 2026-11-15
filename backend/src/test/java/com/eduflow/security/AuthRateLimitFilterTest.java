package com.eduflow.security;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthRateLimitFilterTest {
 @SuppressWarnings("unchecked")
 private ValueOperations<String,String> operations(){return mock(ValueOperations.class);}
 private AuthRateLimitFilter filter(StringRedisTemplate redis){
  AuthRateLimitFilter f=new AuthRateLimitFilter(redis);
  ReflectionTestUtils.setField(f,"loginLimit",10);
  ReflectionTestUtils.setField(f,"registerLimit",5);
  ReflectionTestUtils.setField(f,"refreshLimit",30);
  ReflectionTestUtils.setField(f,"windowSeconds",60L);
  return f;
 }
 @Test void blocksLoginWhenRedisWindowLimitIsExceeded() throws Exception {
  StringRedisTemplate redis=mock(StringRedisTemplate.class);ValueOperations<String,String> ops=operations();
  when(redis.opsForValue()).thenReturn(ops);when(ops.increment(anyString())).thenReturn(11L);
  MockHttpServletRequest request=new MockHttpServletRequest("POST","/api/v1/auth/login");
  request.setServletPath("/api/v1/auth/login");request.setRemoteAddr("192.0.2.10");
  MockHttpServletResponse response=new MockHttpServletResponse();MockFilterChain chain=new MockFilterChain();
  filter(redis).doFilter(request,response,chain);
  assertEquals(429,response.getStatus());assertEquals("60",response.getHeader("Retry-After"));assertNull(chain.getRequest());
 }
 @Test void failsClosedWhenRedisIsUnavailable() throws Exception {
  StringRedisTemplate redis=mock(StringRedisTemplate.class);ValueOperations<String,String> ops=operations();
  when(redis.opsForValue()).thenReturn(ops);when(ops.increment(anyString())).thenThrow(new RedisConnectionFailureException("redis unavailable"));
  MockHttpServletRequest request=new MockHttpServletRequest("POST","/api/v1/auth/login");
  request.setServletPath("/api/v1/auth/login");request.setRemoteAddr("192.0.2.11");
  MockHttpServletResponse response=new MockHttpServletResponse();MockFilterChain chain=new MockFilterChain();
  filter(redis).doFilter(request,response,chain);
  assertEquals(503,response.getStatus());assertNull(chain.getRequest());
 }
}
