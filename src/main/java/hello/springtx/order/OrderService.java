package hello.springtx.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예외와 트랜잭션 커밋, 롤백 - 활용
 - 스프링은 기본적으로 체크 예외는 비즈니스 의미가 있을 때 사용하고, 런타임(언체크) 예외는 복구 불가능한 예외로 가정
 - 해당 정책을 반드시 따를 필요는 없고, rollbackFor 를 이용하여 체크 예외도 롤백이 가능함

 * 비즈니스 요구사항
 - 정상 : 주문시 결제를 성공하면 주문 데이터를 저장하고 결제 상태를 완료로 처리
 - 시스템 예외 : 주문시 내부에 복구 불가능한 예외가 발생하면 전체 데이터를 롤백
 - 비즈니스 예외 : 주문시 결제 잔고가 부족하면 주문 데이터를 저장하고, 결제 상태를 대기 상태로 처리
 - 결제 잔고 부족시 NotEnoughMoneyException 이라는 체크 예외가 발생한다고 가정
 -> 해당 예외는 시스템에 문제가 있어서 발생하는 에외가 아닌 비즈니스 상황의 문제로 발생할 수 있기 때문에 반드시 처리해야하는 경우가 많아 체크 예외를 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    //JPA 는 트랜잭션 커밋 시점에 Order 데이터를 DB 에 반영함
    @Transactional
    public void order(Order order) throws NotEnoughMoneyException {
        log.info("order 호출");
        orderRepository.save(order);

        log.info("결제 프로세스 진입");
        if(order.getUsername().equals("예외")) {
            log.info("시스템 예외 발생");
            throw new RuntimeException("시스템 예외");
        } else if(order.getUsername().equals("잔고부족")) {
            log.info("잔고 부족 비즈니스 예외 발생");
            order.setPayStatus("대기");
            throw new NotEnoughMoneyException("잔고가 부족합니다");
        } else {
            log.info("정상 승인");
            order.setPayStatus("완료");
        }

        log.info("결제 프로세스 완료");
    }
}
