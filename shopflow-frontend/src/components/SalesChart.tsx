"use client";

import { useMemo } from "react";
import type { Order } from "@/lib/types";
import { parseServerDate } from "@/lib/utils";

const DAYS = 14;

function dayKey(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

/** Revenue (confirmed orders) and order count for each of the last 14 days, drawn as plain SVG. */
export default function SalesChart({ orders }: { orders: Order[] }) {
  const days = useMemo(() => {
    const list = Array.from({ length: DAYS }, (_, i) => {
      const d = new Date();
      d.setHours(0, 0, 0, 0);
      d.setDate(d.getDate() - (DAYS - 1 - i));
      return { date: d, key: dayKey(d), revenue: 0, orders: 0 };
    });
    const byKey = new Map(list.map((d) => [d.key, d]));
    for (const o of orders) {
      const day = byKey.get(dayKey(parseServerDate(o.createdAt)));
      if (!day) continue;
      day.orders += 1;
      if (o.status === "CONFIRMED") day.revenue += o.totalAmount;
    }
    return list;
  }, [orders]);

  const max = Math.max(1, ...days.map((d) => d.revenue));
  const W = 700, H = 220, PAD = { l: 48, r: 8, t: 12, b: 28 };
  const innerW = W - PAD.l - PAD.r, innerH = H - PAD.t - PAD.b;
  const slot = innerW / DAYS;
  const money = (n: number) => `$${n.toLocaleString("en-US", { maximumFractionDigits: 0 })}`;
  const ticks = [0, 0.5, 1].map((f) => max * f);
  const empty = days.every((d) => d.orders === 0);

  return (
    <div>
      <svg viewBox={`0 0 ${W} ${H}`} className="w-full h-auto" role="img" aria-label="Revenue per day, last 14 days">
        {ticks.map((t) => {
          const y = PAD.t + innerH - (t / max) * innerH;
          return (
            <g key={t}>
              <line x1={PAD.l} x2={W - PAD.r} y1={y} y2={y} stroke="currentColor" className="text-slate-200" />
              <text x={PAD.l - 6} y={y + 4} textAnchor="end" className="fill-slate-400" fontSize="11">{money(t)}</text>
            </g>
          );
        })}
        {days.map((d, i) => {
          const h = (d.revenue / max) * innerH;
          const x = PAD.l + i * slot + slot * 0.15;
          return (
            <g key={d.key}>
              <rect x={x} y={PAD.t + innerH - h} width={slot * 0.7} height={Math.max(h, d.revenue > 0 ? 2 : 0)} rx={3} className="fill-blue-500">
                <title>{`${d.date.toLocaleDateString("en-US", { month: "short", day: "numeric" })}: ${money(d.revenue)} · ${d.orders} order${d.orders === 1 ? "" : "s"}`}</title>
              </rect>
              {(i % 2 === 0 || i === DAYS - 1) && (
                <text x={x + (slot * 0.7) / 2} y={H - 8} textAnchor="middle" className="fill-slate-400" fontSize="10">
                  {d.date.toLocaleDateString("en-US", { month: "numeric", day: "numeric" })}
                </text>
              )}
            </g>
          );
        })}
      </svg>
      {empty && <p className="text-center text-sm text-slate-400">No orders in the last {DAYS} days.</p>}
    </div>
  );
}
