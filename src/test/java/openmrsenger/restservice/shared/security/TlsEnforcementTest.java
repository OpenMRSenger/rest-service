package openmrsenger.restservice.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import openmrsenger.restservice.RestServiceApplication;
import openmrsenger.restservice.communications.infrastructure.providers.SecurePostAdapter;

import java.net.http.HttpClient;
import java.lang.reflect.Field;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class TlsEnforcementTest {

    @Test
    void testRestTemplateEnforcesTls13() throws Exception {
        RestServiceApplication app = new RestServiceApplication();
        RestTemplate restTemplate = app.restTemplate();
        assertNotNull(restTemplate);

        assertTrue(restTemplate.getRequestFactory() instanceof JdkClientHttpRequestFactory);
        JdkClientHttpRequestFactory factory = (JdkClientHttpRequestFactory) restTemplate.getRequestFactory();
        
        Field clientField = JdkClientHttpRequestFactory.class.getDeclaredField("httpClient");
        clientField.setAccessible(true);
        HttpClient httpClient = (HttpClient) clientField.get(factory);
        
        assertNotNull(httpClient);
        assertNotNull(httpClient.sslParameters());
        assertArrayEquals(new String[]{"TLSv1.3"}, httpClient.sslParameters().getProtocols());
    }

    @Test
    void testSecurePostHttpClientEnforcesTls13() throws Exception {
        SecurePostAdapter adapter = new SecurePostAdapter(
                new ObjectMapper(),
                "http://localhost:8080",
                "group1"
        );

        Field clientField = SecurePostAdapter.class.getDeclaredField("httpClient");
        clientField.setAccessible(true);
        HttpClient httpClient = (HttpClient) clientField.get(adapter);

        assertNotNull(httpClient);
        assertNotNull(httpClient.sslParameters());
        assertArrayEquals(new String[]{"TLSv1.3"}, httpClient.sslParameters().getProtocols());
    }
}
