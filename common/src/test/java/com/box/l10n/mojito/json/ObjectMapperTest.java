package com.box.l10n.mojito.json;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.*;

public class ObjectMapperTest {

    static final ZonedDateTime A_DATE_TIME = ZonedDateTime.of(2023, 12, 06, 15, 22, 5, 123_456_789, ZoneId.systemDefault());
    static final String SERIALIZED_DATE_TIME = "{\"zonedDateTime\":1701876125123}";

    @Test
    public void serialization() {
        ObjectMapper objectMapper = new ObjectMapper();
        Pojo pojo = new Pojo();
        pojo.setZonedDateTime(A_DATE_TIME);
        Assertions.assertThat(objectMapper.writeValueAsStringUnchecked(pojo)).isEqualTo(SERIALIZED_DATE_TIME);
    }

    @Test
    public void deserialization() {
        ObjectMapper objectMapper = new ObjectMapper();
        Pojo pojo = objectMapper.readValueUnchecked(SERIALIZED_DATE_TIME, Pojo.class);
        Assertions.assertThat(pojo.getZonedDateTime()).isEqualTo(ZonedDateTime.of(2023, 12, 06, 15, 22, 5, 123_000_000, ZoneId.systemDefault()));
    }

    static class Pojo {
        ZonedDateTime zonedDateTime;

        public ZonedDateTime getZonedDateTime() {
            return zonedDateTime;
        }

        public void setZonedDateTime(ZonedDateTime zonedDateTime) {
            this.zonedDateTime = zonedDateTime;
        }
    }
}