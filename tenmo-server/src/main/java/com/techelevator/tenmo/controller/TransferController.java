package com.techelevator.tenmo.controller;

import com.techelevator.tenmo.dao.AccountDao;
import com.techelevator.tenmo.dao.UserDao;
import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.model.Transfer;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@PreAuthorize("isAuthenticated()")
public class TransferController {
    private AccountDao accountDao;
    private UserDao userDao;
    public TransferController(AccountDao accountDao, UserDao userDao) {
        this.accountDao = accountDao;
        this.userDao = userDao;
    }

    @RequestMapping(path = "/balance/{id}", method = RequestMethod.GET)
    public BigDecimal getBalance(@PathVariable int id) {
        BigDecimal balance = accountDao.findBalanceByUserId(id);
        if(balance == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Balance not found");
        } else {
            return balance;
        }
    }

    @RequestMapping(path = "/accounts", method = RequestMethod.GET)
    public List<Account> listAccounts() {
        return accountDao.getAllAccounts();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(path = "/send", method = RequestMethod.POST)
    public String send(@RequestBody Transfer transfer) {
        accountDao.saveTransfer(transfer);
        accountDao.subtractBalance(transfer.getFromUser(), transfer.getTransferAmount());
        accountDao.addBalance(transfer.getToUser(), transfer.getTransferAmount());

        return "Success Sending Transfer";
    }

    private boolean checkValidTransaction(Transfer transfer) {
        if(transfer.getFromUser() == transfer.getToUser()) {
            System.out.println("Can't send to yourself");
            return false;
        }

        if(transfer.getTransferAmount().compareTo(accountDao.findBalanceByUserId(transfer.getFromUser())) == 1) {
            System.out.println("Insufficient funds");
            return false;
        }


        if(transfer.getTransferAmount().compareTo(BigDecimal.valueOf(0)) <= 0) {
            System.out.println("Invalid Transfer Amount");
            return false;
        }


        return true;
    }

}