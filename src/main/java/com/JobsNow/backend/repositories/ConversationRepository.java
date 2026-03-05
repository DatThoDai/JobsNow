package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Conversation;
import com.JobsNow.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {
    List<Conversation> findByCandidateUser(User candidate);
    List<Conversation> findByEmployerUser(User employer);

    Optional<Conversation> findByCandidateUserAndEmployerUser(User candidate, User employer);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE " +
            "(c.candidateUser.userId = :userId AND c.unreadCountCandidate > 0) OR " +
            "(c.employerUser.userId = :userId AND c.unreadCountEmployer > 0)")
    Long countUnreadConversations(Integer userId);

    @Query("SELECT c.conversationId FROM Conversation c WHERE " +
            "c.candidateUser.userId = :candidateId AND c.employerUser.userId = :employerId")
    Integer findIdByCandidateAndEmployer(Integer candidateId, Integer employerId);
}
