package trainingmanagement.TrainingManagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trainingmanagement.TrainingManagement.customException.InvalidOtpException;
import trainingmanagement.TrainingManagement.request.OtpRequest;
import trainingmanagement.TrainingManagement.response.OtpResponse;
import trainingmanagement.TrainingManagement.response.PasswordResponse;
import trainingmanagement.TrainingManagement.service.EmailOtpService;
import java.util.Random;

@RestController
@CrossOrigin(
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT, RequestMethod.PATCH},
        origins = {"http://localhost:4200","https://thriving-croissant-5df179.netlify.app"})
public class TwoFactorAuthenticationController
{
    @Autowired
    private EmailOtpService emailService;

    //This will run when employee will hit forget password button
    //Send OTP through mail to change password
    @RequestMapping(value = "/employee/otpMail", method = RequestMethod.PUT)
    public ResponseEntity<String> send2faCodeinEmail(@RequestBody OtpRequest otpRequest) throws Exception
    {
        String twoFaCode = String.valueOf(new Random().nextInt(8999)+1000);
        try
        {
            emailService.sendEmail(otpRequest.getEmpId(),otpRequest.getEmailId(),twoFaCode);
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        emailService.update2FAProperties(otpRequest,twoFaCode);
        return ResponseEntity.status(HttpStatus.OK).body("Otp sent successfully");
    }

    //This is Entering OTP by employee
    @RequestMapping(value="/employee/checkCode", method=RequestMethod.PUT)
    public ResponseEntity<Object> verify(@RequestBody OtpResponse otpResponse)
    {
        boolean isValid;
        try
        {
            isValid = emailService.checkCode(otpResponse);
        }
        catch (InvalidOtpException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        if(isValid)
        {
            return ResponseEntity.status(HttpStatus.OK).body("OTP verified");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Otp expired");
    }

    //Change Password
    @PutMapping("/employee/changePassword")
    public ResponseEntity<?> changePassword( @RequestBody PasswordResponse passwordResponse)
    {
        int change = emailService.changePassword(passwordResponse);
        if(change == 0)
        {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body("Failed to change the password");
        }
        return ResponseEntity.status(HttpStatus.OK).body("Password changed");
    }
}
