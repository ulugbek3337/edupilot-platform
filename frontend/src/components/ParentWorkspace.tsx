import { useEffect, useState } from 'react';
import { api } from '../api';

type Props = { token: string; centerId: number };
type Child = { id: number; studentNumber: string; firstName: string; lastName: string };
type Grade = { id: number; title: string; score: number; maxScore: number; comment?: string | null; createdAt: string };
type Attendance = { id: number; lessonId: number; lessonTitle: string; status: string; note?: string | null; createdAt: string };

export default function ParentWorkspace({ token, centerId }: Props) {
  const [children, setChildren] = useState<Child[]>([]);
  const [selected, setSelected] = useState(0);
  const [grades, setGrades] = useState<Grade[]>([]);
  const [attendance, setAttendance] = useState<Attendance[]>([]);
  const [error, setError] = useState('');
  const [childrenLoading, setChildrenLoading] = useState(true);
  const [recordsLoading, setRecordsLoading] = useState(false);

  useEffect(() => {
    let active = true;
    setChildrenLoading(true);
    setError('');
    setChildren([]);
    setSelected(0);
    setGrades([]);
    setAttendance([]);

    api<Child[]>('/parents/children', { token, centerId })
      .then(rows => {
        if (!active) return;
        setChildren(rows);
        setSelected(rows[0]?.id ?? 0);
      })
      .catch(e => {
        if (active) setError(e instanceof Error ? e.message : 'Farzandlar ro‘yxatini yuklab bo‘lmadi');
      })
      .finally(() => {
        if (active) setChildrenLoading(false);
      });

    return () => { active = false; };
  }, [token, centerId]);

  useEffect(() => {
    let active = true;
    setGrades([]);
    setAttendance([]);
    setError('');

    if (!selected) {
      setRecordsLoading(false);
      return () => { active = false; };
    }

    setRecordsLoading(true);
    Promise.all([
      api<Grade[]>('/parents/children/' + selected + '/grades', { token, centerId }),
      api<Attendance[]>('/parents/children/' + selected + '/attendance', { token, centerId })
    ])
      .then(([nextGrades, nextAttendance]) => {
        if (!active) return;
        setGrades(nextGrades);
        setAttendance(nextAttendance);
      })
      .catch(e => {
        if (active) setError(e instanceof Error ? e.message : 'O‘quv ma’lumotlari yuklanmadi');
      })
      .finally(() => {
        if (active) setRecordsLoading(false);
      });

    return () => { active = false; };
  }, [selected, token, centerId]);

  const child = children.find(item => item.id === selected);

  return (
    <section className="workspace">
      <div className="workspace-heading">
        <div><p className="eyebrow">Ota-ona kabineti</p><h1>Farzandimning natijalari</h1></div>
        <span className="tenant-pill">Markaz #{centerId}</span>
      </div>
      {error && <p className="error-message" role="alert">{error}</p>}
      {childrenLoading ? (
        <section className="panel"><p className="empty-state" role="status">Farzandlar ro‘yxati yuklanmoqda…</p></section>
      ) : children.length === 0 ? (
        <section className="panel"><p className="empty-state">Sizga biriktirilgan o‘quvchi hozircha yo‘q. Markaz administratoridan farzandingizni profilingizga bog‘lashni so‘rang.</p></section>
      ) : (
        <>
          <section className="panel parent-child-picker">
            <label>Farzandni tanlang
              <select value={selected} onChange={event => setSelected(Number(event.target.value))}>
                {children.map(item => <option key={item.id} value={item.id}>{item.firstName} {item.lastName} · {item.studentNumber}</option>)}
              </select>
            </label>
            {child && <p>{child.firstName} {child.lastName}</p>}
          </section>
          {recordsLoading ? (
            <section className="panel"><p className="empty-state" role="status">Baholar va davomat yuklanmoqda…</p></section>
          ) : (
            <div className="workspace-grid">
              <section className="panel">
                <div className="panel-title"><div><p className="eyebrow">Baholash</p><h2>Oxirgi baholar</h2></div></div>
                {grades.length ? grades.map(grade => (
                  <article className="record" key={grade.id}>
                    <strong>{grade.title}</strong><span>{grade.score}/{grade.maxScore}</span><small>{grade.comment ?? ''}</small>
                  </article>
                )) : <p className="empty-state">Baholar yo‘q.</p>}
              </section>
              <section className="panel">
                <div className="panel-title"><div><p className="eyebrow">Davomat</p><h2>Darslardagi ishtirok</h2></div></div>
                {attendance.length ? attendance.map(record => (
                  <article className="record" key={record.id}>
                    <strong>{record.lessonTitle}</strong><span>{record.status}</span><small>{record.note ?? ''}</small>
                  </article>
                )) : <p className="empty-state">Davomat yozuvlari yo‘q.</p>}
              </section>
            </div>
          )}
        </>
      )}
    </section>
  );
}
