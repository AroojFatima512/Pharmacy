package com.example.pharmacyapp;

import javafx.beans.property.*;

    public class Medicine {
        private final StringProperty name;
        private final IntegerProperty id;
        private final StringProperty description;
        private final DoubleProperty price;
        private final IntegerProperty quantity;
        private final IntegerProperty selectedQuantity;

        public Medicine(String name, int id, String description, double price, int quantity) {
            this.name = new SimpleStringProperty(name);
            this.id = new SimpleIntegerProperty(id);
            this.description = new SimpleStringProperty(description);
            this.price = new SimpleDoubleProperty(price);
            this.quantity = new SimpleIntegerProperty(quantity);
            this.selectedQuantity = new SimpleIntegerProperty(quantity);
        }

        public String getName() {
            return name.get();
        }

        public StringProperty nameProperty() {
            return name;
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public int getId() {
            return id.get();
        }

        public IntegerProperty idProperty() {
            return id;
        }

        public void setId(int id) {
            this.id.set(id);
        }

        public String getDescription() {
            return description.get();
        }

        public StringProperty descriptionProperty() {
            return description;
        }

        public void setDescription(String description) {
            this.description.set(description);
        }

        public double getPrice() {
            return price.get();
        }

        public DoubleProperty priceProperty() {
            return price;
        }

        public void setPrice(double price) {
            this.price.set(price);
        }

        public int getQuantity() {
            return quantity.get();
        }

        public IntegerProperty quantityProperty() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity.set(quantity);
        }

        public int getSelectedQuantity() {
            return selectedQuantity.get();
        }

        public IntegerProperty selectedQuantityProperty() {
            return selectedQuantity;
        }

        public void setSelectedQuantity(int selectedQuantity) {
            this.selectedQuantity.set(selectedQuantity);
        }

        @Override
        public String toString() {
           return String.format("\nName: %s, Price: %.2f, Quantity: %d",
                   name.get(), price.get(), selectedQuantity.get());
        }

    }
