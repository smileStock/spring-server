package com.example.smilestock.service;

import com.example.smilestock.entity.Member;
import com.example.smilestock.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.util.Optional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member registerMember(String name, String email) {
        Member member = new Member();
        member.setName(name);
        member.setEmail(email);
        return memberRepository.save(member);
    }

    public boolean login(String name, String Email){
        Optional<Member> optionalMember = memberRepository.findByName(name);
        return optionalMember.map(member -> member.getEmail().equals(Email)).orElse(false);
    }
}
