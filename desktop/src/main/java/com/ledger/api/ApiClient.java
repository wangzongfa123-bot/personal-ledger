package com.ledger.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ledger.model.Category;
import com.ledger.model.Stats;
import com.ledger.model.Transaction;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiClient {

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private <T> T send(HttpRequest req, TypeReference<T> type) throws Exception {
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            String detail = res.body();
            try {
                Map<String, Object> err = mapper.readValue(res.body(), new TypeReference<>() {});
                if (err.get("detail") != null) detail = err.get("detail").toString();
            } catch (Exception ignored) {}
            throw new RuntimeException("HTTP " + res.statusCode() + ": " + detail);
        }
        if (res.body() == null || res.body().isEmpty() || type == null) return null;
        return mapper.readValue(res.body(), type);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");
    }

    public boolean ping() {
        try {
            HttpRequest req = request("/api/health").GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Category> listCategories() throws Exception {
        return send(request("/api/categories").GET().build(),
                new TypeReference<List<Category>>() {});
    }

    public List<Transaction> listTransactions(String kind, Integer categoryId) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        if (kind != null && !kind.isEmpty()) params.put("kind", kind);
        if (categoryId != null) params.put("category_id", categoryId.toString());
        params.put("limit", "200");
        return send(request("/api/transactions" + queryString(params)).GET().build(),
                new TypeReference<List<Transaction>>() {});
    }

    public Transaction createTransaction(double amount, String kind, int categoryId,
                                         String note, LocalDateTime occurredAt) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("kind", kind);
        body.put("category_id", categoryId);
        if (note != null && !note.isBlank()) body.put("note", note);
        if (occurredAt != null) body.put("occurred_at", occurredAt.toString());

        String json = mapper.writeValueAsString(body);
        System.out.println("[ApiClient] POST /api/transactions body=" + json);
        HttpRequest req = request("/api/transactions")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return send(req, new TypeReference<Transaction>() {});
    }

    public void deleteTransaction(int id) throws Exception {
        HttpRequest req = request("/api/transactions/" + id).DELETE().build();
        send(req, null);
    }

    public Stats statsSummary() throws Exception {
        return send(request("/api/stats/summary").GET().build(),
                new TypeReference<Stats>() {});
    }

    private String queryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (var e : params.entrySet()) {
            if (!first) sb.append("&");
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
    }
}
