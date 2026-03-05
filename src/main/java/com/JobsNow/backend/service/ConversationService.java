package com.JobsNow.backend.service;

import com.JobsNow.backend.request.SendFileMessageRequest;
import com.JobsNow.backend.request.SendTextMessageRequest;
import com.JobsNow.backend.response.ConversationResponse;
import com.JobsNow.backend.response.MessageResponse;

import java.util.List;

public interface ConversationService {
    ConversationResponse createConversation(Integer candidateId, Integer employerId);
    List<ConversationResponse> getUserConversations(Integer userId);
    ConversationResponse getConversationById(Integer conversationId, Integer userId);
    MessageResponse sendTextMessage(SendTextMessageRequest request);
    MessageResponse sendFileMessage(SendFileMessageRequest request);
    List<MessageResponse> getMessagesByConversationId(Integer conversationId);
    void markMessagesAsRead(Integer conversationId, Integer userId);
    Long countUnreadConversations(Integer userId);
    Integer findConversationId(Integer candidateId, Integer employerId);
}
