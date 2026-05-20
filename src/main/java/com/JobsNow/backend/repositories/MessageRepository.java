package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByConversation_ConversationIdOrderBySentAtAsc(Integer conversationId);

    List<Message> findByConversation_ConversationIdOrderByMessageIdDesc(
            Integer conversationId, Pageable pageable);

    List<Message> findByConversation_ConversationIdAndMessageIdLessThanOrderByMessageIdDesc(
            Integer conversationId, Integer messageId, Pageable pageable);

    Message findTopByConversation_ConversationIdOrderBySentAtDesc(Integer conversationId);

    void deleteByConversation_ConversationId(Integer conversationId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Message m SET m.isRead = true
            WHERE m.conversation.conversationId = :conversationId
              AND m.isRead = false
              AND m.sender.userId <> :userId
            """)
    int markUnreadAsReadForUser(
            @Param("conversationId") Integer conversationId,
            @Param("userId") Integer userId);
}
