package com.bux.investmentplans.execution;

import java.time.LocalDate;

/** Fan-out message on {@code execution-triggers}, keyed by planId (1.5-1.6). */
public record ExecutionDue(String planId, LocalDate date) {
}
