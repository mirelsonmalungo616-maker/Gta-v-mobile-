// Web Audio API Synthesizer for GTA-style sound effects
// Lazy-loaded to avoid blocking the browser security policy

class SoundEngine {
  private ctx: AudioContext | null = null;
  private masterGain: GainNode | null = null;
  private soundEnabled: boolean = true;
  private activeEngineOsc: OscillatorNode | null = null;
  private activeEngineGain: GainNode | null = null;
  private engineFilter: BiquadFilterNode | null = null;

  init() {
    if (this.ctx) return;
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      this.ctx = new AudioCtx();
      this.masterGain = this.ctx.createGain();
      this.masterGain.gain.setValueAtTime(0.3, this.ctx.currentTime); // default comfortable volume
      this.masterGain.connect(this.ctx.destination);
    } catch (e) {
      console.warn("Web Audio API not supported or blocked by browser:", e);
    }
  }

  setVolume(vol: number) {
    this.init();
    if (this.masterGain && this.ctx) {
      this.masterGain.gain.setValueAtTime(vol, this.ctx.currentTime);
    }
  }

  toggle(enabled: boolean) {
    this.soundEnabled = enabled;
    if (!enabled && this.ctx) {
      this.stopContinuousEngine();
    }
  }

  private createNoiseBuffer(): AudioBuffer {
    if (!this.ctx) return new AudioBuffer({ length: 1, sampleRate: 44100 });
    const bufferSize = this.ctx.sampleRate * 1.5; // 1.5 seconds of noise
    const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      data[i] = Math.random() * 2 - 1;
    }
    return buffer;
  }

  // --- SHORT TRIGGER SOUNDS ---

  playShoot(type: 'FISTS' | 'PISTOL' | 'SMG' | 'SHOTGUN' | 'RPG') {
    this.init();
    if (!this.ctx || !this.soundEnabled) return;
    if (this.ctx.state === 'suspended') {
      this.ctx.resume();
    }

    const now = this.ctx.currentTime;

    if (type === 'FISTS') {
      // Dull thud
      const bOsc = this.ctx.createOscillator();
      const bGain = this.ctx.createGain();
      bOsc.type = 'triangle';
      bOsc.frequency.setValueAtTime(150, now);
      bOsc.frequency.exponentialRampToValueAtTime(30, now + 0.15);
      
      bGain.gain.setValueAtTime(0.4, now);
      bGain.gain.exponentialRampToValueAtTime(0.01, now + 0.15);

      bOsc.connect(bGain);
      bGain.connect(this.masterGain!);
      bOsc.start(now);
      bOsc.stop(now + 0.16);
      return;
    }

    if (type === 'PISTOL') {
      // Clean noise pop
      const noise = this.ctx.createBufferSource();
      noise.buffer = this.createNoiseBuffer();
      const filter = this.ctx.createBiquadFilter();
      filter.type = 'bandpass';
      filter.frequency.setValueAtTime(1000, now);

      const gain = this.ctx.createGain();
      gain.gain.setValueAtTime(0.6, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.12);

      noise.connect(filter);
      filter.connect(gain);
      gain.connect(this.masterGain!);

      // Low-frequency kick for weapon weight
      const lowOsc = this.ctx.createOscillator();
      const lowGain = this.ctx.createGain();
      lowOsc.frequency.setValueAtTime(220, now);
      lowOsc.frequency.exponentialRampToValueAtTime(60, now + 0.1);
      lowGain.gain.setValueAtTime(0.5, now);
      lowGain.gain.exponentialRampToValueAtTime(0.01, now + 0.1);
      lowOsc.connect(lowGain);
      lowGain.connect(this.masterGain!);

      noise.start(now);
      lowOsc.start(now);
      noise.stop(now + 0.15);
      lowOsc.stop(now + 0.12);
    } 
    
    else if (type === 'SMG') {
      // Rapid snappier pop
      const noise = this.ctx.createBufferSource();
      noise.buffer = this.createNoiseBuffer();
      const filter = this.ctx.createBiquadFilter();
      filter.type = 'bandpass';
      filter.frequency.setValueAtTime(1200, now);

      const gain = this.ctx.createGain();
      gain.gain.setValueAtTime(0.5, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.08);

      noise.connect(filter);
      filter.connect(gain);
      gain.connect(this.masterGain!);

      // Small kick
      const lowOsc = this.ctx.createOscillator();
      const lowGain = this.ctx.createGain();
      lowOsc.frequency.setValueAtTime(180, now);
      lowOsc.frequency.exponentialRampToValueAtTime(80, now + 0.06);
      lowGain.gain.setValueAtTime(0.3, now);
      lowGain.gain.exponentialRampToValueAtTime(0.01, now + 0.06);
      lowOsc.connect(lowGain);
      lowGain.connect(this.masterGain!);

      noise.start(now);
      lowOsc.start(now);
      noise.stop(now + 0.1);
      lowOsc.stop(now + 0.08);
    } 
    
    else if (type === 'SHOTGUN') {
      // Multi-layered blast
      const noise = this.ctx.createBufferSource();
      noise.buffer = this.createNoiseBuffer();
      const filter = this.ctx.createBiquadFilter();
      filter.type = 'lowpass';
      filter.frequency.setValueAtTime(1500, now);

      const gain = this.ctx.createGain();
      gain.gain.setValueAtTime(0.8, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.35);

      noise.connect(filter);
      filter.connect(gain);
      gain.connect(this.masterGain!);

      // Massive sub blast
      const lowOsc = this.ctx.createOscillator();
      const lowGain = this.ctx.createGain();
      lowOsc.frequency.setValueAtTime(180, now);
      lowOsc.frequency.exponentialRampToValueAtTime(40, now + 0.25);
      lowGain.gain.setValueAtTime(0.7, now);
      lowGain.gain.exponentialRampToValueAtTime(0.01, now + 0.25);
      lowOsc.connect(lowGain);
      lowGain.connect(this.masterGain!);

      noise.start(now);
      lowOsc.start(now);
      noise.stop(now + 0.4);
      lowOsc.stop(now + 0.3);
    } 
    
    else if (type === 'RPG') {
      // Massive rocket launch sound followed by exhaust whoosh
      const lowOsc = this.ctx.createOscillator();
      lowOsc.type = 'sawtooth';
      const lowGain = this.ctx.createGain();
      lowOsc.frequency.setValueAtTime(80, now);
      lowOsc.frequency.exponentialRampToValueAtTime(20, now + 0.5);
      
      lowGain.gain.setValueAtTime(0.9, now);
      lowGain.gain.exponentialRampToValueAtTime(0.01, now + 0.5);
      
      lowOsc.connect(lowGain);
      lowGain.connect(this.masterGain!);

      const noise = this.ctx.createBufferSource();
      noise.buffer = this.createNoiseBuffer();
      const filter = this.ctx.createBiquadFilter();
      filter.type = 'lowpass';
      filter.frequency.setValueAtTime(300, now);
      filter.frequency.exponentialRampToValueAtTime(80, now + 0.6);

      const noiseGain = this.ctx.createGain();
      noiseGain.gain.setValueAtTime(0.7, now);
      noiseGain.gain.exponentialRampToValueAtTime(0.01, now + 0.6);

      noise.connect(filter);
      filter.connect(noiseGain);
      noiseGain.connect(this.masterGain!);

      lowOsc.start(now);
      noise.start(now);
      lowOsc.stop(now + 0.5);
      noise.stop(now + 0.6);
    }
  }

  playExplosion() {
    this.init();
    if (!this.ctx || !this.soundEnabled) return;
    const now = this.ctx.currentTime;

    // Deep sub shake
    const subOsc = this.ctx.createOscillator();
    subOsc.type = 'sawtooth';
    const subGain = this.ctx.createGain();
    subOsc.frequency.setValueAtTime(100, now);
    subOsc.frequency.exponentialRampToValueAtTime(10, now + 0.8);
    
    subGain.gain.setValueAtTime(1.2, now);
    subGain.gain.exponentialRampToValueAtTime(0.001, now + 0.9);
    subOsc.connect(subGain);
    subGain.connect(this.masterGain!);

    // Noise crackle
    const noise = this.ctx.createBufferSource();
    noise.buffer = this.createNoiseBuffer();
    
    const filter = this.ctx.createBiquadFilter();
    filter.type = 'lowpass';
    filter.frequency.setValueAtTime(800, now);
    filter.frequency.exponentialRampToValueAtTime(150, now + 0.7);

    const noiseGain = this.ctx.createGain();
    noiseGain.gain.setValueAtTime(0.8, now);
    noiseGain.gain.exponentialRampToValueAtTime(0.001, now + 1.2);

    noise.connect(filter);
    filter.connect(noiseGain);
    noiseGain.connect(this.masterGain!);

    subOsc.start(now);
    noise.start(now);
    subOsc.stop(now + 1.0);
    noise.stop(now + 1.5);
  }

  playCash() {
    this.init();
    if (!this.ctx || !this.soundEnabled) return;
    const now = this.ctx.currentTime;

    // Classic coin double chime
    const o1 = this.ctx.createOscillator();
    const o2 = this.ctx.createOscillator();
    const g = this.ctx.createGain();

    o1.type = 'triangle';
    o2.type = 'triangle';

    o1.frequency.setValueAtTime(987.77, now); // B5
    o1.frequency.setValueAtTime(1318.51, now + 0.08); // E6

    o2.frequency.setValueAtTime(1174.66, now + 0.04); // D6
    o2.frequency.setValueAtTime(1567.98, now + 0.12); // G6

    g.gain.setValueAtTime(0.3, now);
    g.gain.exponentialRampToValueAtTime(0.01, now + 0.35);

    o1.connect(g);
    o2.connect(g);
    g.connect(this.masterGain!);

    o1.start(now);
    o2.start(now + 0.04);
    o1.stop(now + 0.4);
    o2.stop(now + 0.4);
  }

  playTireScreech() {
    this.init();
    if (!this.ctx || !this.soundEnabled) return;
    const now = this.ctx.currentTime;

    // Frictional squeal
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    const filter = this.ctx.createBiquadFilter();

    osc.type = 'triangle';
    osc.frequency.setValueAtTime(800, now);
    osc.frequency.linearRampToValueAtTime(850, now + 0.1);
    
    filter.type = 'highpass';
    filter.frequency.setValueAtTime(600, now);

    gain.gain.setValueAtTime(0.15, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.15);

    osc.connect(filter);
    filter.connect(gain);
    gain.connect(this.masterGain!);

    osc.start(now);
    osc.stop(now + 0.16);
  }

  playMissionSuccess() {
    this.init();
    if (!this.ctx || !this.soundEnabled) return;
    const now = this.ctx.currentTime;

    // Uplifting brassy sequence C4 -> E4 -> G4 -> C5 (Major arpeggio)
    const notes = [261.63, 329.63, 392.00, 523.25];
    const times = [0, 0.12, 0.24, 0.36];

    notes.forEach((freq, idx) => {
      const osc = this.ctx!.createOscillator();
      const gain = this.ctx!.createGain();
      
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(freq, now + times[idx]);
      
      const filter = this.ctx!.createBiquadFilter();
      filter.type = 'lowpass';
      filter.frequency.setValueAtTime(800, now + times[idx]);

      gain.gain.setValueAtTime(0, now);
      gain.gain.linearRampToValueAtTime(0.2, now + times[idx] + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.001, now + times[idx] + 0.4);

      osc.connect(filter);
      filter.connect(gain);
      gain.connect(this.masterGain!);

      osc.start(now + times[idx]);
      osc.stop(now + times[idx] + 0.5);
    });
  }

  playMissionFail() {
    this.init();
    if (!this.ctx || !this.soundEnabled) return;
    const now = this.ctx.currentTime;

    // Sad descending slide
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(220, now); // A3
    osc.frequency.linearRampToValueAtTime(110, now + 0.8); // A2

    gain.gain.setValueAtTime(0.3, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.85);

    const filter = this.ctx.createBiquadFilter();
    filter.type = 'lowpass';
    filter.frequency.setValueAtTime(300, now);

    osc.connect(filter);
    filter.connect(gain);
    gain.connect(this.masterGain!);

    osc.start(now);
    osc.stop(now + 0.9);
  }

  playCarHorn() {
    this.init();
    if (!this.ctx || !this.soundEnabled) return;
    const now = this.ctx.currentTime;

    // Double detuned sine wave for real car horn harmony (common at 400Hz and 415Hz)
    const osc1 = this.ctx.createOscillator();
    const osc2 = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc1.type = 'sine';
    osc1.frequency.setValueAtTime(410, now);
    osc2.type = 'sine';
    osc2.frequency.setValueAtTime(425, now);

    gain.gain.setValueAtTime(0.2, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.3);

    osc1.connect(gain);
    osc2.connect(gain);
    gain.connect(this.masterGain!);

    osc1.start(now);
    osc2.start(now);
    osc1.stop(now + 0.31);
    osc2.stop(now + 0.31);
  }

  // --- CONTINUOUS DRIVING ENGINE SOUND ---

  startContinuousEngine() {
    this.init();
    if (!this.ctx || !this.soundEnabled) return;
    if (this.activeEngineOsc) return; // already running

    const now = this.ctx.currentTime;

    this.activeEngineOsc = this.ctx.createOscillator();
    this.activeEngineGain = this.ctx.createGain();
    this.engineFilter = this.ctx.createBiquadFilter();

    // Deep rich sawtooth representing small cylinders
    this.activeEngineOsc.type = 'sawtooth';
    this.activeEngineOsc.frequency.setValueAtTime(45, now);

    this.engineFilter.type = 'lowpass';
    this.engineFilter.frequency.setValueAtTime(140, now);

    // Fade in low-level hum
    this.activeEngineGain.gain.setValueAtTime(0, now);
    this.activeEngineGain.gain.linearRampToValueAtTime(0.12, now + 0.3);

    this.activeEngineOsc.connect(this.engineFilter);
    this.engineFilter.connect(this.activeEngineGain);
    this.activeEngineGain.connect(this.masterGain!);

    this.activeEngineOsc.start(now);
  }

  updateContinuousEngine(speedRatio: number) {
    if (!this.ctx || !this.soundEnabled || !this.activeEngineOsc || !this.engineFilter) return;

    // Pitch rises dynamically based on speedRatio (from 0 to 1)
    const baseFreq = 45;
    const maxFreq = 160;
    const currentFreq = baseFreq + (speedRatio * (maxFreq - baseFreq));
    
    // Filter frequency opens up as RPM increases
    const baseCutoff = 130;
    const maxCutoff = 400;
    const currentCutoff = baseCutoff + (speedRatio * (maxCutoff - baseCutoff));

    this.activeEngineOsc.frequency.setTargetAtTime(currentFreq, this.ctx.currentTime, 0.1);
    this.engineFilter.frequency.setTargetAtTime(currentCutoff, this.ctx.currentTime, 0.15);
  }

  stopContinuousEngine() {
    if (this.activeEngineOsc && this.activeEngineGain && this.ctx) {
      const now = this.ctx.currentTime;
      try {
        this.activeEngineGain.gain.cancelScheduledValues(now);
        this.activeEngineGain.gain.setValueAtTime(this.activeEngineGain.gain.value, now);
        this.activeEngineGain.gain.exponentialRampToValueAtTime(0.001, now + 0.15);
        
        const osc = this.activeEngineOsc;
        setTimeout(() => {
          try {
            osc.stop();
          } catch(e) {}
        }, 200);
      } catch(e) {}
      
      this.activeEngineOsc = null;
      this.activeEngineGain = null;
      this.engineFilter = null;
    }
  }
}

export const sound = new SoundEngine();
