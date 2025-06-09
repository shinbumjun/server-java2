package kr.hhplus.be.server.domain.pointhistory;

import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.domain.point.PointHistory;

public class PointHistoryFactory {
    public static PointHistory createUsageHistory(Point point, int amount) {
        PointHistory history = new PointHistory();
        history.setPointId(point.getId());
        history.setAmount(amount);
        history.setBalance(point.getBalance());
        history.setType("사용");
        return history;
    }
}
