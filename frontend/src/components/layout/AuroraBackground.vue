<template>
  <div class="aurora-container">
    <div class="aurora" aria-hidden="true">
      <div class="aurora__glow aurora__glow--1"></div>
      <div class="aurora__glow aurora__glow--2"></div>
      <div class="aurora__glow aurora__glow--3"></div>
    </div>
    <div class="particles" aria-hidden="true">
      <span v-for="n in 15" :key="n" class="particle" :style="particleStyle(n)"></span>
    </div>
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
const particleStyle = (n) => {
  const size = Math.random() * 4 + 2
  const left = Math.random() * 100
  const delay = Math.random() * 20
  const duration = Math.random() * 10 + 15
  return {
    '--size': `${size}px`,
    '--left': `${left}%`,
    '--delay': `${delay}s`,
    '--duration': `${duration}s`,
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

.aurora-container {
  min-height: 100vh;
  width: 100%;
  position: relative;
  overflow: hidden;
  background: var(--bg-darker, #000000);
  font-family: 'Outfit', sans-serif;
}

.aurora {
  position: fixed;
  inset: 0;
  overflow: hidden;
  z-index: 0;
}

.aurora__glow {
  position: absolute;
  border-radius: 50%;
  opacity: 0.5;
  animation: float 20s ease-in-out infinite;
}

.aurora__glow--1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, var(--neon-magenta) 0%, var(--neon-magenta-dim) 50%, transparent 70%);
  top: -200px;
  left: -100px;
  animation-delay: 0s;
  filter: blur(80px);
  box-shadow:
    0 0 120px var(--neon-magenta),
    0 0 200px var(--neon-magenta-dim);
}

.aurora__glow--2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, var(--neon-cyan) 0%, var(--neon-cyan-dim) 50%, transparent 70%);
  bottom: -150px;
  right: -100px;
  animation-delay: -7s;
  filter: blur(80px);
  box-shadow:
    0 0 120px var(--neon-cyan),
    0 0 200px var(--neon-cyan-dim);
}

.aurora__glow--3 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, var(--neon-green) 0%, var(--neon-green-dim) 50%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -14s;
  filter: blur(80px);
  box-shadow:
    0 0 100px var(--neon-green),
    0 0 180px var(--neon-green-dim);
}

@keyframes float {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(50px, -40px) scale(1.15);
  }
  50% {
    transform: translate(-30px, 30px) scale(0.9);
  }
  75% {
    transform: translate(40px, 20px) scale(1.1);
  }
}

.particles {
  position: fixed;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 1;
}

.particle {
  position: absolute;
  width: var(--size);
  height: var(--size);
  border-radius: 50%;
  left: var(--left);
  animation: rise var(--duration) linear infinite;
  animation-delay: var(--delay);
  opacity: 0;
  box-shadow:
    0 0 6px var(--color),
    0 0 12px var(--color);
}

.particle:nth-child(3n + 1) {
  background: var(--neon-magenta);
  --color: var(--neon-magenta);
}
.particle:nth-child(3n + 2) {
  background: var(--neon-cyan);
  --color: var(--neon-cyan);
}
.particle:nth-child(3n) {
  background: var(--neon-green);
  --color: var(--neon-green);
}

@keyframes rise {
  0% {
    opacity: 0;
    transform: translateY(0) scale(0);
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 0.5;
  }
  100% {
    opacity: 0;
    transform: translateY(-100vh) scale(1);
  }
}

/* Reduced Motion Support */
@media (prefers-reduced-motion: reduce) {
  .aurora__glow {
    animation: none;
  }

  .particle {
    animation: none;
    opacity: 0.6;
  }
}
</style>
