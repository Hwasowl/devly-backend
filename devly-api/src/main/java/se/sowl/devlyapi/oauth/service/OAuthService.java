package se.sowl.devlyapi.oauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sowl.devlyapi.oauth.factory.OAuth2UserFactory;
import se.sowl.devlydomain.oauth.domain.*;
import se.sowl.devlydomain.user.domain.User;
import se.sowl.devlydomain.user.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final DefaultOAuth2UserService defaultOAuth2UserService;
    private final OAuth2UserFactory oAuth2UserFactory;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User loadedUser = defaultOAuth2UserService.loadUser(userRequest);
        OAuth2Profile profile = extractOAuth2Profile(userRequest, loadedUser);
        User user = getOrCreateUser(profile);
        OAuth2User oAuth2User = oAuth2UserFactory.createOAuth2User(userRequest, loadedUser, profile);
        return oAuth2UserFactory.createCustomOAuth2User(user, oAuth2User);
    }

    private OAuth2Profile extractOAuth2Profile(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Provider provider = OAuth2Provider.valueOf(registrationId.toUpperCase());
        return OAuth2Extractor.extract(provider, oAuth2User.getAttributes());
    }

    private User getOrCreateUser(OAuth2Profile oAuth2Profile) {
        return userRepository.findByEmailAndProvider(oAuth2Profile.getEmail(), oAuth2Profile.getProvider())
            .orElseGet(() -> userRepository.save(oAuth2Profile.toUser()));
    }
}
