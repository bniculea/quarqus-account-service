package quarkus;

import java.math.BigDecimal;
import java.util.*;

import jakarta.annotation.PostConstruct;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import quarkus.domain.Account;
import quarkus.domain.AccountStatus;


@Path("/accounts")
public class AccountResource {
    Set<Account> accounts = new HashSet<>();

    @PostConstruct
    public void setup() {
        accounts.add(new Account(123456789L, 987654321L, "George Baird", new BigDecimal("354.23")));
        accounts.add(new Account(121212121L, 888777666L, "Mary Taylor", new BigDecimal("560.03")));
        accounts.add(new Account(545454545L, 222444999L, "Diana Rigg", new BigDecimal("422.00")));
    }
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Account> allAccounts() {
        return accounts;
    }

    @GET
    @Path("/{accountNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    public Account getAccount(@PathParam("accountNumber") Long accountNumber) {
        Optional<Account> response = accounts.stream()
                .filter(acct -> acct.getAccountNumber().equals(accountNumber))
                .findFirst();

        return response.orElseThrow(() -> new WebApplicationException("Account with id of " + accountNumber + " does not exist"));
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAccount(Account account) {
        if (account.getAccountNumber() == null) {
            throw new WebApplicationException("No account number specified", Response.Status.BAD_REQUEST);
        }
        accounts.add(account);
        return Response.status(Response.Status.CREATED)
                .entity(account)
                .build();
    }

    @PUT
    @Path("{accountNumber}/withdrawal")
    public Account withdrawal(@PathParam("accountNumber") Long accountNumber, String amount) {
        Account account = getAccount(accountNumber);
        account.withdrawFunds(new BigDecimal(amount));
        int balanceValue = account.balance.compareTo(BigDecimal.valueOf(0));
        if (balanceValue < 0 && account.accountStatus.equals(AccountStatus.OPEN)) {
            account.markOverdrawn();
        }
        return account;
    }

    @PUT
    @Path("{accountNumber}/deposit")
    public Account deposit(@PathParam("accountNumber") Long accountNumber, String amount) {
        Account account = getAccount(accountNumber);
        account.addFunds(new BigDecimal(amount));
        int balanceValue = account.balance.compareTo(BigDecimal.valueOf(0));
        if (balanceValue > 0 && account.accountStatus.equals(AccountStatus.OVERDRAWN)) {
            account.removeOverdrawnStatus();
        }
        return account;
    }


    @DELETE
    @Path("/{accountNumber}")
    public Response deleteAccount(@PathParam("accountNumber") Long accountNumber) {
        Optional<Account> account = accounts.stream()
                .filter(acct -> acct.getAccountNumber().equals(accountNumber))
                .findFirst();

        account.ifPresent(value -> accounts.remove(value));

        return Response.noContent().build();
    }

    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {

        @Override
        public Response toResponse(Exception exception) {
            int code = 500;
            if (exception instanceof WebApplicationException) {
                code = ((WebApplicationException) exception).getResponse().getStatus();
            }

            JsonObjectBuilder entityBuilder = Json.createObjectBuilder()
                    .add("exceptionType", exception.getClass().getName())
                    .add("code", code);

            if (exception.getMessage() != null) {
                entityBuilder.add("error", exception.getMessage());
            }

            return Response.status(code)
                    .entity(entityBuilder.build())
                    .build();
        }
    }
}
