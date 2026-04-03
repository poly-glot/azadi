package com.azadi.web;

import com.azadi.auth.AuthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /login renders login page with demoMode attribute")
    void login_rendersLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("login"))
            .andExpect(model().attributeExists("demoMode"));
    }

    @Test
    @DisplayName("GET /login-error renders login page with error flag")
    void loginError_setsErrorAttribute() throws Exception {
        mockMvc.perform(get("/login-error"))
            .andExpect(status().isOk())
            .andExpect(view().name("login"))
            .andExpect(model().attribute("error", true))
            .andExpect(model().attributeExists("demoMode"));
    }

    @Test
    @DisplayName("Login page is accessible without authentication")
    void login_noAuthRequired() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }
}
