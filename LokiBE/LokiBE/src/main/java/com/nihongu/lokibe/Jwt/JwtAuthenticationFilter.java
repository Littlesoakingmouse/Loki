package com.nihongu.lokibe.Jwt;

import com.nihongu.lokibe.Util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Lấy token từ request
            String jwt = getJwtFromRequest(request);

            // 2. Nếu có token và token hợp lệ
            if (StringUtils.hasText(jwt) && jwtUtils.validateToken(jwt)) {

                // 3. Lấy User ID từ token
                Long userId = jwtUtils.getUserIdFromToken(jwt);

                // 4. Tạo đối tượng Authentication.
                // Chú ý: Gắn userId vào Principal để SecurityUtils.getCurrentUserId() có thể lấy được
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, // Principal
                        null,   // Credentials (bỏ qua vì dùng token)
                        List.of() // Authorities (Roles - app này đơn giản chưa cần Role)
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 5. Lưu thông tin đăng nhập vào Security Context
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Không thể thiết lập xác thực người dùng trong Security Context", ex);
        }

        // 6. Cho phép request đi tiếp tới Controller
        filterChain.doFilter(request, response);
    }

    // Hàm phụ trợ trích xuất token từ Header "Authorization: Bearer <token>"
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}