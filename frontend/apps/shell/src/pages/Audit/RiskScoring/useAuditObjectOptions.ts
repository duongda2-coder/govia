import { useCallback, useEffect, useState } from "react";
import { auditObjectCategoryApi } from "../../../api/riskScoring";

/**
 * Nap danh muc goc "Loai doi tuong kiem toan" (sheet ZTC_Loai_Dtkt) thanh Select options - dung
 * chung cho truong "Loai doi tuong kiem toan" o Group1/CriteriaQualitative/CriteriaQuantitative.
 */
export function useAuditObjectOptions() {
  const [loading, setLoading] = useState(false);
  const [options, setOptions] = useState<{ value: string; label: string }[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const categories = await auditObjectCategoryApi.list();
      setOptions(categories.map((c) => ({ value: c.id, label: `${c.code} - ${c.name}` })));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return { options, loading, reload: load };
}
