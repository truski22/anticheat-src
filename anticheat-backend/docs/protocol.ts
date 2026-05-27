/**
 * Chess Fraud Detection — WebSocket Protocol (TypeScript)
 *
 * TypeScript type definitions matching the Java protocol records.
 * Use these in the frontend client to send/receive typed messages.
 */

// ─── Message Types ───────────────────────────────────────────────

export enum MessageType {
    // Client → Gateway
    LOGIN = 'LOGIN',
    REGISTER = 'REGISTER',
    USER_INFO_REQUEST = 'USER_INFO_REQUEST',
    GAMES_REQUEST = 'GAMES_REQUEST',
    ANALYZE_GAME = 'ANALYZE_GAME',
    SAVE_GAME = 'SAVE_GAME',
    CHANGE_PASSWORD = 'CHANGE_PASSWORD',
    CHANGE_PASSWORD_EMAIL = 'CHANGE_PASSWORD_EMAIL',
    SEND_EMAIL_CHANGE_PASSWORD = 'SEND_EMAIL_CHANGE_PASSWORD',

    // Gateway → Client
    LOGIN_RESPONSE = 'LOGIN_RESPONSE',
    REGISTER_RESPONSE = 'REGISTER_RESPONSE',
    USER_INFO = 'USER_INFO',
    GAMES = 'GAMES',
    ANALYZE_RESULT = 'ANALYZE_RESULT',
    SAVE_GAME_RESPONSE = 'SAVE_GAME_RESPONSE',
    CHANGE_PASSWORD_EMAIL_RESPONSE = 'CHANGE_PASSWORD_EMAIL_RESPONSE',

    // Future game types
    MOVE = 'MOVE',
    RESIGN = 'RESIGN',
    CHAT = 'CHAT',
    FRAUD_ALERT = 'FRAUD_ALERT',

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

export interface LoginPayload {
    password: string;
}

export interface RegisterPayload {
    email: string;
    password: string;
}

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

export interface ChangePasswordEmailPayload {
    email: string;
    password: string;
}

export interface SendEmailChangePasswordPayload {
    email: string;
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
    legal: boolean;
    white: number[];
    black: number[];
}

export interface ChangePasswordEmailResponsePayload {
    code: string;
}

export interface ErrorPayload {
    code: string;
    message: string;
}

// ─── Future Payloads ─────────────────────────────────────────────

export interface MovePayload {
    from: string;
    to: string;
    player: string;
    timestamp: number;
}

export interface FraudAlertPayload {
    gameId: string;
    fraudProbability: number;
    reason: string;
}

// ─── Typed Message Helpers ───────────────────────────────────────

export type LoginMessage = ChessMessage<LoginPayload>;
export type RegisterMessage = ChessMessage<RegisterPayload>;
export type AnalyzeGameMessage = ChessMessage<AnalyzeGamePayload>;
export type SaveGameMessage = ChessMessage<SaveGamePayload>;
export type ChangePasswordMessage = ChessMessage<ChangePasswordPayload>;
export type ChangePasswordEmailMessage = ChessMessage<ChangePasswordEmailPayload>;
export type SendEmailChangePasswordMessage = ChessMessage<SendEmailChangePasswordPayload>;

export type LoginResponseMessage = ChessMessage<ResponsePayload>;
export type RegisterResponseMessage = ChessMessage<ResponsePayload>;
export type UserInfoMessage = ChessMessage<UserInfoPayload>;
export type GamesMessage = ChessMessage<GamesPayload>;
export type AnalyzeResultMessage = ChessMessage<AnalyzeResultPayload>;
export type SaveGameResponseMessage = ChessMessage<ResponsePayload>;
export type ChangePasswordEmailResponseMessage = ChessMessage<ChangePasswordEmailResponsePayload>;
export type ErrorMessage = ChessMessage<ErrorPayload>;
export type MoveMessage = ChessMessage<MovePayload>;
export type FraudAlertMessage = ChessMessage<FraudAlertPayload>;
