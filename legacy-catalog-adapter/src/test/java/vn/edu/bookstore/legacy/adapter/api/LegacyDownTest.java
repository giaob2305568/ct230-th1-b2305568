package vn.edu.bookstore.legacy.adapter.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import vn.edu.bookstore.legacy.adapter.LegacySoapTestServer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class LegacyDownTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        int port = LegacySoapTestServer.freePort();
        r.add("bookstore.legacy.endpoint", () -> "http://localhost:" + port + "/ws/catalog");
    }

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        this.mvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void heThongKeThuaTatThanh503() throws Exception {
        mvc.perform(get("/api/v1/legacy/books/9780134685991"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("https://api.bookstore.vn/problems/upstream-unavailable"));
    }
}
