package com.azadi.config;

import com.google.cloud.Timestamp;
import com.google.cloud.spring.data.datastore.core.convert.DatastoreCustomConversions;
import com.google.cloud.spring.data.datastore.repository.config.EnableDatastoreRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Configuration
@EnableDatastoreRepositories(basePackages = "com.azadi")
public class DatastoreConfig {

    @Bean
    public DatastoreCustomConversions datastoreCustomConversions() {
        return new DatastoreCustomConversions(List.of(
            new LocalDateToTimestamp(),
            new TimestampToLocalDate(),
            new InstantToTimestamp(),
            new TimestampToInstant()
        ));
    }

    static class LocalDateToTimestamp implements Converter<LocalDate, Timestamp> {
        @Override
        public Timestamp convert(LocalDate source) {
            return Timestamp.ofTimeSecondsAndNanos(
                source.atStartOfDay(ZoneOffset.UTC).toEpochSecond(), 0);
        }
    }

    static class TimestampToLocalDate implements Converter<Timestamp, LocalDate> {
        @Override
        public LocalDate convert(Timestamp source) {
            return Instant.ofEpochSecond(source.getSeconds(), source.getNanos())
                .atZone(ZoneOffset.UTC).toLocalDate();
        }
    }

    static class InstantToTimestamp implements Converter<Instant, Timestamp> {
        @Override
        public Timestamp convert(Instant source) {
            return Timestamp.ofTimeSecondsAndNanos(source.getEpochSecond(), source.getNano());
        }
    }

    static class TimestampToInstant implements Converter<Timestamp, Instant> {
        @Override
        public Instant convert(Timestamp source) {
            return Instant.ofEpochSecond(source.getSeconds(), source.getNanos());
        }
    }
}
