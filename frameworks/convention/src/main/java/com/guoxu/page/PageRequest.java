package com.guoxu.page;

import lombok.Data;

/**
 * PageRequest
 * 分页请求类
 * @author 执笔画棠
 * @date 2025/11/05 21:38
 **/
@Data
public class PageRequest {
    //当前页
    private Long current=1L;

    //每页显示条数
    private Long size=10L;
}
