import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SecurityChatbotComponent } from './security-chatbot.component';
import { environment } from '../../../environments/environment';
import { FormsModule } from '@angular/forms';
import { NO_ERRORS_SCHEMA, importProvidersFrom } from '@angular/core';
import { LucideAngularModule, Bot, X, Send, User, Shield, Loader2 } from 'lucide-angular';

describe('SecurityChatbotComponent', () => {
  let component: SecurityChatbotComponent;
  let fixture: ComponentFixture<SecurityChatbotComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SecurityChatbotComponent, HttpClientTestingModule, FormsModule],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        importProvidersFrom(LucideAngularModule.pick({ Bot, X, Send, User, Shield, Loader2 }))
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SecurityChatbotComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create and initialize with greeting', () => {
    expect(component).toBeTruthy();
    expect(component.messages.length).toBe(1);
    expect(component.messages[0].role).toBe('assistant');
  });

  it('should toggle chat visibility', fakeAsync(() => {
    expect(component.isOpen).toBeFalse();
    
    component.toggleChat();
    expect(component.isOpen).toBeTrue();
    
    tick(150); // wait for setTimeout scrollToBottom
    
    component.toggleChat();
    expect(component.isOpen).toBeFalse();
  }));

  it('should send a message and receive a reply', () => {
    component.newMessage = 'What is a strong password?';
    component.sendMessage();

    expect(component.messages.length).toBe(2);
    expect(component.messages[1].role).toBe('user');
    expect(component.messages[1].content).toBe('What is a strong password?');
    expect(component.isTyping).toBeTrue();
    expect(component.newMessage).toBe('');

    const req = httpTestingController.expectOne(`${environment.apiBaseUrl}/api/ai/chat`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ message: 'What is a strong password?' });

    req.flush({ reply: 'A strong password is at least 12 characters...' });

    expect(component.messages.length).toBe(3);
    expect(component.messages[2].role).toBe('assistant');
    expect(component.messages[2].content).toBe('A strong password is at least 12 characters...');
    expect(component.isTyping).toBeFalse();
  });

  it('should handle API errors gracefully', () => {
    component.newMessage = 'Help me';
    component.sendMessage();

    const req = httpTestingController.expectOne(`${environment.apiBaseUrl}/api/ai/chat`);
    req.flush('Error', { status: 500, statusText: 'Server Error' });

    expect(component.messages.length).toBe(3);
    expect(component.messages[2].role).toBe('assistant');
    expect(component.messages[2].content).toContain('trouble connecting to the server');
    expect(component.isTyping).toBeFalse();
  });

  it('should not send empty messages', () => {
    component.newMessage = '   ';
    component.sendMessage();

    expect(component.messages.length).toBe(1); // Only the initial greeting
    httpTestingController.expectNone(`${environment.apiBaseUrl}/api/ai/chat`);
  });
});
