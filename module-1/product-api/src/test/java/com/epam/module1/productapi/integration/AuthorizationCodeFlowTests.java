package com.epam.module1.productapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Walks the real OAuth2 authorization code flow: form login against stored users, an
 * authorization code protected by PKCE, and a JWT that is then used to call the API.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthorizationCodeFlowTests {

    private static final String CLIENT_ID = "module1-client";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String REDIRECT_URI = "http://127.0.0.1:8089/callback";

    /** The verifier and S256 challenge pair published in RFC 7636, appendix B. */
    private static final String CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final String BODY = """
            {"name": "Lamp", "description": "Brass", "price": 30.00}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JWKSource<SecurityContext> jwkSource;

    @Test
    void anonymousBrowserIsSentToTheLoginPage() throws Exception {
        mockMvc.perform(authorizationRequest().accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/login")));
    }

    @Test
    void wrongPasswordDoesNotLogIn() throws Exception {
        mockMvc.perform(formLogin().user("admin").password("wrong-password"))
                .andExpect(unauthenticated());
    }

    @Test
    void accessTokenCarriesTheRoleOfTheLoggedInUser() throws Exception {
        assertThat(claimsOf(accessToken("admin", "admin-password"))).contains("\"roles\":[\"ADMIN\"]");
        assertThat(claimsOf(accessToken("user", "user-password"))).contains("\"roles\":[\"USER\"]");
    }

    @Test
    void adminTokenCanWrite() throws Exception {
        String token = accessToken("admin", "admin-password");

        mockMvc.perform(post(PRODUCTS_URL).with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void userTokenCanReadButNotWrite() throws Exception {
        String token = accessToken("user", "user-password");

        mockMvc.perform(get(PRODUCTS_URL).with(bearer(token))).andExpect(status().isOk());
        mockMvc.perform(post(PRODUCTS_URL).with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void codeCannotBeExchangedWithoutTheVerifier() throws Exception {
        String code = authorizationCode(login("user", "user-password"));

        mockMvc.perform(post("/oauth2/token").with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", REDIRECT_URI))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String[] parts = accessToken("user", "user-password").split("\\.");
        String forgedClaims = encode(claimsOf(String.join(".", parts))
                .replace("\"roles\":[\"USER\"]", "\"roles\":[\"ADMIN\"]"));
        String forged = parts[0] + "." + forgedClaims + "." + parts[2];

        mockMvc.perform(get(PRODUCTS_URL).with(bearer(forged))).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("https://attacker.example")
                .subject("admin")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of("ADMIN"))
                .build();
        String token = new NimbusJwtEncoder(jwkSource)
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        mockMvc.perform(get(PRODUCTS_URL).with(bearer(token))).andExpect(status().isUnauthorized());
    }

    private String accessToken(String username, String password) throws Exception {
        String code = authorizationCode(login(username, password));

        String response = mockMvc.perform(post("/oauth2/token").with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", REDIRECT_URI)
                        .param("code_verifier", CODE_VERIFIER))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.access_token");
    }

    private MockHttpSession login(String username, String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin().user(username).password(password))
                .andExpect(authenticated())
                .andReturn()
                .getRequest()
                .getSession(false);
    }

    private String authorizationCode(MockHttpSession session) throws Exception {
        String redirect = mockMvc.perform(authorizationRequest().session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        URI redirectUri = URI.create(redirect);
        return UriComponentsBuilder.fromUri(redirectUri).build().getQueryParams().getFirst("code");
    }

    private static MockHttpServletRequestBuilder authorizationRequest() {
        return get("/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", CLIENT_ID)
                .queryParam("redirect_uri", REDIRECT_URI)
                .queryParam("code_challenge", CODE_CHALLENGE)
                .queryParam("code_challenge_method", "S256");
    }

    private static RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }

    private static String claimsOf(String token) {
        byte[] claims = Base64.getUrlDecoder().decode(token.split("\\.")[1]);
        return new String(claims, StandardCharsets.UTF_8);
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
