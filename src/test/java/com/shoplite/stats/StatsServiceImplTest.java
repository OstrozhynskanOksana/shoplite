package com.shoplite.stats;

import com.shoplite.order.OrderRepository;
import com.shoplite.order.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static com.shoplite.support.TestData.assertMoney;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Unit tests for monthly sales (FR-15). */
class StatsServiceImplTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final StatsService service = new StatsServiceImpl(orderRepository);

    @Test
    void monthlySalesCoverTheWholeMonthAndExcludeCancelledOrders() {
        when(orderRepository.countInPeriod(any(), any(), eq(OrderStatus.CANCELLED))).thenReturn(3L);
        when(orderRepository.revenueInPeriod(any(), any(), eq(OrderStatus.CANCELLED)))
                .thenReturn(new BigDecimal("4200.00"));

        StatsService.MonthlySales sales = service.getMonthlySales(YearMonth.of(2026, 2));

        LocalDateTime from = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 3, 1, 0, 0);
        verify(orderRepository).countInPeriod(from, to, OrderStatus.CANCELLED);
        verify(orderRepository).revenueInPeriod(from, to, OrderStatus.CANCELLED);
        assertEquals(YearMonth.of(2026, 2), sales.month());
        assertEquals(3, sales.ordersCount());
        assertMoney("4200.00", sales.revenue());
    }

    @Test
    void decemberEndsOnTheFirstOfJanuary() {
        when(orderRepository.revenueInPeriod(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        StatsService.MonthlySales sales = service.getMonthlySales(YearMonth.of(2026, 12));

        verify(orderRepository).countInPeriod(LocalDateTime.of(2026, 12, 1, 0, 0),
                LocalDateTime.of(2027, 1, 1, 0, 0), OrderStatus.CANCELLED);
        assertEquals(0, sales.ordersCount());
        assertMoney("0", sales.revenue());
    }
}
