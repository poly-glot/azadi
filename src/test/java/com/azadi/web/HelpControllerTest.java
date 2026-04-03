package com.azadi.web;

import com.azadi.help.HelpController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HelpController.class)
@Import(TestSecurityConfig.class)
class HelpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /help/faqs returns faqs view")
    void faqs_rendersCorrectView() throws Exception {
        mockMvc.perform(get("/help/faqs"))
            .andExpect(status().isOk())
            .andExpect(view().name("help/faqs"));
    }

    @Test
    @DisplayName("GET /help/ways-to-pay returns ways-to-pay view")
    void waysToPay_rendersCorrectView() throws Exception {
        mockMvc.perform(get("/help/ways-to-pay"))
            .andExpect(status().isOk())
            .andExpect(view().name("help/ways-to-pay"));
    }

    @Test
    @DisplayName("GET /help/contact-us returns contact-us view")
    void contactUs_rendersCorrectView() throws Exception {
        mockMvc.perform(get("/help/contact-us"))
            .andExpect(status().isOk())
            .andExpect(view().name("help/contact-us"));
    }

    @Test
    @DisplayName("GET /cookies returns legal/cookies view")
    void cookies_rendersCorrectView() throws Exception {
        mockMvc.perform(get("/cookies"))
            .andExpect(status().isOk())
            .andExpect(view().name("legal/cookies"));
    }

    @Test
    @DisplayName("GET /privacy returns legal/privacy view")
    void privacy_rendersCorrectView() throws Exception {
        mockMvc.perform(get("/privacy"))
            .andExpect(status().isOk())
            .andExpect(view().name("legal/privacy"));
    }

    @Test
    @DisplayName("GET /terms returns legal/terms view")
    void terms_rendersCorrectView() throws Exception {
        mockMvc.perform(get("/terms"))
            .andExpect(status().isOk())
            .andExpect(view().name("legal/terms"));
    }
}
