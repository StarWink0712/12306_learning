package com.guoxu.page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PageResponse
 * 分页响应结果
 * @author 执笔画棠
 * @date 2025/11/05 21:39
 **/
//此注解允许用户自定义实现来覆盖框架的默认行为。
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse <T> implements Serializable {

    //序列化版本号
    private static final long serialVersionUID = 1L;

    //当前页
    private Long current;

    private Long size=10L;

    //总数
    private Long total;

    //查询数据列表
    private List<T> records= Collections.emptyList();

    public PageResponse(long current, long size) {
        this(current, size, 0);
    }

    public PageResponse(long current, long size, long total) {
        if (current > 1) {
            this.current = current;
        }
        this.size = size;
        this.total = total;
    }

    public PageResponse setRecords(List<T> records) {
        this.records = records;
        return this;
    }

    public <R> PageResponse<R> convert(Function<? super T, ? extends R> mapper) {
        List<R> collect = this.getRecords().stream().map(mapper).collect(Collectors.toList());
        return ((PageResponse<R>) this).setRecords(collect);
    }

}
