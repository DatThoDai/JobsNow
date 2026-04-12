package com.JobsNow.backend.controllers;

import com.JobsNow.backend.constants.JobsNowConstant;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.request.MarkMessagesReadRequest;
import com.JobsNow.backend.request.SendFileMessageRequest;
import com.JobsNow.backend.request.SendTextMessageRequest;
import com.JobsNow.backend.response.MessageResponse;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.AwsS3Service;
import com.JobsNow.backend.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AwsS3Service awsS3Service;

    @PostMapping("/conversation")
    public ResponseEntity<?> createConversation(@RequestParam Integer candidateId, @RequestParam Integer employerId) {
        return ResponseFactory.success(conversationService.createConversation(candidateId, employerId));
    }

    @GetMapping("/conversations/{userId}")
    public ResponseEntity<?> getUserConversations(@PathVariable Integer userId) {
        return ResponseFactory.success(conversationService.getUserConversations(userId));
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getConversation(@PathVariable Integer conversationId, @RequestParam Integer userId) {
        return ResponseFactory.success(conversationService.getConversationById(conversationId, userId));
    }

    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<?> getMessages(@PathVariable Integer conversationId) {
        return ResponseFactory.success(conversationService.getMessagesByConversationId(conversationId));
    }

    @GetMapping("/conversations/unread/{userId}")
    public ResponseEntity<?> getUnreadCount(@PathVariable Integer userId) {
        return ResponseFactory.success(conversationService.countUnreadConversations(userId));
    }

    @GetMapping("/conversationId")
    public ResponseEntity<?> findConversationId(@RequestParam Integer candidateId, @RequestParam Integer employerId) {
        return ResponseFactory.success(conversationService.findConversationId(candidateId, employerId));
    }

    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<?> deleteConversation(@PathVariable Integer conversationId, @RequestParam Integer userId) {
        conversationService.deleteConversation(conversationId, userId);
        return ResponseFactory.successMessage("Conversation deleted");
    }

    // websocket
    @MessageMapping("/message/text")
    public void sendTextMessage(SendTextMessageRequest request) {
        MessageResponse message = conversationService.sendTextMessage(request);
        messagingTemplate.convertAndSend(
                JobsNowConstant.WS_TOPIC_CONVERSATION + request.getConversationId(),
                message);
    }

    @MessageMapping("/messages/read")
    public void markMessagesAsRead(MarkMessagesReadRequest request) {
        conversationService.markMessagesAsRead(request.getConversationId(), request.getUserId());
    }

    // chỉ để test thôi nha, thực tế sẽ upload file lên S3 trước rồi mới gửi message chứa URL file đó
    @PostMapping("/upload")
    public ResponseEntity<?> uploadChatFile(@RequestParam("file") MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            String extension = originalName.substring(originalName.lastIndexOf("."));
            String baseName = originalName.substring(0, originalName.lastIndexOf("."));
            String s3Key = "chat-files/" + baseName + "_" + System.currentTimeMillis() + extension;
            String fileUrl = awsS3Service.uploadFileToS3(
                    file.getInputStream(), s3Key, file.getContentType());

            return ResponseFactory.success(Map.of(
                    "fileUrl", fileUrl,
                    "fileName", originalName,
                    "fileType", file.getContentType()
            ));
        } catch (Exception e) {
            throw new BadRequestException("Upload failed: " + e.getMessage());
        }
    }


    @MessageMapping("/message/file")
    public void sendFileMessage(SendFileMessageRequest request) {
        MessageResponse message = conversationService.sendFileMessage(request);
        messagingTemplate.convertAndSend(
                JobsNowConstant.WS_TOPIC_CONVERSATION + request.getConversationId(),
                message);
    }

}
