import { Link, Route, Routes } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import LanguageSwitcher from './components/LanguageSwitcher';

function HomePage() {
  const { t } = useTranslation('dashboard');
  return (
    <main className="page">
      <section className="summary-grid">
        <Card title={t('summary.centers')} value="12" />
        <Card title={t('summary.teachers')} value="86" />
        <Card title={t('summary.students')} value="1 320" />
        <Card title={t('summary.attendance')} value="94%" />
      </section>

      <section className="intro-panel">
        <h2>{t('description')}</h2>
        <p>{t('foundation')}</p>
      </section>
    </main>
  );
}

function DashboardPage() {
  const { t } = useTranslation('dashboard');
  return (
    <main className="page">
      <h1>{t('title')}</h1>
      <section className="dashboard-panel">
        <p>{t('welcome')}</p>
        <ul>
          <li>{t('tasks.teachers')}</li>
          <li>{t('tasks.attendance')}</li>
          <li>{t('tasks.grades')}</li>
          <li>{t('tasks.payments')}</li>
        </ul>
      </section>
    </main>
  );
}

function Card({ title, value }: { title: string; value: string }) {
  return (
    <article className="summary-card">
      <p>{title}</p>
      <h2>{value}</h2>
    </article>
  );
}

export default function App() {
  const { t } = useTranslation('common');
  return (
    <>
      <header className="site-header">
        <Link className="brand" to="/">{t('appName')}</Link>
        <nav className="main-navigation" aria-label={t('navigation.mainMenu')}>
          <Link to="/">{t('navigation.home')}</Link>
          <Link to="/dashboard">{t('navigation.dashboard')}</Link>
        </nav>
        <LanguageSwitcher />
      </header>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
      </Routes>
    </>
  );
}
