package openmrsenger.restservice.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import openmrsenger.restservice.RestServiceApplication;
import openmrsenger.restservice.communications.infrastructure.providers.SecurePostAdapter;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.http.HttpClient;
import java.lang.reflect.Field;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TlsEnforcementTest {

    @Test
    void testRestTemplateEnforcesTls13() throws Exception {
        RestServiceApplication app = new RestServiceApplication();
        RestTemplate restTemplate = app.restTemplate();
        assertNotNull(restTemplate);

        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        assertNotNull(factory);

        HttpsURLConnection mockConnection = mock(HttpsURLConnection.class);
        
        java.lang.reflect.Method prepareConnectionMethod = SimpleClientHttpRequestFactory.class.getDeclaredMethod(
                "prepareConnection", HttpURLConnection.class, String.class);
        prepareConnectionMethod.setAccessible(true);
        
        prepareConnectionMethod.invoke(factory, mockConnection, "GET");

        verify(mockConnection).setSSLSocketFactory(any(SSLSocketFactory.class));
    }

    @Test
    void testTls13SocketFactoryEnforcesProtocol() throws Exception {
        SSLSocketFactory mockDelegate = mock(SSLSocketFactory.class);
        SSLSocket mockSocket = mock(SSLSocket.class);
        
        when(mockDelegate.createSocket(any(Socket.class), anyString(), anyInt(), anyBoolean())).thenReturn(mockSocket);
        when(mockDelegate.createSocket(anyString(), anyInt())).thenReturn(mockSocket);
        
        Class<?> factoryClass = Class.forName("openmrsenger.restservice.RestServiceApplication$Tls13SocketFactory");
        java.lang.reflect.Constructor<?> ctor = factoryClass.getDeclaredConstructor(SSLSocketFactory.class);
        ctor.setAccessible(true);
        SSLSocketFactory tls13Factory = (SSLSocketFactory) ctor.newInstance(mockDelegate);

        Socket socket = tls13Factory.createSocket("test-host", 443);
        assertSame(mockSocket, socket);
        verify(mockSocket).setEnabledProtocols(new String[]{"TLSv1.3"});
        
        Socket wrappedSocket = tls13Factory.createSocket(mock(Socket.class), "test-host", 443, true);
        assertSame(mockSocket, wrappedSocket);
        verify(mockSocket, times(2)).setEnabledProtocols(new String[]{"TLSv1.3"});
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
