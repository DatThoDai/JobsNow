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
            "(c.candidateUser.userId = :userId AND c.unreadCountCandidate > 0 AND (c.deletedByCandidate = false OR c.deletedByCandidate IS NULL)) OR " +
            "(c.employerUser.userId = :userId AND c.unreadCountEmployer > 0 AND (c.deletedByEmployer = false OR c.deletedByEmployer IS NULL))")
    Long countUnreadConversations(Integer userId);

    @Query("SELECT c.conversationId FROM Conversation c WHERE " +
            "c.candidateUser.userId = :candidateId AND c.employerUser.userId = :employerId")
    Integer findIdByCandidateAndEmployer(Integer candidateId, Integer employerId);
}
