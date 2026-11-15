import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  accessTokenStorageKey,
  getAppLanguage,
  isAppLanguage,
  localeByLanguage,
  setUserPreferredLanguage,
  supportedLanguages,
} from '../i18n';

export default function LanguageSwitcher() {
  const { t, i18n } = useTranslation('common');
  const [saveError, setSaveError] = useState(false);
  const language = getAppLanguage(i18n.resolvedLanguage ?? i18n.language);

  async function changeLanguage(nextLanguage: string) {
    setSaveError(false);
    if (!isAppLanguage(nextLanguage)) {
      return;
    }

    await setUserPreferredLanguage(nextLanguage);

    const accessToken = window.localStorage?.getItem(accessTokenStorageKey);
    if (!accessToken) {
      return;
    }

    try {
      const response = await fetch('/api/v1/auth/me/language', {
        method: 'PATCH',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
          'Accept-Language': localeByLanguage[getAppLanguage(nextLanguage)],
        },
        body: JSON.stringify({ language: nextLanguage }),
      });
      if (!response.ok) {
        throw new Error(`Language preference update failed with status ${response.status}`);
      }
    } catch (error) {
      console.error('Unable to persist the language preference.', error);
      setSaveError(true);
    }
  }

  return (
    <div className="language-control">
      <label htmlFor="language-select">{t('language.label')}</label>
      <select
        id="language-select"
        value={language}
        onChange={(event) => void changeLanguage(event.target.value)}
      >
        {supportedLanguages.map((supportedLanguage) => (
          <option key={supportedLanguage} value={supportedLanguage}>
            {t(`language.${supportedLanguage}`)}
          </option>
        ))}
      </select>
      {saveError && (
        <span className="language-error" role="status">
          {t('language.saveError')}
        </span>
      )}
    </div>
  );
}
