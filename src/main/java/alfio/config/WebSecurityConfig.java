/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.config;

import alfio.manager.system.ConfigurationManager;
import alfio.manager.user.UserManager;
import alfio.model.system.ConfigurationKeys;
import alfio.model.user.Role;
import alfio.util.Json;
import com.squareup.okhttp.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.regex.Pattern;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    public static final String ADMIN_API = "/admin/api";
    public static final String CSRF_SESSION_ATTRIBUTE = "CSRF_SESSION_ATTRIBUTE";
    public static final String CSRF_PARAM_NAME = "_csrf";
    public static final String OPERATOR = "OPERATOR";
    private static final String SUPERVISOR = "SUPERVISOR";
    public static final String SPONSOR = "SPONSOR";
    private static final String ADMIN = "ADMIN";
    private static final String OWNER = "OWNER";



    private static class BaseWebSecurity extends  WebSecurityConfigurerAdapter {

        @Autowired
        private DataSource dataSource;
        @Autowired
        private PasswordEncoder passwordEncoder;

        @Override
        public void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.jdbcAuthentication().dataSource(dataSource)
                    .usersByUsernameQuery("select username, password, enabled from ba_user where username = ?")
                    .authoritiesByUsernameQuery("select username, role from authority where username = ?")
                    .passwordEncoder(passwordEncoder);
        }
    }

    /**
     * Basic auth configuration for Mobile App.
     * The rules are only valid if the header Authorization is present, otherwise it fallback to the
     * FormBasedWebSecurity rules.
     */
    @Configuration
    @Order(1)
    public static class BasicAuthWebSecurity extends BaseWebSecurity {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.requestMatcher((request) -> request.getHeader("Authorization") != null).sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and().csrf().disable()
            .authorizeRequests()
            .antMatchers(ADMIN_API + "/check-in/**").hasAnyRole(OPERATOR, SUPERVISOR)
            .antMatchers(HttpMethod.GET, ADMIN_API + "/events").hasAnyRole(OPERATOR, SUPERVISOR, SPONSOR)
            .antMatchers(ADMIN_API + "/user-type").hasAnyRole(OPERATOR, SUPERVISOR, SPONSOR)
            .antMatchers(ADMIN_API + "/**").denyAll()
            .antMatchers(HttpMethod.POST, "/api/attendees/sponsor-scan").hasRole(SPONSOR)
            .antMatchers("/**").authenticated()
            .and().httpBasic();
        }
    }

    /**
     * Default form based configuration.
     */
    @Configuration
    @Order(2)
    public static class FormBasedWebSecurity extends BaseWebSecurity {

        @Autowired
        private Environment environment;

        @Autowired
        private UserManager userManager;

        @Autowired
        private ConfigurationManager configurationManager;

        @Bean
        public CsrfTokenRepository getCsrfTokenRepository() {
            HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
            repository.setSessionAttributeName(CSRF_SESSION_ATTRIBUTE);
            repository.setParameterName(CSRF_PARAM_NAME);
            return repository;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            if(environment.acceptsProfiles("!"+Initializer.PROFILE_DEV)) {
                http.requiresChannel().anyRequest().requiresSecure();
            }

            CsrfConfigurer<HttpSecurity> configurer =
                http.exceptionHandling()
                    .accessDeniedPage("/session-expired")
                    .defaultAuthenticationEntryPointFor((request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED), new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"))
                    .and()
                    .headers().cacheControl().disable()
                    .and()
                    .csrf();
            if(environment.acceptsProfiles(Initializer.PROFILE_DEBUG_CSP)) {
                Pattern whiteList = Pattern.compile("^(GET|HEAD|TRACE|OPTIONS)$");
                configurer.requireCsrfProtectionMatcher(new NegatedRequestMatcher((r) -> whiteList.matcher(r.getMethod()).matches() || r.getRequestURI().equals("/report-csp-violation")));
            }
            configurer.csrfTokenRepository(getCsrfTokenRepository())
                .and()
                .authorizeRequests()
                .antMatchers(ADMIN_API + "/configuration/**", ADMIN_API + "/users/**").hasAnyRole(ADMIN, OWNER)
                .antMatchers(ADMIN_API + "/organizations/new").hasRole(ADMIN)
                .antMatchers(ADMIN_API + "/check-in/**").hasAnyRole(ADMIN, OWNER, SUPERVISOR)
                .antMatchers(HttpMethod.GET, ADMIN_API + "/**").hasAnyRole(ADMIN, OWNER, SUPERVISOR)
                .antMatchers(ADMIN_API + "/**").hasAnyRole(ADMIN, OWNER)
                .antMatchers("/admin/**/export/**").hasAnyRole(ADMIN, OWNER)
                .antMatchers("/admin/**").hasAnyRole(ADMIN, OWNER, SUPERVISOR)
                .antMatchers("/api/attendees/**").denyAll()
                .antMatchers("/**").permitAll()
                .and()
                .formLogin()
                .loginPage("/authentication")
                .loginProcessingUrl("/authenticate")
                .failureUrl("/authentication?failed")
                .and().logout().permitAll();


            //
            if(environment.acceptsProfiles("demo")) {
                http.addFilterBefore(new UserCreatorBeforeLoginFilter(userManager, configurationManager, "/authenticate", "/authentication?recaptchaFailed"), UsernamePasswordAuthenticationFilter.class);
            }
        }


        // generate a user if it does not exists, to be used by the demo profile
        private static class UserCreatorBeforeLoginFilter extends GenericFilterBean {

            private final UserManager userManager;
            private final RequestMatcher requestMatcher;
            private final ConfigurationManager configurationManager;
            private final OkHttpClient client = new OkHttpClient();
            private final String recaptchaFailureUrl;

            UserCreatorBeforeLoginFilter(UserManager userManager, ConfigurationManager configurationManager, String loginProcessingUrl, String recaptchaFailureUrl) {
                this.userManager = userManager;
                this.configurationManager = configurationManager;
                this.requestMatcher = new AntPathRequestMatcher(loginProcessingUrl, "POST");
                this.recaptchaFailureUrl = recaptchaFailureUrl;
            }

            private boolean checkRecaptcha(HttpServletRequest req) {
                return configurationManager.getStringConfigValue(alfio.model.system.Configuration.getSystemConfiguration(ConfigurationKeys.RECAPTCHA_SECRET))
                    .map((secret) -> recaptchaRequest(client, secret, req.getParameter("g-recaptcha-response")))
                    .orElse(true);
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;

                if(requestMatcher.matches(req) && !checkRecaptcha(req)) {
                    res.sendRedirect(recaptchaFailureUrl);
                    return;
                }

                //ensure organization/user
                if(requestMatcher.matches(req) && req.getParameter("username") != null && req.getParameter("password") != null) {
                    String username = req.getParameter("username");
                    if(!userManager.usernameExists(username)) {
                        int orgId = userManager.createOrganization(username, "Demo organization", username);
                        userManager.insertUser(orgId, username, "", "", username, Role.OWNER);
                    }
                }

                chain.doFilter(request, response);
            }
        }
    }

    private static boolean recaptchaRequest(OkHttpClient client, String secret, String response) {
        try {
            RequestBody reqBody = new FormEncodingBuilder().add("secret", secret).add("response", response).build();
            Request request = new Request.Builder().url("https://www.google.com/recaptcha/api/siteverify").post(reqBody).build();
            Response resp = client.newCall(request).execute();
            return Json.fromJson(resp.body().string(), RecatpchaResponse.class).success;
        } catch (IOException e) {
            return false;
        }
    }

    @Data
    public static class RecatpchaResponse {
        private boolean success;
    }




}
