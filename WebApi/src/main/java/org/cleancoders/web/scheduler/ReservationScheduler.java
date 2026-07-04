package org.cleancoders.web.scheduler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.cleancoders.systemtask.usecase.ProcessExpiredReservationsUseCase;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 后台定时调度器：每 60 秒自动执行一次过期预约处理。
 * <p>
 * 实现 ContainerResponseFilter 是为了让 Jersey 在启动时立即创建该单例
 * （从而触发 @PostConstruct），filter 方法本身不做任何事。
 */
@Singleton
@Provider
public class ReservationScheduler implements ContainerResponseFilter
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
        LOG.info("[ReservationScheduler] 预约超时处理调度器已启动，每 60 秒执行一次");
        executor.scheduleWithFixedDelay(this::processExpired, 10, 60, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop()
    {
        LOG.info("[ReservationScheduler] 预约超时处理调度器正在停止...");
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

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException
    {
        // no-op: 仅用于让 Jersey 在启动时立即创建此 Provider 单例
    }

    private void processExpired()
    {
        try
        {
            ProcessExpiredReservationsUseCase.Output output = processExpiredReservationsUseCase.execute();
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (output.autoCheckedOut() > 0 || output.expired() > 0)
            {
                LOG.info(String.format("[ReservationScheduler %s] 处理完成: 日期=%s, 自动退座=%d, 超时=%d",
                        time, output.date(), output.autoCheckedOut(), output.expired()));
            }
            else
            {
                LOG.info(String.format("[ReservationScheduler %s] 心跳: 无待处理预约", time));
            }
        }
        catch (Exception e)
        {
            LOG.log(Level.WARNING, "[ReservationScheduler] 处理过期预约时发生异常", e);
        }
    }
}
