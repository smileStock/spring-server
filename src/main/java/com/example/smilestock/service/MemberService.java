package com.example.smilestock.service;

import com.example.smilestock.entity.Member;
import com.example.smilestock.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.*;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member registerMember(String username, String email) {
        Member member = new Member();
        member.setName(username);
        member.setEmail(email);
        return memberRepository.save(member);
    }
}
