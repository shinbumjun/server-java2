package kr.hhplus.be.server.domain.service;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.domain.point.User;
import kr.hhplus.be.server.domain.repository.UserRepository;
import kr.hhplus.be.server.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public User getUser(Long userId) {
        // 예외 조회(Repository) → 서비스 계층
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }
}
