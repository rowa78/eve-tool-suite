package de.ronnywalter.eve.accountservice.config;

import de.ronnywalter.eve.accountservice.util.ZonedDateTimeReadConverter;
import de.ronnywalter.eve.accountservice.util.ZonedDateTimeWriteConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EnableMongoRepositories(basePackages = "de.ronnywalter.eve.accountservice.repository")
@Configuration
public class MongoConfig {
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        List<Converter<?,?>> converters = new ArrayList<>();
        converters.add(ZonedDateTimeToDate.INSTANCE);
        converters.add(DateToZonedDateTime.INSTANCE);
        return new MongoCustomConversions(converters);
    }

    @ReadingConverter
    enum DateToZonedDateTime implements Converter<Date, ZonedDateTime> {

        INSTANCE;

        @Override
        public ZonedDateTime convert(Date date) {
            return date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .truncatedTo(ChronoUnit.MILLIS);
        }
    }

    @WritingConverter
    enum ZonedDateTimeToDate implements Converter<ZonedDateTime, Date> {

        INSTANCE;

        @Override
        public Date convert(ZonedDateTime zonedDateTime) {
            return Date.from(zonedDateTime.toInstant());
        }
    }



}
