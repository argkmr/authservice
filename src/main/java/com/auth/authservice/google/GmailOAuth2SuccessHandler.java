package com.auth.authservice.google;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GmailOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final CodeStore codeStore;

    @Value("${app.base.gmail.redirect.url}")
    private String gmailRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = authorizedClientRepository
                .loadAuthorizedClient(oauthToken.getAuthorizedClientRegistrationId(), authentication, request);

        if (client == null) {
            response.sendRedirect("/login?error=missing-client");
            return;
        }

        // Fetch Gmail user info
        OAuth2User oauthUser = oauthToken.getPrincipal();
        String email = (String) oauthUser.getAttributes().get("email");
        String name = (String) oauthUser.getAttributes().get("name");
        String picture = (String) oauthUser.getAttributes().get("picture");

        System.out.println("Connected Gmail account: " + name + " <" + email + ">");

        String accessToken = client.getAccessToken().getTokenValue();
        String refreshToken = client.getRefreshToken() != null
                ? client.getRefreshToken().getTokenValue()
                : null;

        // Save access, refresh, and user email
        String gmailRetrievalCode = UUID.randomUUID().toString();
        String combinedValue = String.join("_&&_",
                accessToken,
                refreshToken != null ? refreshToken : "NO_REFRESH",
                email != null ? email : "UNKNOWN_EMAIL"
        );

        codeStore.saveCode("gmail", gmailRetrievalCode, combinedValue);

        String redirectUrl = gmailRedirectUrl.replace("{gmail}", gmailRetrievalCode);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
