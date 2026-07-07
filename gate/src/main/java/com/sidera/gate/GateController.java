package com.sidera.gate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gate")
public class GateController {

	private static final Logger log = LoggerFactory.getLogger(GateController.class);

	private final GateAutomationService gateAutomationService;
	private final JwtService jwtService;

	public GateController(GateAutomationService gateAutomationService, JwtService jwtService) {
		this.gateAutomationService = gateAutomationService;
		this.jwtService = jwtService;
	}

	@PostMapping("/open")
	public ResponseEntity<String> openGate(
		@RequestParam("target") String target,
		@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
		log.info("Ricevuta richiesta apertura per target '{}'.", target);

		try {
			String token = extractBearerToken(authorizationHeader);
			if (token == null || !jwtService.isValid(token)) {
				log.warn("Richiesta rifiutata per target '{}': token JWT mancante o non valido.", target);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Autenticazione richiesta.");
			}

			log.info("Token JWT valido per utente '{}'.", jwtService.extractUsername(token));

			String targetTitle = switch (target) {
				case "portone" -> "Comando Portone";
				case "cancello-entrata" -> "Cancello Entrata";
				default -> null;
			};

			if (targetTitle == null) {
				log.warn("Richiesta rifiutata: target '{}' non valido.", target);
				return ResponseEntity.badRequest().body("Target non valido.");
			}

			log.info("Target '{}' risolto in titolo '{}'.", target, targetTitle);
			gateAutomationService.openCommand(targetTitle);
			log.info("Richiesta completata con successo per target '{}'.", target);
			return ResponseEntity.ok("Comando inviato con successo: " + targetTitle + ".");
		} catch (Exception exception) {
			log.error("Errore durante la richiesta per target '{}': {}", target, exception.getMessage(), exception);
			return ResponseEntity.internalServerError()
				.body("Errore durante l'esecuzione dell'automazione: " + exception.getMessage());
		}
	}

	private String extractBearerToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			return null;
		}

		return authorizationHeader.substring(7);
	}
}
