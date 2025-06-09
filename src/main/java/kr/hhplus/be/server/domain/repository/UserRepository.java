package kr.hhplus.be.server.domain.repository;

import kr.hhplus.be.server.domain.point.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
