import {
  Component,
  ElementRef,
  OnDestroy,
  AfterViewChecked,
  ViewChild,
  ChangeDetectorRef
} from '@angular/core';
import { Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { Subscription } from 'rxjs';
import { Chess } from 'chess.js';
import {
  Chart,
  LineElement,
  PointElement,
  LineController,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
  ChartOptions
} from 'chart.js';
import { WebsocketService } from '../../services/websocket.service';
import { AuthService } from '../../services/auth.service';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import {
  MessageType,
  AnalyzeGamePayload,
  AnalyzeResultPayload,
  SaveGamePayload,
  ResponsePayload,
} from '../../models/protocol';

Chart.register(LineElement, PointElement, LineController, CategoryScale, LinearScale, Tooltip, Legend);

@Component({
  selector: 'app-principal',
  standalone: true,
  imports: [NgIf, NavbarComponent],
  templateUrl: './principal.component.html',
  styleUrl: './principal.component.css'
})
export class PrincipalComponent implements AfterViewChecked, OnDestroy {
  @ViewChild('analysisChart') canvasRef!: ElementRef<HTMLCanvasElement>;

  fileName: string = '';
  fileValid: boolean = false;
  selectedFile: File | null = null;
  analizando: boolean = false;
  analysisComplete: boolean = false;
  saveGameResponse: boolean = false;
  analysisData: AnalyzeResultPayload | null = null;
  gameMoves: string = '';

  private chart!: Chart;
  private pendingChartData: { white: number[]; black: number[] } | null = null;
  private chartPendingRender = false;
  private subs: Subscription[] = [];

  constructor(
    private wsService: WebsocketService,
    private authService: AuthService,
    private router: Router,
    private cdRef: ChangeDetectorRef
  ) {
    const token = authService.getToken();
    if (token && !this.wsService.isConnected) {
      this.wsService.connect(token);
    }
  }

  ngAfterViewChecked(): void {
    if (this.chartPendingRender && this.canvasRef?.nativeElement) {
      this.chartPendingRender = false;
      if (this.pendingChartData) {
        this.drawChart(this.pendingChartData.white, this.pendingChartData.black);
        this.pendingChartData = null;
      }
    }
  }

  ngOnDestroy(): void {
    if (this.chart) {
      this.chart.destroy();
    }
    this.subs.forEach(s => s.unsubscribe());
  }

  mostrarNombre(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const isPGN = file.name.toLowerCase().endsWith('.pgn');
      this.fileValid = isPGN;

      if (isPGN) {
        this.fileName = file.name;
        this.selectedFile = file;
      } else {
        this.selectedFile = null;
      }
    } else {
      this.selectedFile = null;
      this.fileValid = false;
      this.fileName = '';
    }
  }

  analyse(): void {
    if (!this.selectedFile) return;

    const reader = new FileReader();
    this.analizando = true;
    this.analysisComplete = false;
    this.saveGameResponse = false;
    this.analysisData = null;

    reader.onload = () => {
      const pgnText = reader.result as string;

      try {
        const chess = new Chess();
        const cleanedPgn = pgnText
          .replace(/\{[\s\S]*?\}/g, '')
          .replace(/\s{2,}/g, ' ')
          .trim();

        const firstMoveIdx = cleanedPgn.search(/\b1\./);
        let movesSection = '';
        if (firstMoveIdx >= 0) {
          movesSection = cleanedPgn.substring(firstMoveIdx).replace(/\s+/g, ' ').trim();
        }
        this.gameMoves = movesSection;
        chess.loadPgn(cleanedPgn);

        // Subscribe to analysis result
        const sub = this.wsService.on<AnalyzeResultPayload>(MessageType.ANALYZE_RESULT).subscribe(payload => {
          this.analysisData = payload;
          this.pendingChartData = { white: payload.white, black: payload.black };
          this.analysisComplete = true;
          this.chartPendingRender = true;
          this.analizando = false;
          this.cdRef.detectChanges();
        });
        this.subs.push(sub);

        const sendRequest = () => {
          if (this.wsService.isConnected) {
            const payload: AnalyzeGamePayload = { moves: movesSection };
            this.wsService.send(MessageType.ANALYZE_GAME, payload);
          } else {
            setTimeout(sendRequest, 200);
          }
        };
        sendRequest();
      } catch (e) {
        console.error('Error analizando PGN:', e);
        this.analizando = false;
      }
    };

    reader.onerror = (error) => {
      console.error('Error al leer el archivo:', error);
      this.analizando = false;
    };

    reader.readAsText(this.selectedFile);
  }

  drawChart(white: number[], black: number[]): void {
    const labels = white.map((_, index) => index);

    const data = {
      labels: labels,
      datasets: [
        {
          label: 'Blancas',
          data: white,
          borderColor: 'blue',
          fill: false,
          tension: 0.2
        },
        {
          label: 'Negras',
          data: black,
          borderColor: 'red',
          fill: false,
          tension: 0.2
        }
      ]
    };

    const options: ChartOptions = {
      responsive: true,
      maintainAspectRatio: true,
      scales: {
        x: {
          title: { display: true, text: 'Índice' }
        },
        y: {
          title: { display: true, text: 'Valor' }
        }
      },
      plugins: {
        legend: {
          labels: {
            font: {
              size: 14
            }
          }
        }
      },
      layout: {
        padding: 20
      }
    };

    if (this.chart) {
      this.chart.destroy();
    }

    const ctx = this.canvasRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    this.chart = new Chart(ctx, {
      type: 'line',
      data: data,
      options: options
    });
  }

  saveGame(): void {
    if (!this.analysisData) return;

    const sub = this.wsService.on<ResponsePayload>(MessageType.SAVE_GAME_RESPONSE).subscribe(payload => {
      this.analysisComplete = false;
      this.analysisData = null;
      this.saveGameResponse = payload.success;
    });
    this.subs.push(sub);

    const savePayload: SaveGamePayload = {
      moves: this.gameMoves,
      legal: this.analysisData.whiteLegal && this.analysisData.blackLegal,
    };
    this.wsService.send(MessageType.SAVE_GAME, savePayload);
  }

  home(): void { this.router.navigate(['/home']); }
}
