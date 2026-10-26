package com.example.chosim.chosim.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 업무 서비스의 트랜잭션을 호출하는 API 경계에만 붙인다. 첫 인자는 대상 ID다. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    AuditAction value();
}
