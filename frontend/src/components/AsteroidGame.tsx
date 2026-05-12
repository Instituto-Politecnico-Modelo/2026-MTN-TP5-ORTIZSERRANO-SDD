/**
 * AsteroidGame.tsx — Asteroid Shooter
 * Canvas puro con requestAnimationFrame — sin librerías externas.
 */

import React, { useEffect, useRef, useState, useCallback } from 'react';

// ─── Constantes ───────────────────────────────────────────────────────────────

const W              = 460;
const H              = 520;
const PLAYER_W       = 18;
const PLAYER_H       = 22;
const PLAYER_SPEED   = 5;
const PLAYER_Y       = H - 55;

const BULLET_W       = 4;
const BULLET_H       = 12;
const BULLET_SPEED   = 10;
const SHOOT_COOLDOWN = 220;

const INIT_INTERVAL  = 500;
const MIN_INTERVAL   = 140;
const SPEED_UP_SCORE = 8;

// ─── Tipos ────────────────────────────────────────────────────────────────────

interface Rock {
  x: number; y: number;
  speed: number; color: string; size: number;
  hp: number; flash: number;
}
interface Bullet { x: number; y: number; }
interface Particle {
  x: number; y: number; vx: number; vy: number;
  life: number; color: string; size: number;
}
interface Keys { left: boolean; right: boolean; shoot: boolean; }
interface GameState {
  playerX: number; rocks: Rock[]; bullets: Bullet[]; particles: Particle[];
  keys: Keys; killed: number; alive: boolean;
  interval: number; lastWave: number; lastShot: number; animId: number;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

const ROCK_COLORS = ['#94a3b8','#64748b','#7c3aed','#dc2626','#d97706','#059669','#b45309'];

function levelOf(killed: number) { return Math.floor(killed / SPEED_UP_SCORE) + 1; }
function rocksPerWave(killed: number) { return 2 + Math.floor(killed / SPEED_UP_SCORE); }

function randRock(interval: number): Rock {
  const size  = 12 + Math.floor(Math.random() * 14);
  const speed = 2 + Math.random() * 2.5 + (INIT_INTERVAL - interval) / 250;
  return {
    x: size + Math.random() * (W - size * 2), y: -size,
    speed, size, color: ROCK_COLORS[Math.floor(Math.random() * ROCK_COLORS.length)],
    hp: size > 20 ? 2 : 1, flash: 0,
  };
}

function explode(rock: Rock): Particle[] {
  return Array.from({ length: 10 }, () => ({
    x: rock.x, y: rock.y,
    vx: (Math.random() - 0.5) * 6, vy: (Math.random() - 0.5) * 6,
    life: 20 + Math.random() * 10, color: rock.color,
    size: 2 + Math.floor(Math.random() * 4),
  }));
}

// ─── Dibujos ──────────────────────────────────────────────────────────────────

function drawPlayer(ctx: CanvasRenderingContext2D, x: number) {
  const px = Math.round(x), py = PLAYER_Y;
  ctx.fillStyle = '#38bdf8'; ctx.fillRect(px - 4, py + 4, 8, 12);
  ctx.fillStyle = '#bae6fd'; ctx.fillRect(px - 2, py, 4, 6);
  ctx.fillStyle = '#0369a1';
  ctx.fillRect(px - 10, py + 10, 7, 6);
  ctx.fillRect(px + 3,  py + 10, 7, 6);
  ctx.fillStyle = '#f97316'; ctx.fillRect(px - 3, py + 16, 6, 4);
  ctx.fillStyle = '#fbbf24'; ctx.fillRect(px - 2, py + 18, 4, 3);
}

function drawRock(ctx: CanvasRenderingContext2D, r: Rock) {
  const rx = Math.round(r.x - r.size / 2), ry = Math.round(r.y - r.size / 2), s = r.size;
  ctx.fillStyle = r.flash > 0 ? '#ffffff' : r.color;
  ctx.fillRect(rx + 2, ry, s - 4, s);
  ctx.fillRect(rx, ry + 2, s, s - 4);
  ctx.fillStyle = 'rgba(0,0,0,0.35)';
  ctx.fillRect(rx + 2, ry, s - 4, 2);
  ctx.fillRect(rx, ry + 2, 2, s - 4);
  ctx.fillStyle = 'rgba(255,255,255,0.25)';
  ctx.fillRect(rx + 4, ry + 2, 4, 2);
  if (r.hp > 1) {
    ctx.fillStyle = '#f87171';
    ctx.fillRect(rx, ry - 5, s, 3);
  }
}

function drawBullet(ctx: CanvasRenderingContext2D, b: Bullet) {
  ctx.fillStyle = '#facc15';
  ctx.fillRect(Math.round(b.x - BULLET_W / 2), Math.round(b.y), BULLET_W, BULLET_H);
  ctx.fillStyle = '#fef08a';
  ctx.fillRect(Math.round(b.x - 1), Math.round(b.y), 2, 4);
}

function drawParticle(ctx: CanvasRenderingContext2D, p: Particle) {
  ctx.globalAlpha = p.life / 30;
  ctx.fillStyle = p.color;
  ctx.fillRect(Math.round(p.x), Math.round(p.y), p.size, p.size);
  ctx.globalAlpha = 1;
}

function drawStars(ctx: CanvasRenderingContext2D, stars: {x:number;y:number;r:number}[]) {
  ctx.fillStyle = 'white';
  stars.forEach(s => ctx.fillRect(s.x, s.y, s.r, s.r));
}

function drawOverlay(
  ctx: CanvasRenderingContext2D,
  stars: {x:number;y:number;r:number}[],
  phase: 'idle' | 'dead',
  killed: number, best: number,
) {
  ctx.fillStyle = '#0f172a'; ctx.fillRect(0, 0, W, H);
  drawStars(ctx, stars);
  drawPlayer(ctx, W / 2);
  ctx.fillStyle = 'rgba(0,0,0,0.55)'; ctx.fillRect(0, 0, W, H);
  ctx.textAlign = 'center';
  if (phase === 'idle') {
    ctx.fillStyle = '#38bdf8'; ctx.font = 'bold 22px monospace';
    ctx.fillText('⬛ ASTEROID SHOOTER ⬛', W / 2, H / 2 - 55);
    ctx.fillStyle = 'white'; ctx.font = '13px monospace';
    ctx.fillText('Esquivá y destruí los bloques', W / 2, H / 2 - 20);
    ctx.fillStyle = '#94a3b8'; ctx.font = '12px monospace';
    ctx.fillText('← →  /  A D  →  moverse', W / 2, H / 2 + 4);
    ctx.fillText('ESPACIO  →  disparar', W / 2, H / 2 + 22);
  } else {
    ctx.fillStyle = '#f87171'; ctx.font = 'bold 24px monospace';
    ctx.fillText('💥 CHOCASTE 💥', W / 2, H / 2 - 55);
    ctx.fillStyle = 'white'; ctx.font = '16px monospace';
    ctx.fillText(`Destruidos: ${killed}`, W / 2, H / 2 - 20);
    ctx.fillStyle = '#fbbf24';
    ctx.fillText(`Mejor: ${best}`, W / 2, H / 2 + 6);
    ctx.fillStyle = '#94a3b8'; ctx.font = '12px monospace';
    ctx.fillText('Presioná Reintentar', W / 2, H / 2 + 34);
  }
  ctx.textAlign = 'left';
}

// ─── Componente ───────────────────────────────────────────────────────────────

const AsteroidGame: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const s         = useRef<GameState>({
    playerX: W / 2, rocks: [], bullets: [], particles: [],
    keys: { left: false, right: false, shoot: false },
    killed: 0, alive: false,
    interval: INIT_INTERVAL, lastWave: 0, lastShot: 0, animId: 0,
  });

