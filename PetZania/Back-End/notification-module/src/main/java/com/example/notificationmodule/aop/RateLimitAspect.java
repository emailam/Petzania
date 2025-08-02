package com.example.notificationmodule.aop;

import com.example.notificationmodule.annotation.RateLimit;
import com.example.notificationmodule.exception.ratelimit.RateLimitExceeded;
import com.example.notificationmodule.service.impl.RateLimitService;
import com.example.notificationmodule.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    private final RateLimitService rateLimitService;
    private final JwtUtil jwtUtil;
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // Get current HTTP request
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        // Extract user email from JWT token
        String userEmail = jwtUtil.getUserIdentifierFromRequest(request);
        String methodName = joinPoint.getSignature().toShortString();

        // Check rate limit
        boolean allowed = rateLimitService.isAllowed(userEmail, methodName, rateLimit.requests(), rateLimit.duration());

        if (!allowed) {
            throw new RateLimitExceeded("Rate limit exceeded: " + rateLimit.requests() + " requests per " + rateLimit.duration() + " seconds");
        }

        return joinPoint.proceed();
    }
}