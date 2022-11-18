package trainingmanagement.TrainingManagement.controller;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.RestController;
import trainingmanagement.TrainingManagement.service.DefaultService;
import trainingmanagement.TrainingManagement.service.SuperAdminService;

import javax.annotation.PostConstruct;

@RestController
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
