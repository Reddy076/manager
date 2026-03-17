import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { LucideAngularModule, Bot, X, Send, User, Loader2 } from 'lucide-angular';
import { MarkdownModule } from 'ngx-markdown';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

@Component({
  selector: 'app-security-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, MarkdownModule],
  templateUrl: './security-chatbot.component.html',
  styleUrl: './security-chatbot.component.css'
})
export class SecurityChatbotComponent implements OnInit {
  isOpen = false;
  messages: ChatMessage[] = [];
  newMessage = '';
  isTyping = false;

  @ViewChild('scrollContainer') private scrollContainer!: ElementRef;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.messages.push({
      role: 'assistant',
      content: 'Hello! I am your AI Cybersecurity Assistant. How can I help you secure your credentials today?'
    });
  }

  toggleChat() {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      setTimeout(() => this.scrollToBottom(), 100);
    }
  }

  sendMessage() {
    if (!this.newMessage.trim() || this.isTyping) return;

    const userText = this.newMessage.trim();
    this.messages.push({ role: 'user', content: userText });
    this.newMessage = '';
    this.isTyping = true;
    this.scrollToBottom();

    this.http.post<{ reply: string }>(`${environment.apiBaseUrl}/api/ai/chat`, { message: userText }).subscribe({
      next: (res) => {
        this.messages.push({ role: 'assistant', content: res.reply });
        this.isTyping = false;
        this.scrollToBottom();
      },
      error: (err) => {
        console.error('Chat error', err);
        this.messages.push({ role: 'assistant', content: 'Sorry, I am having trouble connecting to the server. Please check your connection and try again.' });
        this.isTyping = false;
        this.scrollToBottom();
      }
    });
  }

  scrollToBottom(): void {
    if (!this.scrollContainer) return;
    try {
      setTimeout(() => {
        this.scrollContainer.nativeElement.scrollTop = this.scrollContainer.nativeElement.scrollHeight;
      }, 50);
    } catch(err) { }
  }
}
