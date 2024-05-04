package hello.springtx.propagation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/**
 * Member
 - JPA 를 통해 관리하는 회원 엔티티
 */
@Entity
@Getter
@Setter
public class Member {

    @Id
    @GeneratedValue
    private Long id;
    private String username;

    public Member() {
    }

    public Member(String username ) {
        this.username = username;
    }
}
