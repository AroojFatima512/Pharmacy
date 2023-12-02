package com.example.pharmacyapp;

import java.util.Date;
import java.util.List;

public class Order {
    private int orderId;
    private Date orderDate;
    private User user;
    private List<Medicine> medicines;
    private double totalPrice;
    private String status;
    private static int nextOrderId = 1;


    public Order(User user, List<Medicine> medicines) {
        this.orderId = nextOrderId++;
        this.orderDate = new Date();
        this.user = user;
        this.medicines = medicines;
        this.totalPrice = calculateTotalPrice();
        this.status = "Order is Placed";
    }


    private double calculateTotalPrice() {
        double total = 0.0;
        for (Medicine medicine : medicines) {
            total += medicine.getPrice();
        }
        return total;
    }

    public List<Medicine> getMedicines() {
        return medicines;
    }

    public int getOrderId() {
        return orderId;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public User getUser() {
        return user;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public String getStatus() {
        return status;
    }


}
