package com.auth.authservice.google;

import com.auth.authservice.entities.GoogleUsers;
import com.auth.authservice.jwt.JwtUtils;
import com.auth.authservice.repository.GoogleUsersRepository;
import com.auth.authservice.services.CustomGoogleUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private GoogleUsersRepository googleUsersRepository;

    @Autowired
    private final CodeStore codeStore;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private CustomGoogleUserDetailsService userDetailsService;

    @Autowired
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.redirect.url}")
    private String _redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException{

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        assert email != null;
        String username = email.split("@")[0];

        googleUsersRepository.getByEmail(email).orElseGet(()->
                googleUsersRepository.save(GoogleUsers.builder()
                        .username(username)
                        .email(email)
                        .role("USER")
                        .createdDate(LocalDate.now())
                        .build()));

        OAuth2AuthorizedClient client =
                authorizedClientService.loadAuthorizedClient("google", authentication.getName());


        UserDetails user = userDetailsService.loadUserByUsername(email);

        String jwtToken = jwtUtils.generateToken(user);
        String jwtRetrievalCode = UUID.randomUUID().toString();
        String usernameForGmail = jwtUtils.extractUsername(jwtToken);
        codeStore.saveCode("googleLogin", jwtRetrievalCode, jwtToken);

        String accessToken = client.getAccessToken().getTokenValue();
        String refreshToken = client.getRefreshToken() != null ? client.getRefreshToken().getTokenValue() : null;
        String gmailTokensRetrivalCode = UUID.randomUUID().toString();
        codeStore.saveCode("gmail", gmailTokensRetrivalCode, accessToken+"_&&_"+refreshToken+"_&&_"+usernameForGmail);

        String finalRedirectUrl = _redirectUrl
                .replace("{jwt}", jwtRetrievalCode)
                .replace("{gmail}", gmailTokensRetrivalCode);

        getRedirectStrategy().sendRedirect(request, response, finalRedirectUrl);
    }

}
