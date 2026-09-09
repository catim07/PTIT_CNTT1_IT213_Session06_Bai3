package com.rikkei.b3.controller;

import com.rikkei.b3.client.StockCheckClient;
import com.rikkei.b3.dto.ApiResponse;
import com.rikkei.b3.dto.StockInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders/stock")
public class StockController {

    private final StockCheckClient stockCheckClient;

    public StockController(StockCheckClient stockCheckClient) {
        this.stockCheckClient = stockCheckClient;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<StockInfo>> checkStock(@PathVariable Long productId) {
        StockInfo stockInfo = stockCheckClient.checkStock(productId);
        return ResponseEntity.ok(ApiResponse.success("Stock status retrieved", stockInfo));
    }
}
