import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import authRu from './locales/ru/auth.json';
import commonRu from './locales/ru/common.json';
import coursesRu from './locales/ru/courses.json';
import dashboardRu from './locales/ru/dashboard.json';
import groupsRu from './locales/ru/groups.json';
import notificationsRu from './locales/ru/notifications.json';
import parentsRu from './locales/ru/parents.json';
import paymentsRu from './locales/ru/payments.json';
import studentsRu from './locales/ru/students.json';
import teachersRu from './locales/ru/teachers.json';
import authUz from './locales/uz/auth.json';
import commonUz from './locales/uz/common.json';
import coursesUz from './locales/uz/courses.json';
import dashboardUz from './locales/uz/dashboard.json';
import groupsUz from './locales/uz/groups.json';
import notificationsUz from './locales/uz/notifications.json';
import parentsUz from './locales/uz/parents.json';
import paymentsUz from './locales/uz/payments.json';
import studentsUz from './locales/uz/students.json';
import teachersUz from './locales/uz/teachers.json';

export const supportedLanguages = ['uz', 'ru'] as const;
export type AppLanguage = (typeof supportedLanguages)[number];
export const localeByLanguage: Record<AppLanguage, string> = {
  uz: 'uz-UZ',
  ru: 'ru-RU',
};
export const languageStorageKey = 'eduflow.language';
export const accessTokenStorageKey = 'eduflow.accessToken';

export function isAppLanguage(value: string | null | undefined): value is AppLanguage {
  return value === 'uz' || value === 'ru';
}

function getStorage(): Storage | null {
  if (typeof window === 'undefined' || !('localStorage' in window)) {
    return null;
  }

  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

function getInitialLanguage(): AppLanguage {
  const storage = getStorage();
  if (!storage) {
    return 'uz';
  }
  const savedLanguage = storage.getItem(languageStorageKey);
  return isAppLanguage(savedLanguage) ? savedLanguage : 'uz';
}

const resources = {
  uz: {
    common: commonUz,
    auth: authUz,
    dashboard: dashboardUz,
    students: studentsUz,
    teachers: teachersUz,
    courses: coursesUz,
    groups: groupsUz,
    payments: paymentsUz,
    parents: parentsUz,
    notifications: notificationsUz,
  },
  ru: {
    common: commonRu,
    auth: authRu,
    dashboard: dashboardRu,
    students: studentsRu,
    teachers: teachersRu,
    courses: coursesRu,
    groups: groupsRu,
    payments: paymentsRu,
    parents: parentsRu,
    notifications: notificationsRu,
  },
};

void i18n.use(initReactI18next).init({
  resources,
  lng: getInitialLanguage(),
  fallbackLng: 'uz',
  supportedLngs: [...supportedLanguages],
  defaultNS: 'common',
  ns: [
    'common',
    'auth',
    'dashboard',
    'students',
    'teachers',
    'courses',
    'groups',
    'payments',
    'parents',
    'notifications',
  ],
  interpolation: {
    escapeValue: false,
  },
});

i18n.on('languageChanged', (language) => {
  if (typeof document !== 'undefined' && isAppLanguage(language)) {
    document.documentElement.lang = localeByLanguage[language];
  }
});

export async function setUserPreferredLanguage(language: string): Promise<void> {
  if (!isAppLanguage(language)) {
    throw new Error('Unsupported EduPilot language');
  }
  await i18n.changeLanguage(language);

  const storage = getStorage();
  if (storage) {
    storage.setItem(languageStorageKey, language);
  }
}

export function getAppLanguage(language: string | undefined = i18n.language): AppLanguage {
  return language?.startsWith('ru') ? 'ru' : 'uz';
}

export default i18n;
