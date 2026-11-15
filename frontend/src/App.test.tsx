import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import App from './App';
import i18n, { languageStorageKey } from './i18n';

afterEach(async () => {
  cleanup();
  window.localStorage.clear();
  await i18n.changeLanguage('uz');
});

describe('language switching', () => {
  it('switches visible navigation and page text and remembers the choice', async () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Ko‘p markazli LMS uchun boshlang‘ich platforma' })).toBeTruthy();
    fireEvent.change(screen.getByRole('combobox', { name: 'Til' }), { target: { value: 'ru' } });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Основа LMS для нескольких учебных центров' })).toBeTruthy();
      expect(screen.getByRole('link', { name: 'Главная' })).toBeTruthy();
    });
    expect(window.localStorage.getItem(languageStorageKey)).toBe('ru');
    expect(document.documentElement.lang).toBe('ru-RU');
  });
});
