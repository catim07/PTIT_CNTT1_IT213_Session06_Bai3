package com.rikkei.b3.dto;

public record StockInfo(
    Long productId,
    Integer quantity,
    boolean available,
    String status
) {
    public static StockInfo unavailable(Long productId) {
        return new StockInfo(productId, 0, false, "INVENTORY_SERVICE_TIMEOUT_FALLBACK");
    }
}
