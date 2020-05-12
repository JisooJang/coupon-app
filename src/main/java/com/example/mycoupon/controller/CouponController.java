package com.example.mycoupon.controller;

import com.example.mycoupon.domain.Coupon;
import com.example.mycoupon.domain.Member;
import com.example.mycoupon.exceptions.CouponNotFoundException;
import com.example.mycoupon.exceptions.InvalidPayloadException;
import com.example.mycoupon.service.CouponService;
import com.example.mycoupon.service.MemberService;
import com.example.mycoupon.exceptions.MemberNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/*
JwyAuthorizationFilter 로직을 컨트롤러 진입 전에 먼저 타서 헤더의 토큰이 유효한 지 검사 후 ->
토큰이 유효하면, memberId를 Request attribute에 담아 컨트롤러로 전달한다.
 */
@RestController
@RequestMapping("/coupon")
public class CouponController {
    private final CouponService couponservice;
    private final MemberService memberService;

    @Autowired
    public CouponController(CouponService couponService, MemberService memberService) {
        this.couponservice = couponService;
        this.memberService = memberService;
    }

    @PostMapping("/{num}")
    public ResponseEntity<?> saveCoupon(@PathVariable("num") int num, @RequestAttribute long memberId) {
        if(num > 1000) {
            throw new InvalidPayloadException("The number of coupon should be less than 1000.");
        }
        couponservice.bulkSave(num);

        URI selfLink = URI.create(
                ServletUriComponentsBuilder.fromCurrentRequest().toUriString()
        );
        return ResponseEntity.created(selfLink).build();
    }

    @PutMapping("/user")
    public ResponseEntity<String> assignToUserCoupon(@RequestAttribute("memberId") long memberId) throws MemberNotFoundException { // user_id는 JwtAuthorizationFilter에서 넘겨줌.
        Optional<Member> member = memberService.findById(memberId);
        if(member.isPresent()) {
            return ResponseEntity.ok(couponservice.assignToUser(member.get()));
        } else {
            throw new MemberNotFoundException(memberId);
        }
    }

    @GetMapping("/user")
    public ResponseEntity<List<Coupon>> getUserCoupons(@RequestAttribute("memberId") long memberId) throws MemberNotFoundException {
        List<Coupon> coupons = couponservice.findByMember(memberId);
        if(coupons == null || coupons.size() == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(coupons);
    }

    @PutMapping("/{coupon_code}")
    public ResponseEntity<?> useCoupon(@PathVariable("coupon_code") String couponCode,
                                       @RequestAttribute("memberId") long memberId) throws CouponNotFoundException {
        couponservice.validateCouponCode(couponCode);
        couponservice.updateIsEnabledCouponById(couponCode, memberId, true);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{coupon_code}/cancel")
    public ResponseEntity<?> cancelUseCoupon(@PathVariable("coupon_code") String couponCode,
                                             @RequestAttribute("memberId") long memberId) throws CouponNotFoundException {
        couponservice.validateCouponCode(couponCode);
        couponservice.updateIsEnabledCouponById(couponCode, memberId,false);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/expired")
    public ResponseEntity<List<Coupon>> getExpiredCoupon() {
        List<Coupon> coupons = couponservice.findExpiredToday();
        if(coupons == null || coupons.size() == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(coupons);
    }

}
