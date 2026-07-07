package com.sidera.gate;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GateAutomationService {

	private static final Logger log = LoggerFactory.getLogger(GateAutomationService.class);

	public synchronized void openCommand(String targetTitle) {
		log.info("Avvio automazione Playwright per target '{}'.", targetTitle);

		try (Playwright playwright = Playwright.create()) {
			log.info("Istanza Playwright creata.");

			Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
				.setHeadless(true));
			log.info("Browser Chromium avviato in modalita headless.");

			try (browser) {
				BrowserContext context = browser.newContext();
				log.info("Browser context creato.");

				try (context) {
					Page page = context.newPage();
					log.info("Nuova pagina creata.");

					log.info("Navigazione verso la pagina di login.");
					page.navigate("http://192.168.1.2/login.php");

					log.info("Click sul campo utente.");
					page.locator("input[name=\"User\"]").click();

					log.info("Inserimento username.");
					page.locator("input[name=\"User\"]").fill("admin");

					log.info("Passaggio al campo password.");
					page.locator("input[name=\"User\"]").press("Tab");

					log.info("Inserimento password.");
					page.locator("input[name=\"Password\"]").fill("password");

					log.info("Click sul pulsante Login.");
					page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

					log.info("Selezione della voce 'Casa Andreis'.");
					page.getByText("Casa Andreis").click();

					log.info("Click sul comando finale '{}'.", targetTitle);
					page.getByTitle(targetTitle).click();

					log.info("Automazione completata con successo per target '{}'.", targetTitle);
				}
			}
		} catch (Exception exception) {
			log.error("Automazione Playwright fallita per target '{}': {}", targetTitle, exception.getMessage(), exception);
			throw exception;
		} finally {
			log.info("Chiusura automazione Playwright per target '{}'.", targetTitle);
		}
	}
}
