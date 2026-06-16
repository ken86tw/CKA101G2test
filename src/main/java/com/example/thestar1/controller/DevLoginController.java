package com.example.thestar1.controller; // 照你專案實際的 controller 套件調整

import com.example.thestar1.entity.MemberVO;   // 照 MemberVO 實際所在套件調整
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DevLoginController {

    @GetMapping("/dev/login/{memberId}")
    public String fakeLogin(@PathVariable Integer memberId, HttpSession session) {
        MemberVO member = new MemberVO();
        member.setMemberId(memberId);
        session.setAttribute("loginMember", member);
        return "fake login ok, memberId=" + memberId + ", sessionId=" + session.getId();
    }

    @GetMapping("/dev/employeelogin/{employeeId}")
    public  String fakeEmployeeLogin(@PathVariable Integer employeeId, HttpSession session){
        session.setAttribute("loginemployee", employeeId);
        return"fake employee login OK employeeId = " + employeeId;
    }

}