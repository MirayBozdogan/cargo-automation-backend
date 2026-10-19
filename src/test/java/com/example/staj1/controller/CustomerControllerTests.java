package com.example.staj1.controller;

import com.example.staj1.Dto.CustomerResponse;
import com.example.staj1.service.UserService;
import com.example.staj1.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings({"unchecked","rawtypes"})
class CustomerControllerTests {
    UserService service = mock(UserService.class); MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new UserController(service)).setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }
    @Test void paginationParametersAreNotPassedAsSearchFields() throws Exception {
        when(service.searchCustomers(anyMap(),any())).thenReturn(Page.empty(PageRequest.of(1,10)));
        mvc.perform(get("/users").param("page","1").param("size","10").param("sort","id,desc").param("name","Deneme"))
                .andExpect(status().isOk());
        ArgumentCaptor<Map<String,String>> filters = ArgumentCaptor.forClass((Class)Map.class);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(service).searchCustomers(filters.capture(),page.capture());
        assertEquals(Map.of("name","Deneme"),filters.getValue()); assertEquals(1,page.getValue().getPageNumber());
    }
    @Test void customerPageDoesNotExposePasswordsOrIdentityNumbers() throws Exception {
        when(service.searchCustomers(anyMap(),any())).thenReturn(new PageImpl<>(List.of(new CustomerResponse(42,"Deneme","Musteri","customer@example.test",25,"05555554444",true)),PageRequest.of(0,10),1));
        mvc.perform(get("/users")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(42)).andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].tc").doesNotExist());
    }
    @Test void invalidFilterGetsControlledBadRequest() throws Exception {
        when(service.searchCustomers(anyMap(),any())).thenThrow(new IllegalArgumentException("Desteklenmeyen müşteri arama alanı."));
        mvc.perform(get("/users").param("password","x")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }
    @Test void selectedCustomerDetailDoesNotExposeSecrets() throws Exception {
        when(service.getCustomer(42)).thenReturn(new CustomerResponse(42,"Deneme","Musteri","customer@example.test",25,"05555554444",true));
        mvc.perform(get("/users/customers/42")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.tc").doesNotExist());
    }
}