  const [killed, setKilled] = useState(0);
  const [best,   setBest]   = useState(() => parseInt(localStorage.getItem('asteroid_best3') ?? '0'));
  const [phase,  setPhase]  = useState<'idle' | 'playing' | 'dead'>('idle');

  const stars = useRef(Array.from({ length: 60 }, () => ({
    x: Math.random() * W, y: Math.random() * H,
    r: Math.random() < 0.25 ? 2 : 1,
  })));

  // ── Loop ──────────────────────────────────────────────────────────────────

  const loop = useCallback((ts: number) => {
    const g   = s.current;
    const ctx = canvasRef.current?.getContext('2d');
    if (!ctx || !g.alive) return;

    // Jugador
    if (g.keys.left)  g.playerX = Math.max(PLAYER_W / 2, g.playerX - PLAYER_SPEED);
    if (g.keys.right) g.playerX = Math.min(W - PLAYER_W / 2, g.playerX + PLAYER_SPEED);

    // Disparo
    if (g.keys.shoot && ts - g.lastShot > SHOOT_COOLDOWN) {
      g.bullets.push({ x: g.playerX, y: PLAYER_Y });
      g.lastShot = ts;
    }

    // Balas
    g.bullets.forEach(b => { b.y -= BULLET_SPEED; });
    g.bullets = g.bullets.filter(b => b.y > -BULLET_H);

    // Rocas + flash decay
    g.rocks.forEach(r => { r.y += r.speed; if (r.flash > 0) r.flash--; });

    // Spawner
    if (ts - g.lastWave > g.interval) {
      const count = rocksPerWave(g.killed);
      for (let i = 0; i < count; i++) g.rocks.push(randRock(g.interval));
      g.lastWave = ts;
    }

    // Colisión bala → roca
    const nextBullets: Bullet[] = [];
    const destroyed = new Set<Rock>();
    for (const b of g.bullets) {
      let hit = false;
      for (const r of g.rocks) {
        if (destroyed.has(r)) continue;
        if (
          Math.abs(b.x - r.x) < r.size / 2 + BULLET_W / 2 &&
          Math.abs(b.y - r.y) < r.size / 2 + BULLET_H / 2
        ) {
          r.hp--; r.flash = 6;
          if (r.hp <= 0) {
            destroyed.add(r);
            g.particles.push(...explode(r));
            g.killed++;
            g.interval = Math.max(MIN_INTERVAL, INIT_INTERVAL - (levelOf(g.killed) - 1) * 50);
            setKilled(g.killed);
          }
          hit = true; break;
        }
      }
      if (!hit) nextBullets.push(b);
    }
    g.bullets  = nextBullets;
    g.rocks    = g.rocks.filter(r => !destroyed.has(r) && r.y < H + 30);

    // Partículas
    g.particles.forEach(p => { p.x += p.vx; p.y += p.vy; p.life--; });
    g.particles = g.particles.filter(p => p.life > 0);

    // Colisión roca → jugador
    const crash = g.rocks.some(r =>
      Math.abs(r.x - g.playerX) < PLAYER_W / 2 + r.size / 2 - 3 &&
      Math.abs(r.y - PLAYER_Y)  < PLAYER_H / 2 + r.size / 2 - 3
    );
    if (crash) {
      g.alive = false;
      const nb = Math.max(g.killed, best);
      setBest(nb);
      localStorage.setItem('asteroid_best3', String(nb));
      setPhase('dead');
      return;
    }

    // ── Render ──────────────────────────────────────────────────────────────
    ctx.fillStyle = '#0f172a'; ctx.fillRect(0, 0, W, H);
    drawStars(ctx, stars.current);
    g.particles.forEach(p  => drawParticle(ctx, p));
    g.rocks.forEach(r      => drawRock(ctx, r));
    g.bullets.forEach(b    => drawBullet(ctx, b));
    drawPlayer(ctx, g.playerX);

    // HUD
    ctx.fillStyle = 'rgba(255,255,255,0.85)'; ctx.font = 'bold 13px monospace';
    ctx.fillText(`Destruidos: ${g.killed}`, 10, 22);
    ctx.fillText(`Mejor: ${best}`, W - 100, 22);
    ctx.fillStyle = '#38bdf8'; ctx.font = '11px monospace';
    ctx.fillText(`LVL ${levelOf(g.killed)}`, W / 2 - 16, 22);

    g.animId = requestAnimationFrame(loop);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [best]);

  // ── Start ────────────────────────────────────────────────────────────────

  const start = useCallback(() => {
    const g = s.current;
    g.playerX = W / 2; g.rocks = []; g.bullets = []; g.particles = [];
    g.killed = 0; g.alive = true;
    g.interval = INIT_INTERVAL; g.lastWave = 0; g.lastShot = 0;
    setKilled(0); setPhase('playing');
    cancelAnimationFrame(g.animId);
    g.animId = requestAnimationFrame(loop);
  }, [loop]);

  // ── Teclado ──────────────────────────────────────────────────────────────

  useEffect(() => {
    const down = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft'  || e.key === 'a') s.current.keys.left  = true;
      if (e.key === 'ArrowRight' || e.key === 'd') s.current.keys.right = true;
      if (e.key === ' ' || e.key === 'ArrowUp' || e.key === 'w') {
        e.preventDefault(); s.current.keys.shoot = true;
      }
    };
    const up = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft'  || e.key === 'a') s.current.keys.left  = false;
      if (e.key === 'ArrowRight' || e.key === 'd') s.current.keys.right = false;
      if (e.key === ' ' || e.key === 'ArrowUp' || e.key === 'w') s.current.keys.shoot = false;
    };
    window.addEventListener('keydown', down);
    window.addEventListener('keyup',   up);
    const state = s.current;
    return () => {
      window.removeEventListener('keydown', down);
      window.removeEventListener('keyup',   up);
      cancelAnimationFrame(state.animId);
    };
  }, []);

  // ── Pantalla estática ─────────────────────────────────────────────────────

  useEffect(() => {
    const ctx = canvasRef.current?.getContext('2d');
    if (!ctx || phase === 'playing') return;
    drawOverlay(ctx, stars.current, phase, killed, best);
  }, [phase, killed, best]);

  // ── Botones táctiles ──────────────────────────────────────────────────────

  const pl = () => { s.current.keys.left  = true;  };
  const rl = () => { s.current.keys.left  = false; };
  const pr = () => { s.current.keys.right = true;  };
  const rr = () => { s.current.keys.right = false; };
  const ps = () => { s.current.keys.shoot = true;  };
  const rs = () => { s.current.keys.shoot = false; };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>

      <div style={{
        fontSize: '12px', color: '#64748b', textAlign: 'center',
        background: '#f1f5f9', borderRadius: '8px', padding: '6px 14px',
      }}>
        🎮 Mini juego · <strong>← →</strong> mover · <strong>Espacio / W / ↑</strong> disparar
      </div>

      <canvas
        ref={canvasRef}
        width={W}
        height={H}
        style={{
          borderRadius: '12px',
          border: '2px solid #1e3a5f',
          boxShadow: '0 4px 24px rgba(0,0,0,0.3)',
          display: 'block',
          imageRendering: 'pixelated',
        }}
      />

      {/* Controles táctiles */}
      <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
        <button onMouseDown={pl} onMouseUp={rl} onTouchStart={pl} onTouchEnd={rl} style={ctrlBtn}>◀</button>

        <button
          onMouseDown={ps} onMouseUp={rs} onTouchStart={ps} onTouchEnd={rs}
          style={{ ...ctrlBtn, background: '#854d0e', fontSize: '18px', width: '64px' }}
        >🔫</button>

        <button
          onClick={start}
          style={{
            padding: '10px 20px',
            background: phase === 'dead' ? '#dc2626' : '#0369a1',
            color: 'white', border: 'none', borderRadius: '10px',
            cursor: 'pointer', fontSize: '14px', fontWeight: 700,
            boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
          }}
        >
          {phase === 'idle' ? '▶ Jugar' : phase === 'dead' ? '🔄 Reintentar' : '🔄 Reiniciar'}
        </button>

        <button
          onMouseDown={ps} onMouseUp={rs} onTouchStart={ps} onTouchEnd={rs}
          style={{ ...ctrlBtn, background: '#854d0e', fontSize: '18px', width: '64px' }}
        >🔫</button>

        <button onMouseDown={pr} onMouseUp={rr} onTouchStart={pr} onTouchEnd={rr} style={ctrlBtn}>▶</button>
      </div>

    </div>
  );
};

const ctrlBtn: React.CSSProperties = {
  width: '52px', height: '52px',
  background: '#1e3a5f', color: 'white',
  border: 'none', borderRadius: '10px',
  cursor: 'pointer', fontSize: '20px',
  fontWeight: 700, userSelect: 'none',
  boxShadow: '0 2px 6px rgba(0,0,0,0.25)',
};

export default AsteroidGame;
