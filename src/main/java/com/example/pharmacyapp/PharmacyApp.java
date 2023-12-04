package com.example.pharmacyapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
public class PharmacyApp extends Application {
    private User user;
    private ObservableList<Medicine> pharmacyInventory;
    private TableView<Medicine> medicineTableView;
    private TableView<Order> orderHistoryTableView;
    private ObservableList<Medicine> cart = FXCollections.observableArrayList();
    public static void main(String[] args) { launch(args);}
    @Override
    public void start(Stage primaryStage) {
        Stage authStage = new Stage();
        authStage.setTitle("User Sign Up");
        GridPane authGridPane = createAuthGridPane(authStage);
        Scene authScene = new Scene(authGridPane, 350, 350);
        authStage.setScene(authScene);
        authStage.show();
    }
    public class BackgroundUtil {
        public static void setGridPaneBackground(GridPane gridPane, String imagePath) {
            // Load the background image
            Image backgroundImage = new Image(BackgroundUtil.class.getResourceAsStream(imagePath));

            // Create a background image
            BackgroundImage background = new BackgroundImage(
                    backgroundImage,
                    BackgroundRepeat.REPEAT,
                    BackgroundRepeat.REPEAT,
                    BackgroundPosition.DEFAULT,
                    BackgroundSize.DEFAULT
            );
            // Set the background for the GridPane
            gridPane.setBackground(new Background(background));
        }
    }

    private GridPane createAuthGridPane(Stage authStage) {
        GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(15);
        gridPane.setPadding(new Insets(20, 20, 20, 20));

        BackgroundUtil.setGridPaneBackground(gridPane, "/background.jpg");

        Text title = new Text("Sign Up");
        title.setFont(new Font("Times new roman", 30));
        Label nameLabel = new Label("Name:");
        Label emailLabel = new Label("Email:");
        Label phoneNumberLabel = new Label("Phone Number:");
        Label addressLabel = new Label("Address:");
        Label passwordLabel = new Label("Password");
        Label confirmPasswordLabel = new Label("Confirm Password");

        TextField nameTextField = new TextField();
        TextField emailTextField = new TextField();
        TextField phoneNumberTextField = new TextField();
        TextField addressTextField = new TextField();
        PasswordField passwordTextField = new PasswordField();
        PasswordField confirmPasswordTextField = new PasswordField();

        gridPane.add(title, 0, 0, 2, 1);
        gridPane.add(nameLabel, 0, 1);
        gridPane.add(nameTextField, 1, 1);
        gridPane.add(emailLabel, 0, 2);
        gridPane.add(emailTextField, 1, 2);
        gridPane.add(phoneNumberLabel, 0, 3);
        gridPane.add(phoneNumberTextField, 1, 3);
        gridPane.add(addressLabel, 0, 4);
        gridPane.add(addressTextField, 1, 4);
        gridPane.add(passwordLabel, 0, 5);
        gridPane.add(passwordTextField, 1, 5);
        gridPane.add(confirmPasswordLabel, 0, 6);
        gridPane.add(confirmPasswordTextField, 1, 6);

        Button signUpButton = new Button("Sign Up");
        Button exitButton = new Button("Exit");
        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.getChildren().add(signUpButton);
        hBox.getChildren().add(exitButton);

        gridPane.add(hBox, 1, 7);
        exitButton.setOnAction(e -> {
            authStage.close();
            Platform.exit();
        });

        signUpButton.setOnAction(e -> {
            String fullName = nameTextField.getText();
            String email= emailTextField.getText();
            String phoneNumber = phoneNumberTextField.getText();
            String address = addressTextField.getText();
            String password = passwordTextField.getText();
            String confirmPassword = confirmPasswordTextField.getText();

            user = new User(fullName, address, phoneNumber);
            authStage.close();

            showMedicineListStage();
        });

        return gridPane;
    }

