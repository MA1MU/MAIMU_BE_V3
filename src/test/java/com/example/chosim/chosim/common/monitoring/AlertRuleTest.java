package com.example.chosim.chosim.common.monitoring;

import com.example.chosim.chosim.common.monitoring.AlertMonitor.AlertRule;
import com.example.chosim.chosim.common.monitoring.AlertMonitor.Result;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AlertRuleTest {

    private final Instant t0 = Instant.parse("2026-10-26T00:00:00Z");

    private AlertRule cpuRule() {
        return new AlertRule("CPU", "CPU", "%", 80, 3, m -> 0);
    }

    @Test
    void 순간_튐은_무시하고_3회_연속일_때만_울린다() {
        AlertRule rule = cpuRule();

        assertThat(rule.evaluate(95, t0)).isEqualTo(Result.NONE);
        assertThat(rule.evaluate(20, t0.plusSeconds(10))).isEqualTo(Result.NONE);   // 연속이 끊김
        assertThat(rule.evaluate(90, t0.plusSeconds(20))).isEqualTo(Result.NONE);
        assertThat(rule.evaluate(90, t0.plusSeconds(30))).isEqualTo(Result.NONE);
        assertThat(rule.evaluate(90, t0.plusSeconds(40))).isEqualTo(Result.FIRE);
    }

    @Test
    void 쿨다운_동안은_다시_울리지_않는다() {
        AlertRule rule = cpuRule();
        for (int i = 0; i < 3; i++) rule.evaluate(90, t0);

        assertThat(rule.evaluate(90, t0.plusSeconds(60))).isEqualTo(Result.NONE);
        assertThat(rule.evaluate(90, t0.plus(AlertMonitor.COOLDOWN))).isEqualTo(Result.FIRE);
    }

    @Test
    void 기준_아래로_돌아오면_해소를_한_번_알린다() {
        AlertRule rule = cpuRule();
        for (int i = 0; i < 3; i++) rule.evaluate(90, t0);

        assertThat(rule.evaluate(30, t0.plusSeconds(10))).isEqualTo(Result.RECOVER);
        assertThat(rule.evaluate(30, t0.plusSeconds(20))).isEqualTo(Result.NONE);
    }
}
