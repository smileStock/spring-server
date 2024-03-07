package com.example.smilestock.controller;

import com.example.smilestock.dto.MemberRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.smilestock.service.MemberService;

@RestController
@RequestMapping("/api/v1/member")
public class MemberController {
    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("")
    public String confirmRegisterMember(@RequestBody MemberRequest memberRequest) {
        memberService.registerMember(memberRequest.getName(), memberRequest.getEmail());
        return "회원가입이 완료되었습니다.";
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody MemberRequest memberRequest) {
        boolean loginSuccess = memberService.login(memberRequest.getName(), memberRequest.getEmail());
        if (loginSuccess) {
            return ResponseEntity.ok("로그인 성공");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 실패");
        }
    }
}
