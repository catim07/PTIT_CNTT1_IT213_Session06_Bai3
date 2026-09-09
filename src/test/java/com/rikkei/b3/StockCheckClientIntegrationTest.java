package com.rikkei.b3;

import com.rikkei.b3.client.StockCheckClient;
import com.rikkei.b3.dto.StockInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StockCheckClientIntegrationTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private StockCheckClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        client = new StockCheckClient(restTemplate);
    }

    @Test
    void testCheckStockSuccess() {
        String json = """
                {
                    "productId": 200,
                    "quantity": 50,
                    "available": true,
                    "status": "IN_STOCK"
                }
                """;

        mockServer.expect(requestTo("http://inventory-service/api/stock/200"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        StockInfo stock = client.checkStock(200L);
        mockServer.verify();

        assertNotNull(stock);
        assertTrue(stock.available());
        assertEquals(50, stock.quantity());
    }

    @Test
    void testCheckStockTimeoutFastFallback() {
        long start = System.currentTimeMillis();

        mockServer.expect(requestTo("http://inventory-service/api/stock/201"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        StockInfo stock = client.checkStock(201L);
        long elapsed = System.currentTimeMillis() - start;

        mockServer.verify();

        assertNotNull(stock);
        assertFalse(stock.available());
        assertEquals("INVENTORY_SERVICE_TIMEOUT_FALLBACK", stock.status());
        assertTrue(elapsed < 3000, "Response must return fallback within 3 seconds, elapsed: " + elapsed + "ms");
    }
}
