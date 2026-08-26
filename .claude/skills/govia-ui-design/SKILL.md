---
name: govia-ui-design
description: UI/UX design guidelines for the GOVIA platform's antd-based frontend (frontend/apps/shell, frontend/packages/govia-ui-kit). Use this whenever building a new screen, adding a tab, designing a form/modal, or editing the visual layout of any existing screen in this repo - even if the user just asks to "add a page", "add a tab", "make a CRUD screen", or "make this look nicer", not only when they explicitly say "UI/UX" or "design". Do NOT use for backend, business logic, or pure bug-fixing that has no visual/layout component.
---

# GOVIA UI Design

GOVIA's frontend is one antd v5 app with dozens of near-identical CRUD/master-data screens
built by different sessions over time. The single biggest risk to visual quality here isn't bad
taste — it's a new screen quietly drifting from the pattern every other screen already uses.
Consistency *is* the design system. Before writing custom layout, check whether the platform
already has a component or convention for it.

## Ground truth: read these before designing

Don't guess the theme or patterns — they're small enough to just read:

- `frontend/apps/shell/src/AppRoot.tsx` — the antd `ConfigProvider` theme tokens (currently
  `colorPrimary #2563eb`, `colorBgLayout #eff6ff`, `borderRadius 8`, default antd font stack).
  Never hardcode a color that duplicates a token — reference the token or let antd apply it.
- `frontend/packages/govia-ui-kit/src/components/CrudTable.tsx` and `StandardToolbar.tsx` —
  the mandatory shell for every list screen. `CrudTable` sets `scroll={{ x: "max-content" }}`
  and `getSearchColumnProps` (in `serverColumnHelpers.tsx`) sets `ellipsis: true` on every
  code/name search column — both platform-wide, do not repeat per-screen. See the "Long text in
  a free-width column" callout below before touching either.
- One recent, clean example screen, e.g.
  `frontend/apps/shell/src/pages/Audit/RiskScoring/AuditObjectCategoryTable.tsx` — copy its
  shape for a new CRUD tab rather than designing from scratch.

## The pattern (what "consistent" means here)

**List screen** = `<CrudTable>` (a `Card` wrapping `StandardToolbar` + antd `Table`) inside a
page component that owns `items` / `loading` / `selected` / `modalOpen` / `editing` state.
- Row selection is single-select (`rowSelection.onChange` capturing one row), used to gate
  Edit/Delete — not row checkboxes for bulk multi-select. Don't introduce bulk actions unless
  a screen actually needs them.
- Column search goes through `useClientSearchColumn` from `@govia/ui-kit`, not a separate
  search `Input` above the table.
- Toolbar buttons only render when their handler prop is passed (`onAdd`, `onEdit`, `onDelete`,
  `onImport`, `onExportExcel`, `onExportWord`, `onAttachment`) — gated further by
  `hasPermission("MODULE.ACTION")` from `useAuth()`. A screen with no `VIEW` permission returns
  `null` entirely; individual buttons disappear (not disable) when the specific permission is
  missing. Keep button order Add → Edit → Delete → Import → Export Excel → Export Word →
  Attachment, matching `StandardToolbar`'s own order — don't reorder per screen.

**Add/Edit** = one antd `Modal` reused for both (title swaps between "create"/"edit" via
`t("riskScoring.form.createTitle" / "editTitle")`), containing a `Form<FormValues>` with
`layout="vertical"`. `destroyOnClose` on the Modal so stale field state doesn't leak between
create and edit.

**Delete** = `modal.confirm` from `App.useApp()`, never a custom confirmation dialog.

**Messages** = always `const { message, modal } = App.useApp()` inside the component. Never
`import { message } from "antd"` statically — antd's `App` wrapper is what gives message/modal
access to the theme and correct z-index stacking; a static import silently breaks that.

