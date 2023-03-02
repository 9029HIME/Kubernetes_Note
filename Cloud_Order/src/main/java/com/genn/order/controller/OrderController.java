package com.genn.order.controller;

import com.genn.order.dto.PreOrderDTO;
import com.genn.stock.feign.StockFeign;
import com.genn.stock.response.StockQueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    public OrderController(){
        System.out.println("OrderController实例化");
    }

    @Autowired
    private StockFeign stockFeign;

    @GetMapping("/preOrder/{id}")
    public PreOrderDTO preOrder(@PathVariable("id") Long id){
        StockQueryResponse stockQueryResponse = stockFeign.queryByGoodsId(id);
        PreOrderDTO result = new PreOrderDTO();
        result.setMetaId(System.getProperty("ORDER_META"));
        result.setOrderId(id);
        result.setStock(stockQueryResponse.getStock());
        return result;
    }

}
