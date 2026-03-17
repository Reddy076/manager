import { ComponentFixture, TestBed, fakeAsync, flush } from '@angular/core/testing';
import { VaultEntryModalComponent } from './vault-entry-modal.component';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { CategoryControllerService } from '../../../core/api/api/categoryController.service';
import { FolderControllerService } from '../../../core/api/api/folderController.service';
import { PasswordGeneratorWidgetComponent } from '../password-generator-widget/password-generator-widget.component';
import { of, throwError } from 'rxjs';
import { importProvidersFrom, NO_ERRORS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { environment } from '../../../../environments/environment';
import { LucideAngularModule, Plus, X, Eye, EyeOff, RefreshCw, Copy, Globe, Star, Shield, Lock, Trash2, Key, Edit2, RotateCw, FolderOpen, Folder, ArrowRight, Settings2, AlertTriangle, FileText, File, ChevronDown, Loader, Sparkles } from 'lucide-angular';

describe('VaultEntryModalComponent', () => {
  let component: VaultEntryModalComponent;
  let fixture: ComponentFixture<VaultEntryModalComponent>;
  let mockCategoryService: jasmine.SpyObj<CategoryControllerService>;
  let mockFolderService: jasmine.SpyObj<FolderControllerService>;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    mockCategoryService = jasmine.createSpyObj('CategoryControllerService', ['getAllCategories', 'createCategory']);
    mockFolderService = jasmine.createSpyObj('FolderControllerService', ['getFolders']);

    mockCategoryService.getAllCategories.and.returnValue(of([{ id: 1, name: 'Work' }]) as any);
    mockFolderService.getFolders.and.returnValue(of([{ id: 10, name: 'Taxes' }]) as any);

    await TestBed.configureTestingModule({
      imports: [VaultEntryModalComponent, ReactiveFormsModule, HttpClientTestingModule],
      providers: [
        FormBuilder,
        importProvidersFrom(LucideAngularModule.pick({ Plus, X, Eye, EyeOff, RefreshCw, Copy, Globe, Star, Shield, Lock, Trash2, Key, Edit2, RotateCw, Folder, ArrowRight, FolderOpen, Settings2, AlertTriangle, FileText, File, ChevronDown, Loader, Sparkles })),
        { provide: CategoryControllerService, useValue: mockCategoryService },
        { provide: FolderControllerService, useValue: mockFolderService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(VaultEntryModalComponent, {
        remove: { imports: [PasswordGeneratorWidgetComponent] },
        add: { imports: [] }
      })
      .compileComponents();

    fixture = TestBed.createComponent(VaultEntryModalComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form on init', () => {
    expect(component.entryForm).toBeDefined();
    expect(component.entryForm.get('title')).toBeDefined();
    expect(component.entryForm.get('categoryId')).toBeDefined();
  });

  it('should load categories and folders on init', () => {
    expect(mockCategoryService.getAllCategories).toHaveBeenCalled();
    expect(mockFolderService.getFolders).toHaveBeenCalled();
    expect(component.categories.length).toBe(1);
    expect(component.folders.length).toBe(1);
  });

  describe('autoCategorize', () => {
    it('should not call API if title is empty', () => {
      component.entryForm.patchValue({ title: '   ', websiteUrl: 'test.com', username: 'user' });
      component.autoCategorize();
      httpTestingController.expectNone(`${environment.apiBaseUrl}/api/ai/categorize-entry`);
      expect(component.isCategorizing).toBeFalse();
    });

    it('should patch existing category and append notes if category is found', fakeAsync(() => {
      component.entryForm.patchValue({ title: 'Jira', websiteUrl: 'jira.com', username: 'dev', notes: 'Initial note.' });
      
      component.autoCategorize();
      expect(component.isCategorizing).toBeTrue();

      const req = httpTestingController.expectOne(`${environment.apiBaseUrl}/api/ai/categorize-entry`);
      expect(req.request.method).toBe('POST');
      req.flush({
        category: 'WORK',
        tags: ['development', 'tickets'],
        confidence: 0.99
      });

      flush();
      
      expect(component.entryForm.get('categoryId')?.value).toBe(1); // Mapped to the mocked 'Work' category
      expect(component.entryForm.get('notes')?.value).toContain('Initial note.\n\n--- Auto-Categorized ---');
      expect(component.entryForm.get('notes')?.value).toContain('Tags: development, tickets');
      expect(component.entryForm.get('notes')?.value).toContain('Confidence: 99%');
      expect(mockCategoryService.createCategory).not.toHaveBeenCalled();
      expect(component.isCategorizing).toBeFalse();
    }));

    it('should create new category on the fly if not found', fakeAsync(() => {
      component.entryForm.patchValue({ title: 'Steam', websiteUrl: 'steam.com', username: 'gamer' });
      mockCategoryService.createCategory.and.returnValue(of({ id: 99, name: 'ENTERTAINMENT', icon: 'folder' }) as any);

      component.autoCategorize();

      const req = httpTestingController.expectOne(`${environment.apiBaseUrl}/api/ai/categorize-entry`);
      req.flush({
        category: 'ENTERTAINMENT',
        tags: ['games', 'fun'],
        confidence: 0.85
      });

      flush();
      
      expect(mockCategoryService.createCategory).toHaveBeenCalledWith(jasmine.objectContaining({ name: 'ENTERTAINMENT' }));
      expect(component.categories.length).toBe(2);
      expect(component.entryForm.get('categoryId')?.value).toBe(99);
      expect(component.entryForm.get('notes')?.value).toContain('Tags: games, fun');
      expect(component.isCategorizing).toBeFalse();
    }));

    it('should handle AI API error gracefully', fakeAsync(() => {
      component.entryForm.patchValue({ title: 'Error Site' });
      
      component.autoCategorize();

      const req = httpTestingController.expectOne(`${environment.apiBaseUrl}/api/ai/categorize-entry`);
      req.flush('Connection Refused', { status: 500, statusText: 'Server Error' });

      flush();
      
      expect(component.isCategorizing).toBeFalse();
      expect(component.entryForm.get('categoryId')?.value).toBe(''); // unchanged
      expect(mockCategoryService.createCategory).not.toHaveBeenCalled();
    }));

    it('should handle Category Creation error gracefully', fakeAsync(() => {
      component.entryForm.patchValue({ title: 'New Site', notes: 'Existing' });
      mockCategoryService.createCategory.and.returnValue(throwError(() => new Error('DB Error')));

      component.autoCategorize();

      const req = httpTestingController.expectOne(`${environment.apiBaseUrl}/api/ai/categorize-entry`);
      req.flush({
        category: 'NEW_STUFF',
        tags: ['cool'],
        confidence: 0.50
      });

      flush();
      
      expect(component.isCategorizing).toBeFalse();
      expect(component.entryForm.get('categoryId')?.value).toBe(''); // unchanged because creation failed
      expect(component.entryForm.get('notes')?.value).toContain('Existing\n\n--- Auto-Categorized ---'); // Still appended notes
    }));
  });

  it('should emit save event when form is valid and submitted', fakeAsync(() => {
    spyOn(component.save, 'emit');

    component.entryForm.patchValue({
      title: 'Valid Entry',
      username: 'user@example.com',
      password: 'password123',
      categoryId: 1,
      folderId: 10,
      isFavorite: true
    });

    component.onSubmit();
    flush();

    expect(component.save.emit).toHaveBeenCalledWith(jasmine.objectContaining({
      title: 'Valid Entry',
      username: 'user@example.com',
      categoryId: 1,
      folderId: 10,
      isFavorite: true
    }));
  }));
});
