# Session 06 - Bài 3: Khắc Phục Cascading Failure Do Thiếu Timeout & Đề Xuất Circuit Breaker

## 🔗 1. Phân Tích Chuỗi Lỗi Dây Chuyền (Cascading Failure Chain)

### Diễn Biến Sự Cố Trên Hệ Thống VietMart:
```
[Dịch vụ Kho (Inventory Service) quá tải / Treo DB]
                    │
                    ▼ (Response trễ > 60 giây)
[Khách hàng tạo đơn] ──► [Order Service (Gọi RestTemplate)] ──► (Thread bị ngâm giữ vô hạn)
                    │
                    ▼ (Tất cả Tomcat Request Threads bị chiếm dụng)
[Order Service Cạn Kiệt Thread Pool (Thread Exhaustion)]
                    │
                    ▼ (Trả về 504/500 cho mọi API khác)
[Sập Toàn Bộ Hệ Thống Order & API Gateway]
```

1. **Nguyên nhân cốt lõi**: `StockCheckClient` sử dụng `RestTemplate` thiếu cấu hình `connectTimeout` và `readTimeout`.
2. **Cơ chế gây ra lỗi**: Khi `inventory-service` bị quá tải hoặc phản hồi chậm (vd 60s), mỗi request tới `order-service` sẽ giữ nguyên 1 Worker Thread trong Thread Pool của Tomcat. Với lượng request tăng nhanh, 200 Tomcat Threads mặc định bị cạn kiệt hoàn toàn trong thời gian ngắn. Kết quả: `order-service` bị ngâm treo và ngưng đáp ứng tất cả các request khác (kể cả những API không liên quan tới kho).

---

## 🛠️ 2. Biện Pháp Sửa Lỗi Ngắn Hạn (Timeouts + Fallback)

1. **Cấu hình Timeout trên `RestTemplateConfig`**:
   - `connectTimeout = 1 giây`
   - `readTimeout = 2 giây`
2. **Cơ chế Fallback Nhanh**:
   - Bắt ngoại lệ `ResourceAccessException` và lập tức trả về `StockInfo.unavailable(productId)` với trạng thái `INVENTORY_SERVICE_TIMEOUT_FALLBACK` chỉ trong chưa đầy 2 giây.

---

## 🛡️ 3. Đề Xuất Giải Pháp Dài Hạn Với Resilience4j Circuit Breaker

Để ngăn chặn triệt để Cascading Failure và bảo vệ tài nguyên hệ thống, giải pháp khuyến nghị là tích hợp **Resilience4j Circuit Breaker**:

```yaml
resilience4j.circuitbreaker:
  instances:
    inventoryService:
      slidingWindowSize: 10
      minimumNumberOfCalls: 5
      failureRateThreshold: 50
      waitDurationInOpenState: 10000ms
      permittedNumberOfCallsInHalfOpenState: 3
```

### Cơ chế hoạt động của Circuit Breaker:
1. **Trạng thái Closed (Bình thường)**: Các request được chuyển thẳng tới `inventory-service`.
2. **Trạng thái Open (Ngắt kết nối)**: Khi tỷ lệ lỗi/timeout vượt quá 50%, Circuit Breaker lập tức chuyển sang trạng thái OPEN. Tất cả request tiếp theo tới `inventory-service` sẽ bị ngắt ngay lập tức ở tầng Client và trả về **Fallback** (không cần gửi request qua mạng), giúp `inventory-service` có thời gian tự phục hồi.
3. **Trạng thái Half-Open (Thử nghiệm)**: Sau 10 giây (`waitDurationInOpenState`), Circuit Breaker cho phép 3 request dùng thử. Nếu thành công, chuyển về CLOSED; nếu tiếp tục lỗi, chuyển lại OPEN.

---

## 🚀 Kiểm Thử
```bash
./gradlew test
```
