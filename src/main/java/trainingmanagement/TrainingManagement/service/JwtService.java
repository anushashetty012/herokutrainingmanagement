package trainingmanagement.TrainingManagement.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import trainingmanagement.TrainingManagement.dao.EmployeeDao;
import trainingmanagement.TrainingManagement.entity.Employee;
import trainingmanagement.TrainingManagement.entity.JwtRequest;
import trainingmanagement.TrainingManagement.entity.JwtResponse;
import trainingmanagement.TrainingManagement.util.JwtUtil;

import java.util.HashSet;
import java.util.Set;

@Service
public class JwtService implements UserDetailsService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmployeeDao employeeDao;

    @Autowired
    private AuthenticationManager authenticationManager;

    public JwtResponse createJwtToken(JwtRequest jwtRequest)throws Exception{
        String empId= jwtRequest.getEmpId();
        String password= jwtRequest.getPassword();
        authenticate(empId, password);

        UserDetails userDetails = loadUserByUsername(empId);
        String newGeneratedToken = jwtUtil.generateToken(userDetails);

        Employee employee = employeeDao.findById(empId).get();
        return new JwtResponse(employee, newGeneratedToken);

    }

    @Override
    public UserDetails loadUserByUsername(String empId) throws UsernameNotFoundException {
        Employee employee = employeeDao.findById(empId).get();

        if(employee != null){
            return new org.springframework.security.core.userdetails.User(
                 employee.getEmpId(),
                 employee.getPassword(),
                 getAuthority(employee)
            );
        }else{
            throw new UsernameNotFoundException("User not found with username "+empId);
        }
    }

    private Set getAuthority(Employee employee){
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        employee.getRoles().forEach(roles -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roles.getRoleName()));
                });
        return authorities;

    }

    private void authenticate(String empId, String password) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(empId, password));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }
}
