package com.novabank.transfer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NovaBankApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePageShowsAccounts() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NovaBank Transfer Portal")))
                .andExpect(content().string(containsString("ACC1001")));
    }

    @Test
    void transferWithCorrectPinSucceeds() throws Exception {
        String body = """
                {"fromAccount":"ACC1003","toAccount":"ACC1002","amount":100.00,"pin":"1111","note":"test"}
                """;
        mockMvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference", startsWith("NB")));
    }

    @Test
    void transferWithWrongPinIsRejected() throws Exception {
        String body = """
                {"fromAccount":"ACC1001","toAccount":"ACC1002","amount":10.00,"pin":"0000"}
                """;
        mockMvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferToUnknownAccountIsRejected() throws Exception {
        String body = """
                {"fromAccount":"ACC1001","toAccount":"ACC9999","amount":10.00,"pin":"1234"}
                """;
        mockMvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historyReturnsTransfersForAccount() throws Exception {
        mockMvc.perform(get("/api/transfers/history").param("account", "ACC1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reference", startsWith("NB")));
    }

    @Test
    void templatesCanBeImported() throws Exception {
        String yaml = """
                templates:
                  - name: Pay rent
                    to: ACC1002
                  - name: Tuition fee
                    to: ACC1003
                """;
        mockMvc.perform(post("/api/templates/import").contentType(MediaType.TEXT_PLAIN).content(yaml))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2));
    }

    @Test
    void healthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
