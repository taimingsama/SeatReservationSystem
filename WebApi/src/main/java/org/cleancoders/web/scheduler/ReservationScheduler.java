package org.cleancoders.web.scheduler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.cleancoders.systemtask.usecase.ProcessExpiredReservationsUseCase;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 后台定时调度器：每 60 秒自动执行一次过期预约处理。
 * <p>
 * 在应用启动时自动创建，无需外部触发。
 */
@Singleton
public class ReservationScheduler
{

    private static final Logger LOG = Logger.getLogger(ReservationScheduler.class.getName());

    @Inject
    ProcessExpiredReservationsUseCase processExpiredReservationsUseCase;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r ->
    {
        Thread t = new Thread(r, "reservation-scheduler");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void start()
    {
        LOG.info("预约超时处理调度器已启动，每 60 秒执行一次");
        executor.scheduleWithFixedDelay(this::processExpired, 10, 60, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop()
    {
        LOG.info("预约超时处理调度器正在停止...");
        executor.shutdown();
        try
        {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS))
            {
                executor.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void processExpired()
    {
        try
        {
            ProcessExpiredReservationsUseCase.Output output = processExpiredReservationsUseCase.execute();
            if (output.autoCheckedOut() > 0 || output.expired() > 0)
            {
                LOG.info(() -> String.format("[%s] 处理完成: 自动退座=%d, 超时=%d",
                        output.date(), output.autoCheckedOut(), output.expired()));
            }
        }
        catch (Exception e)
        {
            LOG.log(Level.WARNING, "处理过期预约时发生异常", e);
        }
    }
}
