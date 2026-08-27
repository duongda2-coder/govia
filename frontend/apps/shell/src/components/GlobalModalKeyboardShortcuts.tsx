import { useEffect } from "react";

const OPEN_POPUP_SELECTOR =
  ".ant-select-dropdown, .ant-picker-dropdown, .ant-cascader-dropdown, .ant-tree-select-dropdown, .ant-mentions-dropdown, .ant-dropdown";

function isVisible(el: Element): boolean {
  return (el as HTMLElement).offsetParent !== null;
}

/**
 * Enter = OK, Esc = Cancel/Dong cho MOI Modal cua platform (yeu cau: "tat ca cac man hinh").
 *
 * Esc da hoat dong san theo mac dinh cua antd Modal (prop `keyboard` mac dinh la true) va
 * Modal.confirm (dung cho cac hop thoai Xoa) da tu auto-focus nut OK (`autoFocusButton: 'ok'`
 * mac dinh trong antd), nen phim Enter tren no da duoc trinh duyet xu ly san (nut dang focus).
 *
 * Cai con thieu la Modal thuong (man hinh Them/Sua - `<Modal onOk={handleSubmit}>` dung o moi
 * trang) khong auto-focus nut nao ca, nen go Enter trong 1 o Input khong lam gi. Component nay
 * gan 1 listener DUY NHAT o document, thay vi sua tay tung Modal (~28 man hinh) - bam Enter se
 * tu tim Modal dang hien tren cung va click nut OK (".ant-btn-primary") cua no, TRU cac truong
 * hop Enter co y nghia khac: dang go trong textarea (xuong dong), dang focus san 1 button/link
 * (de trinh duyet tu kich hoat, tranh bam 2 lan), hoac 1 dropdown antd (Select/DatePicker/...)
 * dang mo (de rc-xxx tu chon option dang highlight).
 */
export function GlobalModalKeyboardShortcuts() {
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key !== "Enter") return;
      const target = e.target;
      if (!(target instanceof HTMLElement)) return;

      if (target.tagName === "TEXTAREA") return;
      if (target.tagName === "BUTTON" || target.tagName === "A") return;

      const hasOpenPopup = Array.from(document.querySelectorAll(OPEN_POPUP_SELECTOR)).some(isVisible);
      if (hasOpenPopup) return;

      const visibleWraps = Array.from(document.querySelectorAll(".ant-modal-wrap")).filter(isVisible);
      if (visibleWraps.length === 0) return;
      const topWrap = visibleWraps[visibleWraps.length - 1];

      const okButton = topWrap.querySelector<HTMLButtonElement>(".ant-modal-footer button.ant-btn-primary");
      if (okButton && !okButton.disabled) {
        e.preventDefault();
        okButton.click();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, []);

  return null;
}
