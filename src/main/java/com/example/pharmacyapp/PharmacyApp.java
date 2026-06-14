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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private User registeredUser;
    private java.util.List<User> userDatabase = new java.util.ArrayList<>();
    private double discountMultiplier = 0.0;
    private boolean deliveryDetailsSaved = false;
    private String savedDeliveryInfo = "";
    private Medicine selectedInventoryMedicine = null;

    private String getDbPath() {
        return System.getProperty("user.home") + "/pharmacy_users.txt";
    }

    private void loadUsers() {
        userDatabase.clear();
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(getDbPath()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals("USER") && parts.length >= 6) {
                    userDatabase.add(new User(parts[1], parts[2], parts[3], parts[4], parts[5]));
                } else if (parts[0].equals("ORDER") && parts.length >= 5) {
                    final String email = parts[1];
                    User u = null;
                    for (User usr : userDatabase) {
                        if (usr.getEmail().equals(email)) { u = usr; break; }
                    }
                    if (u != null) {
                        java.util.List<Medicine> meds = new java.util.ArrayList<>();
                        String[] medStrs = parts.length >= 6 ? parts[5].split(";") : parts[4].split(";");
                        for (String mStr : medStrs) {
                            if (mStr.isEmpty()) continue;
                            String[] medParts = mStr.split(":");
                            if (medParts.length >= 3) {
                                String mName = medParts[0];
                                int qty = Integer.parseInt(medParts[1]);
                                double price = Double.parseDouble(medParts[2]);
                                Medicine m = new Medicine(mName, 0, "", price, qty);
                                m.setSelectedQuantity(qty);
                                meds.add(m);
                            }
                        }
                        Order o = new Order(u, meds);
                        o.setStatus(parts[2]);
                        try {
                            o.setOrderDate(new java.util.Date(Long.parseLong(parts[3])));
                        } catch(Exception e) {}
                        u.getOrderHistoryObservable().add(o);
                    }
                } else if (parts[0].equals("ADDRESS") && parts.length >= 3) {
                    final String email = parts[1];
                    for (User usr : userDatabase) {
                        if (usr.getEmail().equals(email)) {
                            usr.addSavedAddress(line.substring(8 + email.length() + 1));
                            break;
                        }
                    }
                } else if (!parts[0].equals("ORDER") && !parts[0].equals("USER") && !parts[0].equals("ADDRESS") && parts.length >= 5) {
                    // Fallback for old data
                    userDatabase.add(new User(parts[0], parts[1], parts[2], parts[3], parts[4]));
                }
            }
        } catch (Exception e) {}
    }

    private void saveUser(User u) {
        userDatabase.add(u);
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(getDbPath(), true))) {
            out.println("USER," + u.getName() + "," + u.getAddress() + "," + u.getContactInfo() + "," + u.getEmail() + "," + u.getPassword());
        } catch (Exception e) {}
    }

    private void saveOrder(Order o) {
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(getDbPath(), true))) {
            StringBuilder sb = new StringBuilder();
            sb.append("ORDER,").append(o.getUser().getEmail()).append(",")
              .append(o.getStatus()).append(",").append(o.getOrderDate().getTime()).append(",")
              .append(o.getTotalPrice()).append(",");
            for (Medicine m : o.getMedicines()) {
                sb.append(m.getName()).append(":").append(m.getSelectedQuantity()).append(":").append(m.getPrice()).append(";");
            }
            out.println(sb.toString());
        } catch (Exception e) {}
    }

    private void updateOrderInFile(Order o) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(getDbPath()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("ORDER," + o.getUser().getEmail() + ",") && line.contains(String.valueOf(o.getOrderDate().getTime()))) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("ORDER,").append(o.getUser().getEmail()).append(",")
                      .append(o.getStatus()).append(",").append(o.getOrderDate().getTime()).append(",")
                      .append(o.getTotalPrice()).append(",");
                    for (Medicine m : o.getMedicines()) {
                        sb.append(m.getName()).append(":").append(m.getSelectedQuantity()).append(":").append(m.getPrice()).append(";");
                    }
                    lines.add(sb.toString());
                } else {
                    lines.add(line);
                }
            }
        } catch (Exception e) {}
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(getDbPath()))) {
            for (String l : lines) out.println(l);
        } catch (Exception e) {}
    }

    private void deleteOrderFromFile(Order o) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(getDbPath()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("ORDER," + o.getUser().getEmail() + ",") && line.contains(String.valueOf(o.getOrderDate().getTime()))) {
                    continue;
                }
                lines.add(line);
            }
        } catch (Exception e) {}
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(getDbPath()))) {
            for (String l : lines) out.println(l);
        } catch (Exception e) {}
    }

    private void saveAddressToFile(User u, String address) {
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(getDbPath(), true))) {
            out.println("ADDRESS," + u.getEmail() + "," + address);
        } catch (Exception e) {}
    }

    private VBox createIconField(String labelText, String iconText, TextField textField) {
        Text label = new Text(labelText);
        label.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
        label.setFill(javafx.scene.paint.Color.web("#34495e"));

        Text icon = new Text(iconText);
        icon.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        
        HBox iconBox = new HBox(icon);
        iconBox.setPadding(new Insets(0, 5, 0, 0));

        textField.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        HBox.setHgrow(textField, Priority.ALWAYS);

        HBox inputBox = new HBox(iconBox, textField);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-padding: 8;");

        VBox container = new VBox(5, label, inputBox);
        container.setPadding(new Insets(0, 0, 5, 0));
        return container;
    }

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) {
        loadUsers();
        preloadIcons();
        showSplashScreen(primaryStage);
    }

    private void preloadIcons() {
        Thread preloader = new Thread(() -> {
            String[] commonIcons = {
                "https://img.icons8.com/color/48/shopping-cart.png",
                "https://img.icons8.com/color/48/000000/order-history.png",
                "https://img.icons8.com/ios-glyphs/48/ffffff/search.png",
                "https://img.icons8.com/color/48/pill.png",
                "https://img.icons8.com/color/48/pills.png",
                "https://img.icons8.com/color/48/doctors-bag.png",
                "https://img.icons8.com/color/48/lungs.png",
                "https://img.icons8.com/color/48/sneeze.png",
                "https://img.icons8.com/color/48/heart-health.png",
                "https://img.icons8.com/color/48/brain.png",
                "https://img.icons8.com/ios-glyphs/48/e74c3c/trash.png",
                "https://img.icons8.com/color/48/000000/shopping-cart.png",
                "https://img.icons8.com/color/48/discount.png",
                "https://img.icons8.com/color/48/receipt.png",
                "https://img.icons8.com/color/48/bank-cards.png",
                "https://img.icons8.com/color/48/clipboard.png",
                "https://img.icons8.com/color/48/marker.png",
                "https://img.icons8.com/color/48/shop.png",
                "https://img.icons8.com/color/48/truck.png",
                "https://img.icons8.com/ios-glyphs/48/ffffff/truck.png",
                "https://img.icons8.com/color/48/back.png",
                "https://img.icons8.com/color/48/order-history.png",
                "https://img.icons8.com/color/48/calendar.png",
                "https://img.icons8.com/color/48/clock.png",
                "https://img.icons8.com/color/48/cash.png",
                "https://img.icons8.com/ios-glyphs/48/ffffff/save.png",
                "https://img.icons8.com/ios-glyphs/48/ffffff/checked-checkbox.png",
                "https://img.icons8.com/color/48/ok.png",
                "https://img.icons8.com/color/48/cancel.png",
                "https://img.icons8.com/color/48/refresh.png",
                "https://img.icons8.com/ios-filled/48/e74c3c/logout-rounded-left.png",
                "https://img.icons8.com/color/48/stomach.png",
                "https://img.icons8.com/color/48/clinic.png",
                "https://img.icons8.com/ios-glyphs/48/ffffff/shopping-cart--v1.png",
                "https://img.icons8.com/ios-filled/48/e74c3c/order-history.png",
                "https://img.icons8.com/color/48/forward.png",
                "https://img.icons8.com/color/48/search.png",
                "https://img.icons8.com/color/48/shopping-bag.png",
                "https://img.icons8.com/color/48/checkout.png",
                "https://img.icons8.com/color/96/ok--v1.png",
                "https://img.icons8.com/color/48/home.png",
                "https://img.icons8.com/color/48/motorcycle.png",
                "https://img.icons8.com/color/48/checked-checkbox.png"
            };
            for (String url : commonIcons) {
                if (!iconCache.containsKey(url)) {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(url, true);
                    iconCache.put(url, img);
                }
            }
        });
        preloader.setDaemon(true);
        preloader.start();
    }

    private void showSplashScreen(Stage splashStage) {
        splashStage.setTitle("PharmaPlus - Welcome");

        // --- Left Side (Content) ---
        Text pharmaTop = new Text("Pharma");
        pharmaTop.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 48));
        pharmaTop.setFill(javafx.scene.paint.Color.web("#0070c0"));
        Text plusTop = new Text("Plus");
        plusTop.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 48));
        plusTop.setFill(javafx.scene.paint.Color.web("#20b2aa"));
        HBox topTitleBox = new HBox(pharmaTop, plusTop);
        topTitleBox.setAlignment(Pos.CENTER_LEFT);

        Text subTitle = new Text("Your Health, Our Priority");
        subTitle.setFont(javafx.scene.text.Font.font("System", 16));
        subTitle.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        
        javafx.scene.shape.Line line1 = new javafx.scene.shape.Line(0,0,40,0); line1.setStroke(javafx.scene.paint.Color.web("#20b2aa"));
        javafx.scene.shape.Line line2 = new javafx.scene.shape.Line(0,0,40,0); line2.setStroke(javafx.scene.paint.Color.web("#20b2aa"));
        HBox subTitleBox = new HBox(15, line1, subTitle, line2);
        subTitleBox.setAlignment(Pos.CENTER_LEFT);
        
        VBox logoBox = new VBox(5, topTitleBox, subTitleBox);
        logoBox.setPadding(new Insets(0, 0, 40, 0));

        Text welcomeTitle = new Text("Welcome to PharmaPlus");
        welcomeTitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 32));
        welcomeTitle.setFill(javafx.scene.paint.Color.web("#2c3e50"));

        Text desc = new Text("Your trusted partner in health and wellness.\nQuality medicines, delivered with care.");
        desc.setFont(javafx.scene.text.Font.font("System", 16));
        desc.setFill(javafx.scene.paint.Color.web("#7f8c8d"));

        Button getStartedBtn = new Button("Get Started  →");
        getStartedBtn.setStyle("-fx-background-color: #0070c0; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 25px; -fx-cursor: hand;");
        getStartedBtn.setPadding(new Insets(12, 35, 12, 35));
        getStartedBtn.setOnAction(e -> {
            splashStage.close();
            showLoginStage();
        });

        // Bottom Icons
        VBox feat1 = createFeatureBox("🛡", "Trusted Quality", "100% Genuine Medicines");
        VBox feat2 = createFeatureBox("🚚", "Fast Delivery", "On-time, Every time");
        VBox feat3 = createFeatureBox("🎧", "24/7 Support", "We're here to help");
        HBox featuresBox = new HBox(30, feat1, feat2, feat3);
        featuresBox.setPadding(new Insets(50, 0, 0, 0));

        VBox leftSide = new VBox(20, logoBox, welcomeTitle, desc, getStartedBtn, featuresBox);
        leftSide.setAlignment(Pos.CENTER_LEFT);
        leftSide.setPadding(new Insets(50, 40, 50, 60));
        leftSide.setPrefWidth(550);

        // --- Right Side (Image/Graphic) ---
        ImageView imageView = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream("/7.jfif"));
            imageView.setImage(image);
            imageView.setFitWidth(350);
            imageView.setPreserveRatio(true);
        } catch (Exception e) {
            System.out.println("Image not found");
        }
        
        javafx.scene.layout.StackPane rightSide = new javafx.scene.layout.StackPane(imageView);
        rightSide.setStyle("-fx-background-color: linear-gradient(to bottom right, #e2f1f8, #f4f9f9); -fx-background-radius: 100 0 0 100;");
        rightSide.setPrefWidth(450);
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        HBox mainLayout = new HBox(leftSide, rightSide);
        mainLayout.setStyle("-fx-background-color: white;");

        Scene splashScene = new Scene(mainLayout, 1000, 600);
        splashStage.setScene(splashScene);
        splashStage.show();
    }

    private VBox createFeatureBox(String icon, String title, String subtitle) {
        Text iconTxt = new Text(icon);
        iconTxt.setFont(javafx.scene.text.Font.font(24));
        iconTxt.setFill(javafx.scene.paint.Color.web("#0070c0"));
        
        Text titleTxt = new Text(title);
        titleTxt.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
        titleTxt.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        
        Text subTxt = new Text(subtitle);
        subTxt.setFont(javafx.scene.text.Font.font("System", 10));
        subTxt.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        
        VBox box = new VBox(5, iconTxt, titleTxt, subTxt);
        box.setAlignment(Pos.CENTER);
        return box;
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
    private VBox createAuthSideColumn(String imagePath, boolean isRightSide) {
        Text pharmaTop = new Text("Pharma");
        pharmaTop.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 48));
        pharmaTop.setFill(javafx.scene.paint.Color.web("#0070c0"));
        Text plusTop = new Text("Plus");
        plusTop.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 48));
        plusTop.setFill(javafx.scene.paint.Color.web("#20b2aa"));
        HBox topTitleBox = new HBox(pharmaTop, plusTop);
        topTitleBox.setAlignment(Pos.CENTER_LEFT);

        Text subTitle = new Text("Your Health, Our Priority");
        subTitle.setFont(javafx.scene.text.Font.font("System", 16));
        subTitle.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        
        javafx.scene.shape.Line line1 = new javafx.scene.shape.Line(0,0,40,0); line1.setStroke(javafx.scene.paint.Color.web("#20b2aa"));
        HBox subTitleBox = new HBox(15, subTitle, line1);
        subTitleBox.setAlignment(Pos.CENTER_LEFT);
        
        VBox logoBox = new VBox(5, topTitleBox, subTitleBox);
        logoBox.setPadding(new Insets(0, 0, 40, 0));

        Text desc = new Text("Delivering trusted healthcare solutions with compassion and excellence.\nYour well-being is our commitment.");
        desc.setFont(javafx.scene.text.Font.font("System", 14));
        desc.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        desc.setWrappingWidth(300);

        VBox contentBox = new VBox(15, logoBox, desc);
        contentBox.setAlignment(Pos.TOP_LEFT);
        
        ImageView imageView = new ImageView();
        try {
            Image image;
            if (imagePath.startsWith("http")) {
                image = new Image(imagePath, true); // true for background loading
            } else if (imagePath.startsWith("file:")) {
                image = new Image(imagePath);
            } else {
                image = new Image(getClass().getResourceAsStream(imagePath));
            }
            imageView.setImage(image);
            imageView.setFitWidth(350);
            imageView.setPreserveRatio(true);
        } catch (Exception e) {
            System.out.println("Could not load image: " + imagePath);
        }
        
        VBox sideColumn = new VBox(40, contentBox, imageView);
        sideColumn.setAlignment(Pos.TOP_LEFT);
        sideColumn.setPadding(new Insets(60, 40, 60, 60));
        
        if (isRightSide) {
            sideColumn.setStyle("-fx-background-color: linear-gradient(to bottom left, #e2f1f8, #f4f9f9); -fx-background-radius: 100 0 0 100;");
        } else {
            sideColumn.setStyle("-fx-background-color: linear-gradient(to bottom right, #e2f1f8, #f4f9f9); -fx-background-radius: 0 100 100 0;");
        }
        
        sideColumn.setPrefWidth(450);
        return sideColumn;
    }

    private HBox createAuthLayout(Stage authStage) {
        // Top Header
        Text pharmaTop = new Text("Pharma");
        pharmaTop.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 30));
        pharmaTop.setFill(javafx.scene.paint.Color.web("#0070c0"));

        Text plusTop = new Text("Plus");
        plusTop.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 30));
        plusTop.setFill(javafx.scene.paint.Color.web("#20b2aa"));

        HBox topTitleBox = new HBox(pharmaTop, plusTop);
        topTitleBox.setAlignment(Pos.CENTER);

        Text subTitle = new Text("Your Health, Our Priority");
        subTitle.setFont(javafx.scene.text.Font.font("System", 14));
        subTitle.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        
        VBox headerBox = new VBox(5, topTitleBox, subTitle);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(20, 0, 20, 0));

        // Main Card
        VBox card = new VBox(15);
        card.setMaxWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        card.setPadding(new Insets(25));
        card.setAlignment(Pos.TOP_LEFT);

        // Icon + Welcome
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(25, javafx.scene.paint.Color.web("#e3f2fd"));
        javafx.scene.image.ImageView userIcon = new javafx.scene.image.ImageView(new javafx.scene.image.Image("https://img.icons8.com/ios-filled/50/1976d2/user.png", true));
        userIcon.setFitWidth(24);
        userIcon.setFitHeight(24);
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(circle, userIcon);
        
        VBox welcomeBox = new VBox(5);
        Text welcomeText = new Text("Create Account");
        welcomeText.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 20));
        welcomeText.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        Text loginContText = new Text("Join PharmaPlus and manage your healthcare easily");
        loginContText.setFont(javafx.scene.text.Font.font("System", 10));
        loginContText.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        welcomeBox.getChildren().addAll(welcomeText, loginContText);

        HBox topCardBox = new HBox(15, iconPane, welcomeBox);
        topCardBox.setAlignment(Pos.CENTER_LEFT);

        // Fields
        TextField nameTextField = new TextField();
        nameTextField.setPromptText("Enter your full name");
        VBox nameBox = createIconField("Full Name", "👤", nameTextField);

        TextField emailTextField = new TextField();
        emailTextField.setPromptText("Enter your email");
        VBox emailBox = createIconField("Email Address", "✉", emailTextField);

        TextField phoneNumberTextField = new TextField();
        phoneNumberTextField.setPromptText("Enter your phone number");
        phoneNumberTextField.textProperty().addListener((o, oldV, newV) -> {
            if (!newV.matches("\\d*")) phoneNumberTextField.setText(newV.replaceAll("[^\\d]", ""));
            if (phoneNumberTextField.getText().length() > 11) phoneNumberTextField.setText(phoneNumberTextField.getText().substring(0, 11));
        });
        VBox phoneBox = createIconField("Phone Number", "📞", phoneNumberTextField);

        PasswordField passwordTextField = new PasswordField();
        passwordTextField.setPromptText("Enter your password");
        VBox passBox = createIconField("Password", "🔒", passwordTextField);

        PasswordField confirmPasswordTextField = new PasswordField();
        confirmPasswordTextField.setPromptText("Confirm your password");
        VBox confPassBox = createIconField("Confirm Password", "🔒", confirmPasswordTextField);

        Button signUpButton = new Button("Create Account");
        signUpButton.setMaxWidth(Double.MAX_VALUE);
        signUpButton.setStyle("-fx-background-color: linear-gradient(to right, #1976d2, #20b2aa); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;");
        signUpButton.setPadding(new Insets(10));

        Text noAccount = new Text("Already have an account? ");
        noAccount.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        Text loginLink = new Text("Sign In");
        loginLink.setFill(javafx.scene.paint.Color.web("#0070c0"));
        loginLink.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
        loginLink.setStyle("-fx-cursor: hand;");
        loginLink.setOnMouseEntered(e -> loginLink.setUnderline(true));
        loginLink.setOnMouseExited(e -> loginLink.setUnderline(false));
        javafx.scene.text.TextFlow loginFlow = new javafx.scene.text.TextFlow(noAccount, loginLink);
        loginFlow.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Divider
        Label divider = new Label("or continue with");
        divider.setTextFill(javafx.scene.paint.Color.web("#bdc3c7"));
        divider.setFont(javafx.scene.text.Font.font("System", 11));
        HBox dividerBox = new HBox(divider);
        dividerBox.setAlignment(Pos.CENTER);

        // Social Buttons
        Button googleBtn = new Button(" Continue with Google");
        try {
            javafx.scene.image.ImageView googleIcon = new javafx.scene.image.ImageView(new javafx.scene.image.Image("https://img.icons8.com/color/48/000000/google-logo.png", true));
            googleIcon.setFitHeight(16);
            googleIcon.setFitWidth(16);
            googleBtn.setGraphic(googleIcon);
        } catch (Exception e) {}
        googleBtn.setStyle("-fx-background-color: white; -fx-border-color: #ecf0f1; -fx-border-radius: 5; -fx-font-size: 10px; -fx-cursor: hand;");
        googleBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(googleBtn, Priority.ALWAYS);

        Button fbBtn = new Button(" Continue with Facebook");
        try {
            javafx.scene.image.ImageView fbIcon = new javafx.scene.image.ImageView(new javafx.scene.image.Image("https://img.icons8.com/color/48/000000/facebook-new.png", true));
            fbIcon.setFitHeight(16);
            fbIcon.setFitWidth(16);
            fbBtn.setGraphic(fbIcon);
        } catch (Exception e) {}
        fbBtn.setStyle("-fx-background-color: white; -fx-border-color: #ecf0f1; -fx-border-radius: 5; -fx-font-size: 10px; -fx-cursor: hand;");
        fbBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fbBtn, Priority.ALWAYS);

        HBox socialBox = new HBox(10, googleBtn, fbBtn);

        // Badge
        Label secureBadge = new Label("✔️ Your data is secure with us");
        secureBadge.setStyle("-fx-background-color: #e8f8f5; -fx-text-fill: #1abc9c; -fx-background-radius: 5; -fx-padding: 8;");
        secureBadge.setMaxWidth(Double.MAX_VALUE);
        secureBadge.setAlignment(Pos.CENTER);

        card.getChildren().addAll(topCardBox, nameBox, emailBox, phoneBox, passBox, confPassBox, signUpButton, loginFlow, dividerBox, socialBox, secureBadge);

        Button backButton = new Button("Back");
        backButton.setGraphic(getIcon("https://img.icons8.com/color/48/back.png", 16));
        backButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 14px;");
        backButton.setOnAction(e -> {
            authStage.close();
            showSplashScreen(new Stage());
        });
        
        HBox topBar = new HBox(backButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20, 0, 0, 20));

        VBox formSideContent = new VBox(card);
        formSideContent.setAlignment(Pos.CENTER);
        VBox.setVgrow(formSideContent, Priority.ALWAYS);

        VBox formSide = new VBox(topBar, formSideContent);
        formSide.setStyle("-fx-background-color: white;");
        formSide.setPrefWidth(550);
        HBox.setHgrow(formSide, Priority.ALWAYS);

        HBox mainLayout = new HBox(formSide, createAuthSideColumn("/background.jpg", true));
        mainLayout.setStyle("-fx-background-color: white;");

        loginLink.setOnMouseClicked(e -> {
            authStage.close();
            showLoginStage();
        });

        signUpButton.setOnAction(e -> {
            String fullName = nameTextField.getText().trim();
            String email = emailTextField.getText().trim();
            String phoneNumber = phoneNumberTextField.getText().trim();
            String password = passwordTextField.getText();
            String confirmPassword = confirmPasswordTextField.getText();

            if (!isValidName(fullName)) {
                showAlert(Alert.AlertType.ERROR, authStage, "Sign Up Error", "Name must contain only letters and spaces.");
                return;
            }
            if (!isValidEmail(email)) {
                showAlert(Alert.AlertType.ERROR, authStage, "Sign Up Error", "Enter a valid email address.");
                return;
            }
            if (phoneNumber.length() != 11) {
                showAlert(Alert.AlertType.ERROR, authStage, "Sign Up Error", "Phone number must be exactly 11 digits.");
                return;
            }
            if (password.length() < 8) {
                showAlert(Alert.AlertType.ERROR, authStage, "Sign Up Error", "Password must be at least 8 characters.");
                return;
            }
            if (!password.equals(confirmPassword)) {
                showAlert(Alert.AlertType.ERROR, authStage, "Sign Up Error", "Passwords do not match.");
                return;
            }
            
            for (User u : userDatabase) {
                if (u.getEmail().equalsIgnoreCase(email)) {
                    showAlert(Alert.AlertType.ERROR, authStage, "Sign Up Error", "This email is already registered! Please go back and Sign In instead.");
                    return;
                }
            }

            User newUser = new User(fullName, "N/A", phoneNumber, email, password);
            saveUser(newUser);
            authStage.close();
            showLoginStage();
        });

        return mainLayout;
    }

    private void showLoginStage() {
        Stage loginStage = new Stage();
        loginStage.setTitle("Login");

        // Main Login Card
        VBox card = new VBox(15);
        card.setMaxWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        card.setPadding(new Insets(25));
        card.setAlignment(Pos.TOP_LEFT);

        // Icon + Welcome
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(25, javafx.scene.paint.Color.web("#e3f2fd"));
        javafx.scene.image.ImageView userIcon = new javafx.scene.image.ImageView(new javafx.scene.image.Image("https://img.icons8.com/ios-filled/50/1976d2/user.png", true));
        userIcon.setFitWidth(24);
        userIcon.setFitHeight(24);
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(circle, userIcon);
        
        VBox welcomeBox = new VBox(5);
        Text welcomeText = new Text("Welcome Back!");
        welcomeText.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 20));
        welcomeText.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        Text loginContText = new Text("Login to continue to your account");
        loginContText.setFont(javafx.scene.text.Font.font("System", 12));
        loginContText.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        welcomeBox.getChildren().addAll(welcomeText, loginContText);

        HBox topCardBox = new HBox(15, iconPane, welcomeBox);
        topCardBox.setAlignment(Pos.CENTER_LEFT);

        // Form Fields
        TextField emailTextField = new TextField();
        emailTextField.setPromptText("Enter your email");
        VBox emailBox = createIconField("Email Address", "✉", emailTextField);

        PasswordField passwordTextField = new PasswordField();
        passwordTextField.setPromptText("Enter your password");
        VBox passBox = createIconField("Password", "🔒", passwordTextField);

        Label forgotPassLabel = new Label("Forgot Password?");
        forgotPassLabel.setTextFill(javafx.scene.paint.Color.web("#0070c0"));
        forgotPassLabel.setFont(javafx.scene.text.Font.font("System", 11));
        HBox forgotBox = new HBox(forgotPassLabel);
        forgotBox.setAlignment(Pos.CENTER_RIGHT);

        Button loginButton = new Button("Login");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle("-fx-background-color: linear-gradient(to right, #1976d2, #20b2aa); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;");
        loginButton.setPadding(new Insets(10));

        // Sign up link
        Text noAccount = new Text("Don't have an account? ");
        noAccount.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        Text signUpLink = new Text("Sign Up");
        signUpLink.setFill(javafx.scene.paint.Color.web("#0070c0"));
        signUpLink.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
        signUpLink.setStyle("-fx-cursor: hand;");
        signUpLink.setOnMouseEntered(e -> signUpLink.setUnderline(true));
        signUpLink.setOnMouseExited(e -> signUpLink.setUnderline(false));
        javafx.scene.text.TextFlow signUpFlow = new javafx.scene.text.TextFlow(noAccount, signUpLink);
        signUpFlow.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Divider
        Label divider = new Label("or continue with");
        divider.setTextFill(javafx.scene.paint.Color.web("#bdc3c7"));
        divider.setFont(javafx.scene.text.Font.font("System", 11));
        HBox dividerBox = new HBox(divider);
        dividerBox.setAlignment(Pos.CENTER);

        // Social Buttons
        Button googleBtn = new Button(" Google");
        try {
            javafx.scene.image.ImageView googleIcon = new javafx.scene.image.ImageView(new javafx.scene.image.Image("https://img.icons8.com/color/48/000000/google-logo.png", true));
            googleIcon.setFitHeight(16);
            googleIcon.setFitWidth(16);
            googleBtn.setGraphic(googleIcon);
        } catch (Exception e) {}
        googleBtn.setStyle("-fx-background-color: white; -fx-border-color: #ecf0f1; -fx-border-radius: 5; -fx-font-size: 10px; -fx-cursor: hand;");
        googleBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(googleBtn, Priority.ALWAYS);

        Button fbBtn = new Button(" Facebook");
        try {
            javafx.scene.image.ImageView fbIcon = new javafx.scene.image.ImageView(new javafx.scene.image.Image("https://img.icons8.com/color/48/000000/facebook-new.png", true));
            fbIcon.setFitHeight(16);
            fbIcon.setFitWidth(16);
            fbBtn.setGraphic(fbIcon);
        } catch (Exception e) {}
        fbBtn.setStyle("-fx-background-color: white; -fx-border-color: #ecf0f1; -fx-border-radius: 5; -fx-font-size: 10px; -fx-cursor: hand;");
        fbBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fbBtn, Priority.ALWAYS);

        HBox socialBox = new HBox(10, googleBtn, fbBtn);

        // Badge
        Label secureBadge = new Label("✔️ Your data is secure with us");
        secureBadge.setStyle("-fx-background-color: #e8f8f5; -fx-text-fill: #1abc9c; -fx-background-radius: 5; -fx-padding: 8;");
        secureBadge.setMaxWidth(Double.MAX_VALUE);
        secureBadge.setAlignment(Pos.CENTER);

        // Add to card
        card.getChildren().addAll(topCardBox, emailBox, passBox, forgotBox, loginButton, signUpFlow, dividerBox, socialBox, secureBadge);

        Button backButton = new Button("Back");
        backButton.setGraphic(getIcon("https://img.icons8.com/color/48/back.png", 16));
        backButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 14px;");
        backButton.setOnAction(e -> {
            loginStage.close();
            showSplashScreen(new Stage());
        });
        
        HBox topBar = new HBox(backButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20, 0, 0, 20));

        VBox rightSideContent = new VBox(card);
        rightSideContent.setAlignment(Pos.CENTER);
        VBox.setVgrow(rightSideContent, Priority.ALWAYS);

        VBox rightSide = new VBox(topBar, rightSideContent);
        rightSide.setStyle("-fx-background-color: white;");
        rightSide.setPrefWidth(550);
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        HBox mainLayout = new HBox(createAuthSideColumn("/6.jfif", false), rightSide);
        mainLayout.setStyle("-fx-background-color: white;");

        // Actions
        loginButton.setOnAction(e -> {
            String email = emailTextField.getText().trim();
            String password = passwordTextField.getText();

            User foundUser = null;
            if (registeredUser != null && registeredUser.getEmail().equals(email) && registeredUser.getPassword().equals(password)) {
                foundUser = registeredUser;
            } else {
                for (User u : userDatabase) {
                    if (u.getEmail().equals(email) && u.getPassword().equals(password)) {
                        foundUser = u;
                        break;
                    }
                }
            }

            if (foundUser == null) {
                showAlert(Alert.AlertType.ERROR, loginStage, "Login Error", "Invalid email or password.");
                return;
            }
            loginStage.close();
            user = foundUser;
            showMedicineListStage();
        });

        signUpLink.setOnMouseClicked(e -> {
            loginStage.close();
            Stage authStage = new Stage();
            authStage.setTitle("User Sign Up");
            HBox authLayout = createAuthLayout(authStage);
            
            Scene authScene = new Scene(authLayout, 1000, 600);
            authStage.setScene(authScene);
            authStage.show();
        });

        Scene loginScene = new Scene(mainLayout, 1000, 600);
        loginStage.setScene(loginScene);
        loginStage.show();
    }

    private boolean isValidName(String name) {
        return name != null && !name.isEmpty() && name.matches("[A-Za-z ]+");
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("\\d+");
    }

    private void showAlert(Alert.AlertType alertType, Stage stage, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private void showMedicineListStage() {
        Stage medicineListStage = new Stage();
        medicineListStage.setTitle("Medicine List and Search");

        pharmacyInventory = FXCollections.observableArrayList(
                new Medicine("Panadol", 1, "Fever and Pain Relief", 50.0, 20),
                new Medicine("Neurofen", 2, "Headache and Pain Relief", 120.0, 50),
                new Medicine("Amoxilin", 3, "Antibiotic", 80.0, 30),
                new Medicine("Ventolin", 4, "Asthma Inhaler", 150.0, 25),
                new Medicine("Zyrtec", 5, "Allergy Relief", 60.0, 40),
                new Medicine("Aspirin", 6, "Pain and Inflammation", 30.0, 15),
                new Medicine("Lipitor", 7, "Cholesterol Control", 80.0, 32),
                new Medicine("Prozac", 8, "Antidepressant", 75.0, 35),
                new Medicine("Nexium", 9, "Acid Reflux Relief", 110.0, 15),
                new Medicine("Synthroid", 10, "Thyroid Medication", 40.0, 24)
        );

        // Header
        javafx.scene.shape.Rectangle iconBg = new javafx.scene.shape.Rectangle(40, 40, javafx.scene.paint.Color.web("white"));
        iconBg.setArcWidth(10);
        iconBg.setArcHeight(10);
        iconBg.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        javafx.scene.image.ImageView pillIcon = new javafx.scene.image.ImageView(new javafx.scene.image.Image("https://img.icons8.com/color/48/000000/pill.png", true));
        pillIcon.setFitWidth(24);
        pillIcon.setFitHeight(24);
        javafx.scene.layout.StackPane topIconPane = new javafx.scene.layout.StackPane(iconBg, pillIcon);

        Text title1 = new Text("Medicine ");
        title1.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 22));
        title1.setFill(javafx.scene.paint.Color.web("#0e2e50"));
        Text title2 = new Text("Inventory");
        title2.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 22));
        title2.setFill(javafx.scene.paint.Color.web("#00b894"));
        javafx.scene.text.TextFlow titleFlow = new javafx.scene.text.TextFlow(title1, title2);
        
        HBox headerBox = new HBox(15, topIconPane, titleFlow);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Search Bar
        TextField searchTextField = new TextField();
        searchTextField.setPromptText("Search medicines by name, description...");
        searchTextField.setStyle("-fx-background-color: transparent; -fx-text-fill: #2c3e50; -fx-prompt-text-fill: #aab0b8; -fx-padding: 0;");
        HBox.setHgrow(searchTextField, Priority.ALWAYS);
        javafx.scene.image.ImageView searchIcon = getIcon("https://img.icons8.com/color/48/search.png", 20);
        HBox searchInputBox = new HBox(8, searchIcon, searchTextField);
        searchInputBox.setAlignment(Pos.CENTER_LEFT);
        searchInputBox.setStyle("-fx-background-color: white; -fx-border-color: #ecf0f1; -fx-border-radius: 8; -fx-padding: 10; -fx-background-radius: 8;");
        HBox.setHgrow(searchInputBox, Priority.ALWAYS);

        Button searchButton = new Button("Search");
        searchButton.setGraphic(getIcon("https://img.icons8.com/ios-glyphs/48/ffffff/search.png", 16));
        searchButton.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        searchButton.setPadding(new Insets(10, 20, 10, 20));

        HBox searchBox = new HBox(15, searchInputBox, searchButton);
        searchBox.setAlignment(Pos.CENTER);

        // Custom Table
        VBox listContainer = new VBox();
        listContainer.setStyle("-fx-background-color: rgba(245, 250, 255, 0.95); -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        
        HBox tableHeader = new HBox(15);
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(15, 20, 15, 20));
        tableHeader.setStyle("-fx-background-color: #f0f8ff; -fx-background-radius: 12 12 0 0;");
        
        Text hId = new Text("ID"); hId.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); hId.setFill(javafx.scene.paint.Color.web("#1976d2"));
        Text hName = new Text("Medicine Name"); hName.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); hName.setFill(javafx.scene.paint.Color.web("#1976d2"));
        Text hDesc = new Text("Description"); hDesc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); hDesc.setFill(javafx.scene.paint.Color.web("#1976d2"));
        Text hPrice = new Text("Price (Rs.)"); hPrice.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); hPrice.setFill(javafx.scene.paint.Color.web("#1976d2"));
        Text hQty = new Text("Quantity"); hQty.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); hQty.setFill(javafx.scene.paint.Color.web("#1976d2"));
        
        HBox hIdBox = new HBox(hId); hIdBox.setPrefWidth(50); hIdBox.setAlignment(Pos.CENTER);
        HBox hNameBox = new HBox(hName); hNameBox.setPrefWidth(200); hNameBox.setAlignment(Pos.CENTER);
        HBox hDescBox = new HBox(hDesc); hDescBox.setPrefWidth(280); hDescBox.setAlignment(Pos.CENTER);
        HBox hPriceBox = new HBox(hPrice); hPriceBox.setPrefWidth(100); hPriceBox.setAlignment(Pos.CENTER);
        HBox hQtyBox = new HBox(hQty); hQtyBox.setPrefWidth(80); hQtyBox.setAlignment(Pos.CENTER);
        
        tableHeader.getChildren().addAll(hIdBox, hNameBox, hDescBox, hPriceBox, hQtyBox);
        
        VBox itemsBox = new VBox();
        renderInventoryItems(itemsBox, pharmacyInventory);
        
        listContainer.getChildren().addAll(tableHeader, itemsBox);
        
        // Action Buttons
        // Action Buttons
        Button backButton = new Button("Logout");
        backButton.setGraphic(getIcon("https://img.icons8.com/ios-filled/48/e74c3c/logout-rounded-left.png", 16));
        backButton.setStyle("-fx-background-color: white; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-border-color: #ecf0f1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        backButton.setPadding(new Insets(12, 30, 12, 30));

        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.setGraphic(getIcon("https://img.icons8.com/ios-glyphs/48/ffffff/shopping-cart--v1.png", 16));
        addToCartButton.setStyle("-fx-background-color: linear-gradient(to right, #1976d2, #20b2aa); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;");
        addToCartButton.setPadding(new Insets(12, 40, 12, 40));

        Button viewHistoryBtn = new Button("View Order History");
        viewHistoryBtn.setGraphic(getIcon("https://img.icons8.com/ios-filled/48/e74c3c/order-history.png", 16));
        viewHistoryBtn.setStyle("-fx-background-color: #fff3e0; -fx-text-fill: #e65100; -fx-font-weight: bold; -fx-border-color: #ffe0b2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        viewHistoryBtn.setPadding(new Insets(12, 30, 12, 30));

        Button nextButton = new Button("Next");
        nextButton.setGraphic(getIcon("https://img.icons8.com/color/48/forward.png", 16));
        nextButton.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2; -fx-font-weight: bold; -fx-border-color: #bbdefb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        nextButton.setPadding(new Insets(12, 30, 12, 30));
        
        javafx.scene.layout.Region spacer1 = new javafx.scene.layout.Region(); HBox.setHgrow(spacer1, Priority.ALWAYS);
        javafx.scene.layout.Region spacer2 = new javafx.scene.layout.Region(); HBox.setHgrow(spacer2, Priority.ALWAYS);
        
        HBox actionBox = new HBox(15, backButton, spacer1, addToCartButton, viewHistoryBtn, spacer2, nextButton);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(20, 0, 0, 0));

        VBox mainLayout = new VBox(20, headerBox, searchBox, listContainer, actionBox);
        mainLayout.setMaxWidth(900);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setPadding(new Insets(30, 40, 30, 40));
        mainLayout.setStyle("-fx-background-color: transparent;");

        javafx.scene.layout.StackPane centerWrapper = new javafx.scene.layout.StackPane(mainLayout);
        centerWrapper.setAlignment(Pos.TOP_CENTER);

        ScrollPane medicineScrollPane = new ScrollPane(centerWrapper);
        medicineScrollPane.setFitToWidth(true);
        medicineScrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        medicineScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");

        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(medicineScrollPane);
        root.setStyle("-fx-background-image: url('/background.jpg'); -fx-background-size: cover;");

        Scene medicineListScene = new Scene(root, 1000, 600);
        medicineListStage.setScene(medicineListScene);
        medicineListStage.show();

        // Handlers
        searchButton.setOnAction(e -> {
            String searchText = searchTextField.getText().trim().toLowerCase();
            if (!searchText.isEmpty()) {
                ObservableList<Medicine> searchResult = FXCollections.observableArrayList();
                for (Medicine medicine : pharmacyInventory) {
                    if (medicine.getName().toLowerCase().contains(searchText) || medicine.getDescription().toLowerCase().contains(searchText)) {
                        searchResult.add(medicine);
                    }
                }
                renderInventoryItems(itemsBox, searchResult);
            } else {
                renderInventoryItems(itemsBox, pharmacyInventory);
            }
        });

        addToCartButton.setOnAction(e -> {
            if (selectedInventoryMedicine != null) {
                boolean found = false;
                for (Medicine m : cart) {
                    if (m.getId() == selectedInventoryMedicine.getId()) {
                        m.setSelectedQuantity(m.getSelectedQuantity() + selectedInventoryMedicine.getSelectedQuantity());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Medicine cartItem = new Medicine(selectedInventoryMedicine.getName(), selectedInventoryMedicine.getId(), selectedInventoryMedicine.getDescription(), selectedInventoryMedicine.getPrice(), selectedInventoryMedicine.getQuantity());
                    cartItem.setSelectedQuantity(selectedInventoryMedicine.getSelectedQuantity());
                    cart.add(cartItem);
                }
                Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText(selectedInventoryMedicine.getName() + " added to cart!"); a.show();
            } else {
                Alert a = new Alert(Alert.AlertType.WARNING); a.setHeaderText(null); a.setContentText("Please select a medicine first."); a.show();
            }
        });

        backButton.setOnAction(e -> {
            medicineListStage.close();
            showLoginStage();
        });
        
        nextButton.setOnAction(e -> {
            medicineListStage.close();
            showCartAndOrderPlacementStage(user.getOrderHistoryObservable());
        });
        
        viewHistoryBtn.setOnAction(e -> {
            medicineListStage.close();
            showOrderHistoryStage(user.getOrderHistoryObservable(), () -> showMedicineListStage());
        });
    }

    // Returns a unique colored medicine icon URL for each medicine by ID
    private String getMedicineIconUrl(int id) {
        String[] icons = {
            "https://img.icons8.com/color/48/pill.png",                // 1 Panadol
            "https://img.icons8.com/color/48/pills.png",              // 2 Neurofen
            "https://img.icons8.com/color/48/doctors-bag.png",        // 3 Amoxilin
            "https://img.icons8.com/color/48/lungs.png",              // 4 Ventolin
            "https://img.icons8.com/color/48/sneeze.png",             // 5 Zyrtec
            "https://img.icons8.com/color/48/pill.png",               // 6 Aspirin
            "https://img.icons8.com/color/48/heart-health.png",       // 7 Lipitor
            "https://img.icons8.com/color/48/brain.png",              // 8 Prozac
            "https://img.icons8.com/color/48/stomach.png",            // 9 Nexium
            "https://img.icons8.com/color/48/clinic.png"              // 10 Synthroid
        };
        int idx = (id - 1) % icons.length;
        return icons[idx < 0 ? 0 : idx];
    }

    private javafx.scene.image.ImageView getMedicineIcon(int id, int size) {
        return getIcon(getMedicineIconUrl(id), size);
    }

    private void renderInventoryItems(VBox container, ObservableList<Medicine> items) {
        container.getChildren().clear();
        for (Medicine m : items) {
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(15, 20, 15, 20));
            row.setStyle("-fx-background-color: transparent; -fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");
            
            row.setOnMouseClicked(e -> {
                selectedInventoryMedicine = m;
                for (javafx.scene.Node n : container.getChildren()) {
                    n.setStyle("-fx-background-color: transparent; -fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");
                }
                row.setStyle("-fx-background-color: #bbdefb; -fx-cursor: hand;");
            });

            // ID Box
            Text idTxt = new Text(String.valueOf(m.getId()));
            idTxt.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
            idTxt.setFill(javafx.scene.paint.Color.web("#1976d2"));
            javafx.scene.layout.StackPane idPane = new javafx.scene.layout.StackPane(idTxt);
            idPane.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 5;");
            idPane.setPrefSize(30, 30);
            HBox idBox = new HBox(idPane); idBox.setPrefWidth(50); idBox.setAlignment(Pos.CENTER);

            // Unique colored icon per medicine
            javafx.scene.image.ImageView medIcon = getMedicineIcon(m.getId(), 24);

            Text nameTxt = new Text(m.getName());
            nameTxt.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));
            nameTxt.setFill(javafx.scene.paint.Color.web("#0e2e50"));
            HBox nameBox = new HBox(10, medIcon, nameTxt);
            nameBox.setPrefWidth(200); nameBox.setAlignment(Pos.CENTER);

            // Description
            Text descTxt = new Text(m.getDescription());
            descTxt.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
            descTxt.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            HBox descBox = new HBox(descTxt); descBox.setPrefWidth(280); descBox.setAlignment(Pos.CENTER);

            // Price
            Text priceTxt = new Text(String.format("%.1f", m.getPrice()));
            priceTxt.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 13));
            priceTxt.setFill(javafx.scene.paint.Color.web("#1976d2"));
            HBox priceBox = new HBox(priceTxt); priceBox.setPrefWidth(100); priceBox.setAlignment(Pos.CENTER);

            // Quantity Spinner with always-blue text
            Spinner<Integer> qtySpinner = new Spinner<>(1, 100, m.getSelectedQuantity());
            qtySpinner.setPrefWidth(70);
            // Style the spinner text field to always show blue text
            qtySpinner.getEditor().setStyle("-fx-text-fill: #1976d2; -fx-font-weight: bold; -fx-background-color: #e3f2fd; -fx-border-color: #bbdefb; -fx-border-radius: 4;");
            qtySpinner.valueProperty().addListener((obs, old, newV) -> {
                m.setSelectedQuantity(newV);
                qtySpinner.getEditor().setStyle("-fx-text-fill: #1976d2; -fx-font-weight: bold; -fx-background-color: #e3f2fd; -fx-border-color: #bbdefb; -fx-border-radius: 4;");
            });
            HBox qtyBox = new HBox(qtySpinner); qtyBox.setPrefWidth(80); qtyBox.setAlignment(Pos.CENTER);

            row.getChildren().addAll(idBox, nameBox, descBox, priceBox, qtyBox);
            container.getChildren().add(row);
        }
    }


    private void renderCartItems(VBox itemsBox, Text subValue, Text dValue, Text fValue) {
        itemsBox.getChildren().clear();
        
        HBox headerRow = new HBox(10);
        headerRow.setPadding(new Insets(15, 20, 15, 20));
        headerRow.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10 10 0 0; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");
        
        Text h1 = new Text("Item"); h1.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); h1.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        Text h2 = new Text("Medicine Details"); h2.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); h2.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        Text h3 = new Text("Price (Rs)"); h3.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); h3.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        Text h4 = new Text("Quantity"); h4.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); h4.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        Text h5 = new Text("Subtotal (Rs)"); h5.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); h5.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        
        h1.setWrappingWidth(50); h1.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        h2.setWrappingWidth(250); h2.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        h3.setWrappingWidth(120); h3.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        h4.setWrappingWidth(120); h4.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        h5.setWrappingWidth(120); h5.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        headerRow.getChildren().addAll(h1, h2, h3, h4, h5);
        itemsBox.getChildren().add(headerRow);
        
        for (int i = 0; i < cart.size(); i++) {
            Medicine m = cart.get(i);
            
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(15, 20, 15, 20));
            row.setStyle("-fx-background-color: white; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");
            
            javafx.scene.layout.StackPane idPane = new javafx.scene.layout.StackPane();
            javafx.scene.shape.Rectangle idBg = new javafx.scene.shape.Rectangle(30, 30, javafx.scene.paint.Color.web("#e3f2fd"));
            idBg.setArcWidth(10); idBg.setArcHeight(10);
            Text idText = new Text(String.valueOf(m.getId())); idText.setFill(javafx.scene.paint.Color.web("#1976d2")); idText.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
            idPane.getChildren().addAll(idBg, idText);
            
            VBox detBox = new VBox(2);
            Text mName = new Text(m.getName()); mName.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14)); mName.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            Text mDesc = new Text(m.getDescription()); mDesc.setFont(javafx.scene.text.Font.font("System", 11)); mDesc.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
            
            HBox nameIconBox = new HBox(10);
            nameIconBox.setAlignment(Pos.CENTER_LEFT);
            
            javafx.scene.image.ImageView iconImg = getMedicineIcon(m.getId(), 28);
            
            detBox.getChildren().addAll(mName, mDesc);
            nameIconBox.getChildren().addAll(iconImg, detBox);
            
            Text priceTxt = new Text("Rs. " + m.getPrice()); priceTxt.setFont(javafx.scene.text.Font.font("System", 13)); priceTxt.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            
            HBox qtyBox = new HBox(5);
            qtyBox.setAlignment(Pos.CENTER);
            
            Button minusBtn = new Button("−"); minusBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #1976d2; -fx-cursor: hand; -fx-font-weight: bold;");
            Text qtyTxt = new Text(String.valueOf(m.getSelectedQuantity())); qtyTxt.setFont(javafx.scene.text.Font.font("System", 14));
            Button plusBtn = new Button("+"); plusBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #1976d2; -fx-cursor: hand; -fx-font-weight: bold;");
            
            qtyBox.setStyle("-fx-border-color: #ecf0f1; -fx-border-radius: 5;");
            qtyBox.getChildren().addAll(minusBtn, qtyTxt, plusBtn);
            
            Text subTxt = new Text(String.format("Rs. %.1f", m.getPrice() * m.getSelectedQuantity()));
            subTxt.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));
            subTxt.setFill(javafx.scene.paint.Color.web("#1976d2"));
            
            Button delBtn = new Button();
            delBtn.setGraphic(getIcon("https://img.icons8.com/ios-glyphs/48/e74c3c/trash.png", 16));
            delBtn.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-border-color: #ffcdd2; -fx-border-radius: 5; -fx-background-radius: 5;");
            
            idPane.setPrefWidth(50);
            nameIconBox.setPrefWidth(250); nameIconBox.setAlignment(Pos.CENTER);
            
            HBox priceWrapper = new HBox(priceTxt); priceWrapper.setPrefWidth(120); priceWrapper.setAlignment(Pos.CENTER);
            qtyBox.setPrefWidth(120);
            HBox subWrapper = new HBox(subTxt); subWrapper.setPrefWidth(120); subWrapper.setAlignment(Pos.CENTER);
            
            row.getChildren().addAll(idPane, nameIconBox, priceWrapper, qtyBox, subWrapper, delBtn);
            itemsBox.getChildren().add(row);
            
            minusBtn.setOnAction(e -> {
                if (m.getSelectedQuantity() > 1) {
                    m.setSelectedQuantity(m.getSelectedQuantity() - 1);
                    renderCartItems(itemsBox, subValue, dValue, fValue);
                }
            });
            plusBtn.setOnAction(e -> {
                m.setSelectedQuantity(m.getSelectedQuantity() + 1);
                renderCartItems(itemsBox, subValue, dValue, fValue);
            });
            delBtn.setOnAction(e -> {
                cart.remove(m);
                renderCartItems(itemsBox, subValue, dValue, fValue);
            });
        }
        
        double total = calculateTotalCartPrice();
        subValue.setText("Rs. " + String.format("%.1f", total));
        dValue.setText("- Rs. " + String.format("%.1f", total * discountMultiplier));
        fValue.setText("Rs. " + String.format("%.1f", total - (total * discountMultiplier)));
    }

    private void showCartAndOrderPlacementStage(ObservableList<Order> orderHistoryObservable) {
        Stage cartStage = new Stage();
        cartStage.setTitle("Cart and Order Placement");

        // Header
        javafx.scene.shape.Rectangle iconBg = new javafx.scene.shape.Rectangle(40, 40, javafx.scene.paint.Color.web("#e3f2fd"));
        iconBg.setArcWidth(10);
        iconBg.setArcHeight(10);
        javafx.scene.image.ImageView cartIcon = new javafx.scene.image.ImageView(new javafx.scene.image.Image("https://img.icons8.com/color/48/000000/shopping-cart.png", true));
        cartIcon.setFitWidth(24);
        cartIcon.setFitHeight(24);
        javafx.scene.layout.StackPane topIconPane = new javafx.scene.layout.StackPane(iconBg, cartIcon);

        VBox titleBox = new VBox(2);
        Text title = new Text("Cart Summary");
        title.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 22));
        title.setFill(javafx.scene.paint.Color.web("#0e2e50"));
        Text subtitle = new Text("Review your items, apply discounts, and proceed to checkout.");
        subtitle.setFont(javafx.scene.text.Font.font("System", 13));
        subtitle.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        titleBox.getChildren().addAll(title, subtitle);

        HBox headerBox = new HBox(15, topIconPane, titleBox);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        User.setCart(cart);

        // Custom ListView / VBox for Items
        VBox itemsBox = new VBox();
        itemsBox.setStyle("-fx-background-color: rgba(235, 245, 255, 0.9); -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        
        // Total Price Section
        VBox totalBox = new VBox(15);
        totalBox.setStyle("-fx-background-color: rgba(235, 245, 255, 0.9); -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        totalBox.setPadding(new Insets(20));
        
        HBox voucherRow = new HBox(15);
        voucherRow.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.image.ImageView vIcon = getIcon("https://img.icons8.com/color/48/discount.png", 20);
        Text vLabel = new Text("Apply Discount"); vLabel.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));
        TextField voucherField = new TextField(); voucherField.setPromptText("Enter voucher code (e.g. DISCOUNT10)");
        voucherField.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-prompt-text-fill: #aab0b8; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
        HBox.setHgrow(voucherField, Priority.ALWAYS);
        Button applyVoucherBtn = new Button("Apply");
        applyVoucherBtn.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        applyVoucherBtn.setPadding(new Insets(8, 20, 8, 20));
        voucherRow.getChildren().addAll(vIcon, vLabel, voucherField, applyVoucherBtn);
        
        HBox subtotalRow = new HBox();
        javafx.scene.image.ImageView sIcon = getIcon("https://img.icons8.com/color/48/receipt.png", 20);
        Text subLabel = new Text("  Total Amount"); subLabel.setFont(Font.font("System", 14));
        javafx.scene.layout.Region sSpacer = new javafx.scene.layout.Region(); HBox.setHgrow(sSpacer, Priority.ALWAYS);
        Text subValue = new Text("Rs. 0.0"); subValue.setFont(Font.font("System", 14));
        subtotalRow.getChildren().addAll(sIcon, subLabel, sSpacer, subValue);
        
        HBox discountRow = new HBox();
        javafx.scene.image.ImageView dIcon = getIcon("https://img.icons8.com/color/48/discount.png", 20);
        Text dLabel = new Text("  Discount"); dLabel.setFont(Font.font("System", 14)); dLabel.setFill(javafx.scene.paint.Color.web("#27ae60"));
        javafx.scene.layout.Region dSpacer = new javafx.scene.layout.Region(); HBox.setHgrow(dSpacer, Priority.ALWAYS);
        Text dValue = new Text("- Rs. 0.0"); dValue.setFill(javafx.scene.paint.Color.web("#27ae60")); dValue.setFont(Font.font("System", 14));
        discountRow.getChildren().addAll(dIcon, dLabel, dSpacer, dValue);
        
        HBox finalRow = new HBox();
        javafx.scene.image.ImageView fIcon = getIcon("https://img.icons8.com/color/48/bank-cards.png", 24);
        Text fLabel = new Text("  Final Amount"); fLabel.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
        javafx.scene.layout.Region fSpacer = new javafx.scene.layout.Region(); HBox.setHgrow(fSpacer, Priority.ALWAYS);
        Text fValue = new Text("Rs. 0.0");
        fValue.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 18));
        fValue.setFill(javafx.scene.paint.Color.web("#e74c3c"));
        finalRow.getChildren().addAll(fIcon, fLabel, fSpacer, fValue);

        applyVoucherBtn.setOnAction(e -> {
            if(voucherField.getText().equalsIgnoreCase("DISCOUNT10")) {
                discountMultiplier = 0.10;
                renderCartItems(itemsBox, subValue, dValue, fValue);
                Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText("Voucher Applied!"); a.show();
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText("Invalid Voucher"); a.show();
            }
        });
        
        totalBox.getChildren().addAll(voucherRow, new javafx.scene.control.Separator(), subtotalRow, discountRow, new javafx.scene.control.Separator(), finalRow);

        renderCartItems(itemsBox, subValue, dValue, fValue);

        // Action Buttons
        Button placeOrderButton = new Button("Proceed to Checkout");
        placeOrderButton.setGraphic(getIcon("https://img.icons8.com/color/48/lock.png", 16));
        placeOrderButton.setStyle("-fx-background-color: linear-gradient(to right, #1976d2, #20b2aa); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;");
        placeOrderButton.setPadding(new Insets(15, 40, 15, 40));

        Button backButton = new Button("Back");
        backButton.setGraphic(getIcon("https://img.icons8.com/color/48/back.png", 16));
        backButton.setStyle("-fx-background-color: white; -fx-text-fill: #1976d2; -fx-font-weight: bold; -fx-border-color: #1976d2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        backButton.setPadding(new Insets(15, 30, 15, 30));
        
        javafx.scene.layout.Region spacerBtn = new javafx.scene.layout.Region(); HBox.setHgrow(spacerBtn, Priority.ALWAYS);
        HBox actionBox = new HBox(15, backButton, spacerBtn, placeOrderButton);
        actionBox.setAlignment(Pos.CENTER);

        Text secureText = new Text("🔒 Your payment details are secure and encrypted.");
        secureText.setFont(javafx.scene.text.Font.font("System", 12));
        secureText.setFill(javafx.scene.paint.Color.web("#27ae60"));
        VBox bottomBox = new VBox(15, actionBox, secureText);
        bottomBox.setAlignment(Pos.CENTER);

        VBox mainLayout = new VBox(25, headerBox, itemsBox, totalBox, bottomBox);
        mainLayout.setMaxWidth(900);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setPadding(new Insets(30, 40, 30, 40));
        mainLayout.setStyle("-fx-background-color: transparent;");

        javafx.scene.layout.StackPane centerWrapper = new javafx.scene.layout.StackPane(mainLayout);
        centerWrapper.setAlignment(Pos.TOP_CENTER);

        ScrollPane cartScrollPane = new ScrollPane(centerWrapper);
        cartScrollPane.setFitToWidth(true);
        cartScrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        cartScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");

        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(cartScrollPane);
        root.setStyle("-fx-background-image: url('/background.jpg'); -fx-background-size: cover;");

        Scene cartScene = new Scene(root, 1000, 600);

        cartStage.setScene(cartScene);
        cartStage.show();

        // Handlers
        placeOrderButton.setOnAction(e -> {
            cartStage.close();
            showCheckoutStage(orderHistoryObservable, cartStage);
        });

        backButton.setOnAction(e -> {
            cartStage.close();
            showMedicineListStage();
        });
    }

    private void showOrderHistoryStage(ObservableList<Order> orderHistoryObservable, Runnable backAction) {
        Stage orderHistoryStage = new Stage();
        orderHistoryStage.setTitle("Order History");

        // Header
        javafx.scene.shape.Rectangle iconBg = new javafx.scene.shape.Rectangle(40, 40, javafx.scene.paint.Color.web("#e3f2fd"));
        iconBg.setArcWidth(10);
        iconBg.setArcHeight(10);
        javafx.scene.image.ImageView histIcon = new javafx.scene.image.ImageView(new javafx.scene.image.Image("https://img.icons8.com/color/48/000000/order-history.png", true));
        histIcon.setFitWidth(24);
        histIcon.setFitHeight(24);
        javafx.scene.layout.StackPane topIconPane = new javafx.scene.layout.StackPane(iconBg, histIcon);

        VBox titleBox = new VBox();
        Text title = new Text("Order History");
        title.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 22));
        title.setFill(javafx.scene.paint.Color.web("#0e2e50"));
        Text subTitle = new Text("Track your past orders");
        subTitle.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        titleBox.getChildren().addAll(title, subTitle);

        HBox headerBox = new HBox(15, topIconPane, titleBox);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Filters
        TextField searchField = new TextField();
        searchField.setPromptText("Search with name or ID/Date");
        searchField.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-prompt-text-fill: #aab0b8; -fx-border-color: #ecf0f1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        ComboBox<String> dateFilter = new ComboBox<>();
        dateFilter.setPromptText("Select Date Range");
        dateFilter.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-border-color: #ecf0f1; -fx-border-radius: 5; -fx-background-radius: 5;");
        dateFilter.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(dateFilter, Priority.ALWAYS);

        HBox filterBox = new HBox(10, searchField, dateFilter);

        // Table (Custom List)
        VBox tableCard = new VBox();
        tableCard.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #ecf0f1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        HBox headerRow = new HBox(
            createHeaderCell("Order ID", 150),
            createHeaderCell("Date & Time", 220),
            createHeaderCell("Total Price", 120),
            createHeaderCell("Items", 150),
            createHeaderCell("Status", 150)
        );
        headerRow.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10 10 0 0; -fx-padding: 15 20 15 20; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");
        headerRow.setAlignment(Pos.CENTER);
        tableCard.getChildren().add(headerRow);
        dateFilter.getItems().addAll("All Time", "Last 2 Days", "Last 7 Days", "Last 30 Days", "Previous Month", "2024", "2025");
        dateFilter.getSelectionModel().selectFirst();
        
        VBox rowsContainer = new VBox();
        tableCard.getChildren().add(rowsContainer);
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a");

        Runnable updateTable = () -> {
            rowsContainer.getChildren().clear();
            String query = searchField.getText().toLowerCase().trim();
            String range = dateFilter.getValue();
            long now = System.currentTimeMillis();
            
            for (int i = 0; i < orderHistoryObservable.size(); i++) {
                Order o = orderHistoryObservable.get(i);
                
                long orderTime = o.getOrderDate().getTime();
                if (range != null && !range.equals("All Time")) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTimeInMillis(orderTime);
                    int year = cal.get(java.util.Calendar.YEAR);
                    int month = cal.get(java.util.Calendar.MONTH);
                    
                    java.util.Calendar currentCal = java.util.Calendar.getInstance();
                    currentCal.setTimeInMillis(now);
                    int currentYear = currentCal.get(java.util.Calendar.YEAR);
                    int currentMonth = currentCal.get(java.util.Calendar.MONTH);
                    int prevMonth = currentMonth == 0 ? 11 : currentMonth - 1;
                    int prevMonthYear = currentMonth == 0 ? currentYear - 1 : currentYear;

                    if (range.equals("Last 2 Days") && (now - orderTime) > 2L * 24 * 60 * 60 * 1000) continue;
                    if (range.equals("Last 7 Days") && (now - orderTime) > 7L * 24 * 60 * 60 * 1000) continue;
                    if (range.equals("Last 30 Days") && (now - orderTime) > 30L * 24 * 60 * 60 * 1000) continue;
                    if (range.equals("2024") && year != 2024) continue;
                    if (range.equals("2025") && year != 2025) continue;
                    if (range.equals("Previous Month") && (year != prevMonthYear || month != prevMonth)) continue;
                }
                
                boolean matches = query.isEmpty() || ("ord00" + o.getOrderId()).contains(query);
                if (!matches) {
                    for (Medicine m : o.getMedicines()) {
                        if (m.getName().toLowerCase().contains(query)) {
                            matches = true;
                            break;
                        }
                    }
                }
                if (!matches) continue;
                
                HBox row = new HBox();
                row.setAlignment(Pos.CENTER);
                row.setPadding(new Insets(15, 20, 15, 20));
                row.setStyle("-fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");
                
                HBox idBox = new HBox(10); idBox.setAlignment(Pos.CENTER_LEFT); idBox.setPrefWidth(150);
                String status = o.getStatus();
                String iconUrl = "https://img.icons8.com/color/48/shopping-bag.png";
                String badgeBg = "#fff3e0"; String badgeTextCol = "#e67e22";
                if (status.equals("Delivered")) {
                    iconUrl = "https://img.icons8.com/color/48/ok.png";
                    badgeBg = "#e8f8f5"; badgeTextCol = "#27ae60";
                } else if (status.equals("Cancelled")) {
                    iconUrl = "https://img.icons8.com/color/48/cancel.png";
                    badgeBg = "#fdedec"; badgeTextCol = "#e74c3c";
                } else if (status.equals("Placed")) {
                    iconUrl = "https://img.icons8.com/color/48/checked-checkbox.png";
                    badgeBg = "#e8f4fd"; badgeTextCol = "#2980b9";
                } else if (status.equals("Pickup")) {
                    iconUrl = "https://img.icons8.com/color/48/shop.png";
                    badgeBg = "#e8f4fd"; badgeTextCol = "#2980b9";
                } else if (status.equals("Shipped")) {
                    iconUrl = "https://img.icons8.com/color/48/in-transit--v1.png";
                    badgeBg = "#fef9e7"; badgeTextCol = "#f39c12";
                }
                
                javafx.scene.shape.Circle iconBgCircle = new javafx.scene.shape.Circle(15, javafx.scene.paint.Color.web(badgeBg));
                javafx.scene.image.ImageView stIcon = getIcon(iconUrl, 16);
                javafx.scene.layout.StackPane iconStack = new javafx.scene.layout.StackPane(iconBgCircle, stIcon);
                Text idText = new Text("ORD00" + o.getOrderId()); idText.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 13)); idText.setFill(javafx.scene.paint.Color.web("#2c3e50"));
                
                Button delBtn = new Button();
                delBtn.setGraphic(getIcon("https://img.icons8.com/ios-glyphs/48/e74c3c/trash.png", 14));
                delBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
                delBtn.setOnAction(e -> {
                    orderHistoryObservable.remove(o);
                    deleteOrderFromFile(o);
                    rowsContainer.getChildren().remove(row);
                });
                
                idBox.getChildren().addAll(iconStack, idText);
                
                HBox dateBox = new HBox(5); dateBox.setAlignment(Pos.CENTER); dateBox.setPrefWidth(220);
                javafx.scene.image.ImageView calIcon = getIcon("https://img.icons8.com/color/48/calendar.png", 14);
                Text dateText = new Text(sdf.format(o.getOrderDate())); dateText.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
                dateBox.getChildren().addAll(calIcon, dateText);
                
                VBox priceBox = new VBox(); priceBox.setAlignment(Pos.CENTER); priceBox.setPrefWidth(120);
                Text priceText = new Text(String.format("Rs. %.1f", o.getTotalPrice()));
                priceText.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 13));
                priceText.setFill(javafx.scene.paint.Color.web("#1976d2"));
                priceBox.getChildren().add(priceText);
                
                VBox itemBox = new VBox(); itemBox.setAlignment(Pos.CENTER); itemBox.setPrefWidth(150);
                ComboBox<String> detailsCombo = new ComboBox<>();
                detailsCombo.setPromptText("View Details");
                detailsCombo.setStyle("-fx-background-color: white; -fx-border-color: #ecf0f1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-text-fill: #2c3e50;");
                for (Medicine m : o.getMedicines()) {
                    detailsCombo.getItems().add(m.getSelectedQuantity() + "x " + m.getName());
                }
                itemBox.getChildren().add(detailsCombo);
                
                VBox statusBox = new VBox(5); statusBox.setAlignment(Pos.CENTER); statusBox.setPrefWidth(150);
                Text stText = new Text(status); stText.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); stText.setFill(javafx.scene.paint.Color.web(badgeTextCol));
                HBox badge = new HBox(5, getIcon(iconUrl, 12), stText);
                badge.setAlignment(Pos.CENTER);
                badge.setMaxWidth(110);
                badge.setPadding(new Insets(5, 10, 5, 10));
                badge.setStyle("-fx-background-color: " + badgeBg + "; -fx-background-radius: 15;");
                statusBox.getChildren().add(badge);
                
                if ((status.equals("Placed") || status.equals("Pickup") || status.equals("Shipped")) && (now - orderTime) <= 2L * 24 * 60 * 60 * 1000) {
                    Button cancelBtn = new Button("Cancel Order");
                    cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-border-color: #e74c3c; -fx-border-radius: 5; -fx-cursor: hand; -fx-font-size: 10px; -fx-padding: 2 5 2 5;");
                    cancelBtn.setOnAction(e -> {
                        o.setStatus("Cancelled");
                        updateOrderInFile(o);
                        orderHistoryStage.close();
                        showOrderHistoryStage(orderHistoryObservable, backAction);
                    });
                    statusBox.getChildren().add(cancelBtn);
                }
                
                row.getChildren().addAll(idBox, dateBox, priceBox, itemBox, statusBox, delBtn);
                rowsContainer.getChildren().add(row);
            }
        };

        searchField.textProperty().addListener((obs, oldV, newV) -> updateTable.run());
        dateFilter.valueProperty().addListener((obs, oldV, newV) -> updateTable.run());
        
        updateTable.run();

        Button backBtn = new Button("Back");
        backBtn.setGraphic(getIcon("https://img.icons8.com/color/48/back.png", 16));
        backBtn.setStyle("-fx-background-color: white; -fx-text-fill: #1976d2; -fx-font-weight: bold; -fx-border-color: #1976d2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        backBtn.setPadding(new Insets(10, 20, 10, 20));
        backBtn.setOnAction(e -> {
            orderHistoryStage.close();
            if (backAction != null) backAction.run();
        });

        Button logoutBtn = new Button("Logout");
        logoutBtn.setGraphic(getIcon("https://img.icons8.com/ios-filled/48/e74c3c/logout-rounded-left.png", 16));
        logoutBtn.setStyle("-fx-background-color: #ffeef0; -fx-text-fill: #ff4757; -fx-font-weight: bold; -fx-border-color: #ffcdd2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        logoutBtn.setPadding(new Insets(10, 20, 10, 20));
        logoutBtn.setOnAction(e -> {
            orderHistoryStage.close();
            showSplashScreen(new Stage());
        });

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actionBox = new HBox(10, backBtn, spacer, logoutBtn);
        actionBox.setAlignment(Pos.CENTER);

        VBox mainLayout = new VBox(20, headerBox, filterBox, tableCard, actionBox);
        mainLayout.setMaxWidth(900);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setPadding(new Insets(30, 40, 30, 40));
        mainLayout.setStyle("-fx-background-color: transparent;");

        ScrollPane historyScrollPane = new ScrollPane(mainLayout);
        historyScrollPane.setFitToWidth(true);
        historyScrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        historyScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");

        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(historyScrollPane);
        root.setStyle("-fx-background-image: url('/background.jpg'); -fx-background-size: cover;");

        Scene orderHistoryScene = new Scene(root, 1000, 600);

        orderHistoryStage.setScene(orderHistoryScene);
        orderHistoryStage.show();
    }

    private void showCheckoutStage(ObservableList<Order> orderHistoryObservable, Stage cartStage) {
        Stage checkoutStage = new Stage();
        checkoutStage.setTitle("Place Order");
        deliveryDetailsSaved = false;
        savedDeliveryInfo = "";

        // Header
        VBox titleBox = new VBox(5);
        Text title = new Text("Place Your Order");
        title.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 26));
        title.setFill(javafx.scene.paint.Color.web("#0e2e50"));
        Text subtitle = new Text("Review your medicines, choose delivery option and confirm your order.");
        subtitle.setFont(javafx.scene.text.Font.font("System", 14));
        subtitle.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        titleBox.getChildren().addAll(title, subtitle);
        
        HBox headerBox = new HBox(15, titleBox);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));

        // Section 1: Order Summary
        VBox sec5 = new VBox(0);
        sec5.setStyle("-fx-background-color: rgba(235, 245, 255, 0.9); -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        
        HBox sumHeader = new HBox(10);
        sumHeader.setPadding(new Insets(20));
        sumHeader.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.image.ImageView sIcon = getIcon("https://img.icons8.com/color/48/clipboard.png", 24);
        Text sec5Title = new Text("Order Summary");
        sec5Title.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
        sec5Title.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        sumHeader.getChildren().addAll(sIcon, sec5Title);
        
        // Custom Table Header
        HBox tHead = new HBox(10);
        tHead.setPadding(new Insets(10, 20, 10, 20));
        tHead.setStyle("-fx-background-color: #f8f9fa;");
        HBox th1Box = new HBox(); th1Box.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(th1Box, Priority.ALWAYS);
        Text th1 = new Text("🔗 Medicine Name"); th1.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); th1.setFill(javafx.scene.paint.Color.web("#34495e"));
        th1Box.getChildren().add(th1);
        HBox th2Box = new HBox(); th2Box.setAlignment(Pos.CENTER_RIGHT); th2Box.setPrefWidth(100);
        Text th2 = new Text("🔒 Quantity"); th2.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); th2.setFill(javafx.scene.paint.Color.web("#34495e"));
        th2Box.getChildren().add(th2);
        HBox th3Box = new HBox(); th3Box.setAlignment(Pos.CENTER_RIGHT); th3Box.setPrefWidth(150);
        Text th3 = new Text("💵 Price (Rs.)"); th3.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12)); th3.setFill(javafx.scene.paint.Color.web("#34495e"));
        th3Box.getChildren().add(th3);
        tHead.getChildren().addAll(th1Box, th2Box, th3Box);
        
        VBox rowsBox = new VBox();
        for (Medicine m : cart) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(15, 20, 15, 20));
            row.setStyle("-fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");
            
            HBox nameCol = new HBox(10); nameCol.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(nameCol, Priority.ALWAYS);
            javafx.scene.image.ImageView pIcon = getMedicineIcon(m.getId(), 18);
            Text mName = new Text(m.getName()); mName.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14)); mName.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            nameCol.getChildren().addAll(pIcon, mName);
            
            HBox qtyCol = new HBox(); qtyCol.setAlignment(Pos.CENTER_RIGHT); qtyCol.setPrefWidth(100);
            Text qtyVal = new Text(String.valueOf(m.getSelectedQuantity()));
            qtyVal.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));
            qtyVal.setFill(javafx.scene.paint.Color.web("#1976d2"));
            javafx.scene.layout.StackPane qtyBg = new javafx.scene.layout.StackPane(qtyVal);
            qtyBg.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 8; -fx-padding: 5 15 5 15;");
            qtyCol.getChildren().add(qtyBg);
            
            HBox priceCol = new HBox(); priceCol.setAlignment(Pos.CENTER_RIGHT); priceCol.setPrefWidth(150);
            Text pVal = new Text(String.format("Rs. %.1f", m.getPrice() * m.getSelectedQuantity()));
            pVal.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));
            pVal.setFill(javafx.scene.paint.Color.web("#1976d2"));
            priceCol.getChildren().add(pVal);
            
            row.getChildren().addAll(nameCol, qtyCol, priceCol);
            rowsBox.getChildren().add(row);
        }

        HBox totalBox = new HBox();
        totalBox.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 0 0 12 12;");
        totalBox.setPadding(new Insets(20));
        totalBox.setAlignment(Pos.CENTER_LEFT);
        Text totalLabel = new Text("Total Amount");
        totalLabel.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
        totalLabel.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        
        double initialTotal = calculateTotalCartPrice();
        double finalTotal = initialTotal - (initialTotal * discountMultiplier);
        Text totalValue = new Text("Rs. " + String.format("%.1f", finalTotal));
        totalValue.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 20));
        totalValue.setFill(javafx.scene.paint.Color.web("#27ae60"));
        
        javafx.scene.image.ImageView walletIcon = getIcon("https://img.icons8.com/color/48/bank-cards.png", 28);
        HBox valBox = new HBox(10, totalValue, walletIcon);
        valBox.setAlignment(Pos.CENTER);
        
        totalBox.getChildren().addAll(totalLabel, spacer, valBox);

        sec5.getChildren().addAll(sumHeader, tHead, rowsBox, totalBox);

        // Section 2: Pickup or Delivery
        VBox sec1 = new VBox(15);
        sec1.setStyle("-fx-background-color: rgba(235, 245, 255, 0.9); -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        sec1.setPadding(new Insets(20));
        
        HBox poHeader = new HBox(15);
        poHeader.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.image.ImageView pinIcon = getIcon("https://img.icons8.com/color/48/marker.png", 24);
        VBox poTitleBox = new VBox(2);
        Text sec1Title = new Text("Pickup or Delivery");
        sec1Title.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
        sec1Title.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        Text sec1Sub = new Text("Choose how you want to receive your order.");
        sec1Sub.setFont(javafx.scene.text.Font.font("System", 13));
        sec1Sub.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        poTitleBox.getChildren().addAll(sec1Title, sec1Sub);
        poHeader.getChildren().addAll(pinIcon, poTitleBox);
        
        ToggleGroup deliveryGroup = new ToggleGroup();
        
        // Custom Radio Cards
        HBox pickupCard = new HBox(15);
        pickupCard.setAlignment(Pos.CENTER_LEFT);
        pickupCard.setPadding(new Insets(15));
        pickupCard.setStyle("-fx-border-color: #ecf0f1; -fx-border-radius: 10; -fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand;");
        pickupCard.setPrefWidth(400);
        javafx.scene.control.RadioButton rb1 = new javafx.scene.control.RadioButton(); rb1.setToggleGroup(deliveryGroup); rb1.setUserData("Pickup");
        javafx.scene.image.ImageView pcIcon = getIcon("https://img.icons8.com/color/48/shopping-bag.png", 24);
        VBox pcText = new VBox(3);
        Text pcTitle = new Text("Pickup"); pcTitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 15));
        Text pcSub = new Text("You will pick up your order\nfrom our store"); pcSub.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        pcText.getChildren().addAll(pcTitle, pcSub);
        pickupCard.getChildren().addAll(rb1, pcIcon, pcText);
        
        HBox deliveryCard = new HBox(15);
        deliveryCard.setAlignment(Pos.CENTER_LEFT);
        deliveryCard.setPadding(new Insets(15));
        deliveryCard.setStyle("-fx-border-color: #ecf0f1; -fx-border-radius: 10; -fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand;");
        deliveryCard.setPrefWidth(400);
        javafx.scene.control.RadioButton rb2 = new javafx.scene.control.RadioButton(); rb2.setToggleGroup(deliveryGroup); rb2.setUserData("Home Delivery");
        javafx.scene.image.ImageView dcIcon = getIcon("https://img.icons8.com/color/48/motorcycle.png", 24);
        VBox dcText = new VBox(3);
        Text dcTitle = new Text("Home Delivery"); dcTitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 15));
        Text dcSub = new Text("We will deliver your order\nto your given address"); dcSub.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        dcText.getChildren().addAll(dcTitle, dcSub);
        deliveryCard.getChildren().addAll(rb2, dcIcon, dcText);
        
        // Listeners to style cards based on selection
        deliveryGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == rb1) {
                pickupCard.setStyle("-fx-border-color: #1976d2; -fx-border-radius: 10; -fx-background-color: #f0f8ff; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-width: 2;");
                pcTitle.setFill(javafx.scene.paint.Color.web("#1976d2"));
                deliveryCard.setStyle("-fx-border-color: #ecf0f1; -fx-border-radius: 10; -fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand;");
                dcTitle.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            } else if (newT == rb2) {
                deliveryCard.setStyle("-fx-border-color: #1976d2; -fx-border-radius: 10; -fx-background-color: #f0f8ff; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-width: 2;");
                dcTitle.setFill(javafx.scene.paint.Color.web("#1976d2"));
                pickupCard.setStyle("-fx-border-color: #ecf0f1; -fx-border-radius: 10; -fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand;");
                pcTitle.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            }
        });
        
        pickupCard.setOnMouseClicked(e -> rb1.setSelected(true));
        deliveryCard.setOnMouseClicked(e -> rb2.setSelected(true));

        HBox typeBox = new HBox(20, pickupCard, deliveryCard);
        
        sec1.getChildren().addAll(poHeader, typeBox);

        // Delivery Info Action
        Button enterDeliveryBtn = new Button("Enter Delivery & Payment Details");
        enterDeliveryBtn.setGraphic(getIcon("https://img.icons8.com/ios-glyphs/48/ffffff/truck.png", 16));
        enterDeliveryBtn.setMaxWidth(Double.MAX_VALUE);
        enterDeliveryBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;");
        enterDeliveryBtn.setPadding(new Insets(15));
        
        enterDeliveryBtn.visibleProperty().bind(deliveryGroup.selectedToggleProperty().isNotNull().and(
            javafx.beans.binding.Bindings.createBooleanBinding(() -> 
                deliveryGroup.getSelectedToggle() != null && deliveryGroup.getSelectedToggle().getUserData().equals("Home Delivery"), 
                deliveryGroup.selectedToggleProperty())
        ));
        enterDeliveryBtn.managedProperty().bind(enterDeliveryBtn.visibleProperty());
        
        enterDeliveryBtn.setOnAction(e -> {
            checkoutStage.hide();
            showDeliveryDetailsStage(checkoutStage);
        });

        // Action Buttons
        Button backBtn = new Button("Back");
        backBtn.setGraphic(getIcon("https://img.icons8.com/color/48/back.png", 16));
        backBtn.setStyle("-fx-background-color: white; -fx-text-fill: #1976d2; -fx-font-weight: bold; -fx-border-color: #bdc3c7; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        backBtn.setPrefWidth(150);
        backBtn.setPadding(new Insets(12, 0, 12, 0));
        
        Button viewHistoryBtn = new Button("View Order History");
        viewHistoryBtn.setGraphic(getIcon("https://img.icons8.com/color/48/order-history.png", 16));
        viewHistoryBtn.setStyle("-fx-background-color: #fff3e0; -fx-text-fill: #e65100; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        viewHistoryBtn.setPrefWidth(200);
        viewHistoryBtn.setPadding(new Insets(12, 0, 12, 0));

        Button confirmBtn = new Button("📋 Confirm Order");
        confirmBtn.setStyle("-fx-background-color: linear-gradient(to right, #1976d2, #20b2aa); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;");
        confirmBtn.setPrefWidth(200);
        confirmBtn.setPadding(new Insets(12, 0, 12, 0));
        
        javafx.scene.layout.Region spacerA = new javafx.scene.layout.Region(); HBox.setHgrow(spacerA, Priority.ALWAYS);
        javafx.scene.layout.Region spacerB = new javafx.scene.layout.Region(); HBox.setHgrow(spacerB, Priority.ALWAYS);

        HBox actionBox = new HBox(15, backBtn, spacerA, viewHistoryBtn, spacerB, confirmBtn);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(20, 0, 0, 0));

        VBox mainLayout = new VBox(20, headerBox, sec5, sec1, enterDeliveryBtn, actionBox);
        mainLayout.setMaxWidth(900);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setPadding(new Insets(30, 40, 40, 40));
        mainLayout.setStyle("-fx-background-color: transparent;");

        javafx.scene.layout.StackPane centerWrapper = new javafx.scene.layout.StackPane(mainLayout);
        centerWrapper.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(centerWrapper);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");

        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(scroll);
        root.setStyle("-fx-background-image: url('/background.jpg'); -fx-background-size: cover;");

        Scene scene = new Scene(root, 1000, 650);
        checkoutStage.setScene(scene);
        
        rb1.setSelected(true); // Default to Pickup

        confirmBtn.setOnAction(e -> {
            if (cart.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText("Cart is empty!"); a.show();
                return;
            }
            boolean isDelivery = deliveryGroup.getSelectedToggle() != null && deliveryGroup.getSelectedToggle().getUserData().equals("Home Delivery");
            
            if (isDelivery && !deliveryDetailsSaved) {
                Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText("Please enter Delivery Details first."); a.show();
                return;
            }
            
            user.placeOrder();
            Order lastOrder = null;
            if (!user.getOrderHistoryObservable().isEmpty()) {
                lastOrder = user.getOrderHistoryObservable().get(user.getOrderHistoryObservable().size() - 1);
                if (!isDelivery) lastOrder.setStatus("Pickup");
                saveOrder(lastOrder);
            }
            checkoutStage.close();
            showSuccessAndDeliveryDetails(checkoutStage, lastOrder, isDelivery, savedDeliveryInfo);
        });
        
        viewHistoryBtn.setOnAction(e -> {
            checkoutStage.close();
            showOrderHistoryStage(orderHistoryObservable, () -> showCheckoutStage(orderHistoryObservable, cartStage));
        });
        
        backBtn.setOnAction(e -> {
            checkoutStage.close();
            cartStage.show();
        });

        checkoutStage.show();
    }


    private VBox createOptionCard(ToggleGroup group, String title, String subtitle, String iconUrl, String colorHex) {
        RadioButton radio = new RadioButton();
        radio.setToggleGroup(group);
        radio.setUserData(title);
        
        javafx.scene.image.ImageView icon = new javafx.scene.image.ImageView(new javafx.scene.image.Image(iconUrl, true));
        icon.setFitWidth(24); icon.setFitHeight(24);
        
        HBox topRow = new HBox(10, radio, icon);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox textBox = new VBox(2);
        Text titleText = new Text(title);
        titleText.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
        titleText.setFill(javafx.scene.paint.Color.web(colorHex));
        Text subText = new Text(subtitle);
        subText.setFont(javafx.scene.text.Font.font("System", 10));
        subText.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        subText.setWrappingWidth(120);
        textBox.getChildren().addAll(titleText, subText);
        
        VBox card = new VBox(8, topRow, textBox);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5;");
        card.setPrefWidth(155);
        card.setMaxWidth(155);
        
        card.setOnMouseClicked(e -> radio.setSelected(true));
        
        radio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                card.setStyle("-fx-background-color: #f0f8ff; -fx-border-color: #1976d2; -fx-border-radius: 5; -fx-background-radius: 5;");
            } else {
                card.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5;");
            }
        });
        
        return card;
    }

    private void showDeliveryDetailsStage(Stage parentStage) {
        Stage stage = new Stage();
        stage.setTitle("Delivery Details");
        
        // 1. Top Header removed, moving Back to bottom
        
        VBox titleBox = new VBox(5);
        titleBox.setAlignment(Pos.CENTER);
        
        javafx.scene.image.ImageView pinIcon = getIcon("https://img.icons8.com/color/48/marker.png", 24);
        javafx.scene.shape.Circle iconCircle = new javafx.scene.shape.Circle(25, javafx.scene.paint.Color.web("#e3f2fd"));
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(iconCircle, pinIcon);
        
        Text title = new Text("Delivery Details");
        title.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 26));
        title.setFill(javafx.scene.paint.Color.web("#0e2e50"));
        Text subtitle = new Text("Provide your address and delivery preferences");
        subtitle.setFont(javafx.scene.text.Font.font("System", 14));
        subtitle.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        titleBox.getChildren().addAll(iconPane, title, subtitle);
        titleBox.setPadding(new Insets(0, 0, 10, 0));

        // 2. Card 1: Address Information
        VBox addressCard = new VBox(15);
        addressCard.setStyle("-fx-background-color: rgba(235, 245, 255, 0.9); -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        addressCard.setPadding(new Insets(25));
        
        HBox aHeader = new HBox(15);
        aHeader.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.image.ImageView aIcon = getIcon("https://img.icons8.com/color/48/home.png", 24);
        VBox aTitleBox = new VBox(2);
        Text aTitle = new Text("Address Information"); aTitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16)); aTitle.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        Text aSub = new Text("Where should we deliver your order?"); aSub.setFont(javafx.scene.text.Font.font("System", 13)); aSub.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        aTitleBox.getChildren().addAll(aTitle, aSub);
        aHeader.getChildren().addAll(aIcon, aTitleBox);
        
        TextField nameField = createStyledTextField("Full Name", user.getName());
        TextField phoneField = createStyledTextField("Phone Number", user.getContactInfo());
        HBox row1 = new HBox(20, createLabeledField("Full Name", nameField), createLabeledField("Phone", phoneField));
        
        TextField addressField = createStyledTextField("Street Address", user.getAddress());
        VBox row2 = createLabeledField("Street Address", addressField);
        
        TextField cityField = createStyledTextField("City", "Lahore");
        TextField areaField = createStyledTextField("Area / Locality", "");
        HBox row3 = new HBox(20, createLabeledField("City", cityField), createLabeledField("Area", areaField));
        
        HBox.setHgrow(nameField.getParent(), Priority.ALWAYS); HBox.setHgrow(phoneField.getParent(), Priority.ALWAYS);
        HBox.setHgrow(cityField.getParent(), Priority.ALWAYS); HBox.setHgrow(areaField.getParent(), Priority.ALWAYS);
        
        CheckBox saveAddressCb = new CheckBox("Save Address");
        saveAddressCb.setStyle("-fx-text-fill: #1976d2; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand;");
        saveAddressCb.setSelected(true);
        HBox cbBox = new HBox(saveAddressCb); cbBox.setAlignment(Pos.CENTER); cbBox.setPadding(new Insets(10, 0, 0, 0));
        
        VBox formBox = new VBox(15, row1, row2, row3, cbBox);
        formBox.setVisible(user.getSavedAddresses().isEmpty());
        formBox.setManaged(user.getSavedAddresses().isEmpty());

        ComboBox<String> savedCombo = new ComboBox<>();
        VBox savedBox = new VBox(10);
        if (!user.getSavedAddresses().isEmpty()) {
            for (String addr : user.getSavedAddresses()) {
                String[] parts = addr.split(";;");
                if (parts.length == 5) savedCombo.getItems().add(parts[0] + " - " + parts[2] + ", " + parts[4]);
            }
            savedCombo.getSelectionModel().selectFirst();
            savedCombo.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
            savedCombo.setMaxWidth(Double.MAX_VALUE);
            
            Button editBtn = new Button("Edit / New Address");
            editBtn.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2; -fx-cursor: hand;");
            editBtn.setOnAction(e -> {
                boolean isVisible = !formBox.isVisible();
                formBox.setVisible(isVisible);
                formBox.setManaged(isVisible);
            });
            
            savedBox.getChildren().addAll(new Text("Select Saved Address:"), savedCombo, editBtn);
        }
        
        addressCard.getChildren().addAll(aHeader, new javafx.scene.control.Separator());
        if (!user.getSavedAddresses().isEmpty()) {
            addressCard.getChildren().add(savedBox);
        }
        addressCard.getChildren().add(formBox);

        // 3. Card 2: Delivery Date & Time
        VBox timeCard = new VBox(15);
        timeCard.setStyle("-fx-background-color: rgba(235, 245, 255, 0.9); -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        timeCard.setPadding(new Insets(25));
        
        HBox tHeader = new HBox(15);
        tHeader.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.image.ImageView tIcon = getIcon("https://img.icons8.com/color/48/calendar.png", 24);
        VBox tTitleBox = new VBox(2);
        Text tTitle = new Text("Delivery Date & Time"); tTitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16)); tTitle.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        Text tSub = new Text("Choose your preferred delivery slot"); tSub.setFont(javafx.scene.text.Font.font("System", 13)); tSub.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        tTitleBox.getChildren().addAll(tTitle, tSub);
        tHeader.getChildren().addAll(tIcon, tTitleBox);
        
        DatePicker datePicker = new DatePicker();
        datePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                java.time.LocalDate today = java.time.LocalDate.now();
                setDisable(empty || date.compareTo(today) < 0);
            }
        });
        // Explicit editor styling to prevent black background / white text issues
        datePicker.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5;");
        datePicker.getEditor().setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-prompt-text-fill: #aab0b8;");
        datePicker.setMaxWidth(Double.MAX_VALUE);
        
        ComboBox<String> timePicker = new ComboBox<>();
        String[] allTimes = {"10:00 AM", "12:00 PM", "02:00 PM", "04:30 PM", "06:00 PM"};
        timePicker.getItems().addAll(allTimes);
        
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            timePicker.getItems().clear();
            if (newDate != null && newDate.equals(java.time.LocalDate.now())) {
                java.time.LocalTime now = java.time.LocalTime.now();
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH);
                for (String t : allTimes) {
                    try {
                        if (java.time.LocalTime.parse(t, formatter).isAfter(now)) {
                            timePicker.getItems().add(t);
                        }
                    } catch (Exception ex) {}
                }
            } else {
                timePicker.getItems().addAll(allTimes);
            }
        });
        
        timePicker.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5;");
        timePicker.setMaxWidth(Double.MAX_VALUE);
        
        HBox rowTime = new HBox(20, createLabeledField("Select Date", datePicker), createLabeledField("Time", timePicker));
        HBox.setHgrow(datePicker.getParent(), Priority.ALWAYS); HBox.setHgrow(timePicker.getParent(), Priority.ALWAYS);
        
        timeCard.getChildren().addAll(tHeader, new javafx.scene.control.Separator(), rowTime);

        // 4. Card 3: Payment Method
        VBox payCard = new VBox(15);
        payCard.setStyle("-fx-background-color: rgba(235, 245, 255, 0.9); -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        payCard.setPadding(new Insets(25));
        
        HBox pHeader = new HBox(15);
        pHeader.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.image.ImageView pIcon = getIcon("https://img.icons8.com/color/48/bank-cards.png", 24);
        VBox pTitleBox = new VBox(2);
        Text pTitleText = new Text("Payment Method"); pTitleText.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16)); pTitleText.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        Text pSubText = new Text("Select a payment option"); pSubText.setFont(javafx.scene.text.Font.font("System", 13)); pSubText.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        pTitleBox.getChildren().addAll(pTitleText, pSubText);
        pHeader.getChildren().addAll(pIcon, pTitleBox);
        
        ToggleGroup paymentGroup = new ToggleGroup();
        
        HBox codCard = new HBox(15);
        codCard.setAlignment(Pos.CENTER_LEFT); codCard.setPadding(new Insets(15));
        codCard.setStyle("-fx-border-color: #ecf0f1; -fx-border-radius: 10; -fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand;");
        codCard.setPrefWidth(400); HBox.setHgrow(codCard, Priority.ALWAYS);
        javafx.scene.control.RadioButton rbCod = new javafx.scene.control.RadioButton(); rbCod.setToggleGroup(paymentGroup); rbCod.setUserData("Cash on Delivery");
        javafx.scene.image.ImageView codIcon = getIcon("https://img.icons8.com/color/48/cash.png", 24);
        VBox codText = new VBox(3);
        Text codTitle = new Text("Cash on Delivery"); codTitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));
        Text codSub = new Text("Pay when you receive your order"); codSub.setFill(javafx.scene.paint.Color.web("#7f8c8d")); codSub.setFont(javafx.scene.text.Font.font(11));
        codText.getChildren().addAll(codTitle, codSub);
        codCard.getChildren().addAll(rbCod, codIcon, codText);
        
        HBox ccCard = new HBox(15);
        ccCard.setAlignment(Pos.CENTER_LEFT); ccCard.setPadding(new Insets(15));
        ccCard.setStyle("-fx-border-color: #ecf0f1; -fx-border-radius: 10; -fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand;");
        ccCard.setPrefWidth(400); HBox.setHgrow(ccCard, Priority.ALWAYS);
        javafx.scene.control.RadioButton rbCc = new javafx.scene.control.RadioButton(); rbCc.setToggleGroup(paymentGroup); rbCc.setUserData("Credit / Debit Card");
        javafx.scene.image.ImageView ccIcon = getIcon("https://img.icons8.com/color/48/bank-cards.png", 24);
        VBox ccText = new VBox(3);
        Text ccTitle = new Text("Credit / Debit Card"); ccTitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));
        Text ccSub = new Text("Pay securely using your card"); ccSub.setFill(javafx.scene.paint.Color.web("#7f8c8d")); ccSub.setFont(javafx.scene.text.Font.font(11));
        ccText.getChildren().addAll(ccTitle, ccSub);
        ccCard.getChildren().addAll(rbCc, ccIcon, ccText);
        
        paymentGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == rbCod) {
                codCard.setStyle("-fx-border-color: #00b894; -fx-border-radius: 10; -fx-background-color: #e8f8f5; -fx-background-radius: 10; -fx-border-width: 2;");
                codTitle.setFill(javafx.scene.paint.Color.web("#00b894"));
                ccCard.setStyle("-fx-border-color: #ecf0f1; -fx-border-radius: 10; -fx-background-color: white; -fx-background-radius: 10;");
                ccTitle.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            } else if (newT == rbCc) {
                ccCard.setStyle("-fx-border-color: #00b894; -fx-border-radius: 10; -fx-background-color: #e8f8f5; -fx-background-radius: 10; -fx-border-width: 2;");
                ccTitle.setFill(javafx.scene.paint.Color.web("#00b894"));
                codCard.setStyle("-fx-border-color: #ecf0f1; -fx-border-radius: 10; -fx-background-color: white; -fx-background-radius: 10;");
                codTitle.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            }
        });
        
        codCard.setOnMouseClicked(e -> rbCod.setSelected(true));
        ccCard.setOnMouseClicked(e -> rbCc.setSelected(true));
        HBox payOptionsBox = new HBox(20, codCard, ccCard);
        
        VBox cardSec = new VBox(10);
        Text cardDetailsTitle = new Text("Enter Card Details");
        cardDetailsTitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 13));
        TextField cardNum = createStyledTextField("0000 0000 0000 0000", null);
        cardNum.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                String clean = newV.replaceAll("[^0-9]", "");
                if (clean.length() > 16) clean = clean.substring(0, 16);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < clean.length(); i++) {
                    if (i > 0 && i % 4 == 0) sb.append(" ");
                    sb.append(clean.charAt(i));
                }
                if (!newV.equals(sb.toString())) cardNum.setText(sb.toString());
            }
        });
        TextField expDate = createStyledTextField("DD/MM", null);
        Text invalidExp = new Text("Invalid Date"); invalidExp.setFill(javafx.scene.paint.Color.RED); invalidExp.setVisible(false);
        expDate.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                String clean = newV.replaceAll("[^0-9]", "");
                if (clean.length() > 4) clean = clean.substring(0, 4);
                if (clean.length() >= 3) clean = clean.substring(0, 2) + "/" + clean.substring(2);
                
                boolean invalid = false;
                if (clean.length() >= 2) {
                    try {
                        int day = Integer.parseInt(clean.substring(0, 2));
                        if (day < 1 || day > 31) invalid = true;
                    } catch (Exception ex) {}
                }
                if (clean.length() == 5) {
                    try {
                        int month = Integer.parseInt(clean.substring(3, 5));
                        if (month < 1 || month > 12) invalid = true;
                    } catch (Exception ex) {}
                }
                
                if (invalid) {
                    expDate.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8; -fx-border-color: red;");
                    invalidExp.setVisible(true);
                } else {
                    expDate.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8; -fx-border-color: #bdc3c7;");
                    invalidExp.setVisible(false);
                }
                if (!newV.equals(clean)) expDate.setText(clean);
            }
        });
        TextField cvv = createStyledTextField("CVV", null);
        cvv.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d{0,3}") ? change : null));
        TextField ccName = createStyledTextField("Name on Card", user.getName());
        ccName.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("^[a-zA-Z\\s]*$") ? change : null));
        
        VBox expBox = createLabeledField("Expiry", expDate);
        expBox.getChildren().add(invalidExp);
        HBox ccRow1 = new HBox(20, createLabeledField("Card Number", cardNum), expBox);
        HBox ccRow2 = new HBox(20, createLabeledField("CVV", cvv), createLabeledField("Name on Card", ccName));
        HBox.setHgrow(cardNum.getParent(), Priority.ALWAYS); HBox.setHgrow(expDate.getParent(), Priority.ALWAYS);
        HBox.setHgrow(cvv.getParent(), Priority.ALWAYS); HBox.setHgrow(ccName.getParent(), Priority.ALWAYS);
        
        Button payBtn = new Button("Pay");
        payBtn.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        payBtn.setPrefWidth(150);
        payBtn.setPadding(new Insets(10));
        payBtn.setFont(javafx.scene.text.Font.font(14));
        boolean[] cardPaid = {false};
        payBtn.setOnAction(e -> {
            if (cardNum.getText().length() < 19 || cvv.getText().length() != 3 || expDate.getText().length() < 5 || ccName.getText().isEmpty()) {
                Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText("Please enter all inputs correctly."); a.show();
                return;
            }
            cardPaid[0] = true;
            Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText("Payment done successfully"); a.show();
        });
        cardSec.getChildren().addAll(cardDetailsTitle, ccRow1, ccRow2, payBtn);
        cardSec.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        cardSec.visibleProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(() -> 
            paymentGroup.getSelectedToggle() != null && paymentGroup.getSelectedToggle().getUserData().equals("Credit / Debit Card"), 
            paymentGroup.selectedToggleProperty()));
        cardSec.managedProperty().bind(cardSec.visibleProperty());
        
        payCard.getChildren().addAll(pHeader, new javafx.scene.control.Separator(), payOptionsBox, cardSec);
        
        rbCod.setSelected(true); // Default
        
        // 5. Action Button
        Button saveBtn = new Button("Save Delivery Info");
        saveBtn.setGraphic(getIcon("https://img.icons8.com/ios-glyphs/48/ffffff/save.png", 16));
        saveBtn.setPrefWidth(250);
        saveBtn.setStyle("-fx-background-color: linear-gradient(to right, #1976d2, #20b2aa); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 8; -fx-cursor: hand;");
        saveBtn.setPadding(new Insets(15));
        HBox saveBtnBox = new HBox(saveBtn); saveBtnBox.setAlignment(Pos.CENTER);
        
        saveBtn.setOnAction(e -> {
            String finalName = "", finalPhone = "", finalStreet = "", finalArea = "", finalCity = "";
            if (!user.getSavedAddresses().isEmpty() && !formBox.isVisible()) {
                String sel = savedCombo.getValue();
                for (String addr : user.getSavedAddresses()) {
                    String[] parts = addr.split(";;");
                    if (parts.length == 5 && (parts[0] + " - " + parts[2] + ", " + parts[4]).equals(sel)) {
                        finalName = parts[0]; finalPhone = parts[1]; finalStreet = parts[2]; finalArea = parts[3]; finalCity = parts[4];
                    }
                }
            } else {
                finalName = nameField.getText(); finalPhone = phoneField.getText();
                finalStreet = addressField.getText(); finalArea = areaField.getText(); finalCity = cityField.getText();
                
                if (finalName.isEmpty() || finalStreet.isEmpty() || finalCity.isEmpty() || finalArea.isEmpty() || finalPhone.isEmpty()) {
                    Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText("Please fill all address fields."); a.show();
                    return;
                }
                if (saveAddressCb.isSelected()) {
                    String addrStr = finalName + ";;" + finalPhone + ";;" + finalStreet + ";;" + finalArea + ";;" + finalCity;
                    user.addSavedAddress(addrStr);
                    saveAddressToFile(user, addrStr);
                }
            }
            
            if (datePicker.getValue() == null || timePicker.getValue() == null) {
                Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText("Please select Date and Time."); a.show();
                return;
            }
            
            savedDeliveryInfo = "Name: " + finalName + "\nAddress: " + finalStreet + ", " + finalArea + ", " + finalCity + "\nDate: " + datePicker.getValue() + "\nTime: " + timePicker.getValue() + "\nPayment: " + paymentGroup.getSelectedToggle().getUserData();
            deliveryDetailsSaved = true;
            Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText("Delivery info saved successfully!"); a.show();
        });

        Button confirmBtn = new Button("Confirm Order");
        confirmBtn.setGraphic(getIcon("https://img.icons8.com/ios-glyphs/48/ffffff/checked-checkbox.png", 16));
        confirmBtn.setMaxWidth(Double.MAX_VALUE);
        confirmBtn.setStyle("-fx-background-color: linear-gradient(to right, #1976d2, #20b2aa); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 8; -fx-cursor: hand;");
        confirmBtn.setPadding(new Insets(15));
        confirmBtn.setOnAction(e -> {
            if (!deliveryDetailsSaved) {
                Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText("Please save delivery details first."); a.show();
                return;
            }
            if (paymentGroup.getSelectedToggle().getUserData().equals("Credit / Debit Card") && !cardPaid[0]) {
                Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText("Please pay with your card before confirming the order."); a.show();
                return;
            }
            if (cart.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText("Cart is empty!"); a.show();
                return;
            }
            user.placeOrder();
            Order lastOrder = user.getOrderHistoryObservable().get(user.getOrderHistoryObservable().size() - 1);
            saveOrder(lastOrder);
            stage.close();
            if (parentStage != null) parentStage.close();
            showSuccessAndDeliveryDetails(null, lastOrder, true, savedDeliveryInfo);
        });
        
        Button backBtn = new Button("Back");
        backBtn.setGraphic(getIcon("https://img.icons8.com/color/48/back.png", 16));
        backBtn.setStyle("-fx-background-color: white; -fx-text-fill: #1976d2; -fx-font-weight: bold; -fx-border-color: #bdc3c7; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        backBtn.setPrefWidth(150);
        backBtn.setPadding(new Insets(15, 0, 15, 0));
        backBtn.setOnAction(e -> { stage.close(); parentStage.show(); });
        
        confirmBtn.setPrefWidth(200);
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottomActions = new HBox(15, backBtn, spacer, confirmBtn);
        bottomActions.setAlignment(Pos.CENTER);
        
        VBox layout = new VBox(20, titleBox, addressCard, timeCard, saveBtnBox, payCard, bottomActions);
        layout.setPadding(new Insets(30, 40, 40, 40));
        layout.setMaxWidth(900);
        layout.setAlignment(Pos.TOP_CENTER);
        
        layout.setStyle("-fx-background-color: transparent;");
        
        javafx.scene.layout.StackPane centerWrapper = new javafx.scene.layout.StackPane(layout);
        centerWrapper.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(centerWrapper);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");
        
        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(scroll);
        root.setStyle("-fx-background-image: url('/background.jpg'); -fx-background-size: cover;");
        
        Scene scene = new Scene(root, 1000, 650);
        scene.getStylesheets().add("data:text/css,.date-picker-popup * { -fx-text-fill: black !important; }");
        try { scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); } catch (Exception ignored) {}
        stage.setScene(scene);
        
        stage.setOnCloseRequest(e -> parentStage.show());
        stage.show();
    }

    private void showSuccessAndDeliveryDetails(Stage parentStage, Order lastOrder, boolean isDelivery, String deliveryInfo) {
        Stage successStage = new Stage();
        successStage.setTitle("Order Status");

        // Main Layout (StackPane for background image)
        javafx.scene.layout.StackPane rootPane = new javafx.scene.layout.StackPane();
        rootPane.setStyle("-fx-background-image: url('/background.jpg'); -fx-background-size: cover;");


        // Content Container
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.setMaxWidth(650);
        contentBox.setPadding(new Insets(40, 20, 40, 20));

        // 1. Header (Checkmark & Title)
        VBox headerBox = new VBox(5);
        headerBox.setAlignment(Pos.CENTER);
        
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane();
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(35, javafx.scene.paint.Color.web("#e8f5e9"));
        circle.setStroke(javafx.scene.paint.Color.web("#27ae60"));
        circle.setStrokeWidth(2);
        Text checkMark = new Text("✔");
        checkMark.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 36));
        checkMark.setFill(javafx.scene.paint.Color.web("#27ae60"));
        iconPane.getChildren().addAll(circle, checkMark);

        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(10, 0, 0, 0));
        Text t1 = new Text("Order Placed");
        t1.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 28));
        t1.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        Text t2 = new Text("Successfully!");
        t2.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 28));
        t2.setFill(javafx.scene.paint.Color.web("#20b2aa"));
        titleBox.getChildren().addAll(t1, t2);

        Text subTitle = new Text("Thank you for your order.");
        subTitle.setFont(javafx.scene.text.Font.font("System", 14));
        subTitle.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        
        headerBox.getChildren().addAll(iconPane, titleBox, subTitle);

        // 2. User Details Card
        VBox userCard = new VBox(15);
        userCard.setStyle("-fx-background-color: rgba(235, 245, 255, 0.9); -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        userCard.setPadding(new Insets(20));
        
        HBox uHeader = new HBox(10);
        uHeader.setAlignment(Pos.CENTER_LEFT);
        Text uIcon = new Text("👤"); uIcon.setFont(javafx.scene.text.Font.font(16));
        Text uTitle = new Text("User Details"); uTitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14)); uTitle.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        uHeader.getChildren().addAll(uIcon, uTitle);
        
        HBox uInfo = new HBox(15);
        uInfo.setAlignment(Pos.CENTER_LEFT);
        Text nameLbl = new Text("Name: "); nameLbl.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        Text nameVal = new Text(user.getName()); nameVal.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        javafx.scene.shape.Line vline = new javafx.scene.shape.Line(0,0,0,15); vline.setStroke(javafx.scene.paint.Color.web("#bdc3c7"));
        Text phoneLbl = new Text("Contact: "); phoneLbl.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
        Text phoneVal = new Text(user.getContactInfo()); phoneVal.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        uInfo.getChildren().addAll(nameLbl, nameVal, vline, phoneLbl, phoneVal);
        
        userCard.getChildren().addAll(uHeader, uInfo);

        // 3. Order Details Card
        VBox orderCard = new VBox(15);
        orderCard.setStyle("-fx-background-color: rgba(235, 245, 255, 0.9); -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        orderCard.setPadding(new Insets(20));
        
        HBox oHeader = new HBox(10);
        oHeader.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.image.ImageView oIcon = getIcon("https://img.icons8.com/color/48/shopping-bag.png", 24);
        Text oTitle = new Text("Order Details"); oTitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14)); oTitle.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        oHeader.getChildren().addAll(oIcon, oTitle);
        orderCard.getChildren().add(oHeader);
        
        if (lastOrder != null) {
            for (Medicine m : lastOrder.getMedicines()) {
                HBox itemRow = new HBox(10);
                itemRow.setAlignment(Pos.CENTER_LEFT);
                javafx.scene.image.ImageView mIcon = getMedicineIcon(m.getId(), 18);
                Text mName = new Text(m.getSelectedQuantity() + "x " + m.getName());
                mName.setFont(javafx.scene.text.Font.font("System", 13));
                mName.setFill(javafx.scene.paint.Color.web("#2c3e50"));
                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                Text mPrice = new Text(String.format("Rs. %.1f", m.getPrice() * m.getSelectedQuantity()));
                mPrice.setFont(javafx.scene.text.Font.font("System", 13));
                itemRow.getChildren().addAll(mIcon, mName, spacer, mPrice);
                orderCard.getChildren().add(itemRow);
            }
            
            HBox totalRow = new HBox(10);
            totalRow.setAlignment(Pos.CENTER_LEFT);
            totalRow.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 8;");
            totalRow.setPadding(new Insets(15));
            javafx.scene.image.ImageView wIcon = getIcon("https://img.icons8.com/color/48/bank-cards.png", 24);
            Text tLabel = new Text("Total Amount"); tLabel.setFont(javafx.scene.text.Font.font("System", 14)); tLabel.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            
            // "PAID" label logic
            boolean isCardPaid = deliveryInfo != null && deliveryInfo.contains("Credit / Debit Card");
            Text paidLabel = new Text();
            if (isCardPaid) {
                paidLabel.setText(" (PAID)");
                paidLabel.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));
                paidLabel.setFill(javafx.scene.paint.Color.web("#e74c3c")); // Red color
            }
            
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            Text tValue = new Text(String.format("Rs. %.1f", lastOrder.getTotalPrice()));
            tValue.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
            tValue.setFill(javafx.scene.paint.Color.web("#27ae60"));
            
            totalRow.getChildren().addAll(wIcon, tLabel, paidLabel, spacer, tValue);
            orderCard.getChildren().add(totalRow);
        }

        contentBox.getChildren().addAll(headerBox, userCard, orderCard);

        // 4. Delivery Details Card (If applicable)
        if (isDelivery && deliveryInfo != null) {
            VBox deliveryCard = new VBox(15);
            deliveryCard.setStyle("-fx-background-color: rgba(235, 245, 255, 0.9); -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
            deliveryCard.setPadding(new Insets(20));
            
            HBox dHeader = new HBox(10);
            dHeader.setAlignment(Pos.CENTER_LEFT);
            Text dIcon = new Text("🚚"); dIcon.setFont(javafx.scene.text.Font.font(16));
            Text dTitle = new Text("Delivery Details"); dTitle.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14)); dTitle.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            dHeader.getChildren().addAll(dIcon, dTitle);
            
            // Parse deliveryInfo to show cleanly
            // Format is: Name: x \nAddress: x \nDate: x \nTime: x \nPayment: x
            String addressStr = "N/A", dateStr = "N/A", timeStr = "N/A", payStr = "N/A";
            for (String line : deliveryInfo.split("\n")) {
                if (line.startsWith("Address: ")) addressStr = line.substring(9);
                if (line.startsWith("Date: ")) dateStr = line.substring(6);
                if (line.startsWith("Time: ")) timeStr = line.substring(6);
                if (line.startsWith("Payment: ")) payStr = line.substring(9);
            }
            
            GridPane dGrid = new GridPane();
            dGrid.setHgap(30); dGrid.setVgap(10);
            
            Text aLbl = new Text("Address:"); aLbl.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
            Text aVal = new Text(addressStr); aVal.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            Text pLbl = new Text("Payment Method:"); pLbl.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
            Text pVal = new Text(payStr); pVal.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            
            Text dtLbl = new Text("Date:"); dtLbl.setFill(javafx.scene.paint.Color.web("#7f8c8d"));
            Text dtVal = new Text(dateStr + "   |   Time: " + timeStr); dtVal.setFill(javafx.scene.paint.Color.web("#2c3e50"));
            
            dGrid.add(aLbl, 0, 0); dGrid.add(aVal, 1, 0);
            dGrid.add(pLbl, 2, 0); dGrid.add(pVal, 3, 0);
            dGrid.add(dtLbl, 0, 1); dGrid.add(dtVal, 1, 1, 3, 1);
            
            deliveryCard.getChildren().addAll(dHeader, dGrid);
            contentBox.getChildren().add(deliveryCard);
        }

        Button browseBtn = new Button("Browse Again");
        browseBtn.setGraphic(getIcon("https://img.icons8.com/color/48/refresh.png", 16));
        browseBtn.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;");
        browseBtn.setPrefWidth(200);
        browseBtn.setPadding(new Insets(12));
        browseBtn.setOnAction(e -> { successStage.close(); showMedicineListStage(); });

        Button historyBtn = new Button("View History");
        historyBtn.setGraphic(getIcon("https://img.icons8.com/color/48/order-history.png", 16));
        historyBtn.setStyle("-fx-background-color: white; -fx-text-fill: #1976d2; -fx-border-color: #1976d2; -fx-border-radius: 8; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;");
        historyBtn.setPrefWidth(200);
        historyBtn.setPadding(new Insets(12));
        historyBtn.setOnAction(e -> {
            successStage.close();
            showOrderHistoryStage(user.getOrderHistoryObservable(), () -> showMedicineListStage());
        });

        Button cancelBtn = new Button("Cancel Order");
        cancelBtn.setGraphic(getIcon("https://img.icons8.com/color/48/cancel.png", 16));
        cancelBtn.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-border-color: #ffcdd2; -fx-border-radius: 8; -fx-cursor: hand;");
        cancelBtn.setPrefWidth(200);
        cancelBtn.setPadding(new Insets(12));
        cancelBtn.setOnAction(e -> {
            if (lastOrder != null) {
                lastOrder.setStatus("Cancelled");
                updateOrderInFile(lastOrder);
            }
            successStage.close();
            showMedicineListStage();
        });

        Button logoutBtn = new Button("Logout");
        logoutBtn.setGraphic(getIcon("https://img.icons8.com/ios-filled/48/e74c3c/logout-rounded-left.png", 16));
        logoutBtn.setStyle("-fx-background-color: #ffeef0; -fx-text-fill: #ff4757; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;");
        logoutBtn.setPrefWidth(200);
        logoutBtn.setPadding(new Insets(12));
        logoutBtn.setOnAction(e -> { successStage.close(); showLoginStage(); });

        HBox topBtnRow = new HBox(20, browseBtn, historyBtn);
        topBtnRow.setAlignment(Pos.CENTER);
        
        HBox bottomBtnRow = new HBox(20, cancelBtn, logoutBtn);
        bottomBtnRow.setAlignment(Pos.CENTER);
        
        VBox btnBox = new VBox(15, topBtnRow, bottomBtnRow);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(10, 0, 0, 0));
        
        contentBox.getChildren().add(btnBox);
        
        // Add contentBox to scrollPane
        javafx.scene.layout.StackPane centerWrapper = new javafx.scene.layout.StackPane(contentBox);
        centerWrapper.setAlignment(Pos.TOP_CENTER);
        
        ScrollPane scroll = new ScrollPane(centerWrapper);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        
        // Ensure scrollpane has a clear background so we see the rootPane image
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");
        contentBox.setStyle("-fx-background-color: transparent;");
        
        // We will put the scrollpane in the center
        HBox alignBox = new HBox(scroll);
        alignBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(scroll, Priority.ALWAYS);
        
        rootPane.getChildren().add(alignBox);

        Scene scene = new Scene(rootPane, 1000, 600);
        successStage.setScene(scene);
        successStage.show();
    }

    private VBox createHeaderCell(String text, double width) {
        Text t = new Text(text); t.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 13)); t.setFill(javafx.scene.paint.Color.web("#1976d2"));
        VBox box = new VBox(t); box.setAlignment(Pos.CENTER); box.setPrefWidth(width);
        return box;
    }

    private TextField createStyledTextField(String prompt, String autofill) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        if (autofill != null) tf.setText(autofill);
        tf.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-prompt-text-fill: #aab0b8; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
        return tf;
    }

    private VBox createLabeledField(String label, Node field) {
        Text text = new Text(label);
        text.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 10));
        text.setFill(javafx.scene.paint.Color.web("#2c3e50"));
        VBox box = new VBox(5, text, field);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private ImageView createBanner(String uri) {
        ImageView banner = new ImageView(new Image(uri, true));
        banner.setFitWidth(820);
        banner.setFitHeight(180);
        banner.setPreserveRatio(false);
        banner.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(820, 180);
        clip.setArcWidth(20); clip.setArcHeight(20);
        banner.setClip(clip);
        
        return banner;
    }

    private double calculateTotalCartPrice() {
        double totalCartPrice = 0.0;
        for (Medicine medicine : cart) {
            totalCartPrice += medicine.getPrice() * medicine.getSelectedQuantity();
        }
        return totalCartPrice;
    }

    private static final java.util.Map<String, javafx.scene.image.Image> iconCache = new java.util.concurrent.ConcurrentHashMap<>();

    private javafx.scene.image.ImageView getIcon(String url, int size) {
        try {
            javafx.scene.image.Image img = iconCache.get(url);
            if (img == null) {
                img = new javafx.scene.image.Image(url, true);
                iconCache.put(url, img);
            }
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            return iv;
        } catch (Exception e) {
            System.err.println("Error loading icon: " + url);
            return new javafx.scene.image.ImageView();
        }
    }

}
