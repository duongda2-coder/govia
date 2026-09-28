/** Diem hien thi 2 chu so thap phan, bo so 0 thua (91.00 -> 91). */
export function formatScore(v: number | null | undefined): string | null {
  if (v === null || v === undefined) return null;
  return String(Math.round(v * 100) / 100);
}

export function formatPercent(v: number | null | undefined): string | null {
  if (v === null || v === undefined) return null;
  return `${Math.round(v * 10000) / 100}%`;
}
