package com.nihongu.lokibe.Config;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

// Thời gian gửi cho app phải kèm múi giờ của server, để máy ở múi giờ khác hiện đúng giờ
class JacksonConfigTest {
    private final JsonMapper mapper = JsonMapper.builder()
            .addModule(new JacksonConfig().offsetLocalDateTimeModule())
            .build();

    @Test
    void localDateTimeIsWrittenWithTheServerOffset() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 18, 10, 0, 0);
        String json = mapper.writeValueAsString(time);

        String expectedOffset = time.atZone(ZoneId.systemDefault()).getOffset().getId();
        assertThat(json).isEqualTo("\"2026-11-18T10:00:00" + expectedOffset +"\"");
        // Readable as an instant on the other side
        OffsetDateTime parsed = OffsetDateTime.parse(json.replace("\"", ""));
        assertThat(parsed.toLocalDateTime()).isEqualTo(time);
    }
}
