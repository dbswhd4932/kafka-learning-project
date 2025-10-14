package com.blog.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

//    @Test
//    @DisplayName("/posts 요청시 Hello World를 출력한다.")
//    void test01() throws Exception {
//
//        PostCreate postCreate = new PostCreate("Title", "Content");
//        String request = objectMapper.writeValueAsString(postCreate);
//
//        // expected
//        mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON_VALUE)
//                .content(request)
//            )
//            .andExpect(MockMvcResultMatchers.status().isOk())
//            .andExpect(MockMvcResultMatchers.content().string("Hello World"))
//            .andDo(print());
//    }

}
