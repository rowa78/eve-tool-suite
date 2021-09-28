package de.ronnywalter.eve.accountservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.ZonedDateTime;


@Getter
@Setter
@Document
@ToString
public class EveCharacter {

    @Id
    private Integer id;
    private String name;
    private String clientId;
    private ZonedDateTime expiryDate;
    private String refreshToken;
    private String accessToken;
}
