package com.guoxu.toolkit;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.function.Function;
import org.opengoofy.index12306.framework.starter.convention.page.PageRequest;
import org.opengoofy.index12306.framework.starter.convention.page.PageResponse;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PageUtil
 * 分页工具类
 * 作用就是前面自定义的pageresponse和baomidou的ipage进行转换
 *
 * @author 执笔画棠
 * @date 2025/11/06 12:31
 **/
public class PageUtil {

    //当执行分页查询时，需要将 PageRequest 转换为 Page 对象传给 Mapper 方法。

    // 分页工具类转换 pagerequest->page
    /**
     * pagerequest是前端传过来的自定义的分页请求
     * 包含当前页码和页大小，这里根据这两个参数创建一个分页工具类
     * 实现了自定义的分页请求到baomidou的page对象转换
     * 分页工具类转换
     * @param pageRequest 分页请求参数
     * @return 分页工具类
     */
    public static Page convert(PageRequest pageRequest) {
        return convert(pageRequest.getCurrent(), pageRequest.getSize());
    }

    // 分页工具类转换
    public static Page convert(long current,long size){
        return new Page(current,size);
    }


    // 分页工具类转换 page->pageresponse
    //这是最简单的转换，它直接将 IPage 中的数据（通常是数据库实体对象，如 UserDO）和分页信息原封不动地放入 PageResponse 中。
    public static PageResponse convert(IPage iPage){
        return buildConventionPage(iPage);
    }

    /*
     *这是最典型的场景——将从数据库查出的 DO (Data Object) 列表转换为 DTO (Data Transfer Object)
     *或 VO (View Object) 列表，以实现数据隔离和封装，避免将数据库实体结构直接暴露给前端。
     * 这里使用了 BeanUtil.convert 方法，它是一个通用的对象转换工具，
     * 可以将一个对象的属性值复制到另一个对象中。
     */
    public static <TARGET, ORIGINAL> PageResponse<TARGET> convert(IPage<ORIGINAL> iPage, Class<TARGET> targetClass){
        //这个是mybatisplus的方法，它可以将ipage中的数据转换为目标类
        //它会遍历 records 列表，并将每个元素应用传入的 Function 进行转换，然后用转换后的结果替换原来的列表。
        iPage.convert(each -> BeanUtil.convert(each,targetClass));
        return buildConventionPage(iPage);
    }


    /*
     * 这是一个更灵活的转换方式，它允许你自定义转换逻辑。
     * 例如，你可以在转换时添加一些业务逻辑，或者只转换部分字段。
     */
    public static <TARGET, ORIGINAL> PageResponse<TARGET> convert(IPage<ORIGINAL> iPage,
                                                                  Function<? super ORIGINAL, ? extends TARGET> mapper) {
        List<TARGET> targetDataList = iPage.getRecords().stream()
                .map(mapper)
                .collect(Collectors.toList());
        return PageResponse.<TARGET>builder()
                .current(iPage.getCurrent())
                .size(iPage.getSize())
                .records(targetDataList)
                .total(iPage.getTotal())
                .build();
    }


    /*
     *这是一个内部辅助方法，遵循 DRY (Don't Repeat Yourself) 原则。
     * 它将从 IPage 拷贝分页信息到 PageResponse 的通用逻辑提取出来，供上面的 convert 方法复用。
     */
    private static PageResponse buildConventionPage(IPage iPage) {
        return PageResponse.builder()
                .current(iPage.getCurrent())
                .size(iPage.getSize())
                .records(iPage.getRecords())
                .total(iPage.getTotal())
                .build();
    }

}
