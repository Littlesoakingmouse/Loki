package com.nihongu.lokibe.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Thời gian trả về cho app luôn kèm múi giờ, VD "2026-11-18T10:00:00+07:00".
 * Entity lưu LocalDateTime theo giờ của server; nếu gửi thiếu múi giờ thì máy ở múi giờ khác sẽ hiện sai giờ.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule offsetLocalDateTimeModule() {
        ZoneId serverZone = ZoneId.systemDefault();
        SimpleModule module = new SimpleModule("offset-local-date-time");
        module.addSerializer(LocalDateTime.class, new ValueSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
                gen.writeString(value.atZone(serverZone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
        });
        return module;
    }
}
