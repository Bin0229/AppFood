package com.example.menu_order;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.menu_order.adapter.MyDrinkAdapter;
import com.example.menu_order.evenbus.MyUpdateCartEvent;
import com.example.menu_order.listener.ICartLoadListener;
import com.example.menu_order.listener.IDrinkLoadListener;
import com.example.menu_order.model.CartModel;
import com.example.menu_order.model.DrinkModel;
import com.example.menu_order.utils.SpaceItemDecoration;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nex3z.notificationbadge.NotificationBadge;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements IDrinkLoadListener, ICartLoadListener {

    @BindView(R.id.recycler_drink)
    RecyclerView recycler_drink;
    @BindView(R.id.mainLayout)
    RelativeLayout mainLayout;
    @BindView(R.id.badge)
    NotificationBadge badge;
    @BindView(R.id.btnCart)
    FrameLayout btnCart;
    @BindView(R.id.btnBack)
    ImageView btnBack;

    IDrinkLoadListener drinkLoadListener;
    ICartLoadListener cartLoadListener;

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        if(EventBus.getDefault().hasSubscriberForEvent(MyUpdateCartEvent.class))
            EventBus.getDefault().removeStickyEvent(MyUpdateCartEvent.class);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onUpdateCart(MyUpdateCartEvent event){
        countCartItem();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        loadDrinkFromFirebase();
    }


    private void loadDrinkFromFirebase() {
        List<DrinkModel> drinkModels = new ArrayList<>();
        FirebaseDatabase.getInstance()
                .getReference("Drink")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            for(DataSnapshot drinkSnapshot:snapshot.getChildren()){
                                DrinkModel drinkModel = drinkSnapshot.getValue(DrinkModel.class);
                                drinkModel.setKey(drinkSnapshot.getKey());
                                drinkModels.add(drinkModel);
                            }
                            drinkLoadListener.onDrinkLoadSuccess(drinkModels);
//                            nameRes = null;
                        }
                        else
                            drinkLoadListener.onDrinkLoadFailed("Can't find drink");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        drinkLoadListener.onDrinkLoadFailed(error.getMessage());
                    }
                });
    }

    private void init(){
        ButterKnife.bind(this);

        drinkLoadListener= this;
        cartLoadListener = this;

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this,2);
        recycler_drink.setLayoutManager(gridLayoutManager);
        recycler_drink.addItemDecoration(new SpaceItemDecoration());

        btnCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));

        btnBack.setOnClickListener(v->finish());
    }

    @Override
    public void onDrinkLoadSuccess(List<DrinkModel> drinkModelList) {
        MyDrinkAdapter adapter = new MyDrinkAdapter(this, drinkModelList,cartLoadListener);
        recycler_drink.setAdapter(adapter);
    }

    @Override
    public void onDrinkLoadFailed(String message) {
        Snackbar.make(mainLayout,message,Snackbar.LENGTH_LONG).show();
    }


    @Override
    public void onCartLoadSuccess(List<CartModel> cartModelList) {
        int cartSum =0;
        for(CartModel cartModel: cartModelList)
            cartSum += cartModel.getQuantity();
        badge.setNumber(cartSum);
    }

    @Override
    public void onCartLoadFailed(String message) {
        Snackbar.make(mainLayout,message,Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        countCartItem();
    }

    private void countCartItem() {
        List<CartModel> cartModels = new ArrayList<>();
        FirebaseDatabase
                .getInstance().getReference("Cart")
                .child("UNIQUE_USER_ID")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot cartSnapshot:snapshot.getChildren())
                        {
                            CartModel cartModel =cartSnapshot.getValue(CartModel.class);
                            cartModel.setKey(cartSnapshot.getKey());
                            cartModels.add(cartModel);
                        }
                        cartLoadListener.onCartLoadSuccess(cartModels);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        cartLoadListener.onCartLoadFailed(error.getMessage());
                    }
                });
    }
}