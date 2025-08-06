package com.example.adoption_and_breeding_module.aop;

import com.example.adoption_and_breeding_module.annotation.RateLimit;
import com.example.adoption_and_breeding_module.exception.ratelimit.RateLimitExceeded;
import com.example.adoption_and_breeding_module.service.impl.RateLimitService;
import com.example.adoption_and_breeding_module.util.JwtUtil;
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