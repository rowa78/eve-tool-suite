package de.ronnywalter.eve.accountservice.controller;

import de.ronnywalter.eve.accountservice.model.EveCharacter;
import de.ronnywalter.eve.accountservice.service.CharacterService;
import de.ronnywalter.eve.accountservice.service.EveCharacterNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final OAuth2AuthorizedClientService clientService;
    private final CharacterService characterService;

    @GetMapping("/loginCharacter")
    public RedirectView redirectWithUsingRedirectView(
            RedirectAttributes attributes) {
        return new RedirectView("oauth2/authorization/eve");
    }

    @GetMapping("/getToken/{characterName}")
    public String getAccountToken(@PathVariable String characterName) {
        try {
            String accessToken = characterService.getAccessToken(characterName);
            return accessToken;
        } catch (EveCharacterNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "EveCharacter Not Found", e);
        }
    }


    /**
     * Begin authentication process.  This will redirect to EVE sign on which will later redirect back
     * to /eveCallback.  The redirect URL you here should be the same as the one you entered for your
     * registered EVE application, and it should point to your callback request mapping.
     *
     * @return redirect URL to EVE sign on
     */
    @RequestMapping(value="/", method= RequestMethod.GET)
    public String index() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if(authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken =
                    (OAuth2AuthenticationToken) authentication;

            Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
            attributes.keySet().forEach(key -> {
                log.debug("attribute: " + key + ": " + attributes.get(key));
            });
            String name = attributes.get("CharacterName").toString();
            Integer id = Integer.valueOf(attributes.get("CharacterID").toString());

            OAuth2AuthorizedClient client =
                    clientService.loadAuthorizedClient(
                            oauthToken.getAuthorizedClientRegistrationId(),
                            oauthToken.getName());

            String refreshToken = client.getRefreshToken().getTokenValue();
            String clientId = client.getClientRegistration().getClientId();

            // TODO: remove this!
            log.debug("Access-Token: " + client.getAccessToken().getTokenValue());

            EveCharacter c = new EveCharacter();
            c.setId(id);
            c.setAccessToken(client.getAccessToken().getTokenValue());
            c.setRefreshToken(client.getRefreshToken().getTokenValue());
            c.setName(name);
            DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            ZonedDateTime zdf = LocalDateTime.parse(attributes.get("ExpiresOn").toString(), dtf).atZone(ZoneId.of("UTC"));

            c.setExpiryDate(zdf);
            c.setClientId(clientId);

            characterService.saveCharacter(c);
        }

        List<EveCharacter> chars = characterService.getEveCharacters();
        StringBuilder result = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
        chars.forEach(c -> {
            result.append("<div>" + c.getName() + ": " + c.getExpiryDate().format(dtf) + "</div>");
        });

        result.append("<br/>");
        result.append(LocalDateTime.now(ZoneId.of("Europe/Berlin")).format(dtf));
        return result.toString();
    }
}
