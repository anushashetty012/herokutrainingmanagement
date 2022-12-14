package trainingmanagement.TrainingManagement.controller;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import trainingmanagement.TrainingManagement.service.DefaultService;
import trainingmanagement.TrainingManagement.service.SuperAdminService;

import javax.annotation.PostConstruct;

@RestController
@CrossOrigin(
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT, RequestMethod.PATCH},
        origins = {"http://localhost:4200","https://thriving-croissant-5df179.netlify.app"})
public class DefaultController
{

    @Autowired
    private DefaultService defaultService;

    @Autowired
    private SuperAdminService superAdminService;

    @PostConstruct
    public void initRoleAndEmployee()
    {
        defaultService.initRoleAndEmployee();
    }
}
