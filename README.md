# fints4k

fints4k is an implementation of the FinTS 3.0 online banking protocol used by most German banks.

It's fast, easy extendable and running on multiple platforms: JVM, Android, (iOS, JavaScript, Windows, MacOS, Linux).

## Features
- Retrieving account information, balances and turnovers (Kontoumsätze und -saldo).
- Transfer money and real-time transfers (SEPA Überweisungen und Echtzeitüberweisung).
- Supports TAN methods chipTAN manual, Flickercode, QrCode and Photo (Matrix code), pushTAN, smsTAN and appTAN.

However, this is quite a low level implementation and in most cases not what you want to use.  
In most cases you want to use a higher level abstraction like [FinTs4kBankingClient](https://git.dankito.net/codinux/BankingClient).

## Setup
Not uploaded to Maven Central yet, will do this the next few days!

Gradle:
```
repositories {
    mavenCentral()
    maven {
        setUrl("https://maven.dankito.net/api/packages/codinux/maven")
    }
}


dependencies {
    implementation("net.codinux.banking:fints4k:1.0.0-Alpha-13")
}
```

Maven:
```

// add Repository https://maven.dankito.net/api/packages/codinux/maven

<dependency>
   <groupId>net.dankito.banking</groupId>
   <artifactId>fints4k-jvm</artifactId>
   <version>1.0.0-Alpha-11</version>
</dependency>
```


## Usage

Quite outdated, have to update it. In most cases use [FinTs4kBankingClient](https://git.dankito.net/codinux/BankingClient).

See e.g. [JavaShowcase](fints4k/src/test/java/net/dankito/banking/fints/JavaShowcase.java) or [FinTsClientTest](fints4k/src/test/kotlin/net/dankito/banking/fints/FinTsClientTest.kt).

```java
    // Set your bank code (Bankleitzahl) here.
    // BankInfo contains e.g. a bank's FinTS server address, country code and BIC (needed for money transfer)
    List<BankInfo> foundBanks = new InMemoryBankFinder().findBankByNameBankCodeOrCity("<bank code, bank name or city>");

    if (foundBanks.isEmpty() == false) { // please also check if bank supports FinTS 3.0
        BankData bank = new BankDataMapper().mapFromBankInfo(foundBanks.get(0));

        // set your customer data (customerId = username you use to log in; pin = online banking pin / password)
        CustomerData customer = new CustomerData("<customer_id>", "<pin>");

        FinTsClientCallback callback = new SimpleFinTsClientCallback(); // see advanced showcase for configuring callback

        FinTsClient finTsClient = new FinTsClient(callback, new Java8Base64Service());

        AddAccountResponse addAccountResponse = finTsClient.addAccount(bank, customer);

        if (addAccountResponse.isSuccessful()) {
            System.out.println("Successfully added account for " + bank.getBankCode() + " " + customer.getCustomerId());

            if (addAccountResponse.getBookedTransactions().isEmpty() == false) {
                System.out.println("Account transactions of last 90 days:");
                showGetTransactionsResponse(addAccountResponse);
            }
        }
        else {
            System.out.println("Could not add account for " + bank.getBankCode() + " " + customer.getCustomerId() + ":");
            showResponseError(addAccountResponse);
        }

        // see advanced show case what else you can do with this library, e.g. retrieving all account transactions and transferring money
    }
```

## Logging

fints4k uses slf4j as logging facade.

So you can use any logger that supports slf4j, like Logback and log4j, to configure and get fints4k's log output.


## Sample applications

### WebApp

Directly requesting bank servers is forbidden in browsers due to CORS.

In order to use fints4k directly in browser you need a CORS proxy like the one from CorsProxy 
[Application.kt](SampleApplications/CorsProxy/src/main/kotlin/net/codinux/web/cors/Application.kt) or https://github.com/Rob--W/cors-anywhere.

Set CORS proxy's URL in WebApp [main.kt](SampleApplications/WebApp/src/main/kotlin/main.kt).

Start sample WebApp then with
```shell
 ./gradlew WebApp:run --continuous
```

## License

Not free for commercial applications. More details to follow or [contact](mailto:sales@codinux.net) us.