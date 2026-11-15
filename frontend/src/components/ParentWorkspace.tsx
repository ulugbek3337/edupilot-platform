import { useEffect, useState } from 'react';
import { api } from '../api';
type Props={token:string;centerId:number};
type Child={id:number;studentNumber:string;firstName:string;lastName:string};
type Grade={id:number;title:string;score:number;maxScore:number;comment?:string|null;createdAt:string};
type Attendance={id:number;lessonId:number;lessonTitle:string;status:string;note?:string|null;createdAt:string};
export default function ParentWorkspace({token,centerId}:Props){
 const [children,setChildren]=useState<Child[]>([]);const [selected,setSelected]=useState<number>(0);const [grades,setGrades]=useState<Grade[]>([]);const [attendance,setAttendance]=useState<Attendance[]>([]);const [error,setError]=useState('');
 useEffect(()=>{api<Child[]>('/parents/children',{token,centerId}).then(rows=>{setChildren(rows);setSelected(rows[0]?.id??0);}).catch(e=>setError(e instanceof Error?e.message:'Farzandlar ro‘yxatini yuklab bo‘lmadi'));},[token,centerId]);
 useEffect(()=>{if(!selected){setGrades([]);setAttendance([]);return;}Promise.all([api<Grade[]>('/parents/children/'+selected+'/grades',{token,centerId}),api<Attendance[]>('/parents/children/'+selected+'/attendance',{token,centerId})]).then(([g,a])=>{setGrades(g);setAttendance(a);}).catch(e=>setError(e instanceof Error?e.message:'O‘quv ma’lumotlari yuklanmadi'));},[selected,token,centerId]);
 const child=children.find(c=>c.id===selected);
 return <section className="workspace"><div className="workspace-heading"><div><p className="eyebrow">Ota-ona kabineti</p><h1>Farzandimning natijalari</h1></div><span className="tenant-pill">Markaz #{centerId}</span></div>
 {error&&<p className="error-message" role="alert">{error}</p>}
 {children.length===0?<section className="panel"><p className="empty-state">Sizga biriktirilgan o‘quvchi hozircha yo‘q. Markaz administratoridan farzandingizni profilingizga bog‘lashni so‘rang.</p></section>:<>
 <section className="panel parent-child-picker"><label>Farzandni tanlang<select value={selected} onChange={e=>setSelected(Number(e.target.value))}>{children.map(c=><option key={c.id} value={c.id}>{c.firstName} {c.lastName} · {c.studentNumber}</option>)}</select></label>{child&&<p>{child.firstName} {child.lastName}</p>}</section>
 <div className="workspace-grid"><section className="panel"><div className="panel-title"><div><p className="eyebrow">Baholash</p><h2>Oxirgi baholar</h2></div></div>{grades.length?grades.map(g=><article className="record" key={g.id}><strong>{g.title}</strong><span>{g.score}/{g.maxScore}</span><small>{g.comment??''}</small></article>):<p className="empty-state">Baholar yo‘q.</p>}</section>
 <section className="panel"><div className="panel-title"><div><p className="eyebrow">Davomat</p><h2>Darslardagi ishtirok</h2></div></div>{attendance.length?attendance.map(a=><article className="record" key={a.id}><strong>{a.lessonTitle}</strong><span>{a.status}</span><small>{a.note??''}</small></article>):<p className="empty-state">Davomat yozuvlari yo‘q.</p>}</section></div></>}
 </section>;
}
