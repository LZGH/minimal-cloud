package com.lzpeng.minimal.system.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.lzpeng.minimal.common.jpa.converter.AbstractIntEnumConverter;
import com.lzpeng.minimal.common.core.domain.enums.IntEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型类型
 * @author: Lzpeng
 */
@Getter
@AllArgsConstructor
public enum NoticeType implements IntEnum {
    /**
     * 顶部滚动通知
     */
    TOP_SCROLL(0, "顶部滚动通知"),
    /**
     * 右下角弹出消息
     */
    MESSAGE(1, "消息"),
    /**
     * 待办 工作流中需要自己审批单据时
     */
    DOING(2, "待办"),
    /**
     * 已办 工作流中自己审批过的单据
     */
    DONE(3, "已办");


    @JsonValue
    private Integer code;
    private String message;

    public static class Converter extends AbstractIntEnumConverter<NoticeType> {
    }
}
