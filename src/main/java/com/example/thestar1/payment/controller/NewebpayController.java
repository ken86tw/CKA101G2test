package com.example.thestar1.payment.controller;

import com.example.thestar1.member.entity.MemberVO;
import com.example.thestar1.order.entity.OrderVO;
import com.example.thestar1.order.repository.OrderRepository;
import com.example.thestar1.order.service.OrderService;
import com.example.thestar1.payment.service.NewebpayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/thestar/newebpay")
public class NewebpayController {

    private final NewebpayService newebpayService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public NewebpayController(NewebpayService newebpayService,
                              OrderRepository orderRepository,
                              OrderService orderService,
                              ObjectMapper objectMapper) {
        this.newebpayService = newebpayService;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/checkout/{orderId}", produces = MediaType.TEXT_HTML_VALUE)
    public String checkout(@PathVariable Integer orderId, HttpSession session) {
        MemberVO member = (MemberVO) session.getAttribute("loginMember");
        if (member == null) {
            throw new IllegalStateException("尚未登入");
        }

        OrderVO order = orderRepository.findById(orderId).orElseThrow();

        if (!order.getMemberId().equals(member.getMemberId())) {
            throw new IllegalArgumentException("無法付款他人訂單");
        }

        if (order.getOrderStatus() != 0) {
            throw new IllegalStateException("此訂單無法付款（狀態：" + order.getOrderStatus() + "）");
        }

        String merchantTradeNo = orderService.renewMerchantTradeNo(orderId);

        return newebpayService.buildCheckoutForm(
                merchantTradeNo,
                order.getTotalAmount() - order.getDiscountAmount(),
                "theStar訂房",
                orderId);
    }

    @PostMapping("/return")
    public String paymentReturn(@RequestParam Map<String, String> params) {
        String tradeInfo = params.get("TradeInfo");
        String tradeSha = params.get("TradeSha");

        if (!newebpayService.verifyTradeSha(tradeInfo, tradeSha)) {
            return "0|TradeSha Error";
        }

        try {
            String decrypted = newebpayService.decryptTradeInfo(tradeInfo);
            JsonNode root = objectMapper.readTree(decrypted);

            String status = root.get("Status").asText();
            if (!"SUCCESS".equals(status)) {
                return "0|" + root.get("Message").asText();
            }

            JsonNode result = root.get("Result");
            String merchantOrderNo = result.get("MerchantOrderNo").asText();
            int amt = result.get("Amt").asInt();
            String tradeNo = result.get("TradeNo").asText();

            orderService.confirmOrder(merchantOrderNo, amt, (byte) 2, tradeNo);
        } catch (Exception e) {
            return "0|" + e.getMessage();
        }

        return "1|OK";
    }
}
