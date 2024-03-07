package com.example.smilestock.controller;


import com.example.smilestock.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 종목 코드 및 기업 코드 가져오기(DB 저장)
    @GetMapping("/corpinfo")
    public ResponseEntity<?> getCorpInfo() {
        return chatService.getCorpInfo();
    }

    // 재무 정보 받아와 DB 저장하기
    @GetMapping("/analysis")
    public ResponseEntity<?> analysis() {
        return chatService.analysis();
    }
}
