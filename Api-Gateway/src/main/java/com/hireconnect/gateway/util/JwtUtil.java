package com.hireconnect.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;

import io.jsonwebtoken.security.Keys;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

	private String SECRET = "bXlzZWNyZXRrZXlteXNlY3JldGtleW15c2VjcmV0a2V5MTI=";
	private SecretKey signingKey;

	@PostConstruct
	public void init() {
		this.signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
	}

	public String generateToken(String email) {
		return Jwts.builder().setSubject(email).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 86400000))
				.signWith(signingKey, SignatureAlgorithm.HS256).compact();
	}

	public String extractEmail(String token) {
		return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody().getSubject();
	}

	public boolean validateToken(String token, String email) {
		return extractEmail(token).equals(email) && !isTokenExpired(token);
	}

	public boolean isTokenExpired(String token) {
		return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody().getExpiration()
				.before(new Date());
	}

	public boolean isValid(String token) {
		try {
			extractEmail(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	public String extractRole(String token) {
		return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody().get("role", String.class);
	           
	}
}
