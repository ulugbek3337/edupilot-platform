import { useEffect, useState } from 'react';
import { api } from '../api';

type Props={token:string;centerId:number;forced:boolean;onPasswordChanged:()=>void};
type StudentProfile={id:number;studentNumber:string;firstName:string;lastName:string;groupName:string;teacherName:string};

export default function StudentWorkspace({token,centerId,forced,onPasswordChanged}:Props){
 const [profile,setProfile]=useState<StudentProfile|null>(null);
 const [currentPassword,setCurrentPassword]=useState('');
 const [newPassword,setNewPassword]=useState('');
 const [confirmPassword,setConfirmPassword]=useState('');
 const [busy,setBusy]=useState(false);
 const [error,setError]=useState('');
 const [notice,setNotice]=useState('');

 useEffect(()=>{if(forced)return;api<StudentProfile>('/student-portal/me',{token,centerId}).then(setProfile).catch(e=>setError(e instanceof Error?e.message:'O‘quvchi ma’lumotlari yuklanmadi'));},[token,centerId,forced]);

 async function changePassword(e:React.FormEvent){
  e.preventDefault();setError('');setNotice('');
  if(newPassword.length<8){setError('Yangi parol kamida 8 ta belgidan iborat bo‘lsin');return;}
  if(newPassword!==confirmPassword){setError('Yangi parollar mos kelmadi');return;}
  setBusy(true);
  try{await api<void>('/auth/change-password',{method:'POST',body:{currentPassword,newPassword},token});setCurrentPassword('');setNewPassword('');setConfirmPassword('');setNotice('Parol muvaffaqiyatli o‘zgartirildi');onPasswordChanged();}
  catch(e){setError(e instanceof Error?e.message:'Parolni o‘zgartirib bo‘lmadi');}
  finally{setBusy(false);}
 }
 return <section className="workspace">
  <div className="workspace-heading"><div><p className="eyebrow">EduPilot o‘quvchi kabineti</p><h1>{forced?'Birinchi kirish: parolni almashtiring':'Mening o‘quv kabinetim'}</h1></div><span className="tenant-pill">Markaz #{centerId}</span></div>
  {forced?<section className="panel"><h2>Vaqtinchalik parolni yangilang</h2><p>Xavfsizlik uchun administrator bergan vaqtinchalik parolni o‘zingiz biladigan yangi parolga almashtiring. Parolni almashtirmaguningizcha boshqa bo‘limlar ochilmaydi.</p></section>:profile&&<section className="panel"><p className="eyebrow">Shaxsiy ma’lumotlar</p><h2>{profile.firstName} {profile.lastName}</h2><div className="record-list"><article className="record"><strong>O‘quvchi kodi</strong><span>{profile.studentNumber}</span></article><article className="record"><strong>Guruh</strong><span>{profile.groupName}</span></article><article className="record"><strong>O‘qituvchi</strong><span>{profile.teacherName}</span></article></div><p className="empty-state">Profil va o‘quv ma’lumotlarini o‘quvchi o‘zgartira olmaydi. Zarur o‘zgarishlar uchun markaz administratoriga murojaat qiling.</p></section>}
  <section className="panel"><div className="panel-title"><div><p className="eyebrow">Hisob xavfsizligi</p><h2>Parolni {forced?'almashtirish':'o‘zgartirish'}</h2></div></div>
   <form className="data-form" onSubmit={changePassword}><label>Joriy parol<input type="password" autoComplete="current-password" required value={currentPassword} onChange={e=>setCurrentPassword(e.target.value)}/></label><label>Yangi parol<input type="password" autoComplete="new-password" minLength={8} required value={newPassword} onChange={e=>setNewPassword(e.target.value)}/></label><label>Yangi parolni takrorlang<input type="password" autoComplete="new-password" minLength={8} required value={confirmPassword} onChange={e=>setConfirmPassword(e.target.value)}/></label><button className="primary-button" disabled={busy}>{busy?'Saqlanmoqda…':'Parolni saqlash'}</button></form>
   {notice&&<p className="success-message" role="status">{notice}</p>}{error&&<p className="error-message" role="alert">{error}</p>}
  </section>
 </section>;
}
