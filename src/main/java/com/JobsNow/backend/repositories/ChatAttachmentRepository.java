package com.JobsNow.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.JobsNow.backend.entity.ChatAttachment;

public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Integer> {
    ChatAttachment findByMessage_MessageId(Integer messageId);
}
