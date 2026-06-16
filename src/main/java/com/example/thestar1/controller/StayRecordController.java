package com.example.thestar1.controller;


import com.example.thestar1.dto.CheckInDTO;
import com.example.thestar1.entity.RoomVO;
import com.example.thestar1.service.StayRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.support.SessionStatus;

import java.util.List;

@RestController
@RequestMapping("/thestar/stayrecord")
public class StayRecordController {

    private final StayRecordService stayRecordService;

    public StayRecordController(StayRecordService stayRecordService) {
        this.stayRecordService = stayRecordService;
    }

    @GetMapping("/rooms/{roomTypeId}")
    public List<RoomVO> availableRoom(@PathVariable Integer roomTypeId){
        return stayRecordService.findAvailableRoom(roomTypeId);
    }

    @PostMapping("/checkin")
    public ResponseEntity<String> checkIn(@RequestBody CheckInDTO dto, HttpSession session){
        Integer employeeId = (Integer) session.getAttribute("loginemployee");
        if(employeeId == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        stayRecordService.checkIn(employeeId,dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("check in OK");
    }
}
