package com.financemanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:test-${random.uuid};DB_CLOSE_DELAY=-1"
})
class FinanceManagerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private Cookie register(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password,
                "fullName", "Test User",
                "phoneNumber", "+1234567890"
        ));
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
        return login(username, password);
    }

    private Cookie login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username, "password", password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpServletResponse response = result.getResponse();
        Cookie cookie = response.getCookie("FMSESSIONID");
        if (cookie == null) {
            cookie = response.getCookie("JSESSIONID");
        }
        assertNotNull(cookie, "session cookie expected");
        return cookie;
    }

    @Test
    void registerLoginAndLogoutFlow() throws Exception {
        Cookie session = register("alice@example.com", "password123");

        mockMvc.perform(get("/api/categories").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[*].name", org.hamcrest.Matchers.hasItem("Salary")));

        mockMvc.perform(post("/api/auth/logout").cookie(session))
                .andExpect(status().isOk());
    }

    @Test
    void registerValidationErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateUsernameConflict() throws Exception {
        register("dup@example.com", "password123");
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "dup@example.com",
                "password", "password123",
                "fullName", "X", "phoneNumber", "+12345"));
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidLoginReturns401() throws Exception {
        register("bob@example.com", "password123");
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "bob@example.com", "password", "wrongpass"));
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void transactionsCrudWithReports() throws Exception {
        Cookie session = register("crud@example.com", "password123");

        // create income
        String incomeBody = objectMapper.writeValueAsString(Map.of(
                "amount", 5000.00,
                "date", LocalDate.now().toString(),
                "category", "Salary",
                "description", "Monthly"));
        MvcResult incomeResult = mockMvc.perform(post("/api/transactions")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(incomeBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andReturn();
        JsonNode incomeNode = objectMapper.readTree(incomeResult.getResponse().getContentAsString());
        long incomeId = incomeNode.get("id").asLong();

        // expense
        String expenseBody = objectMapper.writeValueAsString(Map.of(
                "amount", 1000.00,
                "date", LocalDate.now().toString(),
                "category", "Food"));
        mockMvc.perform(post("/api/transactions")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(expenseBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        // future date rejected
        String futureBody = objectMapper.writeValueAsString(Map.of(
                "amount", 10.0,
                "date", LocalDate.now().plusDays(2).toString(),
                "category", "Salary"));
        mockMvc.perform(post("/api/transactions")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(futureBody))
                .andExpect(status().isBadRequest());

        // invalid category rejected
        String invalidCat = objectMapper.writeValueAsString(Map.of(
                "amount", 10.0,
                "date", LocalDate.now().toString(),
                "category", "Nonexistent"));
        mockMvc.perform(post("/api/transactions")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidCat))
                .andExpect(status().isBadRequest());

        // list
        mockMvc.perform(get("/api/transactions").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions.length()").value(2));

        // update
        String updateBody = objectMapper.writeValueAsString(Map.of(
                "amount", 6000.00, "description", "Bumped"));
        mockMvc.perform(put("/api/transactions/" + incomeId)
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(6000.00));

        // monthly report
        LocalDate now = LocalDate.now();
        mockMvc.perform(get("/api/reports/monthly/" + now.getYear() + "/" + now.getMonthValue())
                .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netSavings").value(5000.00));

        // yearly report
        mockMvc.perform(get("/api/reports/yearly/" + now.getYear()).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netSavings").value(5000.00));

        // delete
        mockMvc.perform(delete("/api/transactions/" + incomeId).cookie(session))
                .andExpect(status().isOk());

        // delete non-existent
        mockMvc.perform(delete("/api/transactions/999999").cookie(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void customCategoryManagement() throws Exception {
        Cookie session = register("cat@example.com", "password123");

        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "SideHustle", "type", "INCOME"));
        mockMvc.perform(post("/api/categories")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isCustom").value(true));

        // duplicate
        mockMvc.perform(post("/api/categories")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
                .andExpect(status().isConflict());

        // conflict with default
        String defaultBody = objectMapper.writeValueAsString(Map.of(
                "name", "Salary", "type", "INCOME"));
        mockMvc.perform(post("/api/categories")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(defaultBody))
                .andExpect(status().isConflict());

        // cannot delete default
        mockMvc.perform(delete("/api/categories/Salary").cookie(session))
                .andExpect(status().isForbidden());

        // delete custom OK
        mockMvc.perform(delete("/api/categories/SideHustle").cookie(session))
                .andExpect(status().isOk());

        // delete missing -> 404
        mockMvc.perform(delete("/api/categories/UnknownXYZ").cookie(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void customCategoryReferencedByTransactionCannotBeDeleted() throws Exception {
        Cookie session = register("ref@example.com", "password123");

        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Bonus", "type", "INCOME"));
        mockMvc.perform(post("/api/categories")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
                .andExpect(status().isCreated());

        String txBody = objectMapper.writeValueAsString(Map.of(
                "amount", 100.00,
                "date", LocalDate.now().toString(),
                "category", "Bonus"));
        mockMvc.perform(post("/api/transactions")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(txBody))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/categories/Bonus").cookie(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void savingsGoalsLifecycle() throws Exception {
        Cookie session = register("goal@example.com", "password123");

        // create transactions for progress
        String tx = objectMapper.writeValueAsString(Map.of(
                "amount", 2000.00,
                "date", LocalDate.now().toString(),
                "category", "Salary"));
        mockMvc.perform(post("/api/transactions")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tx))
                .andExpect(status().isCreated());

        String goalBody = objectMapper.writeValueAsString(Map.of(
                "goalName", "Trip",
                "targetAmount", 10000.00,
                "targetDate", LocalDate.now().plusYears(1).toString(),
                "startDate", LocalDate.now().minusDays(1).toString()));
        MvcResult result = mockMvc.perform(post("/api/goals")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(goalBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentProgress").value(2000.00))
                .andReturn();
        long goalId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // past target date rejected
        String bad = objectMapper.writeValueAsString(Map.of(
                "goalName", "Past",
                "targetAmount", 100.00,
                "targetDate", LocalDate.now().minusDays(1).toString()));
        mockMvc.perform(post("/api/goals")
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bad))
                .andExpect(status().isBadRequest());

        // list
        mockMvc.perform(get("/api/goals").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals.length()").value(1));

        // get
        mockMvc.perform(get("/api/goals/" + goalId).cookie(session))
                .andExpect(status().isOk());

        // update
        String upd = objectMapper.writeValueAsString(Map.of(
                "targetAmount", 5000.00,
                "targetDate", LocalDate.now().plusMonths(6).toString()));
        mockMvc.perform(put("/api/goals/" + goalId)
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(upd))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount").value(5000.00));

        // delete
        mockMvc.perform(delete("/api/goals/" + goalId).cookie(session))
                .andExpect(status().isOk());

        // not found
        mockMvc.perform(get("/api/goals/" + goalId).cookie(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void dataIsolationBetweenUsers() throws Exception {
        Cookie a = register("isoA@example.com", "password123");
        Cookie b = register("isoB@example.com", "password123");

        String txBody = objectMapper.writeValueAsString(Map.of(
                "amount", 100.00,
                "date", LocalDate.now().toString(),
                "category", "Salary"));
        MvcResult res = mockMvc.perform(post("/api/transactions")
                .cookie(a)
                .contentType(MediaType.APPLICATION_JSON)
                .content(txBody))
                .andExpect(status().isCreated())
                .andReturn();
        long txId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // B cannot see A's data
        mockMvc.perform(get("/api/transactions").cookie(b))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions.length()").value(0));

        // B cannot delete A's transaction
        mockMvc.perform(delete("/api/transactions/" + txId).cookie(b))
                .andExpect(status().isNotFound());
    }

    @Test
    void logoutWithoutLoginReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidReportPathReturns400() throws Exception {
        Cookie session = register("rep@example.com", "password123");
        mockMvc.perform(get("/api/reports/monthly/2024/13").cookie(session))
                .andExpect(status().isBadRequest());
    }
}
