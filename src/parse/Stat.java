package parse;

import java.util.Objects;
import java.util.regex.Matcher;
import static parse.Parse.pattern;

/**
 *
 * @author Andrew
 */
public class Stat {
    private String date = "";
    private String time = "";
    private int dispatcher = 0;
    private String login = "";
    private String sessionID = "";
    private String kioskID = "";
    private String wuAgent = "";
    private String status = "";
    private String MTCN = "";
    private StringBuilder msg = new StringBuilder();
    boolean debug = false;
    
    public Stat() {}
    public Stat(String string) { 
        Matcher mather = pattern.matcher(string);
        if (mather.find( )) {
            String s = mather.group(5);
            date = s.substring(0, s.indexOf(" "));
            time= s.substring(s.indexOf(" "), s.lastIndexOf("."));
            
            s = mather.group(8);
            dispatcher = Integer.valueOf(s.substring(s.lastIndexOf("-")));
            
            s = mather.group(12);
            login = s.substring(s.indexOf("=") + 1, s.indexOf("&"));
            kioskID = s.substring(s.lastIndexOf("=") + 1);
            //msg.append(s).append("\n");
        } else {
            System.out.println("Wrong pattern");
        }
    }
    
    public void add(String s) {
        if (s.contains("kyc")) 
            wuAgent = s.substring(s.indexOf("(") + 1, s.indexOf(","));
        
        else if (s.contains("ASSIGN SESSIONID"))
            sessionID = s.substring(s.indexOf("="));
        
        else if (s.startsWith("Request:") || s.startsWith("Response:") || s.startsWith("Req/Res: Response for")
                || s.startsWith("fees") || s.startsWith("SESSIONID") || s.startsWith("net.fmb.ws.wu.exceptions")) {
        }
        
        else if (s.contains("User not found")) {
            status = "Login Error";
            if (debug) msg.append(s.substring(s.lastIndexOf(":") + 1));
        }
        else if (s.contains("User found")) {
            status = "Beneficiary Selection";
            if (debug) msg.append(s.substring(0, s.indexOf(":"))).append("\n");
        }
        else if (s.contains("calculate")) {
            status = "Fee Inquiry";
            if (debug) msg.append(s).append("\n");
        }
        else if (s.contains("Validate")) {
            status = "Confirmation";
            if (debug) msg.append(s).append("\n");
        }
        else if (s.contains("Store")) {
            status = "Payment";
            String temp = s.substring(s.indexOf("TransferCalculations"));
            temp = temp.substring(temp.indexOf("Country"));
            temp = temp.substring(temp.indexOf(")")).replaceAll("\\)", "").replaceAll(",", "").replaceAll("None", "");
            System.out.println("Temp3: " + temp);
            MTCN = temp;
            
            if (debug) msg.append(s).append("\n");
        }
        else if (debug) msg.append(s).append("\n");
    }
   
    
    @Override public String toString() {
        return "fmb;" + kioskID + ";" + wuAgent + ";" + login + ";" + date + " " + time + ";" + status + ";" + MTCN + ";"+  msg;
    }
@Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.login);
        hash = 53 * hash + Objects.hashCode(this.sessionID);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        final Stat other = (Stat) obj;
        if (!Objects.equals(this.login, other.login)) return false;
        
        return Objects.equals(this.sessionID, other.sessionID);
    }
}
