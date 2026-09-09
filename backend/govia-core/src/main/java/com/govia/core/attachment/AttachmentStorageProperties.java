package com.govia.core.attachment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConfigurationProperties(prefix = "govia.attachment")
public class AttachmentStorageProperties {

    /** Thu muc goc luu file tren disk (dev). Production nen thay bang S3/MinIO impl khac cua AttachmentService. */
    private String rootPath = "./data/attachments";

    private long maxFileSizeMb = 25;

    /** Luon tra ve duong dan TUYET DOI. Neu rootPath da tuyet doi (vd GOVIA_ATTACHMENT_ROOT duoc set
     * thanh 1 duong dan co dinh o production) thi dung nguyen. Neu rootPath tuong doi (gia tri mac
     * dinh "./data/attachments"), phai chuan hoa no dua tren 1 diem neo ON DINH (user.home) CHU
     * KHONG dua tren working directory cua tien trinh luc goi ham nay - Path.of(...).toAbsolutePath()
     * don thuan se phu thuoc working directory LUC GOI, tuc la phu thuoc noi app duoc khoi dong (vd
     * IDE run-config khac, script khac, `cd` khac truoc khi chay mvnw). Neu 2 lan khoi dong app tu 2
     * thu muc khac nhau, duong dan tuyet doi tinh ra se khac nhau, nen file luu tu lan chay truoc se
     * "bien mat" o lan chay sau du storagePath trong DB khong doi - day chinh la nguyen nhan bug da
     * gap: "Báo cáo tiến độ -> tải file bị thất bại" (chi bien mat that bai TU 404 sach thay vi loi
     * vo response, khong con tai duoc). Neo vao user.home giai quyet dut diem vi no on dinh voi CUNG
     * 1 nguoi dung tren CUNG 1 may, bat ke tien trinh duoc khoi dong tu thu muc nao. */
    public String getRootPath() {
        Path configured = Path.of(rootPath);
        Path anchored = configured.isAbsolute() ? configured : Path.of(System.getProperty("user.home")).resolve(configured);
        return anchored.toAbsolutePath().normalize().toString();
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public long getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setMaxFileSizeMb(long maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }
}
