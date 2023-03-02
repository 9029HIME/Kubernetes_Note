package com.genn.stock.controller;

import com.genn.stock.feign.StockFeign;
import com.genn.stock.response.StockQueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StockController implements StockFeign {

    @Override
    public StockQueryResponse queryByGoodsId(Long goodsId) {
        StockQueryResponse response = new StockQueryResponse();
        response.setGoodId(goodsId);
        response.setStock(Integer.valueOf(System.getProperty("STOCK_META")));
        return response;
    }
}
