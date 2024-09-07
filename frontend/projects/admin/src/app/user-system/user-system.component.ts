import { Component, OnInit } from '@angular/core';
import { Observable, map, of, share, shareReplay } from 'rxjs';
import { UserService } from '../shared/user.service';
import { RoleType, User, UserType } from '../model/user';
import { Role } from '../model/role';
import { Organization } from '../model/organization';

@Component({
  selector: 'app-user-system',
  templateUrl: './user-system.component.html',
  styleUrls: ['./user-system.component.scss'],
})
export class UserSystemComponent implements OnInit {
  public users$?: Observable<User[]>;
  public roles$: Observable<Map<RoleType, Role>> = of();
  public organizations$?: Observable<Organization[]>;
  public selectedOrganization?: Organization;

  constructor(private readonly userService: UserService) {}

  ngOnInit(): void {
    this.loadUsers();
    this.roles$ = this.userService.getAllRoles().pipe(
      map((roles) => {
        const map: Map<RoleType, Role> = new Map();
        roles.forEach((role) => {
          map.set(role.role, role);
        });
        return map;
      }),
      shareReplay(1)
    );
  }

  private loadUsers(){
    this.users$ = this.userService.getAllUsers();
    this.users$.subscribe((users) => {
      const orgIdToOrg = new Map<number, Organization>();
      users.forEach((user) => {
        user.memberOf.forEach((org) => {
          orgIdToOrg.set(org.id, org);
        });
      });

      const organizations = Array.from(orgIdToOrg.values());
      this.organizations$ = of(organizations);
    });

  }

  enable(user: User) {
    this.userService.enable(user, !user.enabled).subscribe((result) => {
      this.users$ = this.userService.getAllUsers();
    });
  }

  deleteUser(user: User) {
    if (
      window.confirm(`The user ${user.username} will be deleted. Are you sure?`)
    ) {
      this.userService.deleteUser(user).subscribe((result) => {
        this.users$ = this.userService.getAllUsers();
      });
    }
  }

  roleDescription(role: RoleType): Observable<string | undefined> {
    return this.roles$.pipe(map((roles) => roles.get(role)?.description));
  }
}
