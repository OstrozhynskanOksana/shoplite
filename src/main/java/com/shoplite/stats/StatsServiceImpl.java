package com.shoplite.stats;

import com.shoplite.order.OrderRepository;
import com.shoplite.order.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;

/** Monthly sales: number of orders and revenue, cancelled orders excluded (FR-15). */
@Service
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final OrderRepository orderRepository;

    public StatsServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public MonthlySales getMonthlySales(YearMonth month) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        long count = orderRepository.countInPeriod(from, to, OrderStatus.CANCELLED);
        return new MonthlySales(month, count, orderRepository.revenueInPeriod(from, to, OrderStatus.CANCELLED));
    }
}
