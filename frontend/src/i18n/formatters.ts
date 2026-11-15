import { getAppLanguage, localeByLanguage } from './index';

function getLocale(language?: string): string {
  return localeByLanguage[getAppLanguage(language)];
}

export function formatDate(value: Date | number, language?: string): string {
  return new Intl.DateTimeFormat(getLocale(language), { dateStyle: 'medium' }).format(value);
}

export function formatTime(value: Date | number, language?: string): string {
  return new Intl.DateTimeFormat(getLocale(language), { timeStyle: 'short' }).format(value);
}

export function formatNumber(value: number, language?: string): string {
  return new Intl.NumberFormat(getLocale(language)).format(value);
}

export function formatCurrency(value: number, language?: string): string {
  return new Intl.NumberFormat(getLocale(language), {
    style: 'currency',
    currency: 'UZS',
    maximumFractionDigits: 0,
  }).format(value);
}
