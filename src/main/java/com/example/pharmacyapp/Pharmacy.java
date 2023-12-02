package com.example.pharmacyapp;


import javafx.collections.ObservableList;

import java.util.Iterator;
import java.util.List;

public class Pharmacy {
    private String name;
    private int id;
    private String address;
    private List<Medicine> availableMedicines;

    public Pharmacy(String name, int id, String address, List<Medicine> availableMedicines) {
        this.name = name;
        this.id = id;
        this.address = address;
        this.availableMedicines = availableMedicines;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Medicine> getAvailableMedicines() {
        return availableMedicines;
    }

    public void setAvailableMedicines(List<Medicine> availableMedicines) {
        this.availableMedicines = availableMedicines;

    }

    public static void addMedicine(List<Medicine> medicineList, Medicine medicine) {
        medicineList.add(medicine);
    }

    // Remove a medicine by name
    public static void removeMedicineByName(List<Medicine> medicineList, String name) {
        Iterator<Medicine> iterator = medicineList.iterator();
        while (iterator.hasNext()) {
            Medicine medicine = iterator.next();
            if (medicine.getName().equals(name)) {
                iterator.remove();
            }
        }
    }

    // Remove a medicine by ID
    public static void removeMedicineById(List<Medicine> medicineList, int id) {
        Iterator<Medicine> iterator = medicineList.iterator();
        while (iterator.hasNext()) {
            Medicine medicine = iterator.next();
            if (medicine.getId() == id) {
                iterator.remove();
            }
        }
    }

    // Search medicine by name
    public static Medicine searchMedicineByName(List<Medicine> medicineList, String name) {
        for (Medicine medicine : medicineList) {
            if (medicine.getName().equals(name)) {
                return medicine;
            }
        }
        return null; // Medicine not found
    }

    // Search medicine by ID
    public static Medicine searchMedicineById(List<Medicine> medicineList, int id) {
        for (Medicine medicine : medicineList) {
            if (medicine.getId() == id) {
                return medicine;
            }
        }
        return null; // Medicine not found
    }

    // Update medicine details by ID
    public static void updateMedicineById(List<Medicine> medicineList, int id, Medicine updatedMedicine) {
        for (int i = 0; i < medicineList.size(); i++) {
            if (medicineList.get(i).getId() == id) {
                medicineList.set(i, updatedMedicine);
                break;
            }
        }
    }

    public static Medicine searchMedicineByName(ObservableList<Medicine> pharmacyInventory, String medicineName) {
        for (Medicine medicine : pharmacyInventory) {
            if (medicine.getName().equals(medicineName)) {
                return medicine;
            }
        }
        return null; // Medicine not found
    }

}