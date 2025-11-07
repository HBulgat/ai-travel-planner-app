package top.bulgat.ai.travel.plan.aspect;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.bulgat.ai.travel.plan.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.annotation.Resource;

@Aspect
@Component
public class AuthorizationAspect {

    @Resource
    private JwtUtil jwtUtil;

    @Before("@annotation(top.bulgat.ai.travel.plan.annotation.AuthRequired)")
    public void checkAuthorization() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new RuntimeException("Cannot get request attributes");
        }
        HttpServletRequest request = attributes.getRequest();
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        if (!jwtUtil.validateToken(token, username)) {
            throw new RuntimeException("Invalid or expired JWT token");
        }
        System.out.println("Performing authorization check...");
    }
}