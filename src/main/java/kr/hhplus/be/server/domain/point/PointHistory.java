package kr.hhplus.be.server.domain.point;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_id", nullable = false)  // 외래키 컬럼
    private Long pointId;  // Point 엔티티를 참조하는 외래키

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "balance", nullable = false)
    private Integer balance;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // JPA Lifecycle Hooks: 엔티티가 저장될 때 날짜 자동 세팅
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
