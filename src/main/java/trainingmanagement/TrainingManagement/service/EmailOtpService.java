package trainingmanagement.TrainingManagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import trainingmanagement.TrainingManagement.request.OtpRequest;
import trainingmanagement.TrainingManagement.response.OtpResponse;
import trainingmanagement.TrainingManagement.response.PasswordResponse;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class EmailOtpService
{
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String username = "projectrobo456@gmail.com";
    private static final String password = "lzcuzcmvzybwfkhr";

    public boolean sendEmail(String empId,String emailId, String twoFaCode)throws Exception
    {
        String emailId1;
        try
        {
            emailId1 = jdbcTemplate.queryForObject("select email from employee where emp_id=? and delete_status=false", String.class,empId);
        }
        catch (DataAccessException e)
        {
           throw new Exception(e.getMessage());
        }
        if (!(emailId1.equalsIgnoreCase(emailId)))
        {
            throw new Exception("email id not valid");
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.auth","true");
        properties.put("mail.smtp.starttls.enable","true");
        properties.put("mail.smtp.host","smtp.gmail.com");
        properties.put("mail.smtp.port","587");

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(emailId));

        message.setSubject("Two Factor Authentication code from our Service");
        message.setText("Your Two Factor Authentication code is:"+twoFaCode);
        Transport.send(message);
        return true;

    }

    public void update2FAProperties(OtpRequest otpRequest, String twofacode)
    {
        jdbcTemplate.update("INSERT INTO EmployeeOtp(empId,2fa_code,2fa_expire_time) VALUES (?,?,?)", new Object[] {
                otpRequest.getEmpId(),twofacode, (System.currentTimeMillis()/1000) + 120
        });
    }

    public boolean checkCode(OtpResponse response)
    {
        return jdbcTemplate.queryForObject("select count(*) from EmployeeOtp WHERE 2fa_code=? and empId=?"
                + " and 2fa_expire_time >=?", new Object[] {response.getOtpCode(), response.getEmpId(),
                System.currentTimeMillis()/1000}, Integer.class) > 0;
    }

    //Change Password
    public int changePassword(PasswordResponse passwordResponse)
    {
        String query =  "UPDATE employee SET password = ? WHERE emp_id = ? and delete_status=false";
        return jdbcTemplate.update(query,getEncodedPassword(passwordResponse.getPassword()),passwordResponse.getEmpId());

    }

    public String getEncodedPassword(String password) {
        return passwordEncoder.encode(password);
    }
}