    private void showMedicineListStage() {
        Stage medicineListStage = new Stage();
        TableView<Medicine> cartTableView = new TableView<>();
        medicineListStage.setTitle("Medicine List and Search");

        pharmacyInventory = FXCollections.observableArrayList(
                new Medicine("Panadol", 1, "Fever and Pain Relief", 50.0, 20),
                new Medicine("Neurofen", 2, "Headache and Inflammation", 120.0, 50),
                new Medicine("Amoxilin", 3, "Antibiotic", 80.0, 30),
                new Medicine("Ventolin", 4, "Asthma Inhaler", 150.0, 25),
                new Medicine("Zyrtec", 5, "Allergy Relief", 60.0, 40),
                new Medicine("Aspirin", 6, "Pain and Inflammation", 30.0, 15),
                new Medicine("Lipitor", 7, "Cholesterol Control", 80.0, 32),
                new Medicine("Prozac", 8, "Antidepressant", 75.0, 35),
                new Medicine("Nexium", 9, "Acid Reflux Relief", 110.0, 15),
                new Medicine("Synthroid", 10, "Thyroid Medication", 40.0, 24)
        );

        GridPane gridPane = new GridPane();
        gridPane.setVgap(15);
        gridPane.setHgap(15);
        gridPane.setPadding(new Insets(20, 20, 20, 20));

        BackgroundUtil.setGridPaneBackground(gridPane, "/background.jpg");

        medicineTableView = new TableView<>();

        TableColumn<Medicine, Integer> idColumn= new TableColumn<>("Id");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Medicine, String> nameColumn = new TableColumn<>("Medicine Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Medicine, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Medicine, Double> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Medicine, Integer> quantityColumn = new TableColumn<>("Quantity");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("selectedQuantity"));

        quantityColumn.setCellFactory(c -> new TableCell<>() {
            private final Spinner<Integer> quantitySpinner = new Spinner<>(1, Integer.MAX_VALUE, 1);
            {
                quantitySpinner.setEditable(true);
                quantitySpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
                    Medicine medicine = getTableView().getItems().get(getIndex());
                    if (medicine != null) {
                        medicine.setSelectedQuantity(newValue);
                    }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Medicine medicine = getTableView().getItems().get(getIndex());
                    quantitySpinner.getValueFactory().setValue(medicine.getSelectedQuantity());
                    setGraphic(quantitySpinner);
                }
            }
        });

        medicineTableView.getColumns().addAll(idColumn, nameColumn, descriptionColumn, priceColumn, quantityColumn);
        medicineTableView.setItems(pharmacyInventory);

        idColumn.setPrefWidth(0.1 * 600);
        nameColumn.setPrefWidth(0.2 * 600);
        descriptionColumn.setPrefWidth(0.3 * 600);
        priceColumn.setPrefWidth(0.2 * 600);
        quantityColumn.setPrefWidth(0.2 * 600);

        medicineTableView.setPrefWidth(610);
        medicineTableView.setPrefHeight(350);

        // Create a ListView to display medicine names, prices, and quantities
        ListView<String> medicineListView = new ListView<>();
        for (Medicine medicine : pharmacyInventory) {
            String medicineInfo = String.format("%-10d %-10s %-10s Rs.%-15.1f %-15d",
                 medicine.getId(), medicine.getName(), medicine.getDescription(), medicine.getPrice(), medicine.getQuantity());
            medicineListView.getItems().add(medicineInfo);
        }

        TextField searchTextField = new TextField();
        searchTextField.setPromptText("Search Medicine");

        Button searchButton= new Button("Search");
        searchButton.setOnAction(e -> {
            String searchText = searchTextField.getText().trim().toLowerCase();

            if (!searchText.isEmpty()) {
                ObservableList<Medicine> searchResult = FXCollections.observableArrayList();

                for (Medicine medicine : pharmacyInventory) {
                    if (medicine.getName().toLowerCase().contains(searchText)) {
                        searchResult.add(medicine);
                    }
                }

                medicineTableView.setItems(searchResult);
            } else {
                // If the search text is empty, show the entire inventory
                medicineTableView.setItems(pharmacyInventory);
            }
        });

        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.setOnAction(e -> {
            Medicine selectedMedicine = medicineTableView.getSelectionModel().getSelectedItem();
            if (selectedMedicine != null) {
                // Update the selected quantity in the cart
                selectedMedicine.setSelectedQuantity(selectedMedicine.getSelectedQuantity());
                cart.add(selectedMedicine);
                System.out.println("Added Medicine: Name: " + selectedMedicine.getName() +
                        " - Quantity: " + selectedMedicine.getSelectedQuantity());
                // Update the cartTableView to reflect the changes in the cart
                cartTableView.setItems(cart);
            }
        });

        Button nextButton = new Button("Next");
        nextButton.setOnAction(e -> {
            System.out.println("Medicines are added to the Cart");
            medicineListStage.close();
            showCartAndOrderPlacementStage(user.getOrderHistoryObservable());
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            medicineListStage.close();
            Stage authStage = new Stage();
            authStage.setTitle("User Authentication");
            GridPane authGridPane = createAuthGridPane(authStage);
            Scene authScene = new Scene(authGridPane, 500, 350);
            authStage.setScene(authScene);
            authStage.show();
        });

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.getChildren().add(searchTextField);
        hBox.getChildren().add(searchButton);
        hBox.getChildren().add(addToCartButton);
        hBox.getChildren().add(nextButton);
        hBox.getChildren().add(backButton);

        Text title = new Text("Medicine Inventory");
        title.setFont(new Font("Times new roman", 20));

        gridPane.add(title, 0, 0, 2, 1); // Heading
        gridPane.add(medicineTableView, 0, 1, 2, 1);
        gridPane.add(hBox, 1, 2);

        Scene medicineListScene = new Scene(gridPane, 700, 500);
        medicineListStage.setScene(medicineListScene);
        medicineListStage.show();
    }


