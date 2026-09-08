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

    /** Luon tra ve duong dan TUYET DOI (chuan hoa tu rootPath, dau la relative hay khong) - neu
     * de rootPath tuong doi ("./data/attachments" mac dinh), duong dan luu vao DB (storagePath) se
     * phu thuoc working directory LUC UPLOAD; lan sau app khoi dong tu 1 thu muc lam viec khac (vd
     * IDE run-config khac, script khac) thi file cu khong con tim thay duoc dan den download that
     * bai du list/upload van chay binh thuong (bug da gap: "Báo cáo tiến độ -> tải file bị thất bại"). */
    public String getRootPath() {
        return Path.of(rootPath).toAbsolutePath().normalize().toString();
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
