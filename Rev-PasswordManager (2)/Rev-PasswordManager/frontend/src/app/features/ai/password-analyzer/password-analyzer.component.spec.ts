import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PasswordAnalyzerComponent } from './password-analyzer.component';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../../environments/environment';

describe('PasswordAnalyzerComponent', () => {
  let component: PasswordAnalyzerComponent;
  let fixture: ComponentFixture<PasswordAnalyzerComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PasswordAnalyzerComponent, HttpClientTestingModule, FormsModule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PasswordAnalyzerComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not analyze if password input is empty', () => {
    component.passwordInput = '   ';
    component.analyzePassword();
    
    expect(component.isAnalyzing).toBeFalse();
    httpMock.expectNone(`${environment.apiBaseUrl}/api/ai/analyze-password`);
  });

  it('should set analyzing state and handle successful analysis', () => {
    const mockResponse = {
      strength: 'STRONG',
      vulnerabilities: [],
      suggestions: ['Keep it up']
    };

    component.passwordInput = 'strongPassword123!';
    component.analyzePassword();

    expect(component.isAnalyzing).toBeTrue();
    expect(component.errorMessage).toBeNull();
    expect(component.analysisResult).toBeNull();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/ai/analyze-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ password: 'strongPassword123!' });
    
    req.flush(mockResponse);

    expect(component.isAnalyzing).toBeFalse();
    expect(component.analysisResult).toEqual(mockResponse);
  });

  it('should handle HTTP error gracefully', () => {
    component.passwordInput = 'testpass';
    component.analyzePassword();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/ai/analyze-password`);
    
    req.error(new ProgressEvent('Network error'));

    expect(component.isAnalyzing).toBeFalse();
    expect(component.analysisResult).toBeNull();
    expect(component.errorMessage).toBe('Failed to analyze password. Please try again later.');
  });

  it('should return correct badge classes for strengths', () => {
    expect(component.getStrengthBadgeClass('VERY_WEAK')).toBe('bg-danger');
    expect(component.getStrengthBadgeClass('STRONG')).toBe('bg-primary');
    expect(component.getStrengthBadgeClass('VERY_STRONG')).toBe('bg-success');
    expect(component.getStrengthBadgeClass('UNKNOWN')).toBe('bg-secondary');
  });
});
