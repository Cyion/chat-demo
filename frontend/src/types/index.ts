export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
}

export interface UserSummary {
  id: string;
  username: string;
}

export interface MessageResponse {
  id: string;
  chatId: string;
  senderId: string;
  senderUsername: string;
  content: string;
  createdAt: string;
}

export interface ChatResponse {
  id: string;
  participants: UserSummary[];
  createdAt: string;
  lastMessage: MessageResponse | null;
}

export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}

export interface AuthUser {
  id: string;
  username: string;
}
