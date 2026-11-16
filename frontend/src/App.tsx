import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import LanguageSwitcher from './components/LanguageSwitcher';
import AcademicWorkspace from './components/AcademicWorkspace';
import ParentWorkspace from './components/ParentWorkspace';
import StudentWorkspace from './components/StudentWorkspace';
import TeacherWorkspace from './components/TeacherWorkspace';
import NotificationBell from './components/NotificationBell';
import { api, centerKey, tokenKey, type AuthResult, type Center } from './api';

function BrandMark() {
  return <span className="brand-mark" aria-hidden="true"><span>EP</span></span>;
}

function Login({ onLogin }: { onLogin: (result: AuthResult) => void }) {
  const { t } = useTranslation(['common', 'auth']);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const result = await api<AuthResult>('/auth/login', {
        method: 'POST',
        body: { email, password },
      });
      onLogin(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Kirish amalga oshmadi');
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-intro" aria-label="EduPilot haqida">
        <div className="auth-intro-brand"><BrandMark /><span>EduPilot</span></div>
        <p className="eyebrow">Zamonaviy ta’lim boshqaruvi</p>
        <h2>Ta’lim jarayoni —<br />bir joyda, bir tizimda.</h2>
        <p className="auth-intro-copy">
          O‘quv markazingiz, ustozlaringiz, o‘quvchilaringiz va ota-onalar bilan
          ishlash uchun yagona raqamli makon.
        </p>
        <div className="auth-values">
          <span><i aria-hidden="true">✦</i> Milliy qadriyatlar, zamonaviy yondashuv</span>
          <span><i aria-hidden="true">✦</i> Shaffof va tartibli ta’lim jarayoni</span>
          <span><i aria-hidden="true">✦</i> Har bir rol uchun qulay kabinet</span>
        </div>
        <div className="auth-ornament" aria-hidden="true"><span>◇</span><span>✦</span><span>◇</span></div>
      </section>
      <form className="auth-card" onSubmit={submit}>
        <div className="auth-card-heading">
          <p className="eyebrow">Xush kelibsiz</p>
          <h1>{t('title', { ns: 'auth' })}</h1>
          <p>Davom etish uchun hisob ma’lumotlaringizni kiriting.</p>
        </div>
        <label>{t('email', { ns: 'auth' })}
          <input type="email" required value={email} onChange={event => setEmail(event.target.value)} autoComplete="username" />
        </label>
        <label>{t('password', { ns: 'auth' })}
          <input type="password" required value={password} onChange={event => setPassword(event.target.value)} autoComplete="current-password" />
        </label>
        {error && <p className="error-message" role="alert">{error}</p>}
        <button className="primary-button" disabled={busy}>
          {busy ? 'Tekshirilmoqda…' : t('login', { ns: 'auth' })}
        </button>
        <p className="auth-security-note"><span aria-hidden="true">⌑</span> Himoyalangan kirish · EduPilot LMS</p>
      </form>
    </main>
  );
}

