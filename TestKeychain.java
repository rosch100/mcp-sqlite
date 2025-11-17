import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TestKeychain {
    public static void main(String[] args) {
        try {
            System.out.println("Testing security command...");
            Process process = new ProcessBuilder("/usr/bin/security", "--version").start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Output: " + line);
            }
            
            int exitCode = process.waitFor();
            System.out.println("Exit code: " + exitCode);
            System.out.println("Success: " + (exitCode == 0));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

