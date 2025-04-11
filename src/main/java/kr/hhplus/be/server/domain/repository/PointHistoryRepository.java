package kr.hhplus.be.server.domain.repository;

import kr.hhplus.be.server.domain.point.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    // 기본적인 CRUD 메서드는 JpaRepository에서 제공되므로 추가적인 메서드는 필요 없을 수 있습니다.
}
