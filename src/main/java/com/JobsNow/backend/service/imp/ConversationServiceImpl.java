package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.constants.JobsNowConstant;
import com.JobsNow.backend.entity.*;
import com.JobsNow.backend.entity.enums.MessageType;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.repositories.*;
import com.JobsNow.backend.request.SendFileMessageRequest;
import com.JobsNow.backend.request.SendTextMessageRequest;
import com.JobsNow.backend.response.AttachmentResponse;
import com.JobsNow.backend.response.ConversationResponse;
import com.JobsNow.backend.response.MessageResponse;
import com.JobsNow.backend.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final CompanyRepository companyRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatAttachmentRepository chatAttachmentRepository;
    @Override
    public ConversationResponse createConversation(Integer candidateId, Integer employerId) {
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate not found"));
        User employer = userRepository.findById(employerId)
                .orElseThrow(() -> new NotFoundException("Employer not found"));
        var existing = conversationRepository.findByCandidateUserAndEmployerUser(candidate, employer);
        if (existing.isPresent()) {
            return buildConversationResponse(existing.get(), candidateId);
        }
        Conversation conversation = Conversation.builder()
                .candidateUser(candidate)
                .employerUser(employer)
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .unreadCountCandidate(0)
                .unreadCountEmployer(0)
                .build();
        Conversation saved = conversationRepository.save(conversation);
        ConversationResponse response = buildConversationResponse(saved, candidateId);
        messagingTemplate.convertAndSend(
                JobsNowConstant.WS_TOPIC_DATA_CONVERSATION + candidateId, response);
        messagingTemplate.convertAndSend(
                JobsNowConstant.WS_TOPIC_DATA_CONVERSATION + employerId, response);
        return response;
    }

    @Override
    public List<ConversationResponse> getUserConversations(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        List<Conversation> conversations;
        String roleName = user.getRole().getRoleName();
        if ("ROLE_JOBSEEKER".equals(roleName)) {
            conversations = conversationRepository.findByCandidateUser(user);
        } else if ("ROLE_COMPANY".equals(roleName)) {
            conversations = conversationRepository.findByEmployerUser(user);
        } else {
            throw new BadRequestException("Unsupported role for chat");
        }
        return conversations.stream()
                .map(c -> buildConversationResponse(c, userId))
                .collect(Collectors.toList());
    }

    @Override
    public ConversationResponse getConversationById(Integer conversationId, Integer userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        return buildConversationResponse(conversation, userId);
    }

    @Override
    public MessageResponse sendTextMessage(SendTextMessageRequest request) {
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new NotFoundException("Sender not found"));

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .messageType(MessageType.TEXT)
                .build();
        messageRepository.save(message);

        boolean senderIsCandidate = conversation.getCandidateUser()
                .getUserId().equals(request.getSenderId());
        Integer recipientId;
        if (senderIsCandidate) {
            conversation.setUnreadCountEmployer(conversation.getUnreadCountEmployer() + 1);
            recipientId = conversation.getEmployerUser().getUserId();
        } else {
            conversation.setUnreadCountCandidate(conversation.getUnreadCountCandidate() + 1);
            recipientId = conversation.getCandidateUser().getUserId();
        }
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        Long totalUnread = conversationRepository.countUnreadConversations(recipientId);
        messagingTemplate.convertAndSend(
                JobsNowConstant.WS_TOPIC_COUNT_UNREAD + recipientId, totalUnread);
        return buildMessageResponse(message);
    }

    @Override
    public MessageResponse sendFileMessage(SendFileMessageRequest request) {
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new NotFoundException("Sender not found"));

        MessageType type = request.getFileType() != null && request.getFileType().startsWith("image")
                ? MessageType.IMAGE
                : MessageType.FILE;

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .messageType(type)
                .build();
        messageRepository.save(message);

        ChatAttachment attachment = ChatAttachment.builder()
                .message(message)
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .filePath(request.getFilePath())
                .uploadTime(LocalDateTime.now())
                .build();
        chatAttachmentRepository.save(attachment);

        boolean senderIsCandidate = conversation.getCandidateUser()
                .getUserId().equals(request.getSenderId());
        Integer recipientId;
        if (senderIsCandidate) {
            conversation.setUnreadCountEmployer(conversation.getUnreadCountEmployer() + 1);
            recipientId = conversation.getEmployerUser().getUserId();
        } else {
            conversation.setUnreadCountCandidate(conversation.getUnreadCountCandidate() + 1);
            recipientId = conversation.getCandidateUser().getUserId();
        }
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        Long totalUnread = conversationRepository.countUnreadConversations(recipientId);
        messagingTemplate.convertAndSend(
                JobsNowConstant.WS_TOPIC_COUNT_UNREAD + recipientId, totalUnread);
        return buildMessageResponse(message);
    }

    @Override
    public List<MessageResponse> getMessagesByConversationId(Integer conversationId) {
        return messageRepository
                .findByConversation_ConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(this::buildMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void markMessagesAsRead(Integer conversationId, Integer userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        List<Message> messages = messageRepository
                .findByConversation_ConversationIdOrderBySentAtAsc(conversationId);
        for (Message msg : messages) {
            if (!msg.getIsRead() && !msg.getSender().getUserId().equals(userId)) {
                msg.setIsRead(true);
                messageRepository.save(msg);
            }
        }
        if (conversation.getCandidateUser().getUserId().equals(userId)) {
            conversation.setUnreadCountCandidate(0);
        } else {
            conversation.setUnreadCountEmployer(0);
        }
        conversationRepository.save(conversation);
        Long totalUnread = conversationRepository.countUnreadConversations(userId);
        messagingTemplate.convertAndSend(
                JobsNowConstant.WS_TOPIC_COUNT_UNREAD + userId, totalUnread);
    }

    @Override
    public Long countUnreadConversations(Integer userId) {
        return conversationRepository.countUnreadConversations(userId);
    }

    @Override
    public Integer findConversationId(Integer candidateId, Integer employerId) {
        Integer id = conversationRepository.findIdByCandidateAndEmployer(candidateId, employerId);
        return id != null ? id : 0;
    }

    private ConversationResponse buildConversationResponse(Conversation c, Integer currentUserId) {
        boolean isCandidate = c.getCandidateUser().getUserId().equals(currentUserId);
        User otherUser = isCandidate ? c.getEmployerUser() : c.getCandidateUser();
        String otherName;
        String otherAvatar;
        String otherRole = otherUser.getRole().getRoleName();
        if ("ROLE_COMPANY".equals(otherRole)) {
            Company company = companyRepository.findByUser_UserId(otherUser.getUserId())
                    .orElse(null);
            otherName = company != null ? company.getCompanyName() : otherUser.getFullName();
            otherAvatar = company != null ? company.getLogoUrl() : null;
        } else {
            JobSeekerProfile profile = jobSeekerProfileRepository
                    .findByUser_UserId(otherUser.getUserId()).orElse(null);
            otherName = otherUser.getFullName();
            otherAvatar = profile != null ? profile.getAvatarUrl() : null;
        }
        Message lastMsg = messageRepository
                .findTopByConversation_ConversationIdOrderBySentAtDesc(c.getConversationId());
        int unreadCount = isCandidate
                ? c.getUnreadCountCandidate()
                : c.getUnreadCountEmployer();
        return ConversationResponse.builder()
                .conversationId(c.getConversationId())
                .createdAt(c.getCreatedAt())
                .lastMessageAt(c.getLastMessageAt())
                .lastMessage(lastMsg != null ? lastMsg.getContent() : "")
                .otherUserId(otherUser.getUserId())
                .otherUserName(otherName)
                .otherUserAvatar(otherAvatar)
                .unreadCount(unreadCount)
                .build();
    }

    private MessageResponse buildMessageResponse(Message msg) {
        User sender = msg.getSender();
        String senderName;
        String senderRole = sender.getRole().getRoleName();
        if ("ROLE_COMPANY".equals(senderRole)) {
            Company company = companyRepository.findByUser_UserId(sender.getUserId()).orElse(null);
            senderName = company != null ? company.getCompanyName() : sender.getFullName();
        } else {
            senderName = sender.getFullName();
        }

        ChatAttachment att = chatAttachmentRepository.findByMessage_MessageId(msg.getMessageId());
        AttachmentResponse attResponse = null;
        if (att != null) {
            attResponse = AttachmentResponse.builder()
                    .attachmentId(att.getAttachmentId())
                    .fileName(att.getFileName())
                    .filePath(att.getFilePath())
                    .fileType(att.getFileType())
                    .uploadedAt(att.getUploadTime())
                    .build();
        }

        return MessageResponse.builder()
                .messageId(msg.getMessageId())
                .conversationId(msg.getConversation().getConversationId())
                .senderId(sender.getUserId())
                .senderName(senderName)
                .content(msg.getContent())
                .sentAt(msg.getSentAt())
                .isRead(msg.getIsRead())
                .messageType(msg.getMessageType())
                .attachment(attResponse)
                .build();
    }
}
