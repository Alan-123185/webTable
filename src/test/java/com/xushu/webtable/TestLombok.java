package com.xushu.webtable;

import lombok.Data;

@Data
public class TestLombok {
    private String name;

    public static void main(String[] args) {
        TestLombok test = new TestLombok();
        test.setName("test"); // 如果这行不报错，说明 Lombok 正常工作
        System.out.println(test.getName());
    }
}
