package com.tianji.promotion.utils;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class MyLockAspect implements Ordered {

    private final MyLockFactory myLockFactory;

    @Around("@annotation(myLock)")
    public Object tryLock(ProceedingJoinPoint pjp, MyLock myLock) throws Throwable {
        RLock lock = null;
        lock = myLockFactory.getLock(myLock.type(), myLock.name());
        boolean isLock = myLock.lockStrategy().tryLock(lock, myLock);
        if(!isLock) {
            return null;
        }
        try {
            return pjp.proceed();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