export default function App() {
  const { t } = useTranslation(['common', 'auth']);
  const [token, setToken] = useState(() => localStorage.getItem(tokenKey) ?? '');
  const [centers, setCenters] = useState<Center[]>([]);
  const [centerId, setCenterId] = useState<number>(() => Number(localStorage.getItem(centerKey)) || 0);
  const [newCenter, setNewCenter] = useState('');
  const [error, setError] = useState('');
  const [isParent, setIsParent] = useState(false);
  const [isStudent, setIsStudent] = useState(false);
  const [isTeacher, setIsTeacher] = useState(false);
  const [mustChangePassword, setMustChangePassword] = useState(false);
  const [authChecked, setAuthChecked] = useState(false);
  const [userMemberships, setUserMemberships] = useState<Array<{ centerId: number; role: string }>>([]);

  useEffect(() => {
    if (!token) {
      setAuthChecked(true);
      return;
    }
    setAuthChecked(false);
    api<{ mustChangePassword: boolean; memberships: Array<{ centerId: number; role: string }> }>('/auth/me', { token })
      .then(me => {
        setMustChangePassword(me.mustChangePassword);
        setUserMemberships(me.memberships ?? []);
      })
      .catch(() => setMustChangePassword(false))
      .finally(() => setAuthChecked(true));
  }, [token]);

  useEffect(() => {
    if (!token) return;
    api<Center[]>('/centers', { token })
      .then(list => {
        setCenters(list);
        if (!list.some(center => center.id === centerId)) setCenterId(list[0]?.id ?? 0);
      })
      .catch(err => setError(err instanceof Error ? err.message : 'Markazlarni yuklab bo‘lmadi'));
  }, [token]);

  useEffect(() => {
    if (!token || !centerId || !authChecked) {
      setIsParent(false); setIsStudent(false); setIsTeacher(false);
      return;
    }
    const roles = userMemberships.filter(m => m.centerId === centerId).map(m => m.role);
    setIsTeacher(roles.includes('TEACHER'));
    setIsStudent(roles.includes('STUDENT'));
    setIsParent(roles.includes('PARENT'));
  }, [token, centerId, authChecked, userMemberships]);

  function login(result: AuthResult) {
    localStorage.setItem(tokenKey, result.accessToken);
    setMustChangePassword(result.mustChangePassword);
    setIsParent(false); setIsStudent(false); setIsTeacher(false);
    setToken(result.accessToken);
  }

  async function createCenter(event: React.FormEvent) {
    event.preventDefault();
    setError('');
    try {
      const center = await api<Center>(`/centers?name=${encodeURIComponent(newCenter)}`, { method: 'POST', token });
      setCenters(items => [...items, center]);
      setCenterId(center.id);
      localStorage.setItem(centerKey, String(center.id));
      setNewCenter('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Markaz yaratilmadi');
    }
  }

  function chooseCenter(id: number) {
    setCenterId(id);
    localStorage.setItem(centerKey, String(id));
  }

  function logout() {
    localStorage.removeItem(tokenKey);
    localStorage.removeItem(centerKey);
    setToken('');
    setCenters([]);
    setCenterId(0);
  }

  const activeCenter = centers.find(center => center.id === centerId);

  return (
    <div className="app-shell">
      <header className="site-header">
        <a className="brand" href="/" aria-label="EduPilot bosh sahifa">
          <BrandMark />
          <span className="brand-copy"><strong>EduPilot</strong><small>TA’LIM BOSHQARUV TIZIMI</small></span>
        </a>
        <nav className="main-navigation" aria-label="Asosiy navigatsiya">
          <a href="/">{t('navigation.home', 'Bosh sahifa')}</a>
          <a href="#workspace">{t('navigation.dashboard', 'Boshqaruv')}</a>
        </nav>
        <div className="header-actions">
          {token && centerId > 0 && !isStudent && !isTeacher && !mustChangePassword && <NotificationBell token={token} centerId={centerId} />}
          <LanguageSwitcher />
          {token && <button className="secondary-button logout-button" onClick={logout}>{t('logout', { ns: 'auth' })}</button>}
        </div>
      </header>

      {!token ? <Login onLogin={login} /> : !authChecked ? (
        <main className="page"><p className="empty-state">Hisob tekshirilmoqda…</p></main>
      ) : (
        <main className="page" id="workspace">
          {error && <p className="error-message" role="alert">{error}</p>}
          {centers.length === 0 ? (
            <section className="auth-card center-create-card">
              <p className="eyebrow">Birinchi qadam</p>
              <h1>O‘quv markazini yaratish</h1>
              <p>Hisobingizga biriktirilgan markaz topilmadi. Ishni boshlash uchun markaz nomini kiriting.</p>
              <form onSubmit={createCenter}>
                <label>Markaz nomi<input required value={newCenter} onChange={event => setNewCenter(event.target.value)} /></label>
                <button className="primary-button">Markaz yaratish</button>
              </form>
            </section>
          ) : (
            <>
              <div className="center-toolbar">
                <label>Faol markaz
                  <select value={centerId} onChange={event => chooseCenter(Number(event.target.value))}>
                    {centers.map(center => <option key={center.id} value={center.id}>{center.name}</option>)}
                  </select>
                </label>
                <span>{centers.length} ta markaz</span>
              </div>
              {activeCenter && (
                isTeacher
                  ? <TeacherWorkspace token={token} centerId={centerId} forced={mustChangePassword} onPasswordChanged={() => setMustChangePassword(false)} />
                  : mustChangePassword || isStudent
                    ? <StudentWorkspace token={token} centerId={centerId} forced={mustChangePassword} onPasswordChanged={() => setMustChangePassword(false)} />
                    : isParent
                      ? <ParentWorkspace token={token} centerId={centerId} />
                      : <AcademicWorkspace token={token} center={activeCenter} />
              )}
            </>
          )}
        </main>
      )}
      <footer className="site-footer"><span>EduPilot</span><span>Ta’lim kelajagi — bilim va qadriyatlar uyg‘unligida</span></footer>
    </div>
  );
}
