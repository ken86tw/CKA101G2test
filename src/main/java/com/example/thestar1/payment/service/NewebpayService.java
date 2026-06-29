package com.example.thestar1.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class NewebpayService {

    private final String hashKey;
    private final String hashIv;
    private final String merchantId;
    private final String aioUrl;
    private final String returnUrl;
    private final String clientBackUrl;

    public NewebpayService(@Value("${newebpay.hash-key}") String hashKey,
                           @Value("${newebpay.hash-iv}") String hashIv,
                           @Value("${newebpay.merchant-id}") String merchantId,
                           @Value("${newebpay.aio-url}") String aioUrl,
                           @Value("${newebpay.return-url}") String returnUrl,
                           @Value("${newebpay.client-back-url}") String clientBackUrl) {
        this.hashKey = hashKey;
        this.hashIv = hashIv;
        this.merchantId = merchantId;
        this.aioUrl = aioUrl;
        this.returnUrl = returnUrl;
        this.clientBackUrl = clientBackUrl;
    }

    public String buildCheckoutForm(String merchantOrderNo, int amount, String itemDesc, Integer orderId) {
        Map<String, String> params = new HashMap<>();
        params.put("MerchantID", merchantId);
        params.put("RespondType", "JSON");
        params.put("TimeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("Version", "2.0");
        params.put("MerchantOrderNo", merchantOrderNo);
        params.put("Amt", String.valueOf(amount));
        params.put("ItemDesc", itemDesc);
        params.put("ReturnURL", returnUrl);
        params.put("ClientBackURL", clientBackUrl + "?orderId=" + orderId);

        String tradeInfo = aesEncrypt(genDataChain(params));
        String tradeSha = genTradeSha(tradeInfo);

        StringBuilder form = new StringBuilder();
        form.append("<form id=\"newebpay\" method=\"post\" action=\"").append(aioUrl).append("\">");
        form.append("<input type=\"hidden\" name=\"MerchantID\" value=\"").append(merchantId).append("\"/>");
        form.append("<input type=\"hidden\" name=\"TradeInfo\" value=\"").append(tradeInfo).append("\"/>");
        form.append("<input type=\"hidden\" name=\"TradeSha\" value=\"").append(tradeSha).append("\"/>");
        form.append("<input type=\"hidden\" name=\"Version\" value=\"2.0\"/>");
        form.append("</form>");
        form.append("<script>document.getElementById('newebpay').submit();</script>");
        return form.toString();
    }

    public boolean verifyTradeSha(String tradeInfo, String tradeSha) {
        return tradeSha.equals(genTradeSha(tradeInfo));
    }

    public String decryptTradeInfo(String tradeInfo) {
        return aesDecrypt(tradeInfo);
    }

    private String genDataChain(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            sb.append(e.getKey()).append("=").append(urlEncode(e.getValue())).append("&");
        }
        return sb.substring(0, sb.length() - 1);
    }

    private String genTradeSha(String tradeInfo) {
        String raw = "HashKey=" + hashKey + "&" + tradeInfo + "&HashIV=" + hashIv;
        return sha256(raw).toUpperCase();
    }

    private String aesEncrypt(String plaintext) {
        try {
            SecretKeySpec key = new SecretKeySpec(hashKey.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec iv = new IvParameterSpec(hashIv.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("TradeInfo AES 加密失敗", e);
        }
    }

    private String aesDecrypt(String ciphertext) {
        try {
            SecretKeySpec key = new SecretKeySpec(hashKey.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec iv = new IvParameterSpec(hashIv.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] decrypted = cipher.doFinal(hexToBytes(ciphertext));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("TradeInfo AES 解密失敗", e);
        }
    }

    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8)
                .replace("%21", "!")
                .replace("%2A", "*")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%2D", "-")
                .replace("%2E", ".")
                .replace("%5F", "_");
    }

    private String sha256(String s) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA256 計算失敗", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
