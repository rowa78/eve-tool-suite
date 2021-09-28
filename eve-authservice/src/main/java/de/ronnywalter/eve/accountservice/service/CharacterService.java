package de.ronnywalter.eve.accountservice.service;

import de.ronnywalter.eve.accountservice.model.EveCharacter;
import de.ronnywalter.eve.accountservice.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CharacterService {

    private final CharacterRepository characterRepository;

    public void saveCharacter(EveCharacter c) {
        characterRepository.save(c);
    }

    public EveCharacter getEveCharacter(String name) throws EveCharacterNotFoundException {
        return characterRepository.findByName(name).orElseThrow(EveCharacterNotFoundException::new);
    }

    public List<EveCharacter> getEveCharacters() {
        return characterRepository.findAll();
    }

    public String getAccessToken(String characterName) throws EveCharacterNotFoundException {
        EveCharacter eveCharacter = this.getEveCharacter(characterName);
        ZonedDateTime expiryDate = eveCharacter.getExpiryDate();
        //ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).plusSeconds(30);;
        ZonedDateTime now = ZonedDateTime.now();
        if(now.isAfter(expiryDate)) {
            log.debug("get new AccessToken for " + characterName);
            try {
                StringBuilder builder = new StringBuilder();
                builder.append("grant_type=");
                builder.append(URLEncoder.encode("refresh_token", "UTF-8"));
                builder.append("&client_id=");
                builder.append(URLEncoder.encode(eveCharacter.getClientId(), "UTF-8"));
                builder.append("&refresh_token=");
                builder.append(URLEncoder.encode(eveCharacter.getRefreshToken(), "UTF-8"));


                URL obj = new URL("https://login.eveonline.com/oauth/token");
                HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
                // add request header
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                con.setRequestProperty("Host", "login.eveonline.com");
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);

                // Send post request
                con.setDoOutput(true);
                try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                    wr.writeBytes(builder.toString());
                    wr.flush();
                }

                StringBuilder response;
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    String inputLine;
                    response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                }
                log.debug("Response: " + response.toString());
                // read json
                JsonParser jsonParser = JsonParserFactory.getJsonParser();
                Map<String, Object> result = jsonParser.parseMap(response.toString());

                result.keySet().forEach(k -> {
                    log.debug(k + ": " + result.get(k));
                });

                eveCharacter.setAccessToken(result.get("access_token").toString());
                eveCharacter.setRefreshToken(result.get("refresh_token").toString());

                Integer seconds = Integer.parseInt(result.get("expires_in").toString());
                ZonedDateTime newExpiryTime = ZonedDateTime.now().plusSeconds(seconds);
                eveCharacter.setExpiryDate(newExpiryTime);

                characterRepository.save(eveCharacter);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return eveCharacter.getAccessToken();
    }
}
