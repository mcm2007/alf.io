import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OrganizationService } from '../shared/organization.service';
import { Observable, of } from 'rxjs';
import { Organization } from '../model/organization';
import { ConfigurationService } from '../shared/configuration.service';
import { InstanceSetting } from '../model/instance-settings';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Validators } from '@angular/forms';

@Component({
  selector: 'app-organization-edit',
  templateUrl: './organization-edit.component.html',
  styleUrls: ['./organization-edit.component.scss'],
})
export class OrganizationEditComponent implements OnInit {
  public organizationId: string | null = null;
  public organization$: Observable<Organization | null> = of();
  public editMode: boolean | undefined;
  public instanceSetting$: Observable<InstanceSetting> = of();
  public organizationForm: FormGroup;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly organizationService: OrganizationService,
    private readonly configurationService: ConfigurationService,
    formBuilder: FormBuilder,
    private readonly router: Router
  ) {
    this.organizationForm = formBuilder.group({
      id: [null],
      name: [null, Validators.required],
      email: [null, Validators.required],
      description: [null, Validators.required],
      slug: [],
      externalId: [],
    });
  }

  get organizationName() {
    return this.organizationForm.get('name');
  }
  get organizationEmail() {
    return this.organizationForm.get('email');
  }
  get organizationDescription() {
    return this.organizationForm.get('description');
  }

  ngOnInit(): void {
    this.organizationId = this.route.snapshot.paramMap.get('organizationId');
    this.instanceSetting$ = this.configurationService.loadInstanceSetting();

    if (this.organizationId !== null) {
      this.editMode = true;
      this.organization$ = this.organizationService.getOrganization(
        this.organizationId
      );
      this.organization$.subscribe((org) => {
        if (org) this.organizationForm.patchValue(org);
      });
    } else {
      this.editMode = false;
    }
  }

  save(): void {
    let result: Observable<any>;
    if (this.editMode) {
      result = this.organizationService.update(this.organizationForm.value);
    } else {
      result = this.organizationService.create(this.organizationForm.value);
    }

    result.subscribe((res) => {
      if (res === 'OK') {
        this.router.navigate(['/organizations']);
      }
    });
  }
}
