package com.eduflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(new ArrayList<>(List.of(
                Locale.forLanguageTag("uz"),
                Locale.forLanguageTag("uz-UZ"),
                Locale.forLanguageTag("ru"),
                Locale.forLanguageTag("ru-RU")
        )));
        resolver.setDefaultLocale(Locale.forLanguageTag("uz-UZ"));
        return resolver;
    }
}
