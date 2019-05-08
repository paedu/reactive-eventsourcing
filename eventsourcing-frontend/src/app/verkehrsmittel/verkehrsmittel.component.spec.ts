import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { VerkehrsmittelComponent } from './verkehrsmittel.component';

describe('VerkehrsmittelComponent', () => {
  let component: VerkehrsmittelComponent;
  let fixture: ComponentFixture<VerkehrsmittelComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ VerkehrsmittelComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(VerkehrsmittelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
