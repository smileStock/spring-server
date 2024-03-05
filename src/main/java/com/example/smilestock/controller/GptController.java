package com.example.smilestock.controller;


import com.example.smilestock.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/v1/chat-gpt")
public class GptController {
    private final ChatService chatService;

    @Autowired
    public GptController(ChatService chatService) {
        this.chatService = chatService;
    }

    //chat-gpt 와 간단한 채팅 서비스 소스
    @PostMapping("")
    public String test(@RequestBody String question) {
        return chatService.getChatResponse(question);
    }
}