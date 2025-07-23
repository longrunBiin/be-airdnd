package kr.kro.airbob.common.cache.annotaion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisListCacheable {
    String key();                // Redis 키 이름
    int limit() default 100;     // 최대 저장 개수
    Class<?> type();             // 직렬화/역직렬화할 클래스
}
