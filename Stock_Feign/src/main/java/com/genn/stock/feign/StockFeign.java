package com.genn.stock.feign;

import com.genn.stock.response.StockQueryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "cloudstock",path = "/stock")
@RequestMapping("/stock")
public interface StockFeign {
    @GetMapping("/queryByGoodsId/{goodsId}")
    StockQueryResponse queryByGoodsId(@PathVariable("goodsId") Long goodsId);
}
