package kr.hhplus.be.server.domain.service;

public interface OrderHistoryService {
    void record(Long id, String stock_deducted);
}
