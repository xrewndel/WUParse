package parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author Andrew
 */
public class Parse {
    private static final String logName = "wu-service";
    private final static Logger csv = Logger.getLogger("csv");
    private static String path = "";
    private static final String strPattern = "(\\[(.*?)\\])( )(\\[(.*?)\\])( )(\\[(.*?)\\])( )(\\[(.*?)\\])(.*)(.*)";
    public static Pattern pattern = Pattern.compile(strPattern);
    public static Map<Integer, Stat> dispatchers = new TreeMap<>();
    public static Map<String, Stat> sessions = new TreeMap<>();
    
    public static void main(String[] args) {
        String e1 = "[INFO] [09/01/2014 06:16:47.151] [wu-ws-system-akka.actor.default-dispatcher-15] [akka://wu-ws-system/user/wu-ws-service] /user?login=5000003710&pin=4604&emirates_id=63&enroll_my_wu=false&kiosk_id=629";
        String e2 = "[INFO] [09/01/2014 06:16:47.440] [wu-ws-system-akka.actor.default-dispatcher-15] [$Proxy32(akka://wu-ws-system)] Request:";
        String e3 = "net.fmb.ws.wu.exceptions.package$NotFound: Invalid user credentials";
        String e4 = "[INFO] [09/08/2014 09:38:02.380] [wu-ws-system-akka.actor.default-dispatcher-852] [package$(akka://wu-ws-system)] fees1 (40000): Map(promoDiscountAmount -> 0, testQuestionAvailable -> P, principalAmount -> 40000, exchangeRate -> 27.4609307, charges -> 2500, conversionFee -> null, payAmount -> 1098437, grossTotalAmount -> 42500)";
        String e5 = "[INFO] [09/08/2014 09:38:02.790] [wu-ws-system-akka.actor.default-dispatcher-852] [$Proxy41(akka://wu-ws-system)] Request:";
        String e6 = "[WARN] [09/08/2014 09:37:09.585] [wu-ws-system-akka.actor.default-dispatcher-845] [akka://wu-ws-system/user/wu-ws-service] net.fmb.ws.wu.exceptions.package$NotFound: Invalid user credentials encountered while handling request: HttpRequest(GET,http://162.13.59.165:8080/wui/user?login=5000003977&pin=5239&emirates_id=59&enroll_my_wu=false&kiosk_id=469,List(Host: 162.13.59.165:8080, User-Agent: Jakarta Commons-HttpClient/3.1),Empty,HTTP/1.1)";
        String e7 = "[INFO] [09/01/2014 04:38:47.517] [wu-ws-system-akka.actor.default-dispatcher-2] [akka://wu-ws-system/user/IO-HTTP/listener-0] Bound to /0.0.0.0:8080";
        //String strPattern = "(\\[(.*?)\\])( )(\\[(.*?)\\])( )(\\[(.*?)\\])( )(\\[(.*?)\\])(.*)(.*)";
        //pattern = Pattern.compile(strPattern);
        
        Matcher matcher = pattern.matcher(e7);
        if (matcher.find( )) {
            //System.out.println("Count: " + matcher.groupCount());
            for (int i = 0; i < matcher.groupCount(); i++) {
                //System.out.println("Group " + i + ":" + matcher.group(i));
            }
        } else {
           //System.out.println("NO MATCH");
        }
        //System.exit(0);
        
        // settings for log
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        System.setProperty("current.date", dateFormat.format(new Date()));
        // set path
        if (args.length > 1) path = args[1];
        File cfgFile = new File(args[0]);
        PropertyConfigurator.configure(cfgFile.getAbsolutePath());
        
        File[] files = getFilesInDir(path);
        Set<File> flist = new TreeSet<>(Arrays.asList(files));
        for (File file : flist) {
            read(file.getAbsolutePath());
        }
        System.out.println("Total files: " + files.length);
        System.out.println("After all:");
        for (Stat stat : dispatchers.values()) {
            System.out.println(stat);
            csv.debug(stat);
        }
        
        for (Stat stat : sessions.values()) {
            System.out.println(stat);
            csv.debug(stat);
        }
    }
    
    private static File[] getFilesInDir(String path) {
        File[] files = new File(path).listFiles(new FilenameFilter() {
            @Override public boolean accept(File directory, String fileName) {
                return fileName.startsWith(logName);
            }
        });
        
        return files;
    }
    
    // Чтение файла
    private static void read(String filename) {
        System.out.println("Read " + filename);
        readTxtFile(filename);
    }
    
    // Если логин то обновляем статус. запоминаем диспатчер и далее работаем по нему.
    // Диспатчеры за день не повторяются
    // Ждем пока появится SessionID (ASSIGN SESSIONID). 
    // Запоминаем его и удаляем из диспатчеров тк скорее всего новый диспатчер будет дальше обрабатывать
    // Если появился SessionID без ASSIGN то ищем Stat и снова запоминаем диспатчер и дальше работем по нему
    
    private static void readTxtFile(String filename) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            while (in.ready()) {
                String s = in.readLine();
                Matcher matcher = pattern.matcher(s);
                if (matcher.find()) {
                    System.out.println("Parse:" + s);
                    int dispatcher = getDispather(s);
                    String msg = matcher.group(matcher.groupCount() - 1).trim();
                    String sid = getSessionID(s);
                    
                    if (msg.startsWith("/user?login")) {
                        Stat stat = new Stat(s);
                        dispatchers.put(dispatcher, stat);
                    }
                    else if (dispatchers.containsKey(dispatcher)) {
                        Stat stat = dispatchers.get(dispatcher);
                        stat.add(msg);
                        if (msg.contains("ASSIGN SESSIONID")) {
                            sessions.put(sid, stat);
                            dispatchers.remove(dispatcher);
                        }
                    }
                    else if (sessions.containsKey(sid)) {
                        Stat stat = sessions.get(sid);
                        stat.add(msg);
                        dispatchers.put(dispatcher, stat);
                        sessions.remove(sid);
                    }
                    else {
                        System.out.println(s);
                    }
                }
            }
            in.close();
        } catch (Exception ex) { System.out.println(ex);  }
    }
    
    private static int getDispather(String string) {
        Matcher m = pattern.matcher(string);
        m.matches();
        String s = m.group(8);
        return Integer.valueOf(s.substring(s.lastIndexOf("-")));
    }
    
    private static String getSessionID(String s) {
        if (s.contains("SESSIONID")) return s.substring(s.indexOf("="));
        return "";
    }
}