    private void showCartAndOrderPlacementStage(ObservableList<Order> orderHistoryObservable) {
        Stage cartStage = new Stage();
        cartStage.setTitle("Cart and Order Placement");

        GridPane gridPane = new GridPane();
        gridPane.setVgap(15);
        gridPane.setHgap(15);
        gridPane.setPadding(new Insets(20, 20, 20, 20));

        BackgroundUtil.setGridPaneBackground(gridPane, "/background.jpg");

        User.setCart(cart);

        TableView<Medicine> cartTableView = new TableView<>();
        cartTableView.setItems(user.getCartObservable());

        TableColumn<Medicine, Integer> idColumn = new TableColumn<>("Id");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Medicine, String> nameColumn = new TableColumn<>("Medicine Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Medicine, Double> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("Price"));

        TableColumn<Medicine, Integer> selectedQuantityColumn = new TableColumn<>("Selected Quantity");
        selectedQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("selectedQuantity"));

        cartTableView.getColumns().addAll(idColumn, nameColumn, priceColumn, selectedQuantityColumn);
        cartTableView.setItems(FXCollections.observableArrayList(cart));

        idColumn.setPrefWidth(0.2 * 400);
        nameColumn.setPrefWidth(0.3 * 400);
        priceColumn.setPrefWidth(0.3 * 400);
        selectedQuantityColumn.setPrefWidth(0.3 * 400);

        cartTableView.setPrefWidth(400);
        cartTableView.setPrefHeight(200);

        Label totalPrice = new Label("Total Price: Rs." + calculateTotalCartPrice());
        totalPrice.setFont(new Font("Times new roman", 15));

        Button placeOrderButton = new Button("Place Order");
        placeOrderButton.setOnAction(e -> {
            user.placeOrder();
            cart.clear();
            Label order= new Label("Order is Placed");
            order.setFont(new Font("Times new roman", 15));
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            cartStage.close();
            showMedicineListStage();
        });

        Button viewOrderHistoryButton = new Button("View Order History");
        viewOrderHistoryButton.setOnAction(e -> showOrderHistoryStage(orderHistoryObservable));

        HBox buttonBox = new HBox(10, placeOrderButton, backButton, viewOrderHistoryButton);
        buttonBox.setAlignment(Pos.CENTER);

        Text title = new Text("Cart Summary");
        title.setFont(new Font("Times new roman", 20));

        gridPane.add(title, 0, 0, 2, 1); // Heading
        gridPane.add(cartTableView, 0, 1, 2, 1);
        gridPane.add(totalPrice, 0, 2, 2, 1);
        gridPane.add(buttonBox, 0, 3, 2, 1);

        Scene cartScene = new Scene(gridPane, 600, 400);

        cartStage.setScene(cartScene);
        cartStage.show();
    }

    private void showOrderHistoryStage(ObservableList<Order> orderHistoryObservable) {
        Stage orderHistoryStage = new Stage();
        orderHistoryStage.setTitle("Order History");

        GridPane gridPane = new GridPane();
        gridPane.setVgap(15);
        gridPane.setHgap(15);
        gridPane.setPadding(new Insets(20, 20, 20, 20));

        BackgroundUtil.setGridPaneBackground(gridPane, "/background.jpg");

        TableView<Medicine> cartTableView = new TableView<>();
        orderHistoryTableView = new TableView<>();

        TableColumn<Order, Integer> orderIdColumn = new TableColumn<>("Idd");
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("orderId"));

        TableColumn<Order, String> orderNameColumn = new TableColumn<>("User Name");
        orderNameColumn.setCellValueFactory(order -> new SimpleStringProperty(order.getValue().getUser().getName()));

        TableColumn<Order, String> orderDateColumn = new TableColumn<>("Order Date");
        orderDateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));

        TableColumn<Order, Double> totalPriceColumn = new TableColumn<>("Total Price");
        totalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        orderHistoryTableView.getColumns().addAll(orderIdColumn, orderNameColumn, orderDateColumn, totalPriceColumn);
        orderHistoryTableView.setItems(orderHistoryObservable);

        orderIdColumn.setPrefWidth(0.1 * 450);
        orderNameColumn.setPrefWidth(0.2 * 450);
        orderDateColumn.setPrefWidth(0.4 * 450);
        totalPriceColumn.setPrefWidth(0.3 * 450);

        orderHistoryTableView.setPrefWidth(450);
        orderHistoryTableView.setPrefHeight(200);

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> orderHistoryStage.close());

        Text title = new Text("Order History");
        title.setFont(new Font("Times new roman", 20));

        gridPane.add(title, 0, 0, 2, 1); // Heading
        gridPane.add(orderHistoryTableView, 0, 1, 2, 1);
        gridPane.add(closeButton, 0, 2, 2, 1);

        Scene orderHistoryScene = new Scene(gridPane, 500, 300);

        orderHistoryStage.setScene(orderHistoryScene);
        orderHistoryStage.show();
    }

    private double calculateTotalCartPrice() {
        double totalCartPrice = 0.0;
        for (Medicine medicine : cart) {
            totalCartPrice += medicine.getPrice();
        }
        return totalCartPrice;
    }
  }
