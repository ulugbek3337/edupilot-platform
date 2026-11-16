import { useEffect, useState } from 'react';
import { api } from '../api';

type Props = { token: string; centerId: number; forced: boolean; onPasswordChanged: () => void };
type Profile = { id: number; teacherCode: string; firstName: string; lastName: string; specialization?: string };
type Group = { id: number; name: string; groupCode: string; courseName: string };
type Lesson = { id: number; groupId: number; title: string; startsAt: string; endsAt: string; status: string };
type Student = { id: number; studentNumber: string; firstName: string; lastName: string };
type Attendance = { id: number; studentId: number; studentName: string; status: string; note?: string };

export default function TeacherWorkspace({ token, centerId, forced, onPasswordChanged }: Props) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [groups, setGroups] = useState<Group[]>([]);
  const [groupId, setGroupId] = useState(0);
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [lessonId, setLessonId] = useState(0);
  const [students, setStudents] = useState<Student[]>([]);
  const [attendance, setAttendance] = useState<Attendance[]>([]);
  const [studentId, setStudentId] = useState(0);
  const [status, setStatus] = useState('PRESENT');
  const [attendanceNote, setAttendanceNote] = useState('');
  const [gradeTitle, setGradeTitle] = useState('');
  const [score, setScore] = useState('');
  const [maxScore, setMaxScore] = useState('100');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [workspaceLoading, setWorkspaceLoading] = useState(true);
  const [groupLoading, setGroupLoading] = useState(false);
  const [attendanceLoading, setAttendanceLoading] = useState(false);

  useEffect(() => {
    if (forced) { setWorkspaceLoading(false); return; }
    let cancelled = false;
    setWorkspaceLoading(true);
    setError('');
    Promise.all([
      api<Profile>('/teacher-portal/me', { token, centerId }),
      api<Group[]>('/teacher-portal/groups', { token, centerId })
    ]).then(([p, rows]) => {
      if (cancelled) return;
      setProfile(p);
      setGroups(rows);
      setGroupId(rows[0]?.id ?? 0);
    }).catch(e => {
      if (!cancelled) setError(e instanceof Error ? e.message : 'O‘qituvchi ma’lumotlari yuklanmadi');
    }).finally(() => { if (!cancelled) setWorkspaceLoading(false); });
    return () => { cancelled = true; };
  }, [token, centerId, forced]);

  useEffect(() => {
    if (!groupId || forced) {
      setLessons([]);
      setStudents([]);
      setLessonId(0);
      setStudentId(0);
      setGroupLoading(false);
      return;
    }
    let cancelled = false;
    setGroupLoading(true);
    setError('');
    setLessons([]);
    setStudents([]);
    setLessonId(0);
    setStudentId(0);
    Promise.all([
      api<Lesson[]>(`/teacher-portal/groups/${groupId}/lessons`, { token, centerId }),
      api<Student[]>(`/teacher-portal/groups/${groupId}/students`, { token, centerId })
    ]).then(([lessonRows, studentRows]) => {
      if (cancelled) return;
      setLessons(lessonRows);
      setStudents(studentRows);
      setLessonId(lessonRows[0]?.id ?? 0);
      setStudentId(studentRows[0]?.id ?? 0);
    }).catch(e => {
      if (!cancelled) setError(e instanceof Error ? e.message : 'Guruh ma’lumotlari yuklanmadi');
    }).finally(() => { if (!cancelled) setGroupLoading(false); });
    return () => { cancelled = true; };
  }, [groupId, token, centerId, forced]);

  useEffect(() => {
    if (!lessonId || forced) {
      setAttendance([]);
      setAttendanceLoading(false);
      return;
    }
    let cancelled = false;
    setAttendanceLoading(true);
    setError('');
    api<Attendance[]>(`/teacher-portal/lessons/${lessonId}/attendance`, { token, centerId })
      .then(rows => { if (!cancelled) setAttendance(rows); })
      .catch(e => { if (!cancelled) setError(e instanceof Error ? e.message : 'Davomat yuklanmadi'); })
      .finally(() => { if (!cancelled) setAttendanceLoading(false); });
    return () => { cancelled = true; };
  }, [lessonId, token, centerId, forced]);

  async function changePassword(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setNotice('');
    if (newPassword.length < 8 || newPassword !== confirmPassword) {
      setError('Parol kamida 8 belgi bo‘lsin va tasdiqlash bilan mos kelsin');
      return;
    }
    setBusy(true);
    try {
      await api('/auth/change-password', { method: 'POST', body: { currentPassword, newPassword }, token });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setNotice('Parol yangilandi');
      onPasswordChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Parolni yangilab bo‘lmadi');
    } finally {
      setBusy(false);
    }
  }

  async function markAttendance(e: React.FormEvent) {
    e.preventDefault();
    if (!lessonId || !studentId) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      await api('/teacher-portal/attendance', {
        method: 'POST', token, centerId,
        body: { lessonId, studentId, status, note: attendanceNote.trim() || undefined }
      });
      const rows = await api<Attendance[]>(`/teacher-portal/lessons/${lessonId}/attendance`, { token, centerId });
      setAttendance(rows);
      setNotice('Davomat saqlandi');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Davomat saqlanmadi');
    } finally {
      setBusy(false);
    }
  }

  async function addGrade(e: React.FormEvent) {
    e.preventDefault();
    const numericScore = Number(score);
    const numericMax = Number(maxScore);
    if (!studentId || !groupId || !gradeTitle.trim() || !Number.isFinite(numericScore) ||
        !Number.isFinite(numericMax) || numericScore < 0 || numericMax <= 0 || numericScore > numericMax) {
      setError('Baho 0 dan kam bo‘lmasin va maksimal balldan oshmasin');
      return;
    }
    setBusy(true);
    setError('');
    setNotice('');
    try {
      await api('/teacher-portal/grades', {
        method: 'POST', token, centerId,
        body: { studentId, groupId, title: gradeTitle.trim(), score: numericScore, maxScore: numericMax }
      });
      setGradeTitle('');
      setScore('');
      setNotice('Baho saqlandi');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Bahoni saqlab bo‘lmadi');
    } finally {
      setBusy(false);
    }
  }

  return <section className="workspace" aria-busy={workspaceLoading || groupLoading}>
    <div className="workspace-heading">
      <div>
        <p className="eyebrow">EduPilot · Ustozlar uchun</p>
        <h1>{forced ? 'Birinchi kirish: parolni almashtiring' : `Assalomu alaykum, ${profile ? profile.firstName + ' ' + profile.lastName : 'ustoz'}!`}</h1>
      </div>
      <span className="tenant-pill">Markaz #{centerId}</span>
    </div>
    {forced ? <section className="panel">
      <h2>Vaqtinchalik parolni yangilang</h2>
      <p>Davom etishdan oldin administrator bergan vaqtinchalik parolni almashtiring.</p>
      {error && <p className="error-message" role="alert">{error}</p>}
      <form className="data-form" onSubmit={changePassword}>
        <label>Joriy parol<input type="password" required autoComplete="current-password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} /></label>
        <label>Yangi parol<input type="password" required minLength={8} autoComplete="new-password" value={newPassword} onChange={e => setNewPassword(e.target.value)} /></label>
        <label>Parolni takrorlang<input type="password" required minLength={8} autoComplete="new-password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} /></label>
        <button className="primary-button" disabled={busy}>{busy ? 'Saqlanmoqda…' : 'Parolni yangilash'}</button>
      </form>
    </section> : <>
      {error && <p className="error-message" role="alert">{error}</p>}
      {notice && <p className="success-message" role="status">{notice}</p>}
      {workspaceLoading ? <section className="panel"><p className="empty-state" role="status">Kabinet ma’lumotlari yuklanmoqda…</p></section> : <>
        <section className="panel teacher-toolbar">
          <div><p className="eyebrow">Shaxsiy kabinet</p><h2>{profile?.specialization || 'O‘qituvchi'}</h2><p>O‘qituvchi kodi: {profile?.teacherCode}</p></div>
          <label>Guruhni tanlang
            <select value={groupId} onChange={e => setGroupId(Number(e.target.value))} disabled={groupLoading || groups.length === 0}>
              <option value={0}>Guruh yo‘q</option>
              {groups.map(g => <option key={g.id} value={g.id}>{g.name} · {g.courseName}</option>)}
            </select>
          </label>
        </section>
        {groupLoading ? <section className="panel"><p className="empty-state" role="status">Guruh ma’lumotlari yuklanmoqda…</p></section> : <div className="teacher-grid">
          <section className="panel">
            <p className="eyebrow">Dars jadvali</p><h2>Guruh darslari</h2>
            {lessons.length ? <div className="record-list">{lessons.map(l =>
              <button type="button" className={lessonId === l.id ? 'teacher-lesson active' : 'teacher-lesson'} key={l.id} onClick={() => setLessonId(l.id)} aria-pressed={lessonId === l.id}>
                <strong>{l.title}</strong><span>{new Date(l.startsAt).toLocaleString('uz-UZ')}</span><small>{l.status}</small>
              </button>
            )}</div> : <p className="empty-state">Tanlangan guruhda darslar mavjud emas.</p>}
          </section>
          <section className="panel">
            <p className="eyebrow">O‘quvchilar</p><h2>Guruh ro‘yxati · {students.length}</h2>
            {students.length ? <div className="record-list">{students.map(s =>
              <article className="record" key={s.id}><strong>{s.firstName} {s.lastName}</strong><span>{s.studentNumber}</span></article>
            )}</div> : <p className="empty-state">Guruhda o‘quvchilar yo‘q.</p>}
          </section>
          <section className="panel">
            <p className="eyebrow">Davomat</p><h2>Darsga qatnashuv</h2>
            {!lessonId ? <p className="empty-state">Davomat belgilash uchun avval dars qo‘shilishi kerak.</p> : <form className="data-form" onSubmit={markAttendance}>
              <label>O‘quvchi<select required value={studentId} onChange={e => setStudentId(Number(e.target.value))} disabled={!students.length}>
                {students.map(s => <option key={s.id} value={s.id}>{s.firstName} {s.lastName}</option>)}
              </select></label>
              <label>Holat<select value={status} onChange={e => setStatus(e.target.value)}>
                <option value="PRESENT">Qatnashdi</option><option value="ABSENT">Kelmagan</option><option value="LATE">Kechikdi</option><option value="EXCUSED">Sababli</option>
              </select></label>
              <label>Izoh (ixtiyoriy)<input maxLength={1000} value={attendanceNote} onChange={e => setAttendanceNote(e.target.value)} /></label>
              <button className="primary-button" disabled={busy || !studentId || !students.length}>{busy ? 'Saqlanmoqda…' : 'Davomatni saqlash'}</button>
            </form>}
            {attendanceLoading ? <p className="empty-state" role="status">Davomat yuklanmoqda…</p> : <div className="record-list">
              {attendance.map(a => <article className="record" key={a.id}><strong>{a.studentName}</strong><span>{a.status}{a.note ? ` · ${a.note}` : ''}</span></article>)}
              {!attendance.length && lessonId > 0 && <p className="empty-state">Hozircha davomat belgilanmagan.</p>}
            </div>}
          </section>
          <section className="panel">
            <p className="eyebrow">Baholash</p><h2>O‘quvchiga baho qo‘yish</h2>
            <form className="data-form" onSubmit={addGrade}>
              <label>O‘quvchi<select required value={studentId} onChange={e => setStudentId(Number(e.target.value))} disabled={!students.length}>
                {students.map(s => <option key={s.id} value={s.id}>{s.firstName} {s.lastName}</option>)}
              </select></label>
              <label>Topshiriq nomi<input required maxLength={180} value={gradeTitle} onChange={e => setGradeTitle(e.target.value)} /></label>
              <label>Ball<input type="number" min="0" step="0.01" required value={score} onChange={e => setScore(e.target.value)} /></label>
              <label>Maksimal ball<input type="number" min="0.01" step="0.01" required value={maxScore} onChange={e => setMaxScore(e.target.value)} /></label>
              <button className="primary-button" disabled={busy || !studentId || !students.length || !groupId}>{busy ? 'Saqlanmoqda…' : 'Bahoni saqlash'}</button>
            </form>
          </section>
        </div>}
      </>}
    </>}
  </section>;
}
