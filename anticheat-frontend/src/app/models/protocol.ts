/**
 * Chess Fraud Detection — WebSocket Protocol
 *
 * TypeScript type definitions matching the Java protocol records.
 * Used by the frontend to send/receive typed messages over WebSocket.
 */

// ─── Message Types ───────────────────────────────────────────────

export enum MessageType {
  // Client → Gateway (WebSocket)
  USER_INFO_REQUEST = 'USER_INFO_REQUEST',
  GAMES_REQUEST = 'GAMES_REQUEST',
  ANALYZE_GAME = 'ANALYZE_GAME',
  SAVE_GAME = 'SAVE_GAME',
  CHANGE_PASSWORD = 'CHANGE_PASSWORD',

  // Gateway → Client (WebSocket)
  USER_INFO = 'USER_INFO',
  GAMES = 'GAMES',
  ANALYZE_RESULT = 'ANALYZE_RESULT',
  SAVE_GAME_RESPONSE = 'SAVE_GAME_RESPONSE',
  CHANGE_PASSWORD_RESPONSE = 'CHANGE_PASSWORD_RESPONSE',

  // System
  ERROR = 'ERROR',
}

// ─── Message Envelope ────────────────────────────────────────────

export interface ChessMessage<T = unknown> {
  type: MessageType;
  messageId?: string;
  payload?: T;
}

// ─── Client → Gateway Payloads ───────────────────────────────────

export interface AnalyzeGamePayload {
  moves: string;
}

export interface SaveGamePayload {
  moves: string;
  legal: boolean;
}

export interface ChangePasswordPayload {
  password: string;
}

// ─── Gateway → Client Payloads ───────────────────────────────────

export interface ResponsePayload {
  success: boolean;
  message: string;
}

export interface UserInfoPayload {
  email: string;
  totalGames: number;
  cheatGames: number;
  legalGames: number;
}

export interface GameEntry {
  moves: string;
  legal: boolean;
}

export interface GamesPayload {
  games: GameEntry[];
}

export interface AnalyzeResultPayload {
  whiteLegal: boolean;
  blackLegal: boolean;
  white: number[];
  black: number[];
}

export interface ErrorPayload {
  code: string;
  message: string;
}

// ─── REST Auth Types ─────────────────────────────────────────────

export interface LoginRequest {
  user: string;
  password: string;
}

export interface RegisterRequest {
  user: string;
  email: string;
  password: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  code: string;
  password: string;
}

export interface AuthResponse {
  success: boolean;
  message: string;
  token?: string;
}
