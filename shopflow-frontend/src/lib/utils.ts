import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/** Full-page navigation (used to hand the customer over to Stripe's hosted payment page). */
export function redirectTo(url: string) {
  window.location.assign(url)
}

/** Extracts a human-readable message from an axios/ProblemDetail error response. */
export function apiErrorMessage(err: unknown, fallback: string): string {
  const detail = (err as { response?: { data?: { detail?: unknown } } } | null)?.response?.data?.detail
  return typeof detail === "string" && detail ? detail : fallback
}
