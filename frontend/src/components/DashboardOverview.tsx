import { useEffect, useState } from 'react';
import { api, type Center } from '../api';

type Props = { token: string; center: Center; onNavigate: (section: string) => void };
type Metric = { label: string; value: number | null; hint: string; icon: string };

export default function DashboardOverview({ token, center, onNavigate }: Props) {
  const [metrics, setMetrics] = useState<Metric[]>([
    { label: 'O‘quvchilar', value: null, hint: 'Faol o‘quvchilar ro‘yxati', icon: '◈' },
    { label: 'O‘qituvchilar', value: null, hint: 'Markaz jamoasi', icon: '✧' },
    { label: 'Kurslar', value: null, hint: 'O‘quv yo‘nalishlari', icon: '▤' },
    { label: 'Guruhlar', value: null, hint: 'Tashkil etilgan guruhlar', icon: '⌘' },
  ]);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    const endpoints = ['/students', '/teachers', '/courses', '/groups'];
    Promise.all(endpoints.map(path => api<unknown[]>(path, { token, centerId: center.id })))
      .then(results => {
        if (cancelled) return;
        setMetrics(current => current.map((metric, index) => ({
          ...metric,
          value: results[index].length,
          hint: ['Ro‘yxatdagi o‘quvchilar', 'Markazdagi ustozlar', 'Faol o‘quv yo‘nalishlari', 'Tashkil etilgan guruhlar'][index],
        })));
      })
      .catch(reason => {
        if (!cancelled) setError(reason instanceof Error ? reason.message : 'Ko‘rsatkichlarni yuklab bo‘lmadi');
      });
    return () => { cancelled = true; };
  }, [token, center.id]);

  const shortcuts = [
    { title: 'O‘quvchini qabul qilish', description: 'Yangi o‘quvchini guruhga biriktiring va login yarating', section: 'admissions', icon: '+' },
    { title: 'Guruhlarni boshqarish', description: 'Guruhlar, kurslar va mas’ul o‘qituvchilar', section: 'groups', icon: '⌘' },
    { title: 'Davomatni yuritish', description: 'Darslar bo‘yicha qatnashuvni qayd eting', section: 'attendance', icon: '✓' },
    { title: 'Baholash', description: 'O‘quvchilar natijalarini kiriting va kuzating', section: 'grades', icon: '★' },
  ];

  return <section className="dashboard-overview">
    <div className="dashboard-welcome">
      <div><p className="eyebrow">Boshqaruv paneli · {center.name}</p><h2>Assalomu alaykum!</h2><p>Markazingizdagi ta’lim jarayonining bugungi holati.</p></div>
      <span className="dashboard-seal" aria-hidden="true">EP</span>
    </div>
    <div className="metric-grid">
      {metrics.map(metric => <article className="metric-card" key={metric.label}>
        <span className="metric-icon" aria-hidden="true">{metric.icon}</span>
        <span className="metric-label">{metric.label}</span>
        <strong>{metric.value === null ? '—' : metric.value.toLocaleString('uz-UZ')}</strong>
        <small>{metric.hint}</small>
      </article>)}
    </div>
    {error && <p className="error-message" role="alert">Statistikani yuklashda xatolik: {error}</p>}
    <div className="dashboard-section-heading"><div><p className="eyebrow">Tezkor amallar</p><h3>Ishni davom ettiring</h3></div></div>
    <div className="shortcut-grid">
      {shortcuts.map(item => <button type="button" className="shortcut-card" key={item.section} onClick={() => onNavigate(item.section)}>
        <span className="shortcut-icon" aria-hidden="true">{item.icon}</span><strong>{item.title}</strong><small>{item.description}</small><span className="shortcut-arrow" aria-hidden="true">→</span>
      </button>)}
    </div>
    <p className="dashboard-footnote">Ma’lumotlar tanlangan markaz doirasida yuklanadi. Ko‘rsatkichlar yangilanishi uchun sahifani qayta yuklang.</p>
  </section>;
}
