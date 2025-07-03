package kr.kro.airbob.common.lock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key();                    // 스프링 EL 표현식
    String lockName();              // 락 prefix
    long waitTime() default 5L;     // 락을 기다리는 시간 (초)
    long leaseTime() default 3L;    // 락 자동 해제 시간 (초)
}
