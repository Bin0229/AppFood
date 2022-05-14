package com.example.menu_order.listener;

import java.util.List;

import com.example.menu_order.model.CartModel;
import com.example.menu_order.model.DrinkModel;

public interface ICartLoadListener {
        void onCartLoadSuccess(List<CartModel> cartModelList);
        void onCartLoadFailed(String message);

}
