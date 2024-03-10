package com.example.pharmacyapp;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String name;
    private String address;
    private String contactInfo;
    private static List<Medicine> cart;
    private List<Order> orderHistory;
    private ObservableList<Order> orderHistoryObservable = FXCollections.observableArrayList();
    private ObservableList<Medicine> cartObservable = FXCollections.observableArrayList();

    public User(String name, String address, String contactInfo) {
        this.name = name;
        this.address = address;
        this.contactInfo = contactInfo;
        this.cart = new ArrayList<>();
        this.orderHistory = new ArrayList<>();
        this.orderHistoryObservable = FXCollections.observableArrayList();
    }

    public ObservableList<Medicine> getCartObservable() {
        return cartObservable;
    }

    public ObservableList<Order> getOrderHistoryObservable() {
        return orderHistoryObservable;
    }

    public static List<Medicine> getCart() {
        return cart;
    }

    public static void setCart(List<Medicine> newCart) {
        cart = newCart;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public void setOrderHistory(List<Order> orderHistory) {
        this.orderHistory = orderHistory;
    }

    public void addToCart(Medicine medicine) {
        cart.add(medicine);
        cartObservable.setAll(cart); // Update the observable list when adding to cart
    }

    public void removeFromCart(Medicine medicine) {
        cart.remove(medicine);
        cartObservable.setAll(cart); // Update the observable list when removing from cart
    }

    public void clearCart() {
        cart.clear();
        cartObservable.setAll(cart); // Update the observable list when clearing the cart
    }

    public void placeOrder() {
        // Check if the cart is not empty
        if (!cart.isEmpty()) {
            // Create a new Order using the current user and the items in the cart
            Order newOrder = new Order(this, cart);

            // Add the new order to the order history
            orderHistory.add(newOrder);

            // Update the order history observable list
            orderHistoryObservable.setAll(orderHistory);
            cart.clear();
            System.out.println("Order is placed");
        } else {
            System.out.println("Cart is empty. Add items to the cart before placing an order.");
        }
    }

}

