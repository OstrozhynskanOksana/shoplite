package com.shoplite.stats;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Sales statistics for administrators (FR-15). Implementation is planned for a later sprint. */
public interface StatsService {

    record MonthlySales(YearMonth month, long ordersCount, BigDecimal revenue) {
    }

    MonthlySales getMonthlySales(YearMonth month);
}
