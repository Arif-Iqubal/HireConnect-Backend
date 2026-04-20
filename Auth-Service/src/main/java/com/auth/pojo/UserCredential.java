package com.auth.pojo;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredential {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;

	@Column(unique = true, nullable = false)
	private String email;

	private String passwordHash;

	private String role; // CANDIDATE / RECRUITER / ADMIN

	private String provider; // LOCAL / GITHUB

	private LocalDateTime createdAt;
}