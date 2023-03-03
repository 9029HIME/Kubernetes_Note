package com.genn.order.controller;

import com.genn.order.dto.PreOrderDTO;
import com.genn.stock.feign.StockFeign;
import com.genn.stock.response.StockQueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {
    private static final String META = "ORDER_META";

    public OrderController(){
        System.out.println("OrderController实例化");
    }

    @Autowired
    private StockFeign stockFeign;

    @GetMapping("/env")
    public String env(){
        return System.getenv(META);
    }

    @GetMapping("/preOrder/{id}")
    public PreOrderDTO preOrder(@PathVariable("id") Long id){
        StockQueryResponse stockQueryResponse = stockFeign.queryByGoodsId(id);
        PreOrderDTO result = new PreOrderDTO();
        result.setMetaId(System.getenv(META));
        result.setOrderId(id);
        result.setStock(stockQueryResponse.getStock());
        return result;
    }

}
