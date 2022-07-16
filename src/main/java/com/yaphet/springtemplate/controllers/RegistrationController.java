package com.yaphet.springtemplate.controllers;

import com.yaphet.springtemplate.models.AppUser;
import com.yaphet.springtemplate.services.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RequiredArgsConstructor
@Controller
@RequestMapping("")
public class RegistrationController {

    private final RegistrationService appUserRegistrationService;

    @GetMapping
    public String index(){
        return "landing-page";
    }
    @GetMapping("/register")
    public String registrationForm(Model model){
        AppUser appUser = new AppUser();
        model.addAttribute("appUser", appUser);
        return "registration/register-user";
    }
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("appUser") AppUser appUser, BindingResult result){
        if(result.hasErrors()){
            return "registration/register-user";
        }
        appUserRegistrationService.register(appUser);
        return "redirect:/login";
    }
    @GetMapping("/confirm")
    public String confirm(@RequestParam("token") String token){
        appUserRegistrationService.confirmToken(token);
        return "registration/email-verified";
    }


}
