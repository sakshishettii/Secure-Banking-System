# Secure Banking System 

## Overview
This project implements an iterative secure banking system consisting of a bank server and multiple clients (ATMs). The system utilizes both public-key encryption (RSA) and symmetric-key encryption (AES/3DES) methods for security. Users can transfer money to other users and view their account balances securely.

## Features
* **Secure Authentication:** Users authenticate themselves using their IDs and passwords encrypted with RSA and AES/3DES.
* **Account Management:** Users can transfer money between savings and checking accounts and view their balances.
* **Error Handling:** The system handles incorrect user inputs and displays appropriate error messages.

## Files
1. **password:** Stores user IDs and passwords.
2. **balance:** Keeps track of savings and checking account balances for each user.

## Running the System

### Bank Server

* Invoke the bank server using the command: bank <Bank server’s port number>
* The server listens for connections from ATMs.

### ATM Client
* Invoke the ATM client using the command: atm <Bank server’s domain name> <Bank server’s port number>
* The ATM prompts the user to enter their ID and password.
* Upon successful authentication, the user can perform transactions or view balances.

## Usage
1. **Authentication:**
* The ATM encrypts the user's ID and password with the bank server's public key and sends them to the server.
* The server decrypts the received data using its private key and verifies the user's credentials.

2. **Transaction:**
* Users can transfer money between savings and checking accounts securely.
* The server validates the transaction and updates the account balances accordingly.

3. **Balance Inquiry:**

* Users can check their savings and checking account balances securely.
4. **Exiting:**
* Users can exit the system, closing the connection between the ATM and the server.

## Security Measures
* RSA Encryption: Used for secure transmission of sensitive data like user credentials.
* AES/3DES Encryption: Employed for symmetric-key encryption during communication between the ATM and the server.
* Password Security: User passwords are stored securely and transmitted encrypted to ensure confidentiality.

## Error Handling
* Invalid Inputs: The system handles incorrect user inputs gracefully, providing clear error messages.
* Insufficient Funds: Users are notified if their account balance is insufficient for a transaction.

## Contributors
Sakshi Shetty - Developer
