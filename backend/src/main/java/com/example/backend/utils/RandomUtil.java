package com.example.backend.utils;

import java.util.Random;

public class RandomUtil {

    private static final Random RANDOM = new Random();

    public static String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static String generateOrderNo() {
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq = String.format("%04d", RANDOM.nextInt(10000));
        return "OD" + date + seq;
    }
}
