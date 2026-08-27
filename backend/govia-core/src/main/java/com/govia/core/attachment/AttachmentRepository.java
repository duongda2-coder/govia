package com.govia.core.attachment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByEntityNameAndEntityId(String entityName, UUID entityId);

    @Query("select a.entityId as entityId, count(a) as total from Attachment a " +
            "where a.entityName = :entityName and a.entityId in :entityIds group by a.entityId")
    List<EntityAttachmentCount> countByEntityNameAndEntityIdIn(@Param("entityName") String entityName,
                                                                @Param("entityIds") Collection<UUID> entityIds);

    interface EntityAttachmentCount {
        UUID getEntityId();

        Long getTotal();
    }
}
