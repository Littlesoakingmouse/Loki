package com.nihongu.lokibe.Util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
@Slf4j
@Component
public class JwtUtils {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration_time}")
    private long jwtExpirationMs;

    // 1. Chuyển đổi chuỗi Secret thành SecretKey (Cú pháp 0.12.x)
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // 2. Tạo Token (Bỏ chữ 'set', truyền thẳng key vào signWith)
    public String generateToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // 3. Trích xuất User ID (Dùng verifyWith, parseSignedClaims và getPayload)
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload(); // 0.12.x đổi getBody() thành getPayload()
        return Long.parseLong(claims.getSubject());
    }

    // 4. Kiểm tra Token hợp lệ
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (SignatureException ex) {
            log.error("Chữ ký JWT không hợp lệ");
        } catch (MalformedJwtException ex) {
            log.error("Định dạng JWT không hợp lệ");
        } catch (ExpiredJwtException ex) {
            log.error("JWT đã hết hạn");
        } catch (UnsupportedJwtException ex) {
            log.error("JWT không được hỗ trợ");
        } catch (IllegalArgumentException ex) {
            log.error("Chuỗi JWT bị trống");
        }
        return false;
    }
}
