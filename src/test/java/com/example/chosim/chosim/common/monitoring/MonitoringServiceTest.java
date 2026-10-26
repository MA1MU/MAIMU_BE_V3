package com.example.chosim.chosim.common.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MonitoringServiceTest {

    @TempDir
    Path dir;

    private Path writeLog() throws Exception {
        Path file = dir.resolve("maimu.log");
        Files.write(file, List.of(
                "2026-10-26 10:00:00.000 INFO  [http-1] [aaaa1111] [] c.e.Foo - 첫 요청",
                "2026-10-26 10:00:01.000 ERROR [http-2] [bbbb2222] [7] c.e.Bar - 실패",
                "java.lang.IllegalStateException: boom",
                "\tat com.example.Bar.run(Bar.java:10)",
                "2026-10-26 10:00:02.000 INFO  [http-3] [cccc3333] [] c.e.Foo - 세 번째"
        ));
        return file;
    }

    @Test
    void 마지막_N줄만_반환한다() throws Exception {
        List<String> lines = MonitoringService.tail(writeLog(), 2, null);

        assertThat(lines).hasSize(2);
        assertThat(lines.get(1)).contains("세 번째");
    }

    @Test
    void 키워드로_거르면_스택트레이스도_함께_따라온다() throws Exception {
        List<String> lines = MonitoringService.tail(writeLog(), 100, "bbbb2222");

        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).contains("실패");
        assertThat(lines.get(2)).contains("Bar.java:10");
    }

    @Test
    void 파일이_없으면_빈_목록() throws Exception {
        assertThat(MonitoringService.tail(dir.resolve("none.log"), 10, null)).isEmpty();
    }
}
