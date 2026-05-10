package OpenMRSenger.rest_service.application.adapters;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyLinkAdapterIntegrationTest {

    @Test
    void testRealConnectionToDockerContainer() {
        RestTemplate restTemplate = new RestTemplate();

        // 2. Configureer de verbinding naar je lokale Docker container
        String dockerApiUrl = "http://localhost:1337/legacylink/SendSms";

        // De standaard inloggegevens uit de documentatie
        String username = "legacylink-user";
        String password = "legacylink-password";

        String studentGroup = "3";

        // Het testbericht
        String testPhoneNumber = "+31612345678";
        String testMessage = "Testbericht vanuit Java Integratietest!";

        // 3. Bouw de adapter met de echte configuratie
        LegacyLinkAdapter adapter = new LegacyLinkAdapter(
                restTemplate,
                dockerApiUrl,
                username,
                password,
                studentGroup,
                testPhoneNumber,
                testMessage
        );

        System.out.println("Versturen van request naar: " + dockerApiUrl);

        // 4. Vuur het request af!
        // TIP: Kijk nu in je Docker console om te zien of het request binnenkomt.
        boolean isSuccess = adapter.send();

        // 5. Verifieer dat de Docker container een 200 OK (success) heeft teruggegeven
        assertTrue(isSuccess, "De test faalde: De Docker container stuurde geen succes-status terug. " +
                "Kijk in de console of er een 401 Unauthorized of 400 Bad Request error was.");
    }
}