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

    public Order(User user, List<Medicine> medicines) {
        int maxId = 0;
        for (Order o : user.getOrderHistoryObservable()) {
            if (o.getOrderId() > maxId) {
                maxId = o.getOrderId();
            }
        }
        this.orderId = maxId + 1;
        this.orderDate = new Date();
        this.user = user;
        this.medicines = new java.util.ArrayList<>(medicines);
        this.totalPrice = calculateTotalPrice();
        
        this.status = "Placed";
    }

    private double calculateTotalPrice() {
        double total = 0.0;
        for (Medicine medicine : medicines) {
            total += medicine.getPrice() * medicine.getSelectedQuantity();
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

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setOrderDate(Date date) {
        this.orderDate = date;
    }
}
