package trainingmanagement.TrainingManagement.entity;

public class JwtResponse {

    private Employee employee;
    private String jwtToken;

    //private String jwtRefreshToken;

    //Constructor
    public JwtResponse(Employee employee, String jwtToken) {
        this.employee = employee;
        this.jwtToken = jwtToken;
        //this.jwtRefreshToken = jwtRefreshToken;
    }


    //Getter and Setter
    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

//    public String getJwtRefreshToken() {
//        return jwtRefreshToken;
//    }
//
//    public void setJwtRefreshToken(String jwtRefreshToken) {
//        this.jwtRefreshToken = jwtRefreshToken;
//    }
}
