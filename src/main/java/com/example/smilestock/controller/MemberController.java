package com.example.smilestock.controller;

import com.example.smilestock.dto.MemberRequest;
import com.example.smilestock.service.MemberService;
import com.fasterxml.jackson.databind.DatabindContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;


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
    public ResponseEntity<String> login(@RequestBody MemberRequest memberRequest, HttpServletRequest request) {
        boolean loginSuccess = memberService.login(memberRequest.getName(), memberRequest.getEmail());
        if (loginSuccess) {
            HttpSession session = request.getSession();
            session.setAttribute("username",memberRequest.getName());
            return ResponseEntity.ok("로그인 성공");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 실패");
        }
    }

    @GetMapping("/checkLogin")
    public ResponseEntity<String> checkLogin(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username != null) {
            return ResponseEntity.ok("현재 로그인 중인 사용자: " + username);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 되어있지 않습니다.");
        }
    }
}
