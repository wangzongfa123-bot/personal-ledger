package com.ledger.ui;

import com.ledger.api.ApiClient;
import com.ledger.model.Category;
import com.ledger.model.Stats;
import com.ledger.model.Transaction;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class MainController {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BorderPane root = new BorderPane();

    private ApiClient api = new ApiClient("http://127.0.0.1:8000");
    private final TextField apiUrlField = new TextField(api.getBaseUrl());

    // Stats
    private final Label incomeLabel = new Label("¥0.00");
    private final Label expenseLabel = new Label("¥0.00");
    private final Label balanceLabel = new Label("¥0.00");
    private final Label countLabel = new Label("0");

    // Form
    private final ToggleGroup kindGroup = new ToggleGroup();
    private final RadioButton expenseRadio = new RadioButton("支出");
    private final RadioButton incomeRadio = new RadioButton("收入");
    private final TextField amountField = new TextField();
    private final ComboBox<Category> categoryBox = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final TextField noteField = new TextField();
    private final Label formTip = new Label();

    // List & filters
    private final ChoiceBox<String> filterKindBox = new ChoiceBox<>();
    private final ComboBox<Category> filterCategoryBox = new ComboBox<>();
    private final TableView<Transaction> table = new TableView<>();
    private final ObservableList<Transaction> txItems = FXCollections.observableArrayList();

    private final Label statusLabel = new Label("准备就绪");

    private List<Category> allCategories = List.of();

    public MainController() {
        root.setTop(buildTopBar());
        root.setCenter(buildCenter());
        root.setBottom(buildStatusBar());
    }

    public BorderPane getRoot() {
        return root;
    }

    public void bootstrap() {
        loadAll();
    }

    // ========================= UI =========================

    private Region buildTopBar() {
        Label title = new Label("💰 个人记账本 · 桌面端");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: white;");

        Label apiLabel = new Label("API:");
        apiLabel.setStyle("-fx-text-fill: white;");
        apiUrlField.setPrefWidth(260);

        Button connectBtn = new Button("连接");
        connectBtn.setOnAction(e -> {
            api = new ApiClient(apiUrlField.getText().trim());
            loadAll();
        });

        HBox right = new HBox(8, apiLabel, apiUrlField, connectBtn);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox bar = new HBox(title, spacer(), right);
        bar.setPadding(new Insets(14, 18, 14, 18));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: linear-gradient(to right, #4f7cff, #6f5cff);");
        return bar;
    }

    private Region buildCenter() {
        VBox stats = buildStatsPane();

        // Form
        VBox form = buildFormPane();
        form.setPrefWidth(320);

        // Table + filters
        VBox listPane = buildListPane();

        HBox row = new HBox(16, form, listPane);
        HBox.setHgrow(listPane, Priority.ALWAYS);
        row.setPadding(new Insets(0, 16, 16, 16));

        VBox center = new VBox(16, stats, row);
        VBox.setVgrow(row, Priority.ALWAYS);
        center.setPadding(new Insets(16, 0, 0, 0));
        center.setStyle("-fx-background-color: #f4f6fb;");
        return center;
    }

    private VBox buildStatsPane() {
        HBox row = new HBox(12,
                statCard("总收入", incomeLabel, "#15a36a"),
                statCard("总支出", expenseLabel, "#e0533d"),
                statCard("结余", balanceLabel, "#4f7cff"),
                statCard("笔数", countLabel, "#1f2330"));
        row.setPadding(new Insets(0, 16, 0, 16));
        for (var n : row.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        VBox box = new VBox(row);
        return box;
    }

    private Region statCard(String label, Label valueLabel, String color) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        valueLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        VBox v = new VBox(4, l, valueLabel);
        v.setPadding(new Insets(14, 16, 14, 16));
        v.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
        v.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    private VBox buildFormPane() {
        Label title = new Label("记一笔");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));

        expenseRadio.setToggleGroup(kindGroup);
        incomeRadio.setToggleGroup(kindGroup);
        expenseRadio.setSelected(true);
        expenseRadio.setUserData("expense");
        incomeRadio.setUserData("income");
        kindGroup.selectedToggleProperty().addListener((obs, o, n) -> refreshCategoryBox());

        HBox kindRow = new HBox(12, expenseRadio, incomeRadio);

        amountField.setPromptText("0.00");
        categoryBox.setMaxWidth(Double.MAX_VALUE);
        noteField.setPromptText("可选");

        Button submit = new Button("保存");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setStyle("-fx-background-color: #4f7cff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 0 8 0;");
        submit.setOnAction(e -> handleSubmit());

        formTip.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        VBox form = new VBox(10,
                title,
                kindRow,
                labeledRow("金额", amountField),
                labeledRow("分类", categoryBox),
                labeledRow("日期", datePicker),
                labeledRow("备注", noteField),
                submit,
                formTip
        );
        form.setPadding(new Insets(18));
        form.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
        return form;
    }

    private HBox labeledRow(String label, javafx.scene.Node control) {
        Label l = new Label(label);
        l.setMinWidth(48);
        l.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        if (control instanceof Region r) {
            HBox.setHgrow(r, Priority.ALWAYS);
            r.setMaxWidth(Double.MAX_VALUE);
        }
        HBox h = new HBox(8, l, control);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    @SuppressWarnings("unchecked")
    private VBox buildListPane() {
        Label title = new Label("账单流水");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));

        filterKindBox.getItems().addAll("全部", "仅支出", "仅收入");
        filterKindBox.setValue("全部");
        filterKindBox.setOnAction(e -> loadTransactions());

        filterCategoryBox.setPromptText("全部分类");
        filterCategoryBox.setOnAction(e -> loadTransactions());

        Button clearCat = new Button("清除分类");
        clearCat.setOnAction(e -> {
            filterCategoryBox.getSelectionModel().clearSelection();
            filterCategoryBox.setValue(null);
            loadTransactions();
        });

        Button refresh = new Button("刷新");
        refresh.setOnAction(e -> loadAll());

        HBox filters = new HBox(8, filterKindBox, filterCategoryBox, clearCat, spacer(), refresh);
        filters.setAlignment(Pos.CENTER_LEFT);

        // Table columns
        TableColumn<Transaction, String> timeCol = new TableColumn<>("时间");
        timeCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().occurred_at == null ? "" : d.getValue().occurred_at.format(DT_FMT)));
        timeCol.setPrefWidth(140);

        TableColumn<Transaction, String> catCol = new TableColumn<>("分类");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().category == null ? "" : d.getValue().category.toString()));
        catCol.setPrefWidth(120);

        TableColumn<Transaction, String> kindCol = new TableColumn<>("类型");
        kindCol.setCellValueFactory(d -> new SimpleStringProperty(
                "income".equals(d.getValue().kind) ? "收入" : "支出"));
        kindCol.setPrefWidth(60);

        TableColumn<Transaction, String> amountCol = new TableColumn<>("金额");
        amountCol.setCellValueFactory(d -> {
            Transaction t = d.getValue();
            String sign = "income".equals(t.kind) ? "+" : "-";
            return new SimpleStringProperty(sign + "¥" + String.format("%.2f", t.amount));
        });
        amountCol.setPrefWidth(100);

        TableColumn<Transaction, String> noteCol = new TableColumn<>("备注");
        noteCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().note == null ? "" : d.getValue().note));
        noteCol.setPrefWidth(180);

        TableColumn<Transaction, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(80);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button del = new Button("删除");
            {
                del.setStyle("-fx-background-color: transparent; -fx-text-fill: #e0533d; -fx-cursor: hand;");
                del.setOnAction(e -> {
                    Transaction t = getTableView().getItems().get(getIndex());
                    handleDelete(t);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : del);
            }
        });

        table.getColumns().setAll(timeCol, catCol, kindCol, amountCol, noteCol, actionCol);
        table.setItems(txItems);
        table.setPlaceholder(new Label("暂无账单"));
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox box = new VBox(10, title, filters, table);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
        return box;
    }

    private Region buildStatusBar() {
        statusLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        HBox h = new HBox(statusLabel);
        h.setPadding(new Insets(8, 16, 8, 16));
        h.setStyle("-fx-background-color: #eef1f8;");
        return h;
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    // ========================= Actions =========================

    private void loadAll() {
        runAsync(
                () -> {
                    List<Category> cats = api.listCategories();
                    Stats s = api.statsSummary();
                    List<Transaction> txs = api.listTransactions(currentFilterKind(), currentFilterCategoryId());
                    return new Object[]{cats, s, txs};
                },
                result -> {
                    Object[] arr = (Object[]) result;
                    @SuppressWarnings("unchecked") List<Category> cats = (List<Category>) arr[0];
                    Stats s = (Stats) arr[1];
                    @SuppressWarnings("unchecked") List<Transaction> txs = (List<Transaction>) arr[2];
                    allCategories = cats;
                    refreshCategoryBox();
                    refreshFilterCategoryBox();
                    applyStats(s);
                    txItems.setAll(txs);
                    statusLabel.setText("已连接 " + api.getBaseUrl() + " · " + txs.size() + " 条记录");
                },
                err -> statusLabel.setText("连接失败：" + err.getMessage())
        );
    }

    private void loadTransactions() {
        runAsync(
                () -> api.listTransactions(currentFilterKind(), currentFilterCategoryId()),
                txs -> {
                    txItems.setAll(txs);
                    statusLabel.setText("共 " + txs.size() + " 条记录");
                },
                err -> statusLabel.setText("加载失败：" + err.getMessage())
        );
    }

    private void handleSubmit() {
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            tip("请输入正确的金额", true);
            return;
        }
        Category cat = categoryBox.getValue();
        if (cat == null) {
            tip("请选择分类", true);
            return;
        }
        String kind = (String) kindGroup.getSelectedToggle().getUserData();
        LocalDate date = datePicker.getValue() == null ? LocalDate.now() : datePicker.getValue();
        LocalDateTime occurredAt = LocalDateTime.of(date, LocalTime.now());
        String note = noteField.getText() == null ? null : noteField.getText().trim();

        runAsync(
                () -> api.createTransaction(amount, kind, cat.id, note, occurredAt),
                created -> {
                    tip("已保存", false);
                    amountField.clear();
                    noteField.clear();
                    loadAll();
                },
                err -> tip("保存失败：" + err.getMessage(), true)
        );
    }

    private void handleDelete(Transaction t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "确认删除该条账单？", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                runAsync(
                        () -> { api.deleteTransaction(t.id); return null; },
                        ok -> loadAll(),
                        err -> statusLabel.setText("删除失败：" + err.getMessage())
                );
            }
        });
    }

    // ========================= Helpers =========================

    private void refreshCategoryBox() {
        String kind = (String) kindGroup.getSelectedToggle().getUserData();
        List<Category> filtered = allCategories.stream()
                .filter(c -> c.kind.equals(kind))
                .collect(Collectors.toList());
        categoryBox.setItems(FXCollections.observableArrayList(filtered));
        if (!filtered.isEmpty()) categoryBox.getSelectionModel().selectFirst();
    }

    private void refreshFilterCategoryBox() {
        Category old = filterCategoryBox.getValue();
        filterCategoryBox.setItems(FXCollections.observableArrayList(allCategories));
        if (old != null) {
            for (Category c : allCategories) {
                if (c.id == old.id) { filterCategoryBox.setValue(c); break; }
            }
        }
    }

    private String currentFilterKind() {
        return switch (filterKindBox.getValue() == null ? "全部" : filterKindBox.getValue()) {
            case "仅支出" -> "expense";
            case "仅收入" -> "income";
            default -> null;
        };
    }

    private Integer currentFilterCategoryId() {
        Category c = filterCategoryBox.getValue();
        return c == null ? null : c.id;
    }

    private void applyStats(Stats s) {
        incomeLabel.setText("¥" + String.format("%.2f", s.income));
        expenseLabel.setText("¥" + String.format("%.2f", s.expense));
        balanceLabel.setText("¥" + String.format("%.2f", s.balance));
        countLabel.setText(String.valueOf(s.count));
    }

    private void tip(String msg, boolean error) {
        formTip.setText(msg);
        formTip.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (error ? "#e0533d" : "#15a36a") + ";");
    }

    @FunctionalInterface
    private interface Producer<T> { T call() throws Exception; }

    private <T> void runAsync(Producer<T> producer,
                              java.util.function.Consumer<T> onSuccess,
                              java.util.function.Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return producer.call();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> onSuccess.accept(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> onError.accept(task.getException())));
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }
}
