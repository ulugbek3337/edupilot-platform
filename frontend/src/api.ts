const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';
export const tokenKey = 'edupilot.accessToken';
export const centerKey = 'edupilot.centerId';
export type ApiOptions = { method?: string; body?: unknown; token?: string; centerId?: number };
export async function api<T>(path: string, options: ApiOptions = {}): Promise<T> {
 const headers: Record<string,string> = { Accept: 'application/json' };
 if (options.body !== undefined) headers['Content-Type'] = 'application/json';
 if (options.token) headers.Authorization = `Bearer ${options.token}`;
 if (options.centerId) headers['X-Center-Id'] = String(options.centerId);
 const response = await fetch(`${API_BASE}${path}`, { method: options.method ?? 'GET', headers, body: options.body === undefined ? undefined : JSON.stringify(options.body) });
 if (!response.ok) {
  const error = await response.json().catch(() => null) as { message?: string; code?: string } | null;
  throw new Error(error?.message ?? error?.code ?? `HTTP ${response.status}`);
 }
 if (response.status === 204) return undefined as T;
 return response.json() as Promise<T>;
}
export type AuthResult = { accessToken:string; refreshToken:string; userId:number; email:string; firstName:string; lastName:string; preferredLanguage:string };
export type Center = { id:number; name:string; slug:string; status:string };
