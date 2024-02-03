import java.io.*;
import java.net.Socket;
import javax.crypto.*;
import java.security.*;
import java.util.*;

public class ATM {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.exit(1);
        }

        String serverHost = args[0];
        int serverPort = Integer.parseInt(args[1]);
        if (serverPort < 1024 || serverPort > 65535) {
            System.err.println("•  Invalid port number. Port must be between 1024 and 65535.");
            System.exit(1);
        }
        
        String[] validHosts = {
            "remote01.cs.binghamton.edu",
            "remote02.cs.binghamton.edu",
            "remote03.cs.binghamton.edu",
            "remote04.cs.binghamton.edu",
            "remote05.cs.binghamton.edu",
            "remote06.cs.binghamton.edu",
            "remote07.cs.binghamton.edu"
        };

        if (!isValidServerHost(serverHost, validHosts)) {
            System.err.println("•  Invalid server host.");
            System.exit(1);
        }


        try (Socket socket = new Socket(serverHost, serverPort)) {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            PublicKey serverPublicKey = (PublicKey) inputStream.readObject();

            while (true) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

                System.out.print("•  Enter ID: ");
                String userID = userInput.readLine();
                System.out.print("•  Enter password: ");
                String password = userInput.readLine();
                System.out.println();

                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(256);
                SecretKey symmetricKey = keyGenerator.generateKey();
                
                String encryptedSymmetricKeyStr = encryptionSymmetricKey(symmetricKey, serverPublicKey);
                String encryptedIDPasswordStr = encryptionIDPassword(symmetricKey, userID + " " + password);
                String encryptedString = encryptedSymmetricKeyStr + " " + encryptedIDPasswordStr;
                out.println(encryptedString);

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String serverResponse = in.readLine();
                
                if (serverResponse.equals("0")) {
                    System.out.println("• The ID/password is incorrect");
                    System.out.println("    Please try again.\n");
                } else {
                    System.out.println("• Correct ID and password.\n");

                    while (true) {
                        displayMainMenu();
                        Scanner sc = new Scanner(System.in);
                        int option = sc.nextInt();

                        if (option == 1) {
                            while (true) {
                                System.out.println("• Please select an account (enter 1 or 2):");
                                System.out.println("    1. Saving\n    2. Checking\n");
                                String option1 = userInput.readLine();
                                if (option1.equals("1") || option1.equals("2")) {
                                    out.println(option);
                                    System.out.print("• Enter recipient's ID: ");
                                    String recipientID = userInput.readLine();
                                    System.out.print("• Enter the value to transfer: ");
                                    String amount = userInput.readLine();
                                    System.out.println();

                                    while (true) {
                                        if (!amount.matches("[0-9]+")) {
                                            System.out.print("• Please enter a valid amount:");
                                            amount = userInput.readLine();
                                            System.out.println();
                                        }
                                        else{
                                            break;
                                        }
                                    }
                                    
                                    String value = recipientID + " " + amount + " " + option1;
                                    out.println(value);
                                    String response = in.readLine();
                                    
                                    if (response.equals("true")) {
                                        System.out.println("    Your transaction is successfully completed!\n");
                                        break;
                                    } else if (response.equals("false")) {
                                        System.out.println("    Your account does not have enough funds.\n");
                                        break;
                                    } else {
                                        System.out.println("    " + response+"\n");
                                        break;
                                    }
                                } else {
                                    System.out.println("• Incorrect Input for account selection.\n");
                                }
                            }
                        } else if (option == 2) {
                            out.println(option);
                            String data = in.readLine();
                            if (data != null) {
                                String[] balance = data.split(" ");
                                if (balance.length >= 2) {
                                    System.out.println("• Your savings account balance: " + balance[0]);
                                    System.out.println("• Your checking account balance: " + balance[1] + "\n");
                                }
                            }
                        } else if (option == 3) {
                            System.out.println("      ATM IS CLOSED.\n");
                            System.exit(0);
                        } else {
                            System.out.println("• Incorrect input\n");
                        }
                    }
                }
            }
        }
    }

    private static String encryptionSymmetricKey(SecretKey symmetricKey, PublicKey serverPublicKey) throws Exception {
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
        byte[] encryptedSymmetricKey = rsaCipher.doFinal(symmetricKey.getEncoded());
        String encryptedSymmetricKeyStr = Base64.getEncoder().encodeToString(encryptedSymmetricKey);
        return encryptedSymmetricKeyStr;
    }

    private static String encryptionIDPassword(SecretKey symmetricKey, String userData) throws Exception {
        Cipher aesCipher;
        aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
        byte[] encryptedIDPassword = aesCipher.doFinal((userData).getBytes());
        String encryptedIDPasswordStr = Base64.getEncoder().encodeToString(encryptedIDPassword);
        return encryptedIDPasswordStr;
    }

    private static void displayMainMenu() {
        System.out.println("• Please select one of the following actions (enter 1, 2, or 3):");
        System.out.println("    1. Transfer money\n    2. Check account balance\n    3. Exit\n");
    }

    private static boolean isValidServerHost(String serverHost, String[] validHosts) {
        for (String validHost : validHosts) {
            if (serverHost.equalsIgnoreCase(validHost)) {
                return true;
            }
        }
        return false;
    }

}
