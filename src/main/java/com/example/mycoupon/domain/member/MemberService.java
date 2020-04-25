package com.example.mycoupon.domain.member;

import com.example.mycoupon.utils.ValidationRegex;
import com.example.mycoupon.exceptions.IllegalArgumentException;
import com.example.mycoupon.payload.UserModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Transactional
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void validationPassword(String password) {
        if(!Pattern.matches(ValidationRegex.PASSWORD, password)) {
            throw new IllegalArgumentException(
                    "password should be use alphabet, number, special-character at least 1 time each." +
                            "And length should be over 8 characters.");
        }
    }
    public Member signUp(UserModel model) {
        // password 암호화 저장
        // 트랜잭션 레벨 설정
        validationPassword(model.getPassword());
        Member member = new Member(model.getId(), model.getPassword());
        member.setPassword(passwordEncoder.encode(member.getPassword()));
        try {
            return memberRepository.save(member);
        } catch(DataIntegrityViolationException ex) {
            if(ex.getCause() instanceof ConstraintViolationException) {
                throw new IllegalArgumentException("user id already exists.");
            } else {
                throw ex;
            }
        } catch(ConstraintViolationException ex) {
            throw new IllegalArgumentException("Invalid arguments.");
        }

    }
    public Optional<Member> findById(long id) {
        return memberRepository.findById(id);
    }
}
