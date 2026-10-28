package vn.edu.bookstore.legacy.adapter.api;

import org.junit.jupiter.api.AfterAll;
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

@SpringBootTest(properties = "bookstore.legacy.read-timeout=500ms")
class LegacyUnavailableTest {

    static final LegacySoapTestServer SLOW = LegacySoapTestServer.start(2000);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("bookstore.legacy.endpoint", SLOW::url);
    }

    @AfterAll
    static void stop() {
        SLOW.close();
    }

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        this.mvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void heThongKeThuaChamThanh504() throws Exception {
        mvc.perform(get("/api/v1/legacy/books/9780134685991"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.type").value("https://api.bookstore.vn/problems/upstream-unavailable"));
    }
}
