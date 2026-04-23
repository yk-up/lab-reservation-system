package com.lab.reservation.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private Integer page;
    private Integer pageSize;
    private Long total;
    private Long totalPages;
    private List<T> list;

    public static <T> PageResult<T> of(Integer page, Integer pageSize, Long total, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setTotal(total);
        long pages = pageSize == null || pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        result.setTotalPages(pages);
        result.setList(list);
        return result;
    }
}
