import type { CSSProperties } from 'react'

const paths = {
  lock: 'M7 10V7a5 5 0 0 1 10 0v3M6 10h12a1 1 0 0 1 1 1v9H5v-9a1 1 0 0 1 1-1Zm6 4v3',
  arrow: 'M5 12h14m-5-5 5 5-5 5',
  upload: 'M12 15V3m-4 4 4-4 4 4M4 15v5h16v-5',
  download: 'M12 3v12m-4-4 4 4 4-4M4 16v4h16v-4',
  file: 'M13 3H5v18h14V9l-6-6Zm0 0v6h6M8 13h8m-8 4h5',
  key: 'M14 14a6 6 0 1 0-4-4L3 17v4h4v-3h3v-3l4-1ZM17 7h.01',
  shield: 'M12 3 3 7v5c0 5 9 10 9 10s9-5 9-10V7l-9-4Zm-4 9 3 3 5-6',
  sign: 'm4 16-1 5 5-1L21 7l-4-4L4 16Zm10-10 4 4M3 22h18',
  check: 'm5 12 4 4L19 6',
  x: 'm6 6 12 12M6 18 18 6',
  info: 'M12 10v7m0-11h.01M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z',
  help: 'M9 8a3 3 0 1 1 5 2c-2 1-2 2-2 3m0 4h.01M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z',
  transfer: 'M3 7h17m-4-4 4 4-4 4M21 17H4m4-4-4 4 4 4',
  chevron: 'm9 5 7 7-7 7',
  plus: 'M12 4v16M4 12h16',
} as const

export function Icon({ name, size = 20, style }: { name: keyof typeof paths; size?: number; style?: CSSProperties }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" style={style}><path d={paths[name]} /></svg>
}
