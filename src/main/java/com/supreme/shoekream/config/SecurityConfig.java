package com.supreme.shoekream.config;

import com.supreme.shoekream.model.dto.MemberDTO;
import com.supreme.shoekream.model.enumclass.Status;
import com.supreme.shoekream.model.network.security.KakaoOAuth2Response;
import com.supreme.shoekream.model.network.security.KreamPrincipal;
import com.supreme.shoekream.repository.MemberRepository;
import com.supreme.shoekream.service.MemberApiLogicService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

@Configuration
public class SecurityConfig{



    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http
                , OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService
    ) throws Exception {
        http.csrf().disable();
        return http
                .authorizeRequests(auth -> auth.antMatchers("/login").permitAll()
                        .mvcMatchers("/",
                                "https://kauth.kakao.com/oauth/authorize/**",
                                "/login",
                                "/login/**",
                                "/loginOk",
                                "/join",
                                "/api/**",
                                "/find",
                                "/check/**",
                                "/product/**",
                                "/brand/**",
                                "/searchs/**",
                                "/social",
                                "/social/newest",
                                "/social/hashtag/**",
                                "/social/details",
                                "/joinOk",
                                "/notice/**",
                                "/faq",
                                "/auth_policy",
                                "/images/**"
                        ).permitAll()
                   .anyRequest().authenticated()
                )
                .formLogin()
                .loginPage("/login")            // ????????? ?????? ????????? ?????????
                    .defaultSuccessUrl("/")            // ????????? ?????? ??? ?????? ?????????
                    .failureUrl("/login.html?error=true")            // ????????? ?????? ??? ?????? ?????????
                    .usernameParameter("email")            // ????????? ??????????????? ??????
                    .passwordParameter("memberPw")            // ???????????? ??????????????? ??????
                    .loginProcessingUrl("/loginOk")            // ????????? Form Action Url
                    .successHandler(new CustomLoginSuccessHandler())		// ????????? ?????? ??? ?????????
    //              .failureHandler(loginFailureHandler())		// ????????? ?????? ??? ?????????
                    .and()
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .oauth2Login(oAuth -> oAuth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)
                        )
                        .successHandler(new CustomLoginSuccessHandler())
                ).oauth2Login().loginPage("/login")
                .and()
                .build();
    }
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().antMatchers("/js/**", "/img/**", "/css/**","/lib/**");
    }
//        return http
//            .authorizeRequests(auth -> auth.anyRequest().permitAll())
//            .formLogin().and()
//            .build();
//        }
    @Bean
    public PasswordEncoder passwordEncoder(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(MemberRepository memberRepository){
        return email -> memberRepository.findByEmail(email)
                .map(MemberDTO::fromEntity)
                .map(KreamPrincipal::fromFull)
                .orElseThrow(() -> new UsernameNotFoundException("????????? ?????? ??? ???????????? - email" + email));
    }

    //???????????? spring??? ????????? ?????? ???????????? ??????
//    protected void configure(HttpSecurity http) throws Exception {
//        http.sessionManagement()
//                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
//                .sessionFixation().changeSessionId();
//    }

    /**
     * <p>
     * OAuth 2.0 ????????? ????????? ?????? ????????? ????????????.
     * ????????? ?????? ????????? ??????.
     *
     * <p>
     * TODO: ????????? ???????????? ???????????? ?????? ??????. ????????? ???????????? ?????? ?????? ?????? ????????? ???????????? ???????????? ?????? ?????????, ?????? ?????? OAuth ?????? ???????????? ????????? ????????? ?????? ????????? ???????????????.
     *
     * @param memberApiLogicService  ????????? ???????????? ????????? ????????? ????????? ????????? ??????
     * @param passwordEncoder ???????????? ????????? ??????
     * @return {@link OAuth2UserService} OAuth2 ?????? ????????? ????????? ??????????????? ???????????? ????????? ???????????? ??????
     */
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService(
            MemberApiLogicService memberApiLogicService,
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder
    ) {
        final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

        return userRequest -> {
            OAuth2User oAuth2User = delegate.loadUser(userRequest);

            KakaoOAuth2Response kakaoResponse = KakaoOAuth2Response.from(oAuth2User.getAttributes());
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            String providerId = String.valueOf(kakaoResponse.id());
//            String username = registrationId + "_" + providerId;
            String username = kakaoResponse.email().split("@")[0];
            String email = kakaoResponse.email();
            String dummyPassword = passwordEncoder.encode("{bcrypt}" + UUID.randomUUID());
            String imgUrl = "/img/styleImg/empty_profile_img.png";

            return memberRepository.findByEmail(email)
                    .map(MemberDTO::fromEntity)
                    .map(KreamPrincipal::fromFull)
                    .orElseGet(() ->
                            KreamPrincipal.fromFull(//memberDTO
                                    memberApiLogicService.saveUser(
                                            username,
                                            dummyPassword,
                                            kakaoResponse.nickname(),
                                            null,
                                            email,
                                            Status.MEMBER,
                                            null,
                                            imgUrl
                                    )
                            )
                    );
        };
    }

}