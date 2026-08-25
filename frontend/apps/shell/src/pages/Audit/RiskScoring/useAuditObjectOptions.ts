import { useCallback, useEffect, useState } from "react";
import {
  auditObjectProcessApi,
  auditObjectProjectApi,
  auditObjectSubsidiaryApi,
  auditObjectUnitApi,
  AUDIT_OBJECT_REF_TYPE_LABELS,
  type AuditObjectRefType,
} from "../../../api/riskScoring";

export function auditObjectRefValue(type: AuditObjectRefType, id: string): string {
  return `${type}:${id}`;
}

export function parseAuditObjectRefValue(value: string): { type: AuditObjectRefType; id: string } {
  const separatorIndex = value.indexOf(":");
  return { type: value.slice(0, separatorIndex) as AuditObjectRefType, id: value.slice(separatorIndex + 1) };
}

interface AuditObjectOptionGroup {
  label: string;
  options: { value: string; label: string }[];
}

/**
 * Nap gop 4 danh muc "Doi tuong kiem toan" (Don vi/Cong ty con/Du an/Quy trinh) thanh 1 Select gom
 * nhom, dung chung cho truong "Doi tuong kiem toan" o Group1/CriteriaQualitative/CriteriaQuantitative
 * - thay the truoc day la enum "Loai doi tuong" fix cung.
 */
export function useAuditObjectOptions() {
  const [loading, setLoading] = useState(false);
  const [groups, setGroups] = useState<AuditObjectOptionGroup[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [units, subsidiaries, projects, processes] = await Promise.all([
        auditObjectUnitApi.list(),
        auditObjectSubsidiaryApi.list(),
        auditObjectProjectApi.list(),
        auditObjectProcessApi.list(),
      ]);
      setGroups([
        {
          label: AUDIT_OBJECT_REF_TYPE_LABELS.UNIT,
          options: units.map((u) => ({ value: auditObjectRefValue("UNIT", u.id), label: `${u.code} - ${u.name}` })),
        },
        {
          label: AUDIT_OBJECT_REF_TYPE_LABELS.SUBSIDIARY,
          options: subsidiaries.map((s) => ({ value: auditObjectRefValue("SUBSIDIARY", s.id), label: `${s.code} - ${s.name}` })),
        },
        {
          label: AUDIT_OBJECT_REF_TYPE_LABELS.PROJECT,
          options: projects.map((p) => ({ value: auditObjectRefValue("PROJECT", p.id), label: `${p.code} - ${p.name}` })),
        },
        {
          label: AUDIT_OBJECT_REF_TYPE_LABELS.PROCESS,
          options: processes.map((p) => ({ value: auditObjectRefValue("PROCESS", p.id), label: `${p.code} - ${p.name}` })),
        },
      ]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return { groups, loading, reload: load };
}
