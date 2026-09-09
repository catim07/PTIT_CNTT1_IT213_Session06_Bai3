package com.rikkei.b3.client;

import com.rikkei.b3.dto.StockInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class StockCheckClient {

    private static final Logger log = LoggerFactory.getLogger(StockCheckClient.class);
    private static final String INVENTORY_SERVICE_URL = "http://inventory-service/api/stock/";

    private final RestTemplate restTemplate;

    public StockCheckClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public StockInfo checkStock(Long productId) {
        long startTime = System.currentTimeMillis();
        try {
            return restTemplate.getForObject(INVENTORY_SERVICE_URL + productId, StockInfo.class);
        } catch (ResourceAccessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Timeout checking stock for productId: {} after {} ms", productId, duration, e);
            return StockInfo.unavailable(productId);
        } catch (Exception e) {
            log.error("Error checking stock for productId: {}", productId, e);
            return StockInfo.unavailable(productId);
        }
    }
}