**Multi-tab grouping** = a page component wraps related sub-catalogs in one antd `Tabs`
(`items={[{key, label: t(...), children: <XTable/>}, ...]}`) rather than separate routes/menu
entries per catalog. Add a tab, don't add a menu item, when a new catalog is a variant of an
existing group (see `RiskScoringAuditObjectsPage.tsx`'s 5 tabs).

**i18n** = every user-facing string is `t("namespace.key")`, added to *both*
`frontend/apps/shell/src/i18n/locales/vi.json` (primary/reference) and `en.json`. A hardcoded
Vietnamese or English string in JSX is a bug here, not a style nit — grep the two locale files
for an existing key (`code`, `name`, `note`, `active`, common `add`/`edit`/`delete`...) before
inventing a new one; most field labels already exist under `riskScoring.columns.*` or
`common.*`.

## Do / Don't, grounded in the actual antd usage here

| Situation | Do | Don't |
|---|---|---|
| Wrapping a list/table | `<CrudTable>` | A bare `<Table>` in a `<div>`, or a hand-rolled `<Card><Table/></Card>` that reimplements the toolbar |
| Action buttons | Pass handlers to `StandardToolbar` props | Add ad-hoc `<Button>`s beside the table for actions the toolbar already models |
| Spacing between toolbar and table | Let `CrudTable`'s built-in `marginTop: 16` do it | Add another wrapping `<div style={{marginTop: ...}}>` around `CrudTable` |
| Grouping form fields | `Form.Item` per field, vertical layout, logical top-to-bottom order (identifying fields first, flags/notes last) | `Row`/`Col` grid forms for a short catalog form (adds complexity these simple entity forms don't need) — reserve grid layout for forms with 8+ fields that genuinely need two columns |
| Modal width | Default width for short forms (≤5 fields); only pass an explicit `width` when the form has a wide element (e.g. a table picker) that needs it | Widening every modal "for breathing room" — a wide modal around 3 short fields looks unfinished |
| Empty/optional text columns | `render: (v) => v ?? "-"` | Empty cell, or `null` rendered as literal text |
| Boolean/status columns | Render translated labels (`t("common.active")`/`t("common.inactive")`), with a `sorter` if the column is meaningful to sort by | Render raw `true`/`false` or an untranslated string |
| Icons | `@ant-design/icons`, and only the icon that already means that action elsewhere (`PlusOutlined`=add, `EditOutlined`=edit, `DeleteOutlined`=delete, `FileExcelOutlined`/`FileWordOutlined`=export, `UploadOutlined`=import, `PaperClipOutlined`=attachment) | Inventing a new icon for an action that already has a platform-wide icon, or omitting icons on toolbar buttons other screens show icons for |
| Destructive actions | `danger` prop on the Delete `Button`, confirm via `modal.confirm` with `okButtonProps: { danger: true }` | A plain-styled delete button, or deleting without confirmation |
| Column widths | Set an explicit `width` on short/fixed-content columns (code, status, numeric) so the table doesn't reflow oddly; leave free-text columns (name, note) unset to fill remaining space | Fixing width on every column so the table doesn't fill the container width, or leaving all columns unset so a status column stretches absurdly wide |

## Callout: long text in a free-width column

A real bug hit in this repo, worth naming so it doesn't recur. A "note"/"name" column left
without an explicit `width` (correct — see the column-width row in the table above) will, by
default, just word-wrap a long value across many lines, blowing up that one row's height and
making the whole table look broken next to its short-value neighbors.

The instinct is to add `ellipsis: true` to that column (which `getSearchColumnProps` now does
for you, platform-wide). But `ellipsis: true` on any column switches the *whole table* to
`table-layout: fixed`, and a fixed layout needs to know the table's total available width to
divide it between fixed-width and free columns. Without that, the free column can render at
**0px — the text silently disappears**, not just wraps. The fix is `scroll={{ x: "max-content" }}`
on the `Table` itself (already set in `CrudTable`), which gives the layout engine a width to
divide and makes the table scroll horizontally in its own container if content genuinely needs
more room than the page has.

The two only work together. If you ever build a table outside `CrudTable` (see "When you
genuinely need something new" below), and any column needs `ellipsis`, carry both — the
`ellipsis: true` and the `scroll={{ x: "max-content" }}` — or you'll reintroduce this bug in a
new place.

## Visual "polish" pass — do this after the screen works functionally

Functional correctness and visual polish are different passes; don't ship after only the first.
Once a screen/tab works, walk through it as if you were a user seeing it for the first time:

1. **Does it look like its siblings?** Open a neighboring tab/screen side by side (e.g. if
   you just added a tab to `RiskScoringAuditObjectsPage`, compare it against the other 4 tabs).
   Same column title casing, same "-" for empty values, same button set shape, same modal
   field order convention (code → name → descriptive fields → active toggle last).
2. **Translation completeness.** Every label, button, column title, and validation message
   goes through `t()` with an entry in *both* `vi.json` and `en.json` — not just the one you're
   testing in.
3. **Loading and empty states.** `loading` prop wired from a real fetch state (not hardcoded
   `false`), so `CrudTable`'s built-in `Table` loading spinner shows during fetch. If a table
   can legitimately be empty (a fresh tenant, a filtered-to-nothing search), confirm antd's
   default empty state reads sensibly — don't suppress or replace it without reason.
4. **Density.** Scan for accidental double-spacing (nested `Card`s, a manual margin stacked on
   top of a component that already has one) or cramped spacing (form fields with no `Form.Item`
   gap because a raw `<div>` was used instead).
5. **Table overflow.** If a table has many columns or one very wide column, check it doesn't
   force horizontal scroll on the whole page — antd `Table`'s own horizontal scroll
   (`scroll={{ x: ... }}`) should contain it, not the page layout. This is already set on
   `CrudTable`; if you ever build a table outside it, don't skip this prop — see the callout
   below for why.
6. **Permission gating actually hides things.** Log in mentally as a role without
   Create/Edit/Delete/Export/Import and confirm the corresponding buttons are absent, not just
   disabled-looking placeholders.
7. **Modal sizing matches field count.** A 3-field modal shouldn't feel like it's floating in a
   sea of whitespace; an 8+ field modal shouldn't feel cramped into the default width.

## When you genuinely need something new

If a screen needs a layout `CrudTable`/`StandardToolbar` truly can't express (e.g. a
tree/hierarchy view, a dashboard, a wizard), that's fine — GOVIA doesn't force every screen into
the CRUD mold. But first check there isn't already a second established pattern elsewhere in
the repo for that shape of screen (grep for similar page names or component names before
building from scratch), and keep using the same antd theme tokens and `App.useApp()` messaging
convention regardless of layout shape.
