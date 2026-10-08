package com.tianji.learning.utils;

import lombok.Data;

import java.sql.Time;
import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
@Data
public class DelayTask<D> implements Delayed {
    private D data;
    //定义每个任务的执行时间
    private long deadlineNanos;
    //这里第二个参数就是要传入多少时间后执行，比如说20s，之后就现在的时间加上20s传给deadlineNanos
    public DelayTask(D data, Duration delayTime) {
        this.data = data;
        this.deadlineNanos = System.nanoTime()+delayTime.toNanos();
    }
    //这个方法先跟着这么写
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(Math.max(0,deadlineNanos-System.nanoTime()), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        long l=getDelay(TimeUnit.NANOSECONDS)-o.getDelay(TimeUnit.NANOSECONDS);
        if (l>0){
            return 1;
        } else if (l<0) {
            return -1;
        }else {
            return 0;
        }
    }
}
