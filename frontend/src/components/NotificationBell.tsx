import { useEffect, useState } from 'react';
import { api } from '../api';
type Notice={id:number;type:string;title:string;message:string;createdAt:string;readAt:string|null};
export default function NotificationBell({token,centerId}:{token:string;centerId:number}){
 const [items,setItems]=useState<Notice[]>([]);const [open,setOpen]=useState(false);const [error,setError]=useState('');
 async function load(){try{setItems(await api<Notice[]>('/notifications',{token,centerId}));setError('');}catch(e){setError(e instanceof Error?e.message:'Bildirishnomalar yuklanmadi');}}
 useEffect(()=>{void load();},[token,centerId]);
 async function markRead(id:number){try{await api<Notice>('/notifications/'+id+'/read',{method:'PATCH',token,centerId});await load();}catch(e){setError(e instanceof Error?e.message:'Yangilash amalga oshmadi');}}
 const unread=items.filter(n=>!n.readAt).length;
 return <div className="notification-wrap"><button className="secondary-button" onClick={()=>setOpen(v=>!v)} aria-expanded={open}>Bildirishnomalar{unread>0?' · '+unread:''}</button>{open&&<section className="notification-popover"><div className="panel-title"><h2>Bildirishnomalar</h2><button className="secondary-button" onClick={()=>void load()}>Yangilash</button></div>{error&&<p className="error-message">{error}</p>}{items.length===0?<p className="empty-state">Yangi bildirishnoma yo‘q.</p>:items.map(n=><article className={n.readAt?'notice read':'notice'} key={n.id}><strong>{n.title}</strong><p>{n.message}</p><small>{new Date(n.createdAt).toLocaleString()}</small>{!n.readAt&&<button className="secondary-button" onClick={()=>void markRead(n.id)}>O‘qildi deb belgilash</button>}</article>)}</section>}</div>;
}
