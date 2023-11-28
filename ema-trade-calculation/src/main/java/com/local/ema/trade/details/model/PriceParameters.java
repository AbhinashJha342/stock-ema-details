package com.local.ema.trade.details.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PriceParameters {

    private LocalDate date;

    private BigDecimal close;

    private BigDecimal dayRange;

    private BigDecimal openCloseRange;

    private BigDecimal emaDiffPercentage;

    private BigDecimal lowFastEmaPercentage;

    private BigDecimal highFastEmaPercentage;

    private BigDecimal emaAppreciation;

    private BigDecimal netPriceAppreciation;
}
