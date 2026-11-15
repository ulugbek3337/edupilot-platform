package com.eduflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LocaleConfigTest {

    private final LocaleResolver localeResolver = new LocaleConfig().localeResolver();

    @Test
    void defaultsToUzbekWhenNoAcceptLanguageIsProvided() {
        Locale locale = localeResolver.resolveLocale(new MockHttpServletRequest());

        assertEquals("uz", locale.getLanguage());
    }

    @Test
    void resolvesRussianForRegionalAndLanguageOnlyHeaders() {
        MockHttpServletRequest regionalRequest = new MockHttpServletRequest();
        regionalRequest.addHeader("Accept-Language", "ru-RU,ru;q=0.9");
        MockHttpServletRequest languageRequest = new MockHttpServletRequest();
        languageRequest.addHeader("Accept-Language", "ru");

        assertEquals("ru", localeResolver.resolveLocale(regionalRequest).getLanguage());
        assertEquals("ru", localeResolver.resolveLocale(languageRequest).getLanguage());
    }

    @Test
    void returnsLocalizedBackendMessagesForBothSupportedLanguages() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasenames("i18n/messages");
        messages.setDefaultEncoding("UTF-8");

        String uzbek = messages.getMessage("auth.invalid-credentials", null, Locale.forLanguageTag("uz-UZ"));
        String russian = messages.getMessage("auth.invalid-credentials", null, Locale.forLanguageTag("ru-RU"));

        assertEquals("Elektron pochta yoki parol noto‘g‘ri.", uzbek);
        assertEquals("Неверная электронная почта или пароль.", russian);
        assertNotEquals(uzbek, russian);
    }
}
