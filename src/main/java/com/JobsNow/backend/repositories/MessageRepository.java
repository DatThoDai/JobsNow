package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByConversation_ConversationIdOrderBySentAtAsc(Integer conversationId);
    Message findTopByConversation_ConversationIdOrderBySentAtDesc(Integer conversationId);
    void deleteByConversation_ConversationId(Integer conversationId);
}
