"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { ShippingAddress } from "@/lib/types";

export const COUNTRIES = [
  { code: "US", name: "United States" },
  { code: "CA", name: "Canada" },
  { code: "GB", name: "United Kingdom" },
  { code: "IN", name: "India" },
  { code: "AU", name: "Australia" },
];

export const emptyAddress: ShippingAddress = {
  fullName: "",
  line1: "",
  line2: "",
  city: "",
  state: "",
  postalCode: "",
  country: "US",
  phone: "",
};

export function formatAddress(a: ShippingAddress) {
  const country = COUNTRIES.find((c) => c.code === a.country)?.name ?? a.country;
  return [a.fullName, a.line1, a.line2, `${a.city}, ${a.state} ${a.postalCode}`, country, a.phone]
    .filter(Boolean)
    .join("\n");
}

interface Props {
  initial?: ShippingAddress;
  submitLabel: string;
  /** Show the "save to my address book" checkbox (checkout only). */
  allowSave?: boolean;
  pending?: boolean;
  onSubmit: (address: ShippingAddress, save: boolean) => void;
  onCancel?: () => void;
}

/** Delivery address form. Card / bank details are never collected here — they go to Stripe's hosted page. */
export default function AddressForm({ initial = emptyAddress, submitLabel, allowSave, pending, onSubmit, onCancel }: Props) {
  const [form, setForm] = useState<ShippingAddress>({ ...emptyAddress, ...initial });
  const [save, setSave] = useState(true);

  const set = (key: keyof ShippingAddress) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  return (
    <form
      className="space-y-3"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit(form, save);
      }}
    >
      <div className="space-y-1">
        <Label htmlFor="fullName">Full name</Label>
        <Input id="fullName" required maxLength={100} autoComplete="name" value={form.fullName} onChange={set("fullName")} />
      </div>
      <div className="space-y-1">
        <Label htmlFor="line1">Address</Label>
        <Input id="line1" required maxLength={150} autoComplete="address-line1" placeholder="Street address" value={form.line1} onChange={set("line1")} />
      </div>
      <div className="space-y-1">
        <Label htmlFor="line2">Apartment, suite, etc. (optional)</Label>
        <Input id="line2" maxLength={150} autoComplete="address-line2" value={form.line2 ?? ""} onChange={set("line2")} />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <Label htmlFor="city">City</Label>
          <Input id="city" required maxLength={100} autoComplete="address-level2" value={form.city} onChange={set("city")} />
        </div>
        <div className="space-y-1">
          <Label htmlFor="state">State / region</Label>
          <Input id="state" required maxLength={100} autoComplete="address-level1" value={form.state} onChange={set("state")} />
        </div>
        <div className="space-y-1">
          <Label htmlFor="postalCode">Postal code</Label>
          <Input id="postalCode" required maxLength={20} autoComplete="postal-code" value={form.postalCode} onChange={set("postalCode")} />
        </div>
        <div className="space-y-1">
          <Label htmlFor="country">Country</Label>
          <select
            id="country"
            required
            autoComplete="country"
            className="h-8 w-full rounded-lg border border-input bg-transparent px-2 text-sm"
            value={form.country}
            onChange={set("country")}
          >
            {COUNTRIES.map((c) => (
              <option key={c.code} value={c.code}>{c.name}</option>
            ))}
          </select>
        </div>
      </div>
      <div className="space-y-1">
        <Label htmlFor="phone">Phone (optional)</Label>
        <Input id="phone" type="tel" maxLength={30} autoComplete="tel" value={form.phone ?? ""} onChange={set("phone")} />
      </div>

      {allowSave && (
        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input type="checkbox" checked={save} onChange={(e) => setSave(e.target.checked)} />
          Save this address for next time
        </label>
      )}

      <div className="flex gap-2 pt-1">
        <Button type="submit" disabled={pending}>{submitLabel}</Button>
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel}>Cancel</Button>
        )}
      </div>
    </form>
  );
}
