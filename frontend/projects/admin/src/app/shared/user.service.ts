import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, filter, map, Observable, of } from 'rxjs';
import { User, UserInfo} from '../model/user';
import { Role } from '../model/role';
import { ApiKeyBulk } from '../model/apikey-bulk';

@Injectable()
export class UserService {
  constructor(private httpClient: HttpClient) {}

  public checkUserLoggedIn(): Observable<boolean> {
    return this.httpClient
      .get<{ authenticated: boolean }>('/authentication-status', {
        observe: 'response',
      })
      .pipe(
        map((resp) => {
          return resp.status === 200 && (resp.body?.authenticated || false);
        }),
        catchError((err) => {
          console.log('error!', err);
          return of(false);
        })
      );
  }

  getCurrent(): Observable<UserInfo> {
    return this.httpClient.get<UserInfo>('/admin/api/users/current');
  }

  getAllUsers(): Observable<User[]> {
    return this.getAll().pipe(
      map((users) => users.filter((user) => user.type !== 'API_KEY'))
    );
  }

  getAllApiKey(): Observable<User[]> {
    return this.getAll().pipe(
      map((users) => users.filter((user) => user.type === 'API_KEY'))
    );
  }

  getAll(): Observable<User[]> {
    return this.httpClient.get<User[]>('/admin/api/users');
  }

  enable(user: User, enabled: boolean): Observable<string> {
    return this.httpClient.post<string>(
      `/admin/api/users/${user.id}/enable/${enabled}`,
      {}
    );
  }

  deleteUser(user: User): Observable<string> {
    return this.httpClient.delete<string>(`/admin/api/users/${user.id}`);
  }

  getAllRoles(): Observable<Role[]> {
    return this.httpClient.get<Role[]>('/admin/api/roles');
  }

  create(user: User): Observable<any> {
    return this.httpClient.post<any>('/admin/api/users/new', user, {
      params: { baseUrl: window.location.origin },
    });
  }

  getUser(id: number | string) : Observable<User>{
    return this.httpClient.get<User>(`/admin/api/users/${id}`);
  }

  update(user: User) : Observable<any>{
    return this.httpClient.post<any>('/admin/api/users/edit', user);
  }

  createApiBulk(apiKeyBulk: ApiKeyBulk): Observable<any>{
    return this.httpClient.post<any>('/admin/api/api-keys/bulk', apiKeyBulk);
  }
}
