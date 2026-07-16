package com.online.exam;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenUnauthenticated_thenRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void whenAdminAuthenticated_thenAccessAdminPage() throws Exception {
        mockMvc.perform(get("/admin.html"))
                .andExpect(status().isOk());
    }

    @Test
    void publicPagesAreAccessible() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/login.html"))
                .andExpect(status().isOk());
    }
}
