package com.govia.core.attachment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "govia.attachment")
public class AttachmentStorageProperties {

    /** Thu muc goc luu file tren disk (dev). Production nen thay bang S3/MinIO impl khac cua AttachmentService. */
    private String rootPath = "./data/attachments";

    private long maxFileSizeMb = 25;

    public String getRootPath() {
        return rootPath;
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
