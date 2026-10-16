package com.example.staj1.controller;

import com.example.staj1.Dto.StaffResponse;
import com.example.staj1.exception.GlobalExceptionHandler;
import com.example.staj1.service.StaffService;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StaffControllerTests {
    StaffService service = mock(StaffService.class);
    MockMvc mvc;
    String valid = """
        {"name":"Deneme","surname":"Personel","email":"staff@example.test","age":25,
         "tc":"12345678901","telNo":"05555554444","password":"TestOnlyPassword!2026"}
        """;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new StaffController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver()).build();
    }
    @Test void successfulCreationReturns201WithoutPassword() throws Exception {
        when(service.create(any())).thenReturn(new StaffResponse(42, "Deneme", "Personel", "staff@example.test", 25, "12345678901", "05555554444", "STAFF", true));
        mvc.perform(post("/staff").contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.active").value(true)).andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void missingFieldsAreRejectedBeforeService() throws Exception {
        mvc.perform(post("/staff").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(service);
    }
    @Test void invalidIdentityIsRejectedBeforeService() throws Exception {
        mvc.perform(post("/staff").contentType(MediaType.APPLICATION_JSON).content(valid.replace("12345678901", "abcdefghijk")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void activityRequiresBooleanValue() throws Exception {
        mvc.perform(patch("/staff/42/active").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void validActivityChangeReturnsUpdatedStaff() throws Exception {
        when(service.setActive(42, false)).thenReturn(new StaffResponse(42, "Deneme", "Personel", "staff@example.test", 25, "12345678901", "05555554444", "STAFF", false));
        mvc.perform(patch("/staff/42/active").contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }
    @Test void updateRequiresAllProfileFields() throws Exception {
        mvc.perform(put("/staff/42").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void listReturnsPageMetadataWithoutPasswords() throws Exception {
        var staff = new StaffResponse(42,"Deneme","Personel","staff@example.test",25,"12345678901","05555554444","STAFF",true);
        when(service.list(any())).thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(staff), org.springframework.data.domain.PageRequest.of(0,10),1));
        mvc.perform(get("/staff").param("page","0").param("size","10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].role").value("STAFF"))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
    }
    @Test void detailReturnsStaffWithoutPassword() throws Exception {
        when(service.getById(42)).thenReturn(new StaffResponse(42,"Deneme","Personel","staff@example.test",25,"12345678901","05555554444","STAFF",false));
        mvc.perform(get("/staff/42")).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
