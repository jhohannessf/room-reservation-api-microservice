package br.com.jhohannesfreitas.user_ms.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

// Classe não necessária, a authenticação via google e github é feita direto pelo navegador

@RestController
@RequestMapping("/oauth2/authorization")
public class OAuth2Controller {

    @GetMapping("/google")
    public void loginGoogle(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/github")
    public void loginGithub(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/github");
    }
}
