package com.example.healthcheck.api.v1.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.example.healthcheck.security.BringCustomer;
import com.example.healthcheck.service.health.HealthCheckManager;

@WebMvcTest(HealthCheckController.class)
@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.health-check.com", uriPort = 443)
@ExtendWith(RestDocumentationExtension.class)
class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BringCustomer bringCustomer;

    @MockBean
    private HealthCheckManager healthCheckManager;

    @Test
    @DisplayName("[POST] [/api/v1/check/{serviceId}] 서버에 헬스 체크 테스트")
    void checkTest() throws Exception {
        willDoNothing().given(healthCheckManager).check(1L);

        mockMvc.perform(post("/api/v1/check/{serverId}", 1))
            .andExpect(status().isOk())
            .andExpect(handler().methodName("check"))
            .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
            .andDo(document("check",
                pathParameters(parameterWithName("serverId").description("서버 아이디")),
                responseFields(fieldWithPath("resultCode").description("상태 코드"))));
    }
}
