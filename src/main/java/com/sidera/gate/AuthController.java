package com.sidera.gate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private static final String HARDCODED_USERNAME = "lorenzo";
	private static final String HARDCODED_PASSWORD = "lorenzo20202!";

	private final JwtService jwtService;

	public AuthController(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody AuthRequest request) {
		log.info("Tentativo di login per utente '{}'.", request.username());

		if (!HARDCODED_USERNAME.equals(request.username()) || !HARDCODED_PASSWORD.equals(request.password())) {
			log.warn("Login fallito per utente '{}'.", request.username());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali non valide.");
		}

		String token = jwtService.generateToken(request.username());
		log.info("Login riuscito per utente '{}'.", request.username());

		return ResponseEntity.ok(new AuthResponse(token, jwtService.getExpirationInSeconds()));
	}
}
