package kr.hhplus.be.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration // 설정 클래스임을 Spring에 알림
@EnableAsync // @Async가 붙은 메서드를 비동기(새로운 스레드)로 실행할 수 있게 함
public class AsyncConfig {
}
