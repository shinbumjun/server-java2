package kr.hhplus.be.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class LoggingFilter implements Filter {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();

        // 요청 파라미터를 문자열로 변환
        Map<String, String> parameters = httpRequest.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.join(",", entry.getValue())
                ));
        String paramsJson = objectMapper.writeValueAsString(parameters);

        // 요청 로깅
        log.info("들어오는 요청: 메소드={}, URI={}, 파라미터={}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                paramsJson);

        // 다음 필터 또는 서블릿을 호출하기 전에 로깅을 남김
        chain.doFilter(request, response);

        long duration = System.currentTimeMillis() - startTime;

        // 응답 로깅
        log.info("나가는 응답: 상태={}, 소요시간={}ms",
                httpResponse.getStatus(),
                duration);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}