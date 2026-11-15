import { setUserPreferredLanguage } from '../i18n';

export async function applyAuthenticatedLanguage(preferredLanguage: string): Promise<void> {
  await setUserPreferredLanguage(preferredLanguage);
}
